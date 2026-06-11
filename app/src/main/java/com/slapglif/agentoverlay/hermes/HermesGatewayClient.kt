package com.slapglif.agentoverlay.hermes

import com.slapglif.agentoverlay.model.AgentModel
import com.slapglif.agentoverlay.model.AgentThread
import com.slapglif.agentoverlay.model.ChatMessage
import com.slapglif.agentoverlay.model.ChatOptions
import com.slapglif.agentoverlay.model.GatewayChatResponse
import com.slapglif.agentoverlay.model.GatewayConnection
import com.slapglif.agentoverlay.model.PhoneToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class HermesGatewayClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    suspend fun capabilities(baseUrl: String, apiKey: String): GatewayConnection = withContext(Dispatchers.IO) {
        val base = normalizeBaseUrl(baseUrl)
        val modelsRequest = Request.Builder().url("$base/v1/models").bearer(apiKey).get().build()
        httpClient.newCall(modelsRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Hermes gateway rejected connection: HTTP ${response.code}")
        }
        // Probe optional surfaces instead of asserting them: jobs/runs/commands live on
        // the API server's /api side and may be disabled independently of /v1.
        val jobsAvailable = runCatching {
            httpClient.newCall(Request.Builder().url("$base/api/jobs").bearer(apiKey).get().build())
                .execute().use { it.isSuccessful }
        }.getOrDefault(false)
        val capabilities = buildSet {
            add("models"); add("chat_completions"); add("responses"); add("tool_calls")
            if (jobsAvailable) { add("jobs"); add("runs"); add("commands"); add("skills") }
        }
        GatewayConnection.Connected(base, capabilities)
    }

    suspend fun listModels(baseUrl: String, apiKey: String): List<AgentModel> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(normalizeBaseUrl(baseUrl) + "/v1/models")
            .bearer(apiKey)
            .get()
            .build()
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Models API unavailable: HTTP ${response.code}")
                parseModels(response.body?.string().orEmpty())
            }
        }.getOrElse { DEFAULT_MODELS }
    }

    suspend fun listAgentThreads(baseUrl: String, apiKey: String): List<AgentThread> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(normalizeBaseUrl(baseUrl) + "/api/jobs")
            .bearer(apiKey)
            .get()
            .build()
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Jobs API unavailable: HTTP ${response.code}")
                parseJobs(response.body?.string().orEmpty())
            }
        }.getOrElse {
            listOf(AgentThread("mobile-overlay", "Mobile overlay", AgentThread.Status.Idle))
        }
    }

    /**
     * Sends one user turn with full thread [history], then drives the OpenAI-style tool loop:
     * while the model answers with phone tool calls and a [toolExecutor] is provided, each call
     * is executed locally, its result is appended as a `tool` role message, and the conversation
     * is re-sent so the model can react to outcomes. Capped at [MAX_TOOL_ROUNDS].
     */
    suspend fun sendMessage(
        baseUrl: String,
        apiKey: String,
        threadId: String,
        message: String,
        options: ChatOptions,
        history: List<ChatMessage> = emptyList(),
        toolExecutor: (suspend (PhoneToolCall) -> String)? = null
    ): GatewayChatResponse = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", buildSystemInstruction(options, message)))
        history.takeLast(MAX_HISTORY_MESSAGES).forEach { entry ->
            when (entry) {
                is ChatMessage.User -> messages.put(JSONObject().put("role", "user").put("content", entry.text))
                is ChatMessage.Assistant -> messages.put(JSONObject().put("role", "assistant").put("content", entry.text))
                is ChatMessage.Tool -> messages.put(
                    JSONObject().put("role", "user").put("content", "[local ${entry.toolName ?: "tool"} result] ${entry.text}")
                )
                is ChatMessage.Reasoning -> Unit
            }
        }
        messages.put(JSONObject().put("role", "user").put("content", message))

        var response = postChat(baseUrl, apiKey, threadId, messages, options)
        var rounds = 0
        while (toolExecutor != null && response.phoneToolCalls.isNotEmpty() && rounds < MAX_TOOL_ROUNDS) {
            messages.put(assistantToolCallMessage(response))
            response.phoneToolCalls.forEach { call ->
                val result = toolExecutor(call)
                messages.put(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.id)
                        .put("content", result)
                )
            }
            rounds++
            response = postChat(baseUrl, apiKey, threadId, messages, options)
        }
        response
    }

    private fun postChat(
        baseUrl: String,
        apiKey: String,
        threadId: String,
        messages: JSONArray,
        options: ChatOptions
    ): GatewayChatResponse {
        val payload = JSONObject()
            .put("model", options.modelId.ifBlank { "hermes-agent" })
            .put("messages", messages)
            .put("stream", false)
            // session_id and tools_enabled are Hermes gateway extensions; a strict
            // OpenAI-compatible endpoint ignores them and relies on messages + tools.
            .put("session_id", threadId)
            .put("tools_enabled", options.toolCallsEnabled)
            .put("reasoning_effort", when (options.reasoningMode) {
                ChatOptions.ReasoningMode.Auto -> "auto"
                ChatOptions.ReasoningMode.Think -> "medium"
                ChatOptions.ReasoningMode.Deep -> "high"
            })
        if (options.toolCallsEnabled) payload.put("tools", phoneToolSchemas())
        val request = Request.Builder()
            .url(normalizeBaseUrl(baseUrl) + "/v1/chat/completions")
            .bearer(apiKey)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Chat failed: HTTP ${response.code} $body")
            parseChatResponse(body)
        }
    }

    private fun assistantToolCallMessage(response: GatewayChatResponse): JSONObject {
        val toolCalls = JSONArray()
        response.phoneToolCalls.forEach { call ->
            toolCalls.put(
                JSONObject()
                    .put("id", call.id)
                    .put("type", "function")
                    .put("function", JSONObject().put("name", call.name).put("arguments", call.argumentsJson.ifBlank { "{}" }))
            )
        }
        return JSONObject()
            .put("role", "assistant")
            .put("content", response.text)
            .put("tool_calls", toolCalls)
    }

    private fun parseJobs(body: String): List<AgentThread> {
        val trimmed = body.trim()
        val array = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.isBlank() -> JSONArray()
            else -> JSONObject(trimmed).optJSONArray("jobs") ?: JSONObject(trimmed).optJSONArray("data") ?: JSONArray()
        }
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("job_id", item.optString("id", "job-$index"))
            val title = item.optString("name", item.optString("prompt", id)).take(80)
            val status = when (item.optString("status", "idle").lowercase()) {
                "running", "working", "active" -> AgentThread.Status.Running
                "failed", "error" -> AgentThread.Status.Failed
                "completed", "done" -> AgentThread.Status.Completed
                else -> AgentThread.Status.Idle
            }
            AgentThread(id, title, status, messages = listOf(ChatMessage.Tool("Scheduled job surfaced by Hermes Jobs API")))
        }.ifEmpty { listOf(AgentThread("mobile-overlay", "Mobile overlay", AgentThread.Status.Idle)) }
    }

    private fun parseModels(body: String): List<AgentModel> {
        val trimmed = body.trim()
        if (trimmed.isBlank()) return DEFAULT_MODELS
        val root = JSONObject(trimmed)
        val array = root.optJSONArray("data") ?: root.optJSONArray("models") ?: JSONArray()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id", item.optString("model", "")).ifBlank { return@mapNotNull null }
            AgentModel(id, item.optString("name", id))
        }.ifEmpty { DEFAULT_MODELS }
    }

    private fun parseChatResponse(body: String): GatewayChatResponse {
        val json = JSONObject(body)
        val choices = json.optJSONArray("choices") ?: return GatewayChatResponse(body)
        val first = choices.optJSONObject(0) ?: return GatewayChatResponse(body)
        val message = first.optJSONObject("message")
        val text = message?.optString("content")?.takeIf { it.isNotBlank() }
            ?: first.optString("text", body)
        val calls = mutableListOf<PhoneToolCall>()
        val toolCalls = message?.optJSONArray("tool_calls") ?: JSONArray()
        for (index in 0 until toolCalls.length()) {
            val call = toolCalls.optJSONObject(index) ?: continue
            val fn = call.optJSONObject("function") ?: continue
            val name = fn.optString("name")
            if (!name.startsWith("phone.")) continue
            calls += PhoneToolCall(
                id = call.optString("id", "phone-tool-$index"),
                name = name,
                argumentsJson = fn.optString("arguments", "{}")
            )
        }
        return GatewayChatResponse(text, calls)
    }

    private fun phoneToolSchemas(): JSONArray = JSONArray().apply {
        put(phoneTool("phone.snapshot", "Return the current Android app/window, visible accessibility refs, and bounding boxes.", JSONObject()))
        put(phoneTool("phone.accessibility_tree", "Return visible text/contentDescription nodes with Playwright-style refs and bounds.", JSONObject().put("max_elements", JSONObject().put("type", "number"))))
        put(phoneTool("phone.vision_snapshot", "Return structured context for OCR/vision frame fusion.", JSONObject()))
        put(phoneTool("phone.ocr", "Return text-like visible refs; OCR can fall back to accessibility labels.", JSONObject()))
        put(phoneTool("phone.tap", "Tap a semantic ref such as p12, or explicit x/y coordinates.", JSONObject()
            .put("ref", JSONObject().put("type", "string"))
            .put("x", JSONObject().put("type", "number"))
            .put("y", JSONObject().put("type", "number"))))
        put(phoneTool("phone.type", "Set text into the focused or first editable Android field.", JSONObject().put("text", JSONObject().put("type", "string"))))
        put(phoneTool("phone.swipe", "Swipe by coordinates for scrolling or gestures.", JSONObject()
            .put("startX", JSONObject().put("type", "number"))
            .put("startY", JSONObject().put("type", "number"))
            .put("endX", JSONObject().put("type", "number"))
            .put("endY", JSONObject().put("type", "number"))
            .put("durationMs", JSONObject().put("type", "number"))))
        put(phoneTool("phone.back", "Press Android back.", JSONObject()))
        put(phoneTool("phone.home", "Press Android home.", JSONObject()))
        put(phoneTool("phone.recents", "Open Android recents.", JSONObject()))
    }

    private fun phoneTool(name: String, description: String, properties: JSONObject): JSONObject = JSONObject()
        .put("type", "function")
        .put("function", JSONObject()
            .put("name", name)
            .put("description", description)
            .put("parameters", JSONObject()
                .put("type", "object")
                .put("properties", properties)
                .put("additionalProperties", false)))

    private fun buildSystemInstruction(options: ChatOptions, message: String): String {
        val commandMode = message.trimStart().startsWith("/") && options.commandPassthroughEnabled
        return buildString {
            append("You are Hermes Agent serving the Android overlay chat. ")
            append("Expose concise reasoning status, execute tool calls when useful, and keep command output mobile-readable. ")
            append("Reasoning mode: ${options.reasoningMode.name}. ")
            append("Tool calls: ${if (options.toolCallsEnabled) "enabled" else "disabled"}. ")
            if (options.toolCallsEnabled) {
                append("Available phone automation tools use Playwright MCP-style refs and RustDesk-style local input: ")
                append("phone.snapshot, phone.accessibility_tree, phone.vision_snapshot, phone.ocr, phone.tap, phone.type, phone.swipe, phone.back, phone.home, phone.recents. ")
                append("Tool results are executed on-device and returned to you as tool messages; re-snapshot after actions that change the screen. ")
                append("Local slash commands /phone, /tap <ref|x y>, /type <text>, /swipe x1 y1 x2 y2, /back, /home, and /recents execute on-device through AccessibilityService. ")
                append("Prefer semantic refs and bounding boxes over blind coordinates; ask for confirmation before destructive actions. ")
            }
            if (commandMode) append("The user message is a raw Hermes slash command; pass it through exactly and execute the matching Hermes command/skill behavior when supported. ")
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val MAX_HISTORY_MESSAGES = 24
        private const val MAX_TOOL_ROUNDS = 4
        val DEFAULT_MODELS = listOf(
            AgentModel("hermes-agent", "Hermes Agent"),
            AgentModel("gpt-5.5", "GPT-5.5"),
            AgentModel("claude-sonnet-4.6", "Claude Sonnet"),
            AgentModel("gemini-3.1-flash-lite", "Gemini Flash Lite")
        )

        fun normalizeBaseUrl(baseUrl: String): String {
            val trimmed = baseUrl.trim().trimEnd('/')
            return if (trimmed.endsWith("/v1")) trimmed.removeSuffix("/v1") else trimmed
        }
    }
}

private fun Request.Builder.bearer(apiKey: String): Request.Builder = apply {
    if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
}

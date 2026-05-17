package com.slapglif.agentoverlay.hermes

import com.slapglif.agentoverlay.model.AgentModel
import com.slapglif.agentoverlay.model.AgentThread
import com.slapglif.agentoverlay.model.ChatMessage
import com.slapglif.agentoverlay.model.ChatOptions
import com.slapglif.agentoverlay.model.GatewayConnection
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
        val url = normalizeBaseUrl(baseUrl) + "/v1/models"
        val request = Request.Builder()
            .url(url)
            .bearer(apiKey)
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Hermes gateway rejected connection: HTTP ${response.code}")
            GatewayConnection.Connected(normalizeBaseUrl(baseUrl), setOf("chat_completions", "responses", "runs", "jobs", "models", "commands", "skills", "tool_calls"))
        }
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

    suspend fun sendMessage(baseUrl: String, apiKey: String, threadId: String, message: String, options: ChatOptions): String = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("model", options.modelId.ifBlank { "hermes-agent" })
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", buildSystemInstruction(options, message)))
                .put(JSONObject().put("role", "user").put("content", message)))
            .put("stream", false)
            .put("session_id", threadId)
            .put("tools_enabled", options.toolCallsEnabled)
            .put("reasoning_effort", when (options.reasoningMode) {
                ChatOptions.ReasoningMode.Auto -> "auto"
                ChatOptions.ReasoningMode.Think -> "medium"
                ChatOptions.ReasoningMode.Deep -> "high"
            })
        val request = Request.Builder()
            .url(normalizeBaseUrl(baseUrl) + "/v1/chat/completions")
            .bearer(apiKey)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Chat failed: HTTP ${response.code} $body")
            parseAssistantText(body)
        }
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

    private fun parseAssistantText(body: String): String {
        val json = JSONObject(body)
        val choices = json.optJSONArray("choices") ?: return body
        val first = choices.optJSONObject(0) ?: return body
        return first.optJSONObject("message")?.optString("content")
            ?: first.optString("text", body)
    }

    private fun buildSystemInstruction(options: ChatOptions, message: String): String {
        val commandMode = message.trimStart().startsWith("/") && options.commandPassthroughEnabled
        return buildString {
            append("You are Hermes Agent serving the Android overlay chat. ")
            append("Expose concise reasoning status, execute tool calls when useful, and keep command output mobile-readable. ")
            append("Reasoning mode: ${options.reasoningMode.name}. ")
            append("Tool calls: ${if (options.toolCallsEnabled) "enabled" else "disabled"}. ")
            if (commandMode) append("The user message is a raw Hermes slash command; pass it through exactly and execute the matching Hermes command/skill behavior when supported. ")
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val DEFAULT_MODELS = listOf(
            AgentModel("hermes-agent", "Hermes Agent"),
            AgentModel("gpt-5.5", "GPT-5.5"),
            AgentModel("claude-sonnet-4.6", "Claude Sonnet"),
            AgentModel("gemini-3.1-flash-lite", "Gemini Flash Lite")
        )

        fun normalizeBaseUrl(baseUrl: String): String = GatewayEndpointPolicy.requireNormalizedBaseUrl(baseUrl)
    }
}

private fun Request.Builder.bearer(apiKey: String): Request.Builder = apply {
    if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
}

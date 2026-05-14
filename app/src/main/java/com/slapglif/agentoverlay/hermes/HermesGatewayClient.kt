package com.slapglif.agentoverlay.hermes

import com.slapglif.agentoverlay.model.AgentThread
import com.slapglif.agentoverlay.model.ChatMessage
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
            GatewayConnection.Connected(normalizeBaseUrl(baseUrl), setOf("chat_completions", "responses", "runs", "jobs"))
        }
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

    suspend fun sendMessage(baseUrl: String, apiKey: String, threadId: String, message: String): String = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("model", "hermes-agent")
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", message)))
            .put("stream", false)
            .put("session_id", threadId)
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

    private fun parseAssistantText(body: String): String {
        val json = JSONObject(body)
        val choices = json.optJSONArray("choices") ?: return body
        val first = choices.optJSONObject(0) ?: return body
        return first.optJSONObject("message")?.optString("content")
            ?: first.optString("text", body)
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun normalizeBaseUrl(baseUrl: String): String {
            val trimmed = baseUrl.trim().trimEnd('/')
            return if (trimmed.endsWith("/v1")) trimmed.removeSuffix("/v1") else trimmed
        }
    }
}

private fun Request.Builder.bearer(apiKey: String): Request.Builder = apply {
    if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
}

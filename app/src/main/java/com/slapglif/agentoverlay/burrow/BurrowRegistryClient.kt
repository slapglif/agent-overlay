package com.slapglif.agentoverlay.burrow

import com.slapglif.agentoverlay.model.BurrowHost
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class BurrowRegistryClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    suspend fun discoverHosts(registryUrl: String): List<BurrowHost> = withContext(Dispatchers.IO) {
        val result = CompletableDeferred<Result<List<BurrowHost>>>()
        val reqId = "agent-overlay-${UUID.randomUUID()}"
        val request = Request.Builder().url(normalizeRegistryUrl(registryUrl)).build()
        var socket: WebSocket? = null
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONObject().put("type", "register").put("name", "agent-overlay-android").toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "registered" -> {
                            val initialPeers = parsePeers(json.optJSONArray("peers") ?: JSONArray())
                            webSocket.send(JSONObject().put("type", "peers").put("req_id", reqId).toString())
                            if (initialPeers.isNotEmpty() && !result.isCompleted) result.complete(Result.success(initialPeers))
                        }
                        "peers" -> if (!result.isCompleted) result.complete(Result.success(parsePeers(json.optJSONArray("peers") ?: JSONArray())))
                        "error" -> if (!result.isCompleted) result.complete(Result.failure(IOException(json.optString("message", text))))
                    }
                }.onFailure { if (!result.isCompleted) result.complete(Result.failure(it)) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!result.isCompleted) result.complete(Result.failure(t))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!result.isCompleted) result.complete(Result.success(emptyList()))
            }
        }
        socket = httpClient.newWebSocket(request, listener)
        try {
            kotlinx.coroutines.withTimeout(7000) { result.await() }.getOrThrow()
        } finally {
            socket?.close(1000, "agent-overlay discovery complete")
        }
    }

    private fun parsePeers(array: JSONArray): List<BurrowHost> = (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val caps = item.optJSONObject("capabilities")
        BurrowHost(
            id = item.optString("id", "peer-$index"),
            name = item.optString("name", "Burrow peer $index"),
            status = item.optString("status", caps?.optString("status", "unknown") ?: "unknown"),
            model = caps?.optString("model").orEmpty(),
            tools = caps?.optJSONArray("tools")?.toStringList().orEmpty(),
            skills = caps?.optJSONArray("skills")?.toStringList().orEmpty(),
            tags = caps?.optJSONArray("tags")?.toStringList().orEmpty(),
            task = item.optString("task", "")
        )
    }

    private fun JSONArray.toStringList(): List<String> = (0 until length()).mapNotNull { optString(it).takeIf { value -> value.isNotBlank() } }

    companion object {
        fun normalizeRegistryUrl(value: String): String {
            val trimmed = value.trim().ifBlank { "wss://reg.ai-smith.net" }
            return when {
                trimmed.startsWith("ws://") || trimmed.startsWith("wss://") -> trimmed
                trimmed.startsWith("https://") -> "wss://" + trimmed.removePrefix("https://")
                trimmed.startsWith("http://") -> "ws://" + trimmed.removePrefix("http://")
                else -> "wss://$trimmed"
            }.trimEnd('/')
        }
    }
}

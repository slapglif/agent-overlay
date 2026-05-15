package com.slapglif.agentoverlay.hermes

import com.slapglif.agentoverlay.model.ChatOptions
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HermesGatewayClientTest {
    @Test fun normalizeBaseUrlRemovesTrailingV1() {
        assertEquals("http://127.0.0.1:8642", HermesGatewayClient.normalizeBaseUrl("http://127.0.0.1:8642/v1"))
    }

    @Test fun normalizeBaseUrlTrimsTrailingSlash() {
        assertEquals("http://10.0.2.2:8642", HermesGatewayClient.normalizeBaseUrl(" http://10.0.2.2:8642/ "))
    }

    @Test fun sendMessageIncludesHermesOptionsAndPhoneToolContract() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""
            {"choices":[{"message":{"content":"ack"}}]}
        """.trimIndent()).setHeader("Content-Type", "application/json"))
        server.start()
        try {
            val client = HermesGatewayClient()
            val result = client.sendMessage(
                baseUrl = server.url("/").toString(),
                apiKey = "secret",
                threadId = "thread-1",
                message = "/phone inspect",
                options = ChatOptions(
                    modelId = "hermes-agent",
                    reasoningMode = ChatOptions.ReasoningMode.Deep,
                    toolCallsEnabled = true,
                    commandPassthroughEnabled = true
                )
            )

            assertEquals("ack", result.text)
            val request = server.takeRequest()
            assertEquals("Bearer secret", request.getHeader("Authorization"))
            assertEquals("/v1/chat/completions", request.path)
            val body = request.body.readUtf8()
            assertTrue(body.contains("\"session_id\":\"thread-1\""))
            assertTrue(body.contains("\"reasoning_effort\":\"high\""))
            assertTrue(body.contains("phone.snapshot"))
            assertTrue(body.contains("\"tools\""))
            assertTrue(body.contains("raw Hermes slash command"))
        } finally {
            server.shutdown()
        }
    }

    @Test fun sendMessageParsesPhoneToolCalls() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""
            {"choices":[{"message":{"content":"I'll inspect the phone.","tool_calls":[{"id":"call-1","type":"function","function":{"name":"phone.snapshot","arguments":"{}"}}]}}]}
        """.trimIndent()).setHeader("Content-Type", "application/json"))
        server.start()
        try {
            val result = HermesGatewayClient().sendMessage(
                baseUrl = server.url("/").toString(),
                apiKey = "",
                threadId = "thread-1",
                message = "inspect my phone",
                options = ChatOptions(toolCallsEnabled = true)
            )
            assertEquals("I'll inspect the phone.", result.text)
            assertEquals(1, result.phoneToolCalls.size)
            assertEquals("phone.snapshot", result.phoneToolCalls.single().name)
        } finally {
            server.shutdown()
        }
    }

    @Test fun listModelsParsesOpenAiStyleData() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""
            {"object":"list","data":[{"id":"model-a","name":"Model A"}]}
        """.trimIndent()).setHeader("Content-Type", "application/json"))
        server.start()
        try {
            val models = HermesGatewayClient().listModels(server.url("/").toString(), "")
            assertEquals("model-a", models.single().id)
            assertEquals("Model A", models.single().label)
        } finally {
            server.shutdown()
        }
    }
}

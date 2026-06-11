package com.slapglif.agentoverlay.hermes

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayDiscoveryTest {
    // Port 1 is a privileged port with nothing listening, so the implicit
    // 10.0.2.2/127.0.0.1 candidates fail fast and never hit the mock server.
    // subnetOverride = "" skips the /24 sweep so only extraCandidates are probed.
    private val deadPort = 1

    @Test fun discoverReturnsGatewayForOkResponse() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""
            {"object":"list","data":[]}
        """.trimIndent()).setHeader("Content-Type", "application/json"))
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            val results = GatewayDiscovery().discover(
                port = deadPort,
                extraCandidates = listOf(base),
                subnetOverride = ""
            )

            assertEquals(1, results.size)
            assertEquals(base, results.single().baseUrl)
            assertFalse(results.single().requiresAuth)
            assertTrue(results.single().latencyMs >= 0)
            val request = server.takeRequest()
            assertEquals("/v1/models", request.path)
            assertNull(request.getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }

    @Test fun discoverMarksGatewayAsRequiringAuthOn401() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            val results = GatewayDiscovery().discover(
                port = deadPort,
                extraCandidates = listOf(base),
                subnetOverride = ""
            )

            assertEquals(1, results.size)
            assertEquals(base, results.single().baseUrl)
            assertTrue(results.single().requiresAuth)
        } finally {
            server.shutdown()
        }
    }

    @Test fun discoverIgnoresHostsWithNonGatewayResponses() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(500))
        server.start()
        try {
            val results = GatewayDiscovery().discover(
                port = deadPort,
                extraCandidates = listOf(server.url("/").toString().trimEnd('/')),
                subnetOverride = ""
            )

            assertTrue(results.isEmpty())
        } finally {
            server.shutdown()
        }
    }

    @Test fun discoverDeduplicatesCandidatesByBaseUrl() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""
            {"object":"list","data":[]}
        """.trimIndent()).setHeader("Content-Type", "application/json"))
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            val results = GatewayDiscovery().discover(
                port = deadPort,
                extraCandidates = listOf(base, "$base/"),
                subnetOverride = ""
            )

            assertEquals(1, results.size)
            assertEquals(base, results.single().baseUrl)
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }
}

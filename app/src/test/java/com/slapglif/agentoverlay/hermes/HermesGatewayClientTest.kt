package com.slapglif.agentoverlay.hermes

import org.junit.Assert.assertEquals
import org.junit.Test

class HermesGatewayClientTest {
    @Test fun normalizeBaseUrlRemovesTrailingV1() {
        assertEquals("http://127.0.0.1:8642", HermesGatewayClient.normalizeBaseUrl("http://127.0.0.1:8642/v1"))
    }

    @Test fun normalizeBaseUrlTrimsTrailingSlash() {
        assertEquals("http://10.0.2.2:8642", HermesGatewayClient.normalizeBaseUrl(" http://10.0.2.2:8642/ "))
    }

    @Test(expected = IllegalArgumentException::class) fun normalizeBaseUrlRejectsRemoteCleartext() {
        HermesGatewayClient.normalizeBaseUrl("http://hermes-mobile.example.com:8642")
    }
}

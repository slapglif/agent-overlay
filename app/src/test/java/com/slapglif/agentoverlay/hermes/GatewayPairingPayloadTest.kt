package com.slapglif.agentoverlay.hermes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GatewayPairingPayloadTest {
    @Test fun parsesAgentOverlayPairingDeepLink() {
        val payload = GatewayPairingPayload.parse(
            "agent-overlay://pair?url=https%3A%2F%2Fhermes-mobile.example.com%2Fv1&token=pairing-token-placeholder"
        )

        requireNotNull(payload)
        assertEquals("https://hermes-mobile.example.com", payload.gatewayUrl)
        assertEquals("pairing-token-placeholder", payload.apiKey)
        assertEquals(GatewayEndpointPolicy.Mode.RemoteTunnel, payload.mode)
    }

    @Test fun rejectsPairingDeepLinkWithRemoteCleartextUrl() {
        val payload = GatewayPairingPayload.parse(
            "agent-overlay://pair?url=http%3A%2F%2Fhermes-mobile.example.com%3A8642&token=pairing-token-placeholder"
        )

        assertNull(payload)
    }

    @Test fun rejectsPairingDeepLinkWithoutToken() {
        val payload = GatewayPairingPayload.parse(
            "agent-overlay://pair?url=https%3A%2F%2Fhermes-mobile.example.com"
        )

        assertNull(payload)
    }
}

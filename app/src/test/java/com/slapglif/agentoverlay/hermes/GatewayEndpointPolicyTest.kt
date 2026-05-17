package com.slapglif.agentoverlay.hermes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayEndpointPolicyTest {
    @Test fun allowsLocalCleartextDevelopmentEndpoints() {
        val validation = GatewayEndpointPolicy.validate(" http://10.0.2.2:8642/v1/ ")

        assertTrue(validation.allowed)
        assertEquals(GatewayEndpointPolicy.Mode.LocalDevelopment, validation.mode)
        assertEquals("http://10.0.2.2:8642", validation.normalizedBaseUrl)
    }

    @Test fun allowsHttpsCloudflareTunnelEndpoints() {
        val validation = GatewayEndpointPolicy.validate("https://hermes-mobile.example.com/v1")

        assertTrue(validation.allowed)
        assertEquals(GatewayEndpointPolicy.Mode.RemoteTunnel, validation.mode)
        assertEquals("https://hermes-mobile.example.com", validation.normalizedBaseUrl)
    }

    @Test fun rejectsRemoteCleartextEndpoints() {
        val validation = GatewayEndpointPolicy.validate("http://hermes-mobile.example.com:8642")

        assertFalse(validation.allowed)
        assertEquals(GatewayEndpointPolicy.Mode.Blocked, validation.mode)
    }

    @Test fun rejectsUnsupportedEndpointSchemes() {
        val validation = GatewayEndpointPolicy.validate("ftp://hermes-mobile.example.com")

        assertFalse(validation.allowed)
        assertEquals(GatewayEndpointPolicy.Mode.Blocked, validation.mode)
    }
}

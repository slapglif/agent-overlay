package com.slapglif.agentoverlay.hermes

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class GatewayPairingPayload(
    val gatewayUrl: String,
    val apiKey: String,
    val mode: GatewayEndpointPolicy.Mode
) {
    companion object {
        fun parse(uriText: String?): GatewayPairingPayload? {
            val uri = runCatching { URI(uriText?.trim().orEmpty()) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase() ?: return null
            if (scheme !in setOf("agent-overlay", "slapglif-agent-overlay")) return null
            val action = uri.host ?: uri.path.trimStart('/')
            if (action != "pair") return null

            val params = runCatching { parseQuery(uri.rawQuery ?: return null) }.getOrNull() ?: return null
            val requestedUrl = params["url"].orEmpty()
            val token = params["token"].orEmpty().trim()
            if (token.isBlank()) return null

            val validation = GatewayEndpointPolicy.validate(requestedUrl)
            if (!validation.allowed) return null
            return GatewayPairingPayload(
                gatewayUrl = validation.normalizedBaseUrl,
                apiKey = token,
                mode = validation.mode
            )
        }

        private fun parseQuery(query: String): Map<String, String> = query
            .split('&')
            .mapNotNull { part ->
                val keyValue = part.split('=', limit = 2)
                if (keyValue.isEmpty()) return@mapNotNull null
                val key = decode(keyValue[0])
                val value = decode(keyValue.getOrElse(1) { "" })
                key to value
            }
            .toMap()

        private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }
}

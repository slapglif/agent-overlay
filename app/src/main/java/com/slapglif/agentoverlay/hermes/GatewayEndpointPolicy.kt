package com.slapglif.agentoverlay.hermes

import java.net.URI

object GatewayEndpointPolicy {
    enum class Mode { LocalDevelopment, RemoteTunnel, Blocked }

    data class Validation(
        val rawValue: String,
        val normalizedBaseUrl: String,
        val mode: Mode,
        val allowed: Boolean,
        val reason: String
    )

    private val localCleartextHosts = setOf(
        "10.0.2.2",
        "127.0.0.1",
        "localhost",
        "0.0.0.0",
        "::1"
    )

    fun validate(baseUrl: String): Validation {
        val raw = baseUrl.trim()
        if (raw.isBlank()) {
            return blocked(baseUrl, "Gateway URL is required")
        }

        val normalized = normalizePath(raw)
        val uri = runCatching { URI(normalized) }.getOrElse {
            return blocked(baseUrl, "Gateway URL is not a valid URI")
        }
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()?.removeSurrounding("[", "]")

        if (scheme !in setOf("http", "https")) {
            return blocked(baseUrl, "Gateway URL must use http for local development or https for remote tunnels")
        }
        if (host.isNullOrBlank()) {
            return blocked(baseUrl, "Gateway URL must include a host")
        }
        if (!uri.query.isNullOrBlank() || !uri.fragment.isNullOrBlank()) {
            return blocked(baseUrl, "Gateway URL must not include query parameters or fragments")
        }
        if (scheme == "https") {
            return Validation(baseUrl, normalized, Mode.RemoteTunnel, allowed = true, reason = "HTTPS remote tunnel endpoint")
        }
        if (host in localCleartextHosts) {
            return Validation(baseUrl, normalized, Mode.LocalDevelopment, allowed = true, reason = "Local development cleartext endpoint")
        }
        return blocked(baseUrl, "Remote Hermes gateways must use HTTPS/WSS via a trusted tunnel or reverse proxy")
    }

    fun requireNormalizedBaseUrl(baseUrl: String): String {
        val validation = validate(baseUrl)
        require(validation.allowed) { validation.reason }
        return validation.normalizedBaseUrl
    }

    private fun normalizePath(value: String): String {
        val trimmed = value.trim().trimEnd('/')
        return if (trimmed.endsWith("/v1")) trimmed.removeSuffix("/v1") else trimmed
    }

    private fun blocked(rawValue: String, reason: String): Validation = Validation(
        rawValue = rawValue,
        normalizedBaseUrl = rawValue.trim().trimEnd('/'),
        mode = Mode.Blocked,
        allowed = false,
        reason = reason
    )
}

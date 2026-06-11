package com.slapglif.agentoverlay.hermes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

data class DiscoveredGateway(val baseUrl: String, val requiresAuth: Boolean, val latencyMs: Long)

/**
 * Auto-discovers Hermes Agent gateways on the local network.
 *
 * Contract:
 * - A gateway is any host answering `GET <base>/v1/models` (no auth header sent) with
 *   HTTP 200 (`requiresAuth = false`) or HTTP 401/403 (`requiresAuth = true`). Connection
 *   failures, timeouts, and any other status code mean the candidate is absent.
 * - Candidates are probed in this order: [discover]'s `extraCandidates` (normalized, no
 *   trailing slash), then the well-known local hosts `http://10.0.2.2:<port>` (Android
 *   emulator host loopback) and `http://127.0.0.1:<port>`, then every host `1..254` of each
 *   local /24 IPv4 subnet. Subnets are derived from [NetworkInterface.getNetworkInterfaces]
 *   (loopback/down interfaces skipped, site-local IPv4 only, the device's own address
 *   excluded) so the sweep works in a plain JVM without Android APIs.
 * - `subnetOverride` replaces the interface-derived prefixes (e.g. `"192.168.1"`). Passing
 *   an empty/blank string skips the subnet sweep entirely so callers (and unit tests) can
 *   probe only the explicit candidates; `null` means "derive from device interfaces".
 * - Probes run with bounded parallelism ([MAX_PARALLEL_PROBES] simultaneous requests) and
 *   short per-call timeouts so a full /24 sweep completes in a few seconds. Cancelling the
 *   calling coroutine cancels all outstanding probes.
 * - Results are de-duplicated by base URL and sorted with extraCandidate/localhost hits
 *   first, then ascending latency.
 */
class GatewayDiscovery(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(600, TimeUnit.MILLISECONDS)
        .callTimeout(1500, TimeUnit.MILLISECONDS)
        .build()
) {
    suspend fun discover(
        port: Int = DEFAULT_PORT,
        extraCandidates: List<String> = emptyList(),
        subnetOverride: String? = null
    ): List<DiscoveredGateway> = coroutineScope {
        val priorityCandidates = LinkedHashSet<String>()
        extraCandidates.mapNotNull { it.trim().trimEnd('/').ifBlank { null } }.forEach { priorityCandidates += it }
        priorityCandidates += "http://10.0.2.2:$port"
        priorityCandidates += "http://127.0.0.1:$port"

        val candidates = LinkedHashSet(priorityCandidates)
        val (prefixes, ownAddresses) = when {
            subnetOverride == null -> localSubnetPrefixes()
            subnetOverride.isBlank() -> emptyList<String>() to emptySet<String>()
            else -> listOf(subnetOverride.trim().trimEnd('.')) to emptySet<String>()
        }
        for (prefix in prefixes) {
            for (host in 1..254) {
                val address = "$prefix.$host"
                if (address in ownAddresses) continue
                candidates += "http://$address:$port"
            }
        }

        val semaphore = Semaphore(MAX_PARALLEL_PROBES)
        val hits = candidates.map { baseUrl ->
            async(Dispatchers.IO) { semaphore.withPermit { probe(baseUrl) } }
        }.awaitAll().filterNotNull()

        hits.sortedWith(
            compareBy<DiscoveredGateway> { if (it.baseUrl in priorityCandidates) 0 else 1 }
                .thenBy { it.latencyMs }
        )
    }

    private fun probe(baseUrl: String): DiscoveredGateway? {
        val startedAt = System.nanoTime()
        return runCatching {
            val request = Request.Builder()
                .url("$baseUrl/v1/models")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                val latencyMs = (System.nanoTime() - startedAt) / 1_000_000
                when (response.code) {
                    200 -> DiscoveredGateway(baseUrl, requiresAuth = false, latencyMs = latencyMs)
                    401, 403 -> DiscoveredGateway(baseUrl, requiresAuth = true, latencyMs = latencyMs)
                    else -> null
                }
            }
        }.getOrNull()
    }

    private fun localSubnetPrefixes(): Pair<List<String>, Set<String>> {
        val prefixes = LinkedHashSet<String>()
        val ownAddresses = HashSet<String>()
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching
            for (iface in interfaces) {
                runCatching {
                    if (!iface.isUp || iface.isLoopback) return@runCatching
                    for (address in iface.inetAddresses) {
                        if (address !is Inet4Address || address.isLoopbackAddress || !address.isSiteLocalAddress) continue
                        val host = address.hostAddress ?: continue
                        if (!host.contains('.')) continue
                        ownAddresses += host
                        prefixes += host.substringBeforeLast('.')
                    }
                }
            }
        }
        return prefixes.toList() to ownAddresses
    }

    companion object {
        const val DEFAULT_PORT = 8642
        private const val MAX_PARALLEL_PROBES = 64
    }
}

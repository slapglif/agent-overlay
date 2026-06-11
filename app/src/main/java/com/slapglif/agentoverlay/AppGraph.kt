package com.slapglif.agentoverlay

import android.content.Context
import com.slapglif.agentoverlay.data.AgentOverlayRepository
import com.slapglif.agentoverlay.data.AppPreferences
import com.slapglif.agentoverlay.hermes.GatewayDiscovery
import com.slapglif.agentoverlay.hermes.HermesGatewayClient

/**
 * Process-wide singletons shared by the activity, view model, and overlay service so
 * preferences, the HTTP connection pool, and gateway state have one source of truth.
 */
object AppGraph {
    @Volatile private var repositoryInstance: AgentOverlayRepository? = null
    @Volatile private var discoveryInstance: GatewayDiscovery? = null

    fun repository(context: Context): AgentOverlayRepository =
        repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: AgentOverlayRepository(
                AppPreferences(context.applicationContext),
                HermesGatewayClient()
            ).also { repositoryInstance = it }
        }

    fun gatewayDiscovery(): GatewayDiscovery =
        discoveryInstance ?: synchronized(this) {
            discoveryInstance ?: GatewayDiscovery().also { discoveryInstance = it }
        }
}

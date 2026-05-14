package com.slapglif.agentoverlay.data

import com.slapglif.agentoverlay.hermes.HermesGatewayClient
import com.slapglif.agentoverlay.model.AgentThread
import com.slapglif.agentoverlay.model.GatewayConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AgentOverlayRepository(
    private val preferencesStore: AppPreferences,
    private val client: HermesGatewayClient
) {
    val preferences: Flow<StoredPreferences> = preferencesStore.preferences

    suspend fun saveGatewayUrl(value: String) = preferencesStore.saveGatewayUrl(value)
    suspend fun saveApiKey(value: String) = preferencesStore.saveApiKey(value)

    suspend fun checkConnection(): GatewayConnection {
        val prefs = preferences.first()
        return client.capabilities(prefs.gatewayUrl, prefs.apiKey)
    }

    suspend fun loadThreads(): List<AgentThread> {
        val prefs = preferences.first()
        return client.listAgentThreads(prefs.gatewayUrl, prefs.apiKey)
    }

    suspend fun sendMessage(threadId: String, message: String): String {
        val prefs = preferences.first()
        return client.sendMessage(prefs.gatewayUrl, prefs.apiKey, threadId, message)
    }
}

package com.slapglif.agentoverlay.data

import com.slapglif.agentoverlay.burrow.BurrowRegistryClient
import com.slapglif.agentoverlay.hermes.HermesGatewayClient
import com.slapglif.agentoverlay.model.AgentModel
import com.slapglif.agentoverlay.model.AgentThread
import com.slapglif.agentoverlay.model.BurrowHost
import com.slapglif.agentoverlay.model.ChatOptions
import com.slapglif.agentoverlay.model.GatewayChatResponse
import com.slapglif.agentoverlay.model.GatewayConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AgentOverlayRepository(
    private val preferencesStore: AppPreferences,
    private val client: HermesGatewayClient,
    private val burrowClient: BurrowRegistryClient = BurrowRegistryClient()
) {
    val preferences: Flow<StoredPreferences> = preferencesStore.preferences

    suspend fun saveGatewayUrl(value: String) = preferencesStore.saveGatewayUrl(value)
    suspend fun saveApiKey(value: String) = preferencesStore.saveApiKey(value)
    suspend fun saveBurrowRegistryUrl(value: String) = preferencesStore.saveBurrowRegistryUrl(value)

    suspend fun checkConnection(): GatewayConnection {
        val prefs = preferences.first()
        return client.capabilities(prefs.gatewayUrl, prefs.apiKey)
    }

    suspend fun loadThreads(): List<AgentThread> {
        val prefs = preferences.first()
        return client.listAgentThreads(prefs.gatewayUrl, prefs.apiKey)
    }

    suspend fun loadModels(): List<AgentModel> {
        val prefs = preferences.first()
        return client.listModels(prefs.gatewayUrl, prefs.apiKey)
    }

    suspend fun sendMessage(threadId: String, message: String, options: ChatOptions): GatewayChatResponse {
        val prefs = preferences.first()
        return client.sendMessage(prefs.gatewayUrl, prefs.apiKey, threadId, message, options)
    }

    suspend fun discoverBurrowHosts(): List<BurrowHost> {
        val prefs = preferences.first()
        return burrowClient.discoverHosts(prefs.burrowRegistryUrl)
    }
}

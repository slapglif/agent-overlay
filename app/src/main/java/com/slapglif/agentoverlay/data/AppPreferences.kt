package com.slapglif.agentoverlay.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.slapglif.agentoverlay.model.ChatOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "agent_overlay")

data class StoredPreferences(
    val gatewayUrl: String = "http://10.0.2.2:8642",
    val apiKey: String = "",
    val burrowRegistryUrl: String = "wss://reg.ai-smith.net",
    val chatOptions: ChatOptions = ChatOptions()
)

class AppPreferences(private val context: Context) {
    val preferences: Flow<StoredPreferences> = context.dataStore.data.map { prefs ->
        StoredPreferences(
            gatewayUrl = prefs[GATEWAY_URL] ?: "http://10.0.2.2:8642",
            apiKey = prefs[API_KEY] ?: "",
            burrowRegistryUrl = prefs[BURROW_REGISTRY_URL] ?: "wss://reg.ai-smith.net",
            chatOptions = ChatOptions(
                modelId = prefs[CHAT_MODEL_ID] ?: "hermes-agent",
                reasoningMode = prefs[CHAT_REASONING]?.let { stored ->
                    ChatOptions.ReasoningMode.entries.firstOrNull { it.name == stored }
                } ?: ChatOptions.ReasoningMode.Auto,
                toolCallsEnabled = prefs[CHAT_TOOLS] ?: true,
                commandPassthroughEnabled = prefs[CHAT_PASSTHROUGH] ?: true
            )
        )
    }

    suspend fun saveGatewayUrl(value: String) {
        context.dataStore.edit { it[GATEWAY_URL] = value.trim().trimEnd('/') }
    }

    suspend fun saveApiKey(value: String) {
        context.dataStore.edit { it[API_KEY] = value.trim() }
    }

    suspend fun saveBurrowRegistryUrl(value: String) {
        context.dataStore.edit { it[BURROW_REGISTRY_URL] = value.trim().trimEnd('/') }
    }

    suspend fun saveChatOptions(options: ChatOptions) {
        context.dataStore.edit {
            it[CHAT_MODEL_ID] = options.modelId
            it[CHAT_REASONING] = options.reasoningMode.name
            it[CHAT_TOOLS] = options.toolCallsEnabled
            it[CHAT_PASSTHROUGH] = options.commandPassthroughEnabled
        }
    }

    companion object {
        private val GATEWAY_URL = stringPreferencesKey("gateway_url")
        private val API_KEY = stringPreferencesKey("api_key")
        private val BURROW_REGISTRY_URL = stringPreferencesKey("burrow_registry_url")
        private val CHAT_MODEL_ID = stringPreferencesKey("chat_model_id")
        private val CHAT_REASONING = stringPreferencesKey("chat_reasoning_mode")
        private val CHAT_TOOLS = booleanPreferencesKey("chat_tools_enabled")
        private val CHAT_PASSTHROUGH = booleanPreferencesKey("chat_command_passthrough")
    }
}

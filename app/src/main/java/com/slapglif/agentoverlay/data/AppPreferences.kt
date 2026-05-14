package com.slapglif.agentoverlay.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "agent_overlay")

data class StoredPreferences(
    val gatewayUrl: String = "http://10.0.2.2:8642",
    val apiKey: String = ""
)

class AppPreferences(private val context: Context) {
    val preferences: Flow<StoredPreferences> = context.dataStore.data.map { prefs ->
        StoredPreferences(
            gatewayUrl = prefs[GATEWAY_URL] ?: "http://10.0.2.2:8642",
            apiKey = prefs[API_KEY] ?: ""
        )
    }

    suspend fun saveGatewayUrl(value: String) {
        context.dataStore.edit { it[GATEWAY_URL] = value.trim().trimEnd('/') }
    }

    suspend fun saveApiKey(value: String) {
        context.dataStore.edit { it[API_KEY] = value.trim() }
    }

    companion object {
        private val GATEWAY_URL = stringPreferencesKey("gateway_url")
        private val API_KEY = stringPreferencesKey("api_key")
    }
}

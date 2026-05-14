package com.slapglif.agentoverlay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.slapglif.agentoverlay.data.AgentOverlayRepository
import com.slapglif.agentoverlay.data.AppPreferences
import com.slapglif.agentoverlay.hermes.HermesGatewayClient
import com.slapglif.agentoverlay.model.AgentThread
import com.slapglif.agentoverlay.model.ChatMessage
import com.slapglif.agentoverlay.model.GatewayConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AgentOverlayRepository) : ViewModel() {
    private val _state = MutableStateFlow(AgentOverlayUiState())
    val state: StateFlow<AgentOverlayUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.preferences.collect { prefs ->
                _state.update { it.copy(gatewayUrl = prefs.gatewayUrl, apiKey = prefs.apiKey) }
            }
        }
    }

    fun setGatewayUrl(value: String) {
        _state.update { it.copy(gatewayUrl = value) }
        viewModelScope.launch { repository.saveGatewayUrl(value) }
    }

    fun setApiKey(value: String) {
        _state.update { it.copy(apiKey = value) }
        viewModelScope.launch { repository.saveApiKey(value) }
    }

    fun connect() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching { repository.checkConnection() }
            .onSuccess { connection ->
                _state.update { it.copy(connection = connection, isLoading = false) }
                refresh()
            }
            .onFailure { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching { repository.loadThreads() }
            .onSuccess { threads ->
                _state.update { current ->
                    val selected = current.selectedThreadId ?: threads.firstOrNull()?.id
                    current.copy(threads = threads, selectedThreadId = selected, isLoading = false)
                }
            }
            .onFailure { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
    }

    fun selectThread(id: String) {
        _state.update { it.copy(selectedThreadId = id) }
    }

    fun sendMessage(text: String) = viewModelScope.launch {
        val threadId = _state.value.selectedThreadId ?: "mobile-overlay"
        if (text.isBlank()) return@launch
        _state.update { current ->
            current.copy(
                threads = current.threads.upsertMessage(threadId, ChatMessage.User(text)),
                isLoading = true,
                error = null
            )
        }
        runCatching { repository.sendMessage(threadId, text) }
            .onSuccess { response ->
                _state.update { current ->
                    current.copy(
                        threads = current.threads.upsertMessage(threadId, ChatMessage.Assistant(response)),
                        isLoading = false
                    )
                }
            }
            .onFailure { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val prefs = AppPreferences(context.applicationContext)
                val client = HermesGatewayClient()
                return MainViewModel(AgentOverlayRepository(prefs, client)) as T
            }
        }
    }
}

data class AgentOverlayUiState(
    val gatewayUrl: String = "http://10.0.2.2:8642",
    val apiKey: String = "",
    val connection: GatewayConnection = GatewayConnection.Disconnected,
    val threads: List<AgentThread> = emptyList(),
    val selectedThreadId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

private fun List<AgentThread>.upsertMessage(threadId: String, message: ChatMessage): List<AgentThread> {
    val existing = firstOrNull { it.id == threadId }
    return if (existing == null) {
        this + AgentThread(threadId, "Mobile overlay", AgentThread.Status.Running, listOf(message))
    } else {
        map { if (it.id == threadId) it.copy(messages = it.messages + message) else it }
    }
}

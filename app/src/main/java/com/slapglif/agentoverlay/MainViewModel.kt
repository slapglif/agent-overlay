package com.slapglif.agentoverlay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.slapglif.agentoverlay.data.AgentOverlayRepository
import com.slapglif.agentoverlay.data.AppPreferences
import com.slapglif.agentoverlay.hermes.HermesGatewayClient
import com.slapglif.agentoverlay.model.AgentModel
import com.slapglif.agentoverlay.model.AgentThread
import com.slapglif.agentoverlay.model.ChatMessage
import com.slapglif.agentoverlay.model.ChatOptions
import com.slapglif.agentoverlay.model.GatewayConnection
import com.slapglif.agentoverlay.phone.PhoneAutomationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AgentOverlayRepository) : ViewModel() {
    private val phoneAutomation = PhoneAutomationController()
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
                val models = repository.loadModels()
                _state.update { current ->
                    current.copy(
                        connection = connection,
                        availableModels = models,
                        chatOptions = current.chatOptions.copy(modelId = current.chatOptions.modelId.ifBlank { models.firstOrNull()?.id ?: "hermes-agent" }),
                        isLoading = false
                    )
                }
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

    fun selectModel(modelId: String) {
        _state.update { it.copy(chatOptions = it.chatOptions.copy(modelId = modelId)) }
    }

    fun setReasoningMode(mode: ChatOptions.ReasoningMode) {
        _state.update { it.copy(chatOptions = it.chatOptions.copy(reasoningMode = mode)) }
    }

    fun setToolCallsEnabled(enabled: Boolean) {
        _state.update { it.copy(chatOptions = it.chatOptions.copy(toolCallsEnabled = enabled)) }
    }

    fun setCommandPassthroughEnabled(enabled: Boolean) {
        _state.update { it.copy(chatOptions = it.chatOptions.copy(commandPassthroughEnabled = enabled)) }
    }

    fun sendMessage(text: String) = viewModelScope.launch {
        val threadId = _state.value.selectedThreadId ?: "mobile-overlay"
        if (text.isBlank()) return@launch
        val options = _state.value.chatOptions
        val phoneContext = phoneAutomation.currentSnapshot()?.let {
            "\n\nCurrent phone context for optional phone tools:\n" + it.packageName + " " + it.width + "x" + it.height + " elements=" + it.elements.size
        }.orEmpty()
        _state.update { current ->
            current.copy(
                threads = current.threads.upsertMessage(threadId, ChatMessage.User(text)),
                isLoading = true,
                error = null
            )
        }
        runCatching { repository.sendMessage(threadId, text + phoneContext, options) }
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

    fun inspectPhone() {
        val threadId = _state.value.selectedThreadId ?: "mobile-overlay"
        val result = phoneAutomation.inspect()
        _state.update { current ->
            current.copy(
                threads = current.threads.upsertMessage(
                    threadId,
                    ChatMessage.Tool(
                        text = result.message,
                        toolName = result.action,
                        bounds = result.snapshot?.elements?.map { it.bounds }.orEmpty()
                    )
                ),
                error = if (result.ok) null else result.message
            )
        }
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
    val availableModels: List<AgentModel> = HermesGatewayClient.DEFAULT_MODELS,
    val chatOptions: ChatOptions = ChatOptions(),
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

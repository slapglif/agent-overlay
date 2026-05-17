package com.slapglif.agentoverlay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.slapglif.agentoverlay.data.AgentOverlayRepository
import com.slapglif.agentoverlay.data.AppPreferences
import com.slapglif.agentoverlay.hermes.GatewayPairingPayload
import com.slapglif.agentoverlay.hermes.HermesGatewayClient
import com.slapglif.agentoverlay.model.AgentModel
import com.slapglif.agentoverlay.model.AgentThread
import com.slapglif.agentoverlay.model.ChatMessage
import com.slapglif.agentoverlay.model.ChatOptions
import com.slapglif.agentoverlay.model.GatewayConnection
import com.slapglif.agentoverlay.model.PhoneAutomationAction
import com.slapglif.agentoverlay.model.PhoneAutomationResult
import com.slapglif.agentoverlay.model.PhoneScreenSnapshot
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

    fun applyPairingIntent(uriText: String?) {
        val payload = GatewayPairingPayload.parse(uriText) ?: return
        _state.update { it.copy(gatewayUrl = payload.gatewayUrl, apiKey = payload.apiKey, error = null) }
        viewModelScope.launch {
            repository.saveGatewayUrl(payload.gatewayUrl)
            repository.saveApiKey(payload.apiKey)
        }
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
        parseLocalPhoneCommand(text)?.let { action ->
            appendUserMessage(threadId, text)
            applyPhoneResult(threadId, phoneAutomation.perform(action), loading = false)
            return@launch
        }
        val options = _state.value.chatOptions
        val phoneContext = phoneAutomation.currentSnapshot()?.toHermesContext().orEmpty()
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
                        threads = current.threads.upsertMessage(threadId, ChatMessage.Assistant(response.text)),
                        isLoading = false
                    )
                }
                response.phoneToolCalls.forEach { call ->
                    val result = phoneAutomation.executeTool(call.name, org.json.JSONObject(call.argumentsJson.ifBlank { "{}" }))
                    applyPhoneResult(threadId, result, loading = false)
                }
            }
            .onFailure { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
    }

    fun inspectPhone() {
        val threadId = _state.value.selectedThreadId ?: "mobile-overlay"
        applyPhoneResult(threadId, phoneAutomation.inspect(), loading = _state.value.isLoading)
    }

    fun performPhoneAction(action: PhoneAutomationAction) {
        val threadId = _state.value.selectedThreadId ?: "mobile-overlay"
        applyPhoneResult(threadId, phoneAutomation.perform(action), loading = _state.value.isLoading)
    }

    private fun appendUserMessage(threadId: String, text: String) {
        _state.update { current -> current.copy(threads = current.threads.upsertMessage(threadId, ChatMessage.User(text))) }
    }

    private fun applyPhoneResult(threadId: String, result: PhoneAutomationResult, loading: Boolean) {
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
                phoneSnapshot = result.snapshot ?: current.phoneSnapshot,
                lastPhoneResult = result,
                isLoading = loading,
                error = if (result.ok) null else result.message
            )
        }
    }

    private fun parseLocalPhoneCommand(text: String): PhoneAutomationAction? {
        val parts = text.trim().split(Regex("\\s+"), limit = 2)
        val command = parts.firstOrNull()?.lowercase() ?: return null
        val arg = parts.getOrNull(1).orEmpty().trim()
        return when (command) {
            "/phone", "/snapshot" -> PhoneAutomationAction.Snapshot
            "/back" -> PhoneAutomationAction.Back
            "/home" -> PhoneAutomationAction.Home
            "/recents" -> PhoneAutomationAction.Recents
            "/tap" -> parseTap(arg)
            "/type" -> if (arg.isNotBlank()) PhoneAutomationAction.TypeText(arg) else null
            "/swipe" -> parseSwipe(arg)
            else -> null
        }
    }

    private fun parseTap(arg: String): PhoneAutomationAction? {
        if (arg.matches(Regex("p\\d+", RegexOption.IGNORE_CASE))) return PhoneAutomationAction.TapRef(arg.lowercase())
        val nums = arg.split(',', ' ').mapNotNull { it.toIntOrNull() }
        return if (nums.size >= 2) PhoneAutomationAction.Tap(nums[0], nums[1]) else null
    }

    private fun parseSwipe(arg: String): PhoneAutomationAction? {
        val nums = arg.split(',', ' ').mapNotNull { it.toIntOrNull() }
        return if (nums.size >= 4) PhoneAutomationAction.Swipe(nums[0], nums[1], nums[2], nums[3]) else null
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
    val phoneSnapshot: PhoneScreenSnapshot? = null,
    val lastPhoneResult: PhoneAutomationResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

private fun PhoneScreenSnapshot.toHermesContext(): String = buildString {
    append("\n\nCurrent phone automation context (RustDesk-style local input, Playwright MCP-style refs):\n")
    append("screen=").append(width).append('x').append(height).append(" rotation=").append(rotation)
    packageName?.let { append(" package=").append(it) }
    windowTitle?.let { append(" window=").append(it) }
    append("\nrefs:\n")
    elements.take(12).forEach { element ->
        append('[').append(element.ref).append("] ")
            .append(element.text.ifBlank { element.contentDescription.orEmpty() }.take(80))
            .append(" bounds=")
            .append(element.bounds.left).append(',').append(element.bounds.top).append(',')
            .append(element.bounds.right).append(',').append(element.bounds.bottom)
            .append(" flags=")
            .append(listOfNotNull("click".takeIf { element.clickable }, "edit".takeIf { element.editable }, "scroll".takeIf { element.scrollable }).joinToString("/").ifBlank { "view" })
            .append('\n')
    }
}


private fun List<AgentThread>.upsertMessage(threadId: String, message: ChatMessage): List<AgentThread> {
    val existing = firstOrNull { it.id == threadId }
    return if (existing == null) {
        this + AgentThread(threadId, "Mobile overlay", AgentThread.Status.Running, listOf(message))
    } else {
        map { if (it.id == threadId) it.copy(messages = it.messages + message) else it }
    }
}

package com.slapglif.agentoverlay.model

sealed interface GatewayConnection {
    data object Disconnected : GatewayConnection
    data class Connected(val baseUrl: String, val capabilities: Set<String>) : GatewayConnection
}

data class AgentThread(
    val id: String,
    val title: String,
    val status: Status,
    val messages: List<ChatMessage> = emptyList()
) {
    enum class Status { Running, Idle, Failed, Completed }
}

data class AgentModel(
    val id: String,
    val label: String = id
)

data class ChatOptions(
    val modelId: String = "hermes-agent",
    val reasoningMode: ReasoningMode = ReasoningMode.Auto,
    val toolCallsEnabled: Boolean = true,
    val commandPassthroughEnabled: Boolean = true
) {
    enum class ReasoningMode { Auto, Think, Deep }
}

data class PhoneBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    fun centerX(): Int = left + width / 2
    fun centerY(): Int = top + height / 2
}

data class PhoneElement(
    val ref: String,
    val text: String,
    val contentDescription: String? = null,
    val bounds: PhoneBounds,
    val clickable: Boolean = false,
    val editable: Boolean = false,
    val scrollable: Boolean = false,
    val source: Source = Source.Accessibility,
    val confidence: Float = 1f
) {
    enum class Source { Accessibility, Ocr, Vision }
}

data class PhoneScreenSnapshot(
    val width: Int,
    val height: Int,
    val rotation: Int,
    val timestampMillis: Long = System.currentTimeMillis(),
    val packageName: String? = null,
    val windowTitle: String? = null,
    val elements: List<PhoneElement> = emptyList()
)

sealed interface PhoneAutomationAction {
    data object Snapshot : PhoneAutomationAction
    data object Back : PhoneAutomationAction
    data object Home : PhoneAutomationAction
    data object Recents : PhoneAutomationAction
    data class Tap(val x: Int, val y: Int) : PhoneAutomationAction
    data class TapRef(val ref: String) : PhoneAutomationAction
    data class TypeText(val text: String) : PhoneAutomationAction
    data class Swipe(val startX: Int, val startY: Int, val endX: Int, val endY: Int, val durationMs: Long = 280) : PhoneAutomationAction
}

data class PhoneAutomationTool(
    val name: String,
    val description: String,
    val parameters: List<String>
)

data class PhoneAutomationResult(
    val ok: Boolean,
    val action: String,
    val message: String,
    val snapshot: PhoneScreenSnapshot? = null
)

data class PhoneToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String
)

data class GatewayChatResponse(
    val text: String,
    val phoneToolCalls: List<PhoneToolCall> = emptyList()
)

sealed interface ChatMessage {
    val text: String
    val timestampMillis: Long

    data class User(
        override val text: String,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : ChatMessage

    data class Assistant(
        override val text: String,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : ChatMessage

    data class Reasoning(
        override val text: String,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : ChatMessage

    data class Tool(
        override val text: String,
        override val timestampMillis: Long = System.currentTimeMillis(),
        val toolName: String? = null,
        val bounds: List<PhoneBounds> = emptyList()
    ) : ChatMessage
}

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
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : ChatMessage
}

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

    data class Tool(
        override val text: String,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : ChatMessage
}

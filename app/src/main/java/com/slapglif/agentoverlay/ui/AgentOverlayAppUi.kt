package com.slapglif.agentoverlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.slapglif.agentoverlay.AgentOverlayUiState
import com.slapglif.agentoverlay.model.AgentThread
import com.slapglif.agentoverlay.model.ChatMessage
import com.slapglif.agentoverlay.model.GatewayConnection

@Composable
fun AgentOverlayAppUi(
    state: AgentOverlayUiState,
    onGatewayUrlChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onStartOverlay: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSelectThread: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val selectedThread = state.threads.firstOrNull { it.id == state.selectedThreadId }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Text("Agent Overlay", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Hermes Gateway floating C&C", color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(16.dp))
        GatewayConfigCard(state, onGatewayUrlChanged, onApiKeyChanged, onConnect, onRefresh, onStartOverlay)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth < 720.dp) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThreadList(state.threads, state.selectedThreadId, onSelectThread, Modifier.weight(0.38f).fillMaxWidth())
                    ChatPane(selectedThread, draft, { draft = it }, {
                        onSendMessage(draft)
                        draft = ""
                    }, Modifier.weight(0.62f).fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThreadList(state.threads, state.selectedThreadId, onSelectThread, Modifier.weight(0.38f).fillMaxHeight())
                    ChatPane(selectedThread, draft, { draft = it }, {
                        onSendMessage(draft)
                        draft = ""
                    }, Modifier.weight(0.62f).fillMaxHeight())
                }
            }
        }
        if (state.isLoading) Box(Modifier.fillMaxWidth().testTag("loading-indicator"), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
private fun GatewayConfigCard(
    state: AgentOverlayUiState,
    onGatewayUrlChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onStartOverlay: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                state.gatewayUrl,
                onGatewayUrlChanged,
                label = { Text("Gateway URL") },
                supportingText = { Text("Emulator: http://10.0.2.2:8642 • Device: http://<LAN-IP>:8642") },
                modifier = Modifier.fillMaxWidth().testTag("gateway-url-field")
            )
            OutlinedTextField(
                state.apiKey,
                onApiKeyChanged,
                label = { Text("API_SERVER_KEY") },
                supportingText = { Text("Same bearer token configured in ~/.hermes/.env") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("api-key-field")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConnect, modifier = Modifier.testTag("connect-button")) { Text("Connect") }
                Button(onClick = onRefresh, modifier = Modifier.testTag("refresh-button")) { Text("Refresh agents") }
                Button(onClick = onStartOverlay, modifier = Modifier.testTag("start-overlay-button")) { Text("Start floating icon") }
            }
            Text(connectionText(state.connection), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ThreadList(threads: List<AgentThread>, selectedId: String?, onSelectThread: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.testTag("thread-list"), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Text("Agents / threads", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(threads.ifEmpty { listOf(AgentThread("mobile-overlay", "Mobile overlay", AgentThread.Status.Idle)) }) { thread ->
                    val selected = thread.id == selectedId
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else MaterialTheme.colorScheme.background,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectThread(thread.id) }
                            .padding(12.dp)
                    ) {
                        Text(thread.title, fontWeight = FontWeight.SemiBold)
                        Text("${thread.status} • ${thread.id}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatPane(
    thread: AgentThread?,
    draft: String,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier.testTag("chat-pane"), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Text(thread?.title ?: "Select a thread", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(thread?.messages.orEmpty()) { message -> MessageBubble(message) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(draft, onDraftChanged, label = { Text("Message Hermes") }, modifier = Modifier.weight(1f).testTag("message-field"))
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSend, enabled = draft.isNotBlank(), modifier = Modifier.testTag("send-button")) { Text("Send") }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message is ChatMessage.User
    val color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(Modifier.fillMaxWidth(0.86f).background(color, RoundedCornerShape(18.dp)).padding(12.dp)) {
            Text(if (isUser) "You" else "Hermes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(message.text)
        }
    }
}

private fun connectionText(connection: GatewayConnection): String = when (connection) {
    GatewayConnection.Disconnected -> "Disconnected"
    is GatewayConnection.Connected -> "Connected to ${connection.baseUrl} • ${connection.capabilities.joinToString()}"
}

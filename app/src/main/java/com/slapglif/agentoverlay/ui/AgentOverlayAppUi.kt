package com.slapglif.agentoverlay.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slapglif.agentoverlay.AgentOverlayUiState
import com.slapglif.agentoverlay.model.AgentThread
import com.slapglif.agentoverlay.model.ChatMessage
import com.slapglif.agentoverlay.model.GatewayConnection
import com.slapglif.agentoverlay.ui.theme.AgentColors

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

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF11121A), AgentColors.Void, Color(0xFF050608))
                )
            )
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroHeader(state = state, onStartOverlay = onStartOverlay)
            GatewayConfigCard(state, onGatewayUrlChanged, onApiKeyChanged, onConnect, onRefresh)
            state.error?.let { ErrorStrip(it) }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                if (maxWidth < 720.dp) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThreadList(state.threads, state.selectedThreadId, onSelectThread, Modifier.weight(0.42f).fillMaxWidth())
                        ChatPane(selectedThread, draft, { draft = it }, {
                            onSendMessage(draft)
                            draft = ""
                        }, Modifier.weight(0.58f).fillMaxWidth())
                    }
                } else {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThreadList(state.threads, state.selectedThreadId, onSelectThread, Modifier.weight(0.35f).fillMaxHeight())
                        ChatPane(selectedThread, draft, { draft = it }, {
                            onSendMessage(draft)
                            draft = ""
                        }, Modifier.weight(0.65f).fillMaxHeight())
                    }
                }
            }
        }
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .testTag("loading-indicator"),
                contentAlignment = Alignment.Center
            ) {
                GlassPanel(Modifier.width(156.dp)) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = AgentColors.Indigo)
                        Text("Syncing", color = AgentColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroHeader(state: AgentOverlayUiState, onStartOverlay: () -> Unit) {
    GlassPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(AgentColors.RayRed, AgentColors.Indigo)))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("☤", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Agent Overlay",
                        color = AgentColors.Text,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.8).sp
                    )
                    Text(
                        "Hermes Gateway floating C&C",
                        color = AgentColors.Muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                MiniStatusDot(state.connection)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Gateway", if (state.connection is GatewayConnection.Connected) "online" else "local")
                StatPill("Agents", state.threads.size.coerceAtLeast(1).toString())
                StatPill("Mode", "overlay")
            }
            Button(
                onClick = onStartOverlay,
                modifier = Modifier.fillMaxWidth().testTag("start-overlay-button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AgentColors.Text, contentColor = Color(0xFF101116))
            ) { Text("Start floating icon", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun MiniStatusDot(connection: GatewayConnection) {
    val connected = connection is GatewayConnection.Connected
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (connected) AgentColors.Success else AgentColors.Warn)
        )
        Text(if (connected) "ONLINE" else "READY", color = AgentColors.Subtle, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GatewayConfigCard(
    state: AgentOverlayUiState,
    onGatewayUrlChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit
) {
    GlassPanel {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Command link", color = AgentColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(connectionText(state.connection), color = AgentColors.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedTextField(
                state.gatewayUrl,
                onGatewayUrlChanged,
                label = { Text("Gateway URL") },
                supportingText = { Text("Emulator 10.0.2.2 • device LAN/IP • HTTPS tunnel ready") },
                singleLine = true,
                colors = textFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("gateway-url-field")
            )
            OutlinedTextField(
                state.apiKey,
                onApiKeyChanged,
                label = { Text("API_SERVER_KEY") },
                supportingText = { Text("Bearer token from Hermes API server") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = textFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("api-key-field")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CommandButton("Connect", onConnect, Modifier.weight(1f).testTag("connect-button"), primary = true)
                CommandButton("Refresh", onRefresh, Modifier.weight(1f).testTag("refresh-button"))
            }
        }
    }
}

@Composable
private fun ThreadList(threads: List<AgentThread>, selectedId: String?, onSelectThread: (String) -> Unit, modifier: Modifier = Modifier) {
    GlassPanel(modifier.testTag("thread-list")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Agents / threads", color = AgentColors.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("DISCORD-STYLE", color = AgentColors.Subtle, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(threads.ifEmpty { listOf(AgentThread("mobile-overlay", "Mobile overlay", AgentThread.Status.Idle)) }) { thread ->
                    ThreadRow(thread, selected = thread.id == selectedId, onClick = { onSelectThread(thread.id) })
                }
            }
        }
    }
}

@Composable
private fun ThreadRow(thread: AgentThread, selected: Boolean, onClick: () -> Unit) {
    val statusColor = when (thread.status) {
        AgentThread.Status.Running -> AgentColors.Success
        AgentThread.Status.Failed -> AgentColors.RayRed
        AgentThread.Status.Completed -> AgentColors.Indigo
        AgentThread.Status.Idle -> AgentColors.Warn
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) AgentColors.Indigo.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.035f))
            .border(1.dp, if (selected) AgentColors.Indigo.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor))
        Column(Modifier.weight(1f)) {
            Text(thread.title, color = AgentColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("# ${thread.id}", color = AgentColors.Subtle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace)
        }
        Text(thread.status.name.lowercase(), color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    GlassPanel(modifier.testTag("chat-pane")) {
        val messages = thread?.messages.orEmpty()
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        }
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(AgentColors.IndigoDeep), contentAlignment = Alignment.Center) {
                    Text("#", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(thread?.title ?: "Select a thread", color = AgentColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Gateway session • live command transcript", color = AgentColors.Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.16f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    item { EmptyTranscript() }
                } else {
                    items(messages) { message -> MessageBubble(message) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    draft,
                    onDraftChanged,
                    label = { Text("Message Hermes") },
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 58.dp).testTag("message-field")
                )
                Button(
                    onClick = onSend,
                    enabled = draft.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AgentColors.Indigo, disabledContainerColor = AgentColors.SurfaceHigh, contentColor = Color.White),
                    modifier = Modifier.height(58.dp).testTag("send-button")
                ) { Text("Send", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message is ChatMessage.User
    val isTool = message is ChatMessage.Tool
    val accent = when {
        isUser -> AgentColors.Indigo
        isTool -> AgentColors.Warn
        else -> AgentColors.Info
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Row(
            Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!isUser) Avatar(if (isTool) "⚙" else "H", accent)
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isUser) AgentColors.Indigo.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.045f))
                    .border(1.dp, if (isUser) AgentColors.Indigo.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Text(if (isUser) "You" else if (isTool) "Gateway" else "Hermes", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(message.text, color = AgentColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
            }
            if (isUser) Avatar("Y", accent)
        }
    }
}

@Composable
private fun Avatar(label: String, color: Color) {
    Box(Modifier.size(28.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.2f)).border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyTranscript() {
    Column(
        Modifier.fillMaxWidth().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("No messages yet", color = AgentColors.Text, fontWeight = FontWeight.SemiBold)
        Text("Pick a running agent thread and send Hermes a command.", color = AgentColors.Muted, fontSize = 13.sp)
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label.uppercase(), color = AgentColors.Subtle, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = AgentColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CommandButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        border = if (primary) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) AgentColors.IndigoDeep else Color.White.copy(alpha = 0.045f),
            contentColor = Color.White
        )
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AgentColors.Panel.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) { content() }
}

@Composable
private fun ErrorStrip(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AgentColors.RayRed.copy(alpha = 0.12f))
            .border(1.dp, AgentColors.RayRed.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) { Text(message, color = AgentColors.RayRed, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AgentColors.Text,
    unfocusedTextColor = AgentColors.Text,
    focusedContainerColor = Color.White.copy(alpha = 0.035f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.025f),
    focusedBorderColor = AgentColors.Indigo,
    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
    focusedLabelColor = AgentColors.Info,
    unfocusedLabelColor = AgentColors.Muted,
    focusedSupportingTextColor = AgentColors.Subtle,
    unfocusedSupportingTextColor = AgentColors.Subtle,
    cursorColor = AgentColors.Info
)

private fun connectionText(connection: GatewayConnection): String = when (connection) {
    GatewayConnection.Disconnected -> "Disconnected"
    is GatewayConnection.Connected -> "Connected to ${connection.baseUrl}"
}

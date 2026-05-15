package com.slapglif.agentoverlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slapglif.agentoverlay.AgentOverlayUiState
import com.slapglif.agentoverlay.model.AgentThread
import com.slapglif.agentoverlay.model.ChatMessage
import com.slapglif.agentoverlay.model.ChatOptions
import com.slapglif.agentoverlay.model.GatewayConnection
import com.slapglif.agentoverlay.model.PhoneAutomationAction
import com.slapglif.agentoverlay.model.PhoneScreenSnapshot
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
    onSelectThread: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onReasoningModeChanged: (ChatOptions.ReasoningMode) -> Unit,
    onToolCallsToggled: (Boolean) -> Unit,
    onCommandPassthroughToggled: (Boolean) -> Unit,
    onInspectPhone: () -> Unit,
    onPerformPhoneAction: (PhoneAutomationAction) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val selectedThread = state.threads.firstOrNull { it.id == state.selectedThreadId }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1B1C27), AgentColors.Void, Color(0xFF040508)),
                    radius = 1250f
                )
            )
    ) {
        Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActivityRail()
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroHeader(state = state, onStartOverlay = onStartOverlay, onConnect = onConnect)
                if (state.connection !is GatewayConnection.Connected) {
                    GatewayConfigCard(state, onGatewayUrlChanged, onApiKeyChanged, onConnect, onRefresh)
                }
                state.error?.let { ErrorStrip(it) }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    if (maxWidth < 720.dp) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ThreadList(state.threads, state.selectedThreadId, onSelectThread, Modifier.weight(0.08f).fillMaxWidth())
                            ChatPane(selectedThread, draft, { draft = it }, {
                                onSendMessage(draft)
                                draft = ""
                            }, state, onSelectModel, onReasoningModeChanged, onToolCallsToggled, onCommandPassthroughToggled, onInspectPhone, onPerformPhoneAction, Modifier.weight(0.92f).fillMaxWidth())
                        }
                    } else {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ThreadList(state.threads, state.selectedThreadId, onSelectThread, Modifier.weight(0.34f).fillMaxHeight())
                            ChatPane(selectedThread, draft, { draft = it }, {
                                onSendMessage(draft)
                                draft = ""
                            }, state, onSelectModel, onReasoningModeChanged, onToolCallsToggled, onCommandPassthroughToggled, onInspectPhone, onPerformPhoneAction, Modifier.weight(0.66f).fillMaxHeight())
                        }
                    }
                }
            }
        }
        if (state.isLoading) LoadingPill()
    }
}

@Composable
private fun ActivityRail() {
    Column(
        Modifier
            .width(56.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(28.dp))
            .background(AgentColors.Surface)
            .border(1.dp, AgentColors.Border, RoundedCornerShape(28.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        RailIcon("⌂", active = false)
        RailIcon("✣", active = true)
        RailIcon("▣", active = false)
        RailIcon("⌁", active = false)
        RailIcon("⚙", active = false)
        Spacer(Modifier.weight(1f))
        RailIcon("?", active = false)
    }
}

@Composable
private fun RailIcon(label: String, active: Boolean) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) AgentColors.Indigo.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (active) AgentColors.Indigo.copy(alpha = 0.30f) else Color.Transparent, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (active) AgentColors.Indigo else AgentColors.Muted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeroHeader(state: AgentOverlayUiState, onStartOverlay: () -> Unit, onConnect: () -> Unit) {
    GlassPanel {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AgentColors.Indigo, AgentColors.Info))).border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape), contentAlignment = Alignment.Center) {
                    Text("AO", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Agent Overlay",
                    color = AgentColors.Text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.7).sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                MiniStatusDot(state.connection)
            }
            Text(
                "Tiny command button → quick actions → agent view",
                color = AgentColors.Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StatPill("Gateway", if (state.connection is GatewayConnection.Connected) "online" else "local")
                StatPill("Agents", state.threads.size.coerceAtLeast(1).toString())
                StatPill("Mode", "hover")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CommandButton("Connect", onConnect, Modifier.weight(1f).testTag("hero-connect-button"), primary = true)
                CommandButton("Float", onStartOverlay, Modifier.weight(1f).testTag("start-overlay-button"))
            }
        }
    }
}

@Composable
private fun BubbleClusterMark() {
    Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(AgentColors.Indigo.copy(alpha = 0.16f)))
        Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AgentColors.Indigo, AgentColors.Info))).border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape), contentAlignment = Alignment.Center) {
            Text("AO", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape).background(AgentColors.Success).border(2.dp, AgentColors.Panel, CircleShape))
    }
}

@Composable
private fun FlowHintStrip() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AgentColors.SurfaceHigh)
            .border(1.dp, AgentColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Icon", color = AgentColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text("→", color = AgentColors.Subtle, fontSize = 12.sp)
        Text("quick tray", color = AgentColors.Muted, fontSize = 12.sp)
        Text("→", color = AgentColors.Subtle, fontSize = 12.sp)
        Text("agent view", color = AgentColors.Indigo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text("⚙ full screen", color = AgentColors.Subtle, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniStatusDot(connection: GatewayConnection) {
    val connected = connection is GatewayConnection.Connected
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(if (connected) AgentColors.Success else AgentColors.Warn))
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
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings / command link", color = AgentColors.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(connectionText(state.connection), color = AgentColors.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedTextField(
                state.gatewayUrl,
                onGatewayUrlChanged,
                label = { Text("Gateway URL") },
                supportingText = { Text("10.0.2.2 on emulator • LAN/IP on device") },
                singleLine = true,
                colors = textFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("gateway-url-field")
            )
            OutlinedTextField(
                state.apiKey,
                onApiKeyChanged,
                label = { Text("API_SERVER_KEY") },
                supportingText = { Text("Used only by the gateway session") },
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
                Text("Agent bubble roster", color = AgentColors.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("HOVER-FIRST", color = AgentColors.Subtle, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    val statusColor = statusColor(thread.status)
    val bg by animateColorAsState(if (selected) AgentColors.Indigo.copy(alpha = 0.12f) else AgentColors.SurfaceHigh, label = "thread-bg")
    val stroke by animateColorAsState(if (selected) AgentColors.Indigo.copy(alpha = 0.50f) else AgentColors.Border, label = "thread-border")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, stroke, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AgentAvatar(thread.title, statusColor)
        Column(Modifier.weight(1f)) {
            Text(thread.title, color = AgentColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${thread.id} • tap title for full screen", color = AgentColors.Subtle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace)
        }
        Text(thread.status.name.lowercase(), color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AgentAvatar(title: String, color: Color) {
    val initials = title.split(" ").take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" }.ifBlank { "A" }
    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)))
        Box(Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.28f)).border(1.dp, color.copy(alpha = 0.58f), CircleShape), contentAlignment = Alignment.Center) {
            Text(initials.take(2), color = AgentColors.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.align(Alignment.BottomEnd).size(10.dp).clip(CircleShape).background(color).border(1.dp, AgentColors.Panel, CircleShape))
    }
}

@Composable
private fun ChatPane(
    thread: AgentThread?,
    draft: String,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    state: AgentOverlayUiState,
    onSelectModel: (String) -> Unit,
    onReasoningModeChanged: (ChatOptions.ReasoningMode) -> Unit,
    onToolCallsToggled: (Boolean) -> Unit,
    onCommandPassthroughToggled: (Boolean) -> Unit,
    onInspectPhone: () -> Unit,
    onPerformPhoneAction: (PhoneAutomationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showActivity by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    GlassPanel(modifier.testTag("chat-pane")) {
        val messages = thread?.messages.orEmpty()
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        }
        Column(Modifier.padding(12.dp)) {
            ChatHeader(thread, state, onSelectModel)
            Spacer(Modifier.height(8.dp))
            AgentActivityPill(state, expanded = showActivity, onToggle = { showActivity = !showActivity })
            state.phoneSnapshot?.let { snapshot ->
                Spacer(Modifier.height(8.dp))
                PhoneAutomationStrip(
                    snapshot = snapshot,
                    onInspectPhone = onInspectPhone,
                    onBack = { onPerformPhoneAction(PhoneAutomationAction.Back) },
                    onHome = { onPerformPhoneAction(PhoneAutomationAction.Home) },
                    onRecents = { onPerformPhoneAction(PhoneAutomationAction.Recents) },
                    onTapRef = { ref -> onPerformPhoneAction(PhoneAutomationAction.TapRef(ref)) }
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(AgentColors.Surface)
                    .border(1.dp, AgentColors.Border, RoundedCornerShape(22.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) item { EmptyTranscript() } else items(messages) { message -> MessageBubble(message) }
            }
            if (draft.startsWith("/")) {
                Spacer(Modifier.height(8.dp))
                SlashCommandPalette(onDraftChanged, onReasoningModeChanged, onToolCallsToggled, onCommandPassthroughToggled, onInspectPhone, onPerformPhoneAction)
            } else if (showAddMenu) {
                Spacer(Modifier.height(8.dp))
                ComposerAccessorySheet(onDraftChanged, onReasoningModeChanged, onToolCallsToggled, onInspectPhone) { showAddMenu = false }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showAddMenu = !showAddMenu },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AgentColors.SurfaceHigh, contentColor = AgentColors.Text),
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Open chat tools" }.testTag("composer-add")
                ) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                OutlinedTextField(
                    draft,
                    onDraftChanged,
                    placeholder = { Text("Ask Hermes…") },
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp, max = 128.dp).testTag("message-field"),
                    minLines = 1,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Button(
                    onClick = onSend,
                    enabled = draft.isNotBlank(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AgentColors.Indigo, disabledContainerColor = AgentColors.SurfaceHigh, contentColor = Color.White),
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Send message" }.testTag("send-button")
                ) { Text("Send", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun ChatHeader(thread: AgentThread?, state: AgentOverlayUiState, onSelectModel: (String) -> Unit) {
    val status = thread?.status ?: AgentThread.Status.Idle
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AgentColors.SurfaceHigh)
            .border(1.dp, AgentColors.Border, RoundedCornerShape(20.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AgentAvatar(thread?.title ?: "Hermes", statusColor(status))
        Column(Modifier.weight(1f)) {
            Text(thread?.title ?: "Hermes", color = AgentColors.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (state.isLoading) "Working…" else "Ready · ${friendlyModel(state.chatOptions.modelId)}", color = AgentColors.Muted, fontSize = 12.sp)
        }
        ModelMenu(state, onSelectModel)
        Text("⋯", color = AgentColors.Subtle, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("↗", color = AgentColors.Subtle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModelMenu(state: AgentOverlayUiState, onSelectModel: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Text(
            "${friendlyModel(state.chatOptions.modelId)} ▾",
            color = AgentColors.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(AgentColors.Surface)
                .border(1.dp, AgentColors.Border, RoundedCornerShape(999.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("model-switcher")
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.availableModels.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelectModel(model.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AgentActivityPill(state: AgentOverlayUiState, expanded: Boolean, onToggle: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (state.isLoading) AgentColors.Indigo.copy(alpha = 0.12f) else AgentColors.SurfaceHigh)
            .border(1.dp, if (state.isLoading) AgentColors.Indigo.copy(alpha = 0.32f) else AgentColors.Border, RoundedCornerShape(18.dp))
            .clickable { onToggle() }
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (state.isLoading) AgentColors.Indigo else AgentColors.Success))
            Text(if (state.isLoading) "Hermes is working" else "Hermes can reason, use tools, and run commands when needed", color = AgentColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 2)
            Text(if (expanded) "⌃" else "⌄", color = AgentColors.Subtle, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                ActivityStep("Chooses a model automatically unless you switch it", done = true)
                ActivityStep("Uses tools and skills only when the request calls for it", done = true)
                ActivityStep("Phone automation exposes Playwright-style refs, OCR/vision boxes, and tap/type/swipe tools", done = true)
                ActivityStep("Type / to open the Hermes command palette", done = true)
            }
        }
    }
}

@Composable
private fun ActivityStep(text: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (done) "✓" else "○", color = if (done) AgentColors.Success else AgentColors.Subtle, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text, color = AgentColors.Muted, fontSize = 12.sp)
    }
}

@Composable
private fun PhoneAutomationStrip(
    snapshot: PhoneScreenSnapshot,
    onInspectPhone: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRecents: () -> Unit,
    onTapRef: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AgentColors.Success.copy(alpha = 0.08f))
            .border(1.dp, AgentColors.Success.copy(alpha = 0.24f), RoundedCornerShape(18.dp))
            .padding(9.dp)
            .testTag("phone-tools-strip"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Phone", color = AgentColors.Success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${snapshot.packageName ?: "active app"} · ${snapshot.elements.size} refs", color = AgentColors.Text, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            MiniAction("Inspect", "phone-inspect-button", onInspectPhone)
            MiniAction("Back", "phone-back-button", onBack)
            MiniAction("Home", "phone-home-button", onHome)
            MiniAction("Recents", "phone-recents-button", onRecents)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            snapshot.elements.take(4).forEachIndexed { index, element ->
                Text(
                    "${element.ref} ${element.text.ifBlank { element.contentDescription.orEmpty() }.take(14)}",
                    color = AgentColors.Text,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AgentColors.Surface)
                        .border(1.dp, AgentColors.Border, RoundedCornerShape(999.dp))
                        .clickable { onTapRef(element.ref) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("phone-ref-p$index")
                )
            }
        }
    }
}

@Composable
private fun MiniAction(label: String, tag: String, onClick: () -> Unit) {
    Text(
        label,
        color = AgentColors.Text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AgentColors.Surface)
            .border(1.dp, AgentColors.Border, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(tag)
    )
}

@Composable
private fun ComposerAccessorySheet(
    onDraftChanged: (String) -> Unit,
    onReasoningModeChanged: (ChatOptions.ReasoningMode) -> Unit,
    onToolCallsToggled: (Boolean) -> Unit,
    onInspectPhone: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AgentColors.SurfaceHigh)
            .border(1.dp, AgentColors.Border, RoundedCornerShape(18.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AccessoryAction("Deep answer") {
            onReasoningModeChanged(ChatOptions.ReasoningMode.Deep)
            onDraftChanged("Think this through: ")
            onDismiss()
        }
        AccessoryAction("Use web") {
            onToolCallsToggled(true)
            onDraftChanged("Search and answer: ")
            onDismiss()
        }
        AccessoryAction("Use files") {
            onToolCallsToggled(true)
            onDraftChanged("Use my files to answer: ")
            onDismiss()
        }
        AccessoryAction("Inspect phone") {
            onToolCallsToggled(true)
            onInspectPhone()
            onDismiss()
        }
    }
}

@Composable
private fun AccessoryAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = AgentColors.Text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AgentColors.Surface)
            .border(1.dp, AgentColors.Border, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
private fun SlashCommandPalette(
    onDraftChanged: (String) -> Unit,
    onReasoningModeChanged: (ChatOptions.ReasoningMode) -> Unit,
    onToolCallsToggled: (Boolean) -> Unit,
    onCommandPassthroughToggled: (Boolean) -> Unit,
    onInspectPhone: () -> Unit,
    onPerformPhoneAction: (PhoneAutomationAction) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AgentColors.SurfaceHigh)
            .border(1.dp, AgentColors.Indigo.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Hermes commands", color = AgentColors.Subtle, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        CommandRow("/commands", "Show available Hermes commands") {
            onCommandPassthroughToggled(true)
            onDraftChanged("/commands")
        }
        CommandRow("/skills", "Browse and load skills") {
            onCommandPassthroughToggled(true)
            onDraftChanged("/skills")
        }
        CommandRow("/phone", "Inspect visible phone UI with refs and boxes") {
            onToolCallsToggled(true)
            onCommandPassthroughToggled(true)
            onInspectPhone()
            onDraftChanged("/phone ")
        }
        CommandRow("/tap", "Tap a phone ref or x y coordinate") {
            onToolCallsToggled(true)
            onDraftChanged("/tap ")
        }
        CommandRow("/type", "Type into the focused phone field") {
            onToolCallsToggled(true)
            onDraftChanged("/type ")
        }
        CommandRow("/back", "Press Android back now") {
            onToolCallsToggled(true)
            onPerformPhoneAction(PhoneAutomationAction.Back)
            onDraftChanged("/back")
        }
        CommandRow("/home", "Press Android home now") {
            onToolCallsToggled(true)
            onPerformPhoneAction(PhoneAutomationAction.Home)
            onDraftChanged("/home")
        }
        CommandRow("/reason", "Ask for a deeper answer") {
            onReasoningModeChanged(ChatOptions.ReasoningMode.Deep)
            onDraftChanged("/reason ")
        }
        CommandRow("/tools", "Allow app, file, browser, or shell actions") {
            onToolCallsToggled(true)
            onDraftChanged("/tools ")
        }
    }
}

@Composable
private fun CommandRow(command: String, description: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(command, color = AgentColors.Indigo, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(description, color = AgentColors.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun friendlyModel(modelId: String): String = when {
    modelId.equals("hermes-agent", ignoreCase = true) -> "Auto"
    modelId.contains("gpt", ignoreCase = true) -> "GPT"
    modelId.contains("claude", ignoreCase = true) -> "Claude"
    modelId.contains("gemini", ignoreCase = true) -> "Gemini"
    else -> modelId.substringAfterLast('/').take(14)
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message is ChatMessage.User
    val isTool = message is ChatMessage.Tool
    val isReasoning = message is ChatMessage.Reasoning
    val accent = when {
        isUser -> AgentColors.Indigo
        isTool -> AgentColors.Warn
        isReasoning -> AgentColors.Success
        else -> AgentColors.Info
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Row(Modifier.fillMaxWidth(0.92f), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
            if (!isUser) Avatar(if (isTool) "⚙" else if (isReasoning) "◇" else "H", accent)
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isUser) AgentColors.Indigo.copy(alpha = 0.14f) else if (isReasoning) AgentColors.Success.copy(alpha = 0.08f) else AgentColors.SurfaceHigh)
                    .border(1.dp, if (isUser) AgentColors.Indigo.copy(alpha = 0.42f) else if (isReasoning) AgentColors.Success.copy(alpha = 0.24f) else AgentColors.Border, RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Text(if (isUser) "You" else if (isTool) "Activity" else if (isReasoning) "Reasoning" else "Hermes", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(message.text, color = AgentColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
            }
            if (isUser) Avatar("Y", accent)
        }
    }
}

@Composable
private fun Avatar(label: String, color: Color) {
    Box(Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)).border(1.dp, color.copy(alpha = 0.45f), CircleShape), contentAlignment = Alignment.Center) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyTranscript() {
    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("No messages yet", color = AgentColors.Text, fontWeight = FontWeight.SemiBold)
        Text("Ask normally. Hermes will choose models, tools, and commands when needed.", color = AgentColors.Muted, fontSize = 13.sp)
    }
}

@Composable
private fun LoadingPill() {
    Box(Modifier.fillMaxWidth().padding(16.dp).testTag("loading-indicator"), contentAlignment = Alignment.Center) {
        GlassPanel(Modifier.width(156.dp)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = AgentColors.Indigo)
                Text("Syncing", color = AgentColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(AgentColors.SurfaceHigh).border(1.dp, AgentColors.Border, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
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
        border = if (primary) null else BorderStroke(1.dp, AgentColors.Border),
        colors = ButtonDefaults.buttonColors(containerColor = if (primary) AgentColors.IndigoDeep else AgentColors.SurfaceHigh, contentColor = AgentColors.Text)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = AgentColors.Panel),
        border = BorderStroke(1.dp, AgentColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) { content() }
}

@Composable
private fun ErrorStrip(message: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AgentColors.RayRed.copy(alpha = 0.12f)).border(1.dp, AgentColors.RayRed.copy(alpha = 0.24f), RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text(message, color = AgentColors.RayRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AgentColors.Text,
    unfocusedTextColor = AgentColors.Text,
    focusedContainerColor = AgentColors.SurfaceHigh,
    unfocusedContainerColor = AgentColors.SurfaceHigh,
    focusedBorderColor = AgentColors.Indigo,
    unfocusedBorderColor = AgentColors.Border,
    focusedLabelColor = AgentColors.Info,
    unfocusedLabelColor = AgentColors.Muted,
    focusedSupportingTextColor = AgentColors.Subtle,
    unfocusedSupportingTextColor = AgentColors.Subtle,
    cursorColor = AgentColors.Info
)

private fun statusColor(status: AgentThread.Status): Color = when (status) {
    AgentThread.Status.Running -> AgentColors.Success
    AgentThread.Status.Failed -> AgentColors.RayRed
    AgentThread.Status.Completed -> AgentColors.Indigo
    AgentThread.Status.Idle -> AgentColors.Warn
}

private fun connectionText(connection: GatewayConnection): String = when (connection) {
    GatewayConnection.Disconnected -> "Disconnected"
    is GatewayConnection.Connected -> "Connected to ${connection.baseUrl}"
}

package com.slapglif.agentoverlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slapglif.agentoverlay.overlay.OverlayService
import com.slapglif.agentoverlay.ui.AgentOverlayAppUi
import com.slapglif.agentoverlay.ui.theme.AgentOverlayTheme

class MainActivity : ComponentActivity() {
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { startOverlayServiceIfAllowed() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgentOverlayTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val viewModel: MainViewModel = viewModel(factory = MainViewModel.factory(applicationContext))
                    val state by viewModel.state.collectAsState()
                    AgentOverlayAppUi(
                        state = state,
                        onGatewayUrlChanged = viewModel::setGatewayUrl,
                        onApiKeyChanged = viewModel::setApiKey,
                        onConnect = viewModel::connect,
                        onRefresh = viewModel::refresh,
                        onStartOverlay = { requestOverlayAndStartService() },
                        onSendMessage = viewModel::sendMessage,
                        onSelectThread = viewModel::selectThread,
                        onSelectModel = viewModel::selectModel,
                        onReasoningModeChanged = viewModel::setReasoningMode,
                        onToolCallsToggled = viewModel::setToolCallsEnabled,
                        onCommandPassthroughToggled = viewModel::setCommandPassthroughEnabled
                    )
                }
            }
        }
    }

    private fun requestOverlayAndStartService() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startOverlayServiceIfAllowed()
        }
    }

    private fun startOverlayServiceIfAllowed() {
        if (Settings.canDrawOverlays(this)) {
            startForegroundService(Intent(this, OverlayService::class.java))
        }
    }
}

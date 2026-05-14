package com.slapglif.agentoverlay.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors: ColorScheme = darkColorScheme(
    primary = Color(0xFF9F7AEA),
    secondary = Color(0xFF38BDF8),
    tertiary = Color(0xFFF472B6),
    background = Color(0xFF080B12),
    surface = Color(0xFF111827),
    onPrimary = Color.White,
    onSecondary = Color(0xFF07111C),
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFE5E7EB)
)

@Composable
fun AgentOverlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}

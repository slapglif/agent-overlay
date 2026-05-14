package com.slapglif.agentoverlay.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AgentColors {
    val Void = Color(0xFFF3F2ED)
    val Panel = Color(0xFFFAF9F4)
    val Surface = Color(0xFFEDEBE3)
    val SurfaceHigh = Color(0xFFFFFFFF)
    val Border = Color(0xFFD8D6CC)
    val Text = Color(0xFF1F2328)
    val Muted = Color(0xFF5F6368)
    val Subtle = Color(0xFF8A8A84)
    val Indigo = Color(0xFF6C5CE7)
    val IndigoDeep = Color(0xFF5146C6)
    val RayRed = Color(0xFFF26207)
    val Success = Color(0xFF2DA44E)
    val Warn = Color(0xFFB7791F)
    val Info = Color(0xFF3B82F6)
}

private val Colors: ColorScheme = lightColorScheme(
    primary = AgentColors.Indigo,
    secondary = AgentColors.Info,
    tertiary = AgentColors.RayRed,
    background = AgentColors.Void,
    surface = AgentColors.Panel,
    surfaceVariant = AgentColors.Surface,
    outline = AgentColors.Border,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AgentColors.Text,
    onSurface = AgentColors.Text,
    onSurfaceVariant = AgentColors.Muted,
    error = Color(0xFFB42318)
)

@Composable
fun AgentOverlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}

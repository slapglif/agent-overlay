package com.slapglif.agentoverlay.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AgentColors {
    val Void = Color(0xFF07080A)
    val Panel = Color(0xFF0F1014)
    val Surface = Color(0xFF17181E)
    val SurfaceHigh = Color(0xFF20212A)
    val Border = Color(0x1AFFFFFF)
    val Text = Color(0xFFF7F8F8)
    val Muted = Color(0xFF8A8F98)
    val Subtle = Color(0xFF62666D)
    val Indigo = Color(0xFF7170FF)
    val IndigoDeep = Color(0xFF5E6AD2)
    val RayRed = Color(0xFFFF6363)
    val Success = Color(0xFF10B981)
    val Warn = Color(0xFFFFBC33)
    val Info = Color(0xFF55B3FF)
}

private val Colors: ColorScheme = darkColorScheme(
    primary = AgentColors.Indigo,
    secondary = AgentColors.Info,
    tertiary = AgentColors.RayRed,
    background = AgentColors.Void,
    surface = AgentColors.Panel,
    surfaceVariant = AgentColors.Surface,
    outline = AgentColors.Border,
    onPrimary = Color.White,
    onSecondary = Color(0xFF07111C),
    onBackground = AgentColors.Text,
    onSurface = AgentColors.Text,
    onSurfaceVariant = AgentColors.Muted,
    error = AgentColors.RayRed
)

@Composable
fun AgentOverlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}

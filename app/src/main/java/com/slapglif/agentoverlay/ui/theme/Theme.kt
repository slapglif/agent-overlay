package com.slapglif.agentoverlay.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AgentColors {
    val Void = Color(0xFF07080B)
    val Panel = Color(0xFF101116)
    val Surface = Color(0xFF181A20)
    val SurfaceHigh = Color(0xFF22252D)
    val Border = Color(0xFF343844)
    val Text = Color(0xFFF4F6FB)
    val Muted = Color(0xFFB2B8C4)
    val Subtle = Color(0xFF7B8290)
    val Indigo = Color(0xFF8B83FF)
    val IndigoDeep = Color(0xFF675BE8)
    val RayRed = Color(0xFFFF7A1A)
    val Success = Color(0xFF45D483)
    val Warn = Color(0xFFFFC857)
    val Info = Color(0xFF72B8FF)
}

private val Colors: ColorScheme = darkColorScheme(
    primary = AgentColors.Indigo,
    onPrimary = Color(0xFF101035),
    primaryContainer = AgentColors.IndigoDeep,
    onPrimaryContainer = Color.White,
    secondary = AgentColors.Info,
    onSecondary = Color(0xFF07121F),
    tertiary = AgentColors.RayRed,
    onTertiary = Color(0xFF241000),
    background = AgentColors.Void,
    onBackground = AgentColors.Text,
    surface = AgentColors.Panel,
    onSurface = AgentColors.Text,
    surfaceVariant = AgentColors.Surface,
    onSurfaceVariant = AgentColors.Muted,
    surfaceContainerLowest = AgentColors.Void,
    surfaceContainerLow = AgentColors.Panel,
    surfaceContainer = AgentColors.Surface,
    surfaceContainerHigh = AgentColors.SurfaceHigh,
    outline = AgentColors.Border,
    outlineVariant = AgentColors.Border.copy(alpha = 0.72f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val AgentTypography = Typography(
    headlineMedium = Typography().headlineMedium.copy(
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.7).sp
    ),
    titleMedium = Typography().titleMedium.copy(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    labelMedium = Typography().labelMedium.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
)

private val AgentShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

@Composable
fun AgentOverlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AgentTypography, shapes = AgentShapes, content = content)
}

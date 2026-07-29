package com.dsagent.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val White = Color(0xFFFFFFFF)
val LightBlue = Color(0xFF4FC3F7)
val LightBlueVariant = Color(0xFF29B6F6)
val DarkText = Color(0xFF1E293B)
val GrayText = Color(0xFF64748B)
val LightGray = Color(0xFFF1F5F9)
val GrayBorder = Color(0xFFE2E8F0)
val ErrorRed = Color(0xFFFF6B6B)
val SuccessGreen = Color(0xFF81C784)
val CodeBackground = Color(0xFF1E293B)

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkCard = Color(0xFF2D2D2D)
val DarkTextLight = Color(0xFFE0E0E0)
val DarkBorder = Color(0xFF3D3D3D)

private val LightColorScheme = lightColorScheme(
    primary = LightBlue, onPrimary = White,
    secondary = LightBlueVariant, background = White,
    surface = White, onBackground = DarkText,
    onSurface = DarkText, surfaceVariant = LightGray,
    outline = GrayBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = LightBlue, onPrimary = White,
    secondary = LightBlueVariant, background = DarkBackground,
    surface = DarkSurface, onBackground = DarkTextLight,
    onSurface = DarkTextLight, surfaceVariant = DarkCard,
    outline = DarkBorder
)

@Composable
fun DSAgentTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}

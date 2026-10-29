package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VoiceNavColorScheme = darkColorScheme(
    primary = AmberAccent,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = AmberAccent,
    secondary = AmberAccent,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = MainText,
    surface = DarkSurface,
    onSurface = MainText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = MutedText,
    error = ErrorRed,
    onError = DarkBackground
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VoiceNavColorScheme,
        typography = Typography,
        content = content
    )
}

package com.lucafilippi.universebible.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CinematicColors = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color(0xFF0B0D13),
    primaryContainer = Color(0xFF35225B),
    onPrimaryContainer = Color(0xFFEDE4FF),
    secondary = Color(0xFFA78BFA),
    background = Color(0xFF0B0D13),
    onBackground = Color(0xFFF4F4F5),
    surface = Color(0xFF171A24),
    onSurface = Color(0xFFF4F4F5),
    surfaceContainer = Color(0xFF202331),
    onSurfaceVariant = Color(0xFFA1A1B5),
    error = Color(0xFFFF8D8D),
)

@Composable
fun UniverseBibleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CinematicColors, content = content)
}

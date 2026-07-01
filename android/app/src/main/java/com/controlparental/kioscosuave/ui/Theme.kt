package com.controlparental.kioscosuave.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KioscoColors = darkColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color.White,
    secondary = Color(0xFF10B981),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0),
    error = Color(0xFFEF4444)
)

@Composable
fun KioscoTheme(content: @Composable () -> Unit) {
    // Siempre tema oscuro para coherencia con el diseño del proyecto.
    MaterialTheme(colorScheme = KioscoColors, content = content)
}

package com.gymlogger.ui.theme


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

import androidx.compose.material3.ColorScheme



fun getDynamicColorScheme(hue: Float): ColorScheme {
    val primaryColor = Color.hsv(hue, 0.8f, 1.0f)
    return darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor,
        onPrimaryContainer = Color.White,
        secondary = Color(0xFF2C2C2E),
        onSecondary = Color.White,
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1C1C1E),
        onSurfaceVariant = Color(0xFF8E8E93),
        outline = Color(0xFF3A3A3C),
        error = Color(0xFFFF453A),
        onError = Color.White
    )
}

@Composable
fun GymBroTheme(
    hue: Float = 210f, // Default blue hue
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = getDynamicColorScheme(hue)

    SystemAppearance(isDarkTheme = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

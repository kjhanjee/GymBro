package com.gymlogger.ui.theme

import android.app.Activity
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.ColorScheme

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun getDynamicColorScheme(hue: Float): ColorScheme {
    val primaryColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.8f, 1.0f)))
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            activity?.window?.let { window ->
                window.statusBarColor = Color.Black.toArgb()
                window.navigationBarColor = Color.Black.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

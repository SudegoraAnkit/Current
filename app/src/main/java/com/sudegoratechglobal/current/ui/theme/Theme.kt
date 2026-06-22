package com.sudegoratechglobal.current.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Low-Contrast, calming GenZ palette (no pure #000000 or pure #FFFFFF)
val DarkCharcoal = Color(0xFF161917)
val CardSurface = Color(0xFF222623)
val SageGreen = Color(0xFFB1D7C2)
val SoftLavender = Color(0xFFD3C5E3)
val SoftPeach = Color(0xFFEBD2BE)
val SoftPink = Color(0xFFE9B7BD)
val CleanOffWhite = Color(0xFFE7ECE8)
val MutedText = Color(0xFF909A93)
val BorderColor = Color(0xFF2F3631)

// Gradient Brushes for Premium Visual Aesthetics
val SageToLavenderGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF99C7AD), Color(0xFFBCABC7))
)
val LavenderToPeachGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFBCABC7), Color(0xFFD4BBA5))
)
val DarkBackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF131614), Color(0xFF1E211F))
)
val FrogFocusGradient = Brush.radialGradient(
    colors = listOf(Color(0xFF2B3A31), Color(0xFF161917)),
    radius = 1200f
)

private val DarkColorScheme = darkColorScheme(
    primary = SageGreen,
    secondary = SoftLavender,
    tertiary = SoftPeach,
    background = DarkCharcoal,
    surface = CardSurface,
    onPrimary = Color(0xFF1E3527),
    onSecondary = Color(0xFF2E223D),
    onTertiary = Color(0xFF3B281A),
    onBackground = CleanOffWhite,
    onSurface = CleanOffWhite,
    outline = BorderColor
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A7D60),
    secondary = Color(0xFF6C578A),
    tertiary = Color(0xFF8C5F40),
    background = Color(0xFFF3F7F4),
    surface = Color(0xFFE6EDE8),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF242A25),
    onSurface = Color(0xFF242A25),
    outline = Color(0xFFD1DDD4)
)

@Composable
fun CurrentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

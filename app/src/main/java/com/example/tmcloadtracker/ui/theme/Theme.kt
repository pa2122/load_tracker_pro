package com.example.tmcloadtracker.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SafetyAmber,
    onPrimary = MidnightSteel,
    primaryContainer = CharcoalCard,
    onPrimaryContainer = SafetyAmber,
    secondary = AmberGold,
    onSecondary = MidnightSteel,
    secondaryContainer = CharcoalCard,
    onSecondaryContainer = CloudWhite,
    tertiary = EmeraldGreen,
    onTertiary = CloudWhite,
    background = MidnightSteel,
    onBackground = CloudWhite,
    surface = CharcoalCard,
    onSurface = CloudWhite,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = CloudWhite,
    error = CrimsonRed,
    onError = CloudWhite
)

private val LightColorScheme = lightColorScheme(
    primary = MidnightSteel,
    onPrimary = CloudWhite,
    secondary = SafetyAmber,
    onSecondary = MidnightSteel,
    tertiary = EmeraldGreen,
    background = CloudWhite,
    surface = CloudWhite,
    onPrimaryContainer = MidnightSteel,
    onSecondaryContainer = MidnightSteel,
    onTertiary = MidnightSteel,
    onBackground = MidnightSteel,
    onSurface = MidnightSteel
)

@Composable
fun LoadTrackerProTheme(
    darkTheme: Boolean = true, // Default to v2.0 High-Contrast Night/Day Driver Theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

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
    primary = CobaltBlue,
    secondary = SkyBlue,
    tertiary = SlateGrey,
    background = SlateNavy,
    surface = SlateGrey,
    onPrimary = CloudWhite,
    onSecondary = SlateNavy,
    onTertiary = CloudWhite,
    onBackground = CloudWhite,
    onSurface = CloudWhite,
    error = ErrorRed,
    onError = CloudWhite
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltBlue,
    secondary = SlateGrey,
    tertiary = SkyBlue,
    background = CloudWhite,
    surface = CloudWhite,
    onPrimary = CloudWhite,
    onSecondary = CloudWhite,
    onTertiary = SlateNavy,
    onBackground = SlateNavy,
    onSurface = SlateNavy
)

@Composable
fun LoadTrackerProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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

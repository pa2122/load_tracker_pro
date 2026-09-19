package com.example.tmcloadtracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. Safety Amber Theme (Default v2.0)
private val SafetyAmberColorScheme = darkColorScheme(
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

// 2. Cobalt Blue Theme (Classic v1.x)
private val CobaltBlueColorScheme = darkColorScheme(
    primary = CobaltBlue,
    onPrimary = SlateNavy,
    primaryContainer = SlateGrey,
    onPrimaryContainer = SkyBlue,
    secondary = SkyBlue,
    onSecondary = SlateNavy,
    secondaryContainer = SlateGrey,
    onSecondaryContainer = CloudWhite,
    tertiary = EmeraldGreen,
    onTertiary = CloudWhite,
    background = SlateNavy,
    onBackground = CloudWhite,
    surface = SlateGrey,
    onSurface = CloudWhite,
    surfaceVariant = SlateGrey,
    onSurfaceVariant = CloudWhite,
    error = CrimsonRed,
    onError = CloudWhite
)

// 3. High-Vis Lime Theme (Daytime Driver)
private val HiVisLimeColorScheme = darkColorScheme(
    primary = NeonLime,
    onPrimary = DeepForest,
    primaryContainer = DarkMossCard,
    onPrimaryContainer = NeonLime,
    secondary = LimeGold,
    onSecondary = DeepForest,
    secondaryContainer = DarkMossCard,
    onSecondaryContainer = CloudWhite,
    tertiary = EmeraldGreen,
    onTertiary = CloudWhite,
    background = DeepForest,
    onBackground = CloudWhite,
    surface = DarkMossCard,
    onSurface = CloudWhite,
    surfaceVariant = DarkMossCard,
    onSurfaceVariant = CloudWhite,
    error = CrimsonRed,
    onError = CloudWhite
)

// 4. Night Vision Red Theme (2:00 AM Night Mode)
private val NightVisionRedColorScheme = darkColorScheme(
    primary = CrimsonRed,
    onPrimary = PitchBlack,
    primaryContainer = DarkMaroonCard,
    onPrimaryContainer = BrightRed,
    secondary = BrightRed,
    onSecondary = PitchBlack,
    secondaryContainer = DarkMaroonCard,
    onSecondaryContainer = CloudWhite,
    tertiary = BrightRed,
    onTertiary = CloudWhite,
    background = PitchBlack,
    onBackground = CloudWhite,
    surface = DarkMaroonCard,
    onSurface = CloudWhite,
    surfaceVariant = DarkMaroonCard,
    onSurfaceVariant = CloudWhite,
    error = BrightRed,
    onError = PitchBlack
)

@Composable
fun LoadTrackerProTheme(
    themePreset: String = "safety_amber",
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (themePreset) {
            "cobalt_blue" -> CobaltBlueColorScheme
            "hi_vis_lime" -> HiVisLimeColorScheme
            "night_vision_red" -> NightVisionRedColorScheme
            else -> SafetyAmberColorScheme
        }
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

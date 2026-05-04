package com.modih.mail.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ModihDarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = BgPrimary,
    primaryContainer = AccentGoldSubtle,
    onPrimaryContainer = AccentGoldLight,

    secondary = ProPurple,
    onSecondary = BgPrimary,
    secondaryContainer = DevPurpleSubtle,
    onSecondaryContainer = ProPurpleDim,

    tertiary = Success,
    onTertiary = BgPrimary,

    background = BgPrimary,
    onBackground = TextPrimary,

    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgCardLight,
    onSurfaceVariant = TextSecondary,

    outline = GlassBorder,
    outlineVariant = DividerColor,

    error = Danger,
    onError = BgPrimary,
)

@Composable
fun ModihMailTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            val window = activity?.window
            if (window != null) {
                window.statusBarColor = BgPrimary.toArgb()
                window.navigationBarColor = BgPrimary.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = ModihDarkColorScheme,
        typography = ModihTypography,
        content = content
    )
}

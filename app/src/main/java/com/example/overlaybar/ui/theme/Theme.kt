//// app/src/main/java/com/example/overlaybar/ui/theme/Theme.kt
//// Created 2026-04-22
//// theme module


/// Imports


package com.example.overlaybar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.overlaybar.data.THEME_AUTO
import com.example.overlaybar.data.resolve_dark_theme


/// Symbols


// --- ONEUI 8 DARK COLOR SCHEME ---
// Mirrors the deep-black neutral palette used across Galaxy devices in dark mode.
private val one_ui_dark_color_scheme = darkColorScheme(
    primary              = SamsungBlueDark,
    onPrimary            = OneUIDarkOnPrimary,
    primaryContainer     = OneUIDarkContainer,
    onPrimaryContainer   = OneUIDarkOnContainer,

    secondary            = OneUIDarkSecondary,
    onSecondary          = OneUIDarkOnPrimary,
    secondaryContainer   = OneUIDarkContainer,
    onSecondaryContainer = OneUIDarkOnContainer,

    tertiary             = OneUIDarkSecondary,
    onTertiary           = OneUIDarkOnPrimary,
    tertiaryContainer    = OneUIDarkContainer,
    onTertiaryContainer  = OneUIDarkOnContainer,

    background           = OneUIDarkBackground,
    onBackground         = OneUIDarkOnSurface,

    surface              = OneUIDarkSurface,
    onSurface            = OneUIDarkOnSurface,
    surfaceVariant       = OneUIDarkSurfaceVar,
    onSurfaceVariant     = OneUIDarkOnSurfaceVar,

    outline              = OneUIDarkOutline,
    outlineVariant       = OneUIDarkOutlineVar,

    error                = OneUIRed,
    onError              = OneUIDarkOnPrimary,
    errorContainer       = OneUIDarkContainer,
    onErrorContainer     = OneUIRed,
)

// --- ONEUI 8 LIGHT COLOR SCHEME ---
// Clean near-white surfaces with Samsung Blue accents, matching Galaxy light mode.
private val one_ui_light_color_scheme = lightColorScheme(
    primary              = SamsungBlue,
    onPrimary            = OneUILightOnPrimary,
    primaryContainer     = OneUILightContainer,
    onPrimaryContainer   = OneUILightOnContainer,

    secondary            = OneUILightSecondary,
    onSecondary          = OneUILightOnPrimary,
    secondaryContainer   = OneUILightContainer,
    onSecondaryContainer = OneUILightOnContainer,

    tertiary             = OneUILightSecondary,
    onTertiary           = OneUILightOnPrimary,
    tertiaryContainer    = OneUILightContainer,
    onTertiaryContainer  = OneUILightOnContainer,

    background           = OneUILightBackground,
    onBackground         = OneUILightOnSurface,

    surface              = OneUILightSurface,
    onSurface            = OneUILightOnSurface,
    surfaceVariant       = OneUILightSurfaceVar,
    onSurfaceVariant     = OneUILightOnSurfaceVar,

    outline              = OneUILightOutline,
    outlineVariant       = OneUILightOutlineVar,

    error                = OneUIRed,
    onError              = OneUILightOnPrimary,
    errorContainer       = OneUILightContainer,
    onErrorContainer     = OneUIRed,
)


/// Public API


// --- THEME PROVIDER ---
// On Android 12+ Samsung devices, dynamic color derives from the wallpaper through
// Samsung's own Material You engine — this naturally aligns with OneUI theming.
// On older devices, the explicit OneUI color schemes above are used as fallback.
@Composable
fun OverlaybarTheme(
    theme_mode: Int = THEME_AUTO,
    content: @Composable () -> Unit
) {
    val dark_theme = resolve_dark_theme(theme_mode, isSystemInDarkTheme())
    val color_scheme = if (dark_theme) one_ui_dark_color_scheme else one_ui_light_color_scheme

    MaterialTheme(
        colorScheme = color_scheme,
        typography  = Typography,
        content     = content
    )
} // OverlaybarTheme

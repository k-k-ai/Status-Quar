//// app/src/main/java/com/example/overlaybar/ui/theme/Color.kt
//// Created 2026-04-22
//// color module


/// Imports


package com.example.overlaybar.ui.theme

import androidx.compose.ui.graphics.Color


/// Colors


// --- ONEUI 8 / GLASS UI COLOR SYSTEM ---
// Aligned with Samsung One UI 8 design language and the Glass UI visual refresh
// targeting Galaxy S20+ / S24 Ultra devices.

// --- PRIMARY: MUTED SAMSUNG UTILITY BLUE ---
val SamsungBlue         = Color(0xFF7F98A8)
val SamsungBlueDark     = Color(0xFFA9BBC8)
val OneUILightSecondary = Color(0xFF8297A4)
val OneUIDarkSecondary  = Color(0xFFA9BBC8)

// --- DARK-THEME NEUTRAL SURFACES ---
val OneUIDarkBackground  = Color(0xFF000000)
val OneUIDarkSurface     = Color(0xFF191919)
val OneUIDarkSurfaceVar  = Color(0xFF242424)
val OneUIDarkOutline     = Color(0xFF303030)
val OneUIDarkOutlineVar  = Color(0xFF252525)

// --- LIGHT-THEME NEUTRAL SURFACES ---
val OneUILightBackground  = Color(0xFFEFF3F7)
val OneUILightSurface     = Color(0xFFF8F8FA)
val OneUILightSurfaceVar  = Color(0xFFECEFF3)
val OneUILightOutline     = Color(0xFFD8DDE2)
val OneUILightOutlineVar  = Color(0xFFE5E9EE)

// --- ON-COLOR TOKENS (CONTENT PLACED ON TOP OF SURFACES) ---
val OneUIDarkOnSurface    = Color(0xFFE8E8E8)  // Primary text on dark surfaces
val OneUIDarkOnSurfaceVar = Color(0xFF888888)  // Secondary text / captions
val OneUIDarkOnPrimary    = Color(0xFFFFFFFF)

val OneUILightOnSurface    = Color(0xFF1C1C1C)  // Primary text on light surfaces
val OneUILightOnSurfaceVar = Color(0xFF6D7680)  // Secondary text / captions
val OneUILightOnPrimary    = Color(0xFFFFFFFF)

// --- CONTAINER / CHIP COLORS ---
val OneUIDarkContainer     = Color(0xFF303941)
val OneUILightContainer    = Color(0xFFE2E8EE)
val OneUIDarkOnContainer   = Color(0xFFE9F0F4)
val OneUILightOnContainer  = Color(0xFF27323A)

// --- SAMSUNG ONE UI 8.5 COMPONENT TOKENS ---
val OneUIScreenBackgroundLight = Color(0xFFEFF3F7)
val OneUIScreenBackgroundDark = Color(0xFF000000)
val OneUICardLight = Color(0xFFF8F8FA)
val OneUICardDark = Color(0xFF191919)
val OneUIDockLight = Color(0xF4F5F7FA)
val OneUIFrostOverlay = Color(0x9CE9EEF5)
val OneUIAccentMutedBlue = Color(0xFF9CADB8)
val OneUIAccentMutedPink = Color(0xFFE8C8D4)
val OneUISwitchTrackOnLight = Color(0xFFA7BAC5)
val OneUISwitchTrackOffLight = Color(0xFFA8ABB0)
val OneUISwitchTrackOnDark = Color(0xFF7F98A8)
val OneUISwitchTrackOffDark = Color(0xFF4B4B4F)
val OneUISwitchThumb = Color(0xFFF8F9FA)

// --- GLASS UI OVERLAY TOKENS ---
// Used exclusively by BarUi.kt for the system-overlay status widgets.
// These approximate the frosted-glass aesthetic of OneUI 8.5 without requiring
// a RenderEffect blur pass (compatible with API 29+).
val GlassDarkFill    = Color(0x22000000)  // ~13 % black fill  (dark bg context)
val GlassLightFill   = Color(0x22FFFFFF)  // ~13 % white fill  (light bg context)
val GlassDarkBorder  = Color(0x3DFFFFFF)  // ~24 % white stroke (dark bg context)
val GlassLightBorder = Color(0x26000000)  // ~15 % black stroke (light bg context)

// --- SEMANTIC / STATUS COLORS ---
// OneUI 8 uses slightly different semantic hues than stock Material 3.
val OneUIGreen  = Color(0xFF12C479)  // Charging / success
val OneUIRed    = Color(0xFFFF3B30)  // Low-battery / error
val OneUIOrange = Color(0xFFFF9500)  // Mid-battery / warning

//// app/src/main/java/com/example/overlaybar/ui/theme/Type.kt
//// Created 2026-04-22
//// type module


/// Imports


package com.example.overlaybar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


/// Symbols


// --- ONEUI 8 TYPOGRAPHY SCALE ---
// Sized and weighted to match Samsung One UI's settings-app and system-UI type
// hierarchy. `FontFamily.Default` on Galaxy devices resolves to Samsung's own
// system typeface, giving authentic on-device rendering for free.
//
// Scale snapshot (OneUI reference):
//   headlineLarge  34 sp  — large collapsing page title (LargeTopAppBar)
//   headlineMedium 28 sp  — section headers / dialog titles
//   headlineSmall  24 sp  — sub-section headers
//   titleLarge     20 sp  — card / group titles
//   titleMedium    16 sp  — list-item primary label
//   titleSmall     14 sp  — list-item secondary label / subsection
//   bodyLarge      16 sp  — body copy
//   bodyMedium     14 sp  — body copy (secondary)
//   bodySmall      12 sp  — captions, helper text
//   labelLarge     14 sp  — button labels (medium weight)
//   labelMedium    12 sp  — chip / tab labels
//   labelSmall     11 sp  — section group headers (gray, all-caps optional)

val Typography = Typography(

    // --- DISPLAY / HERO ---
    displayLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Light,
        fontSize      = 57.sp,
        lineHeight    = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Light,
        fontSize      = 45.sp,
        lineHeight    = 52.sp,
        letterSpacing = (-0.15).sp
    ),
    displaySmall = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 36.sp,
        lineHeight    = 44.sp,
        letterSpacing = 0.sp
    ),

    // --- HEADLINE ---
    // headlineLarge drives the LargeTopAppBar collapsed/expanded title.
    headlineLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 34.sp,
        lineHeight    = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 28.sp,
        lineHeight    = 36.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = 0.sp
    ),

    // --- TITLE ---
    titleLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Medium,
        fontSize      = 20.sp,
        lineHeight    = 28.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Medium,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp
    ),

    // --- BODY ---
    bodyLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.sp
    ),

    // --- LABEL ---
    labelLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.sp
    ),
    // labelSmall is used for section group headers in SettingsScreen (gray, small).
    labelSmall = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.sp
    ),
)

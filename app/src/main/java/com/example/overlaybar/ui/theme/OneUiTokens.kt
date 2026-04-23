//// app/src/main/java/com/example/overlaybar/ui/theme/OneUiTokens.kt
//// Created 2026-04-22
//// oneuitokens module


/// Imports


package com.example.overlaybar.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp


/// Tokens


// --- ONE UI 8.5 DESIGN TOKENS ---
// Foundational spacing/radius scale used across the settings + editor surfaces.
//
// Rules this enforces:
//   • Spacing snaps to a 4 dp grid. No orphan values.
//   • Radii are restricted to a small whitelist so every card/chip/pill belongs
//     to the same family. New surfaces should reuse one of the shapes below
//     instead of declaring fresh RoundedCornerShape values inline.
//   • The accent color (Samsung blue) is reserved for selected/active states
//     and a small set of explicit emphasis points — most surfaces are neutral.
//
// Reference for the One UI 8.5 visual grammar this targets:
//   - large airy top regions; content begins lower than expected
//   - broad slab cards rather than tight individual rows
//   - chunky filled selected states (dock-style), not subtle tints
//   - blur/haze as background ambience, not decorative gradients on cards

object OneUi {

    // --- SPACING SCALE (4 DP GRID) ---
    val SpaceXS    = 4.dp
    val SpaceS     = 8.dp
    val SpaceM     = 12.dp
    val SpaceL     = 16.dp
    val SpaceXL    = 20.dp
    val Space2XL   = 24.dp
    val Space3XL   = 32.dp
    val Space4XL   = 48.dp

    // --- COMPOSED PADDINGS ---
    val ScreenHorizontalPadding = Space2XL        // 24 — Samsung settings gutters
    val ScreenTopBreath         = Space4XL        // 48 — large calm top region
    val ScreenBottomBreath      = Space4XL        // 48 — air below the last slab
    val CardOuterSpacing        = SpaceL          // 16 — gap between major slab cards
    val CardInternalPadding     = Space2XL        // 24 — inside a slab card
    val InnerSurfacePadding     = SpaceXL         // 20 — inside an inner editor surface
    val RowVerticalPadding      = SpaceXL         // 20 — rows inside cards
    val DividerInset            = Space2XL        // 24 — horizontal divider inset

    // --- RADII (SINGLE PILL FAMILY) ---
    val RadiusCard   = 32.dp                      // big slab cards
    val RadiusInner  = 16.dp                      // inner surfaces inside a card
    val RadiusChip   = 16.dp                      // chip / segment pills
    val RadiusSmall  = 10.dp                      // mini icon tiles
    val RadiusPill   = 999.dp                     // full capsule

    val ShapeCard  = RoundedCornerShape(RadiusCard)
    val ShapeInner = RoundedCornerShape(RadiusInner)
    val ShapeChip  = RoundedCornerShape(RadiusChip)
    val ShapeSmall = RoundedCornerShape(RadiusSmall)
    val ShapePill  = RoundedCornerShape(RadiusPill)

    // --- STROKES / DIVIDERS ---
    val DividerThickness = 0.5.dp
    val HairlineStroke   = 0.75.dp
} // OneUi

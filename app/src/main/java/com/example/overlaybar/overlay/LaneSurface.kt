//// app/src/main/java/com/example/overlaybar/overlay/LaneSurface.kt
//// Created 2026-04-22
//// lanesurface module


/// Imports


package com.example.overlaybar.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.overlaybar.data.OverlayElementId
import com.example.overlaybar.data.WEATHER_BACKDROP_CLOUDY
import com.example.overlaybar.data.WEATHER_BACKDROP_RAINY
import com.example.overlaybar.data.WEATHER_BACKDROP_SNOWY
import com.example.overlaybar.data.WEATHER_BACKDROP_SUNNY
import com.example.overlaybar.data.WEATHER_BACKDROP_THUNDER
import com.example.overlaybar.data.WEATHER_BACKDROP_WINDY


/// Types


internal data class OverlayLaneEntry(
    val id: OverlayElementId,
    val order: Int,
    val pillScale: Float,
    val offsetPx: Int,
    val customFill: Color?,
    val customStroke: Color?,
    val backdropType: Int = -1,
    val alpha: Float = 1f,
    val timerArcProgress: Float = -1f,
    val battery_level_slice: Float = -1f,
    val battery_level_value: Int? = null,
    val battery_charging: Boolean = false,
    val battery_full: Boolean = false,
    val onTap: (() -> Unit)? = null,
    val content: @Composable () -> Unit
)

internal val LANE_SHAPE = RoundedCornerShape(percent = 100)


/// Functions


@Composable
internal fun BoxScope.overlay_lane(
    elements: List<OverlayLaneEntry>,
    is_left_lane: Boolean,
    element_gap_dp: Int,
    lane_fill: Color,
    lane_border: Color,
    merged: Boolean,
    text_color: Color,
    vertical_offset_dp: Int,
    solarBrightness: Float,
    clearWayheadProgress: Float,
    onReportBounds: (OverlayElementId, Rect) -> Unit
) {
    if (elements.isEmpty()) return
    val density = LocalDensity.current
    val half_gap = (element_gap_dp / 2).dp
    
    Row(
        modifier = Modifier
            .align(if (is_left_lane) Alignment.CenterStart else Alignment.CenterEnd)
            .offset(y = vertical_offset_dp.dp)
            .then(
                if (merged) {
                    Modifier.lane_surface(
                        fill = elements.firstOrNull()?.customFill ?: lane_fill,
                        border = elements.firstOrNull()?.customStroke ?: lane_border,
                        pill_scale = elements.map { it.pillScale }.maxOrNull() ?: 1.0f,
                        solarBrightness = solarBrightness,
                        clearWayheadProgress = clearWayheadProgress
                    )
                } else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (merged) {
            elements.forEachIndexed { index, entry ->
                val mergedHitPadding = (10 * entry.pillScale).dp
                val mergedHitHeight = (24 * entry.pillScale).dp
                val entryAlpha by animateFloatAsState(
                    targetValue = entry.alpha,
                    animationSpec = snap(),
                    label = "merged_lane_entry_alpha"
                )
                if (index > 0) {
                    Spacer(Modifier.width(half_gap))
                    Box(modifier = Modifier.width(1.dp).height((12 * entry.pillScale).dp).background(text_color.copy(alpha = 0.18f)))
                    Spacer(Modifier.width(half_gap))
                }
                Box(
                    modifier = Modifier
                        .padding(
                            start = if (is_left_lane) with(density) { entry.offsetPx.toDp() } else 0.dp,
                            end = if (!is_left_lane) with(density) { entry.offsetPx.toDp() } else 0.dp
                        )
                        .height(mergedHitHeight)
                        .padding(horizontal = mergedHitPadding)
                        .graphicsLayer { alpha = entryAlpha }
                        .onGloballyPositioned { onReportBounds(entry.id, it.boundsInRoot()) }
                        .then(if (entry.onTap != null) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { entry.onTap() } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    val itemContent = entry.content
                    itemContent()
                }
            }
        } else {
            elements.forEach { entry ->
                val entryAlpha by animateFloatAsState(
                    targetValue = entry.alpha,
                    animationSpec = snap(),
                    label = "lane_entry_alpha"
                )
                Box(
                    modifier = Modifier
                        .padding(
                            start = if (is_left_lane) with(density) { entry.offsetPx.toDp() } else 0.dp,
                            end = if (!is_left_lane) with(density) { entry.offsetPx.toDp() } else 0.dp
                        )
                        .graphicsLayer { alpha = entryAlpha }
                        .then(
                            if (entry.timerArcProgress >= 0f)
                                Modifier.drawWithContent {
                                    drawContent()
                                    val progress = entry.timerArcProgress.coerceIn(0f, 1f)
                                    if (progress > 0f) {
                                        val strokeWidth = 2.5.dp.toPx()
                                        val inset = strokeWidth / 2f
                                        drawArc(
                                            color = Color(0x60FF3B3B),
                                            startAngle = -90f,
                                            sweepAngle = 360f * progress,
                                            useCenter = false,
                                            topLeft = Offset(inset, inset),
                                            size = Size(
                                                (size.width - strokeWidth).coerceAtLeast(0f),
                                                (size.height - strokeWidth).coerceAtLeast(0f)
                                            ),
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        )
                                    }
                                }
                            else Modifier
                        )
                        .lane_surface(
                            fill = entry.customFill ?: lane_fill,
                            border = entry.customStroke ?: lane_border,
                            pill_scale = entry.pillScale,
                            backdrop = entry.backdropType,
                            solarBrightness = solarBrightness,
                            clearWayheadProgress = clearWayheadProgress,
                            battery_level_slice = entry.battery_level_slice,
                            battery_level_value = entry.battery_level_value,
                            battery_charging = entry.battery_charging,
                            battery_full = entry.battery_full
                        )
                        .onGloballyPositioned { onReportBounds(entry.id, it.boundsInRoot()) }
                        .then(if (entry.onTap != null) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { entry.onTap() } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    val itemContent = entry.content
                    itemContent()
                }
                if (entry != elements.last()) Spacer(Modifier.width(element_gap_dp.dp))
            }
        }
    }
} // overlay_lane

internal fun Modifier.lane_surface(
    fill: Color,
    border: Color,
    pill_scale: Float,
    backdrop: Int = -1,
    solarBrightness: Float = 1f,
    clearWayheadProgress: Float = 1f,
    battery_level_slice: Float = -1f,
    battery_level_value: Int? = null,
    battery_charging: Boolean = false,
    battery_full: Boolean = false
): Modifier {
    val h_pad = (10 * pill_scale).dp
    val pill_height = (24 * pill_scale).dp
    return this
        .height(pill_height)
        .drawBehind {
            val r = size.height / 2f
            val path = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, r, r)) }
            drawPath(path, fill)
            if (backdrop != -1) {
                withTransform({ clipPath(path) }) {
                    when (backdrop) {
                        WEATHER_BACKDROP_CLOUDY  -> draw_cloud_bunches(clear_wayhead_progress = clearWayheadProgress)
                        WEATHER_BACKDROP_SUNNY   -> draw_sunny_glow(solarBrightness, clearWayheadProgress)
                        WEATHER_BACKDROP_RAINY   -> draw_rain_streaks()
                        WEATHER_BACKDROP_SNOWY   -> draw_snow_crystals()
                        WEATHER_BACKDROP_THUNDER -> draw_thunder_bolt()
                        WEATHER_BACKDROP_WINDY   -> draw_wind_streaks()
                    }
                }
            }
            if (border != Color.Transparent) {
                if (battery_level_slice in 0f..1f) {
                    val strokeWidth = 3.dp.toPx()
                    val inset = strokeWidth / 2f
                    val indicatorLevel = battery_level_value ?: (battery_level_slice * 100f).toInt()
                    val indicatorColor = battery_level_color(
                        level = indicatorLevel,
                        charging = battery_charging,
                        full = battery_full
                    )
                    val gradient = when {
                        battery_full || indicatorLevel >= 100 -> listOf(indicatorColor, indicatorColor)
                        battery_charging -> listOf(
                            Color(0xFF7EE7A7),
                            indicatorColor
                        )
                        else -> listOf(
                            Color(0xFF34C759),
                            Color(0xFFFFC857),
                            Color(0xFFFF453A)
                        )
                    }
                    drawPath(
                        path = path,
                        color = border.copy(alpha = 0.18f),
                        style = Stroke(width = strokeWidth)
                    )
                    clipPath(path) {
                        drawRect(
                            brush = Brush.horizontalGradient(gradient),
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width * battery_level_slice, size.height)
                        )
                        drawRect(
                            color = fill,
                            topLeft = Offset(size.width * battery_level_slice, 0f),
                            size = Size((size.width * (1f - battery_level_slice)).coerceAtLeast(0f), size.height)
                        )
                    }
                    drawRoundRect(
                        brush = Brush.horizontalGradient(gradient),
                        topLeft = Offset(inset, inset),
                        size = Size((size.width - strokeWidth).coerceAtLeast(0f), (size.height - strokeWidth).coerceAtLeast(0f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius((size.height - strokeWidth).coerceAtLeast(0f) / 2f),
                        style = Stroke(width = strokeWidth)
                    )
                    clipPath(path) {
                        drawRect(
                            color = fill,
                            topLeft = Offset(size.width * battery_level_slice, 0f),
                            size = Size((size.width * (1f - battery_level_slice)).coerceAtLeast(0f), size.height)
                        )
                    }
                } else {
                    drawPath(path, color = border, style = Stroke(width = 3.dp.toPx()))
                }
            }
        }
        .padding(horizontal = h_pad)
} // lane_surface


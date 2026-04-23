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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
                                    drawTimerArc(entry.timerArcProgress, 2.5.dp.toPx())
                                }
                            else Modifier
                        )
                        .lane_surface(
                            fill = entry.customFill ?: lane_fill,
                            border = entry.customStroke ?: lane_border,
                            pill_scale = entry.pillScale,
                            backdrop = entry.backdropType,
                            solarBrightness = solarBrightness,
                            clearWayheadProgress = clearWayheadProgress
                        )
                        .onGloballyPositioned { onReportBounds(entry.id, it.boundsInRoot()) }
                        .then(if (entry.onTap != null) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { entry.onTap.invoke() } else Modifier),
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
    clearWayheadProgress: Float = 1f
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
        }
        .then(if (border != Color.Transparent) Modifier.border(1.5.dp, border, LANE_SHAPE) else Modifier)
        .padding(horizontal = h_pad)
} // lane_surface

internal fun DrawScope.drawTimerArc(progress: Float, strokeWidthPx: Float) {
    if (progress <= 0f) return
    val w = size.width
    val h = size.height
    val r = h / 2f
    val path = Path().apply {
        moveTo(w / 2f, 0f)
        lineTo(r, 0f)
        arcTo(Rect(0f, 0f, 2f * r, h), startAngleDegrees = -90f, sweepAngleDegrees = -180f, forceMoveTo = false)
        lineTo(w - r, h)
        arcTo(Rect(w - 2f * r, 0f, w, h), startAngleDegrees = 90f, sweepAngleDegrees = -180f, forceMoveTo = false)
        lineTo(w / 2f, 0f)
    }
    val measure = PathMeasure()
    measure.setPath(path, false)
    val totalLen = measure.length
    if (totalLen <= 0f) return
    val seg = Path()
    measure.getSegment(0f, progress * totalLen, seg, true)
    drawPath(seg, Color(0x60FF3B3B), style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))
} // drawTimerArc

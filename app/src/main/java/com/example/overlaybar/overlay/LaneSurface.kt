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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
private val BATTERY_DEPLETED_STROKE = Color(0xFF7F8791)


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
                                        val outline = build_round_rect_outline_path(
                                            width = size.width,
                                            height = size.height,
                                            corner = size.height / 2f
                                        )
                                        draw_outline_progress_stroke(
                                            path = outline,
                                            color = Color(0x60FF3B3B),
                                            progress = progress,
                                            stroke_width = strokeWidth
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
            val fill_path = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, r, r)) }
            val outline_path = build_round_rect_outline_path(size.width, size.height, r)
            drawPath(fill_path, fill)
            if (backdrop != -1) {
                withTransform({ clipPath(fill_path) }) {
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
                    draw_battery_level_stroke(
                        width = size.width,
                        height = size.height,
                        corner = r,
                        active_color = border,
                        level_slice = battery_level_slice,
                        stroke_width = 3.dp.toPx()
                    )
                } else {
                    drawPath(outline_path, color = border, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
        .padding(horizontal = h_pad)
} // lane_surface

internal fun DrawScope.draw_battery_level_stroke(
    width: Float,
    height: Float,
    corner: Float,
    active_color: Color,
    level_slice: Float,
    stroke_width: Float,
    depleted_color: Color = BATTERY_DEPLETED_STROKE
) {
    val resolved_slice = level_slice.coerceIn(0f, 1f)
    val outline_path = build_round_rect_outline_path(width, height, corner)
    val stroke = Stroke(width = stroke_width, cap = StrokeCap.Round)
    if (resolved_slice >= 1f) {
        drawPath(outline_path, color = active_color, style = stroke)
        return
    }
    drawPath(outline_path, color = depleted_color, style = stroke)
    if (resolved_slice <= 0f) return
    val active_contour = build_left_level_contour_path(width, height, corner, resolved_slice)
    if (!active_contour.isEmpty) {
        drawPath(active_contour, color = active_color, style = stroke)
    }
}

internal fun DrawScope.draw_outline_progress_stroke(
    path: Path,
    color: Color,
    progress: Float,
    stroke_width: Float
) {
    val resolved_progress = progress.coerceIn(0f, 1f)
    if (resolved_progress <= 0f) return
    val measure = PathMeasure()
    measure.setPath(path, true)
    val length = measure.length
    if (length <= 0f) return
    val stop = length * resolved_progress
    val segment = Path()
    measure.getSegment(0f, stop, segment, true)
    drawPath(segment, color = color, style = Stroke(width = stroke_width, cap = StrokeCap.Round))
}

internal fun build_round_rect_outline_path(width: Float, height: Float, corner: Float): Path {
    if (width <= 0f || height <= 0f) return Path()
    val r = corner.coerceIn(0f, minOf(width, height) / 2f)
    val top_left = Rect(0f, 0f, 2f * r, 2f * r)
    val top_right = Rect(width - 2f * r, 0f, width, 2f * r)
    val bottom_right = Rect(width - 2f * r, height - 2f * r, width, height)
    val bottom_left = Rect(0f, height - 2f * r, 2f * r, height)
    return Path().apply {
        moveTo(width / 2f, 0f)
        lineTo(width - r, 0f)
        arcTo(top_right, 270f, 90f, false)
        lineTo(width, height - r)
        arcTo(bottom_right, 0f, 90f, false)
        lineTo(r, height)
        arcTo(bottom_left, 90f, 90f, false)
        lineTo(0f, r)
        arcTo(top_left, 180f, 90f, false)
        lineTo(width / 2f, 0f)
        close()
    }
}

private fun build_left_level_contour_path(width: Float, height: Float, corner: Float, level_slice: Float): Path {
    if (width <= 0f || height <= 0f) return Path()
    val resolved_slice = level_slice.coerceIn(0f, 1f)
    if (resolved_slice <= 0f) return Path()
    if (resolved_slice >= 1f) return build_round_rect_outline_path(width, height, corner)

    val r = corner.coerceIn(0f, minOf(width, height) / 2f)
    val cutoff = (width * resolved_slice).coerceIn(0f, width)
    val top_left = Rect(0f, 0f, 2f * r, 2f * r)
    val top_right = Rect(width - 2f * r, 0f, width, 2f * r)
    val bottom_right = Rect(width - 2f * r, height - 2f * r, width, height)
    val bottom_left = Rect(0f, height - 2f * r, 2f * r, height)

    return Path().apply {
        when {
            cutoff <= r -> {
                val normalized = ((cutoff - r) / r).coerceIn(-1f, 0f)
                val bottom_angle = Math.toDegrees(kotlin.math.acos(normalized.toDouble())).toFloat()
                val top_angle = 360f - bottom_angle
                arcTo(top_left, top_angle, 180f - top_angle, true)
                lineTo(0f, height - r)
                arcTo(bottom_left, 180f, bottom_angle - 180f, false)
            }

            cutoff < width - r -> {
                moveTo(cutoff, 0f)
                lineTo(r, 0f)
                arcTo(top_left, 270f, -90f, false)
                lineTo(0f, height - r)
                arcTo(bottom_left, 180f, -90f, false)
                lineTo(cutoff, height)
            }

            else -> {
                val normalized = ((cutoff - (width - r)) / r).coerceIn(0f, 1f)
                val bottom_angle = Math.toDegrees(kotlin.math.acos(normalized.toDouble())).toFloat()
                val top_angle = 360f - bottom_angle
                arcTo(top_right, top_angle, 270f - top_angle, true)
                lineTo(r, 0f)
                arcTo(top_left, 270f, -90f, false)
                lineTo(0f, height - r)
                arcTo(bottom_left, 180f, -90f, false)
                lineTo(width - r, height)
                arcTo(bottom_right, 90f, bottom_angle - 90f, false)
            }
        }
    }
}


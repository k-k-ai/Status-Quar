//// app/src/main/java/com/example/overlaybar/overlay/ExpandedBatteryCard.kt
//// Created 2026-04-22
//// expandedbatterycard module


/// Imports


package com.example.overlaybar.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val BATTERY_VISIBLE_INSIGHT_SLOTS = 4


/// Functions


@Composable
internal fun expanded_battery_card(
    battery_snapshot: BatterySnapshot,
    text_color: Color,
    font_family: FontFamily,
    font_scale: Float
) {
    val level_color = battery_level_color(battery_snapshot.level, battery_snapshot.charging, battery_snapshot.full)
    val dim = text_color.copy(alpha = 0.56f)
    var nowMillis by remember(
        battery_snapshot.updated_at_millis,
        battery_snapshot.current_ma,
        battery_snapshot.charging
    ) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(
        battery_snapshot.updated_at_millis,
        battery_snapshot.current_ma,
        battery_snapshot.charging,
        battery_snapshot.full
    ) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(if (battery_snapshot.current_ma != null) 5_000L else 15_000L)
        }
    }
    val resolved_remaining = battery_snapshot.projected_mah_remaining(nowMillis)
    val resolved_runtime_minutes = battery_snapshot.projected_runtime_minutes(nowMillis)
    val cycle_footer = remember(battery_snapshot.cycles, battery_snapshot.cycle_source) {
        buildString {
            battery_snapshot.cycles?.let { count ->
                append(format_cycle_count(count))
                battery_snapshot.cycle_source?.takeIf { it.isNotBlank() }?.let { source ->
                    append(" · ")
                    append(source)
                }
            } ?: run {
                append("Cycles unavailable")
                if (battery_snapshot.learned_sample_count > 0) {
                    append(" · ")
                    append("${battery_snapshot.learned_sample_count} health samples")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
    ) {
        // Header row: "Battery" label + charge source
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Battery",
                color = dim,
                fontSize = (11f * font_scale).sp,
                fontFamily = font_family,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )
            val plug = battery_snapshot.plug_label
            if (plug.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    plug_badge(plug, level_color)
                    Text(
                        plug,
                        color = level_color.copy(alpha = 0.85f),
                        fontSize = (11f * font_scale).sp,
                        fontFamily = font_family,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                }
            } else if (battery_snapshot.full) {
                Text(
                    "Charged",
                    color = level_color.copy(alpha = 0.85f),
                    fontSize = (11f * font_scale).sp,
                    fontFamily = font_family,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Hero row: big % + state
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${battery_snapshot.level}%",
                color = text_color,
                fontSize = (36f * font_scale).sp,
                fontFamily = font_family,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                battery_snapshot.state_label,
                color = level_color.copy(alpha = 0.92f),
                fontSize = (12.5f * font_scale).sp,
                fontFamily = font_family,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 5.dp),
                letterSpacing = 0.sp
            )
        }

        Spacer(Modifier.height(6.dp))

        // Level bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .background(text_color.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((battery_snapshot.level / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(level_color, RoundedCornerShape(4.dp))
            )
        }

        Spacer(Modifier.height(10.dp))

        // Primary stats row
        primary_stats_row(
            battery = battery_snapshot,
            resolved_remaining_mah = resolved_remaining,
            resolved_runtime_minutes = resolved_runtime_minutes,
            text_color = text_color,
            dim = dim,
            fontFamily = font_family,
            fontScale = font_scale
        )

        Spacer(Modifier.height(10.dp))

        // Insight sub-card with cycling fade
        insight_sub_card(battery_snapshot, text_color, dim, level_color, font_family, font_scale)

        Text(
            text = cycle_footer,
            color = dim,
            fontSize = (10f * font_scale).sp,
            fontFamily = font_family,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun primary_stats_row(
    battery: BatterySnapshot,
    resolved_remaining_mah: Int?,
    resolved_runtime_minutes: Int?,
    text_color: Color,
    dim: Color,
    fontFamily: FontFamily,
    fontScale: Float
    ) {
    val mah_text = resolved_remaining_mah?.let { "${format_mah(it)} mAh" } ?: "—"
    val rate_ma  = if (battery.charging) battery.charge_ma else battery.drain_ma
    val rate_text = when {
        battery.full -> "100%"
        rate_ma != null -> {
            val arrow = if (battery.charging) "↑" else "↓"
            "$arrow ${format_mah(rate_ma)}mA"
        }
        battery.session_percent_per_hour != null -> {
            val arrow = if (battery.charging) "↑" else "↓"
            "$arrow ${battery.session_percent_per_hour}%/h"
        }
        else -> "—"
    }
    val rate_label = when {
        battery.full -> "full"
        battery.charging -> "charging"
        else -> "draining"
    }
    val time_text = when {
        battery.full -> "Now"
        else -> resolved_runtime_minutes?.let { format_duration_minutes(it) } ?: "—"
    }
    val time_label = when {
        battery.full -> "charged"
        battery.charging -> "to full"
        else -> "remaining"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        battery_stat(
            value = mah_text,
            label = "remaining",
            text_color = text_color,
            dim = dim,
            fontFamily = fontFamily,
            fontScale = fontScale,
            modifier = Modifier.weight(1f)
        )
        stat_divider(dim)
        battery_stat(
            value = rate_text,
            label = rate_label,
            text_color = text_color,
            dim = dim,
            fontFamily = fontFamily,
            fontScale = fontScale,
            modifier = Modifier.weight(1f)
        )
        stat_divider(dim)
        battery_stat(
            value = time_text,
            label = time_label,
            text_color = text_color,
            dim = dim,
            fontFamily = fontFamily,
            fontScale = fontScale,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun insight_sub_card(
    battery: BatterySnapshot,
    text_color: Color,
    dim: Color,
    accent: Color,
    fontFamily: FontFamily,
    fontScale: Float
) {
    val insights = remember(battery) { compute_battery_insights(battery) }
    if (insights.isEmpty()) return

    val visible_insights = remember(insights) {
        mutableStateListOf<BatteryInsight>().apply {
            addAll(initial_visible_battery_insights(insights))
        }
    }
    var auto_cycle_slot by remember(insights) { mutableIntStateOf(0) }

    LaunchedEffect(insights) {
        if (insights.size <= BATTERY_VISIBLE_INSIGHT_SLOTS) return@LaunchedEffect
        while (true) {
            delay(4000L)
            val slot = auto_cycle_slot % visible_insights.size.coerceAtLeast(1)
            val replacement = pick_replacement_battery_insight(
                pool = insights,
                visible = visible_insights,
                replace_index = slot
            ) ?: continue
            visible_insights[slot] = replacement
            auto_cycle_slot = (slot + 1) % visible_insights.size
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(2) { row_index ->
            val start = row_index * 2
            val row_items = visible_insights.drop(start).take(2)
            if (row_items.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row_items.forEachIndexed { column_index, insight ->
                        val slotIndex = start + column_index
                        insight_panel(
                            insight = insight,
                            text_color = text_color,
                            dim = dim,
                            accent = accent,
                            fontFamily = fontFamily,
                            fontScale = fontScale,
                            modifier = Modifier.weight(1f),
                            onTap = if (insights.size > BATTERY_VISIBLE_INSIGHT_SLOTS) {
                                {
                                    pick_replacement_battery_insight(
                                        pool = insights,
                                        visible = visible_insights,
                                        replace_index = slotIndex
                                    )?.let { replacement ->
                                        visible_insights[slotIndex] = replacement
                                    }
                                }
                            } else null
                        )
                    }
                    repeat(2 - row_items.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (insights.size > BATTERY_VISIBLE_INSIGHT_SLOTS) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                dot_indicator(
                    count = visible_insights.size,
                    active = auto_cycle_slot % visible_insights.size.coerceAtLeast(1),
                    dim = dim,
                    accent = accent
                )
            }
        }
    }
}

@Composable
private fun insight_panel(
    insight: BatteryInsight,
    text_color: Color,
    dim: Color,
    accent: Color,
    fontFamily: FontFamily,
    fontScale: Float,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .drawBehind { draw_frosted_glass_noise(intensity = 0.42f) }
            .background(text_color.copy(alpha = 0.055f), RoundedCornerShape(12.dp))
            .then(
                if (onTap != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTap
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedContent(
            targetState = insight,
            transitionSpec = {
                (fadeIn(tween(520)) togetherWith fadeOut(tween(380)))
            },
            label = "battery_insight_fade"
        ) { current_insight ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                insight_graphic(current_insight, accent, dim)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        current_insight.headline,
                        color = text_color,
                        fontSize = (12f * fontScale).sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        letterSpacing = 0.sp
                    )
                    Text(
                        current_insight.label,
                        color = dim,
                        fontSize = (9f * fontScale).sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        letterSpacing = 0.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun insight_graphic(insight: BatteryInsight, accent: Color, dim: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val progress = insight.primary_fraction ?: 0.5f
            when (insight.kind) {
                InsightKind.CHARGED_IN -> draw_session_transfer(accent, progress, charging = true)
                InsightKind.DRAINED_OUT -> draw_session_transfer(accent, progress, charging = false)
                InsightKind.CAPACITY    -> draw_capacity_glyph(accent, progress)
                InsightKind.WARMTH      -> draw_thermometer(accent, progress)
                InsightKind.POWER_SAVE  -> draw_leaf(accent, insight.active)
                InsightKind.RATE        -> draw_pulse(accent, progress)
                InsightKind.LEVEL_DELTA -> draw_level_change(accent, progress)
                InsightKind.SPEED       -> draw_bolt(accent, progress)
            }
        }
    }
} // insight_graphic

@Composable
private fun dot_indicator(count: Int, active: Int, dim: Color, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until count.coerceAtMost(6)) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(
                        if (i == active) accent.copy(alpha = 0.85f) else dim.copy(alpha = 0.30f),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
} // dot_indicator

@Composable
private fun plug_badge(plug: String, accent: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(10.dp)) { draw_bolt(accent) }
    }
} // plug_badge

@Composable
private fun battery_stat(
    value: String,
    label: String,
    text_color: Color,
    dim: Color,
    fontFamily: FontFamily,
    fontScale: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            value,
            color = text_color,
            fontSize = (12f * fontScale).sp,
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            maxLines = 1
        )
        Text(
            label,
            color = dim,
            fontSize = (9.5f * fontScale).sp,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun stat_divider(color: Color) {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(28.dp)
            .background(color.copy(alpha = 0.28f))
    )
} // stat_divider

// --- Graphics ---

private fun DrawScope.draw_bolt(color: Color, progress: Float = 1f) {
    val w = size.width; val h = size.height
    val clamped = progress.coerceIn(0f, 1f)
    drawRoundRect(
        color = color.copy(alpha = 0.14f),
        topLeft = Offset(w * 0.12f, h * 0.76f),
        size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.10f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.06f)
    )
    drawRoundRect(
        color = color.copy(alpha = 0.45f),
        topLeft = Offset(w * 0.12f, h * 0.76f),
        size = androidx.compose.ui.geometry.Size(w * 0.76f * clamped, h * 0.10f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.06f)
    )
    val path = Path().apply {
        moveTo(w * 0.54f, 0f)
        lineTo(w * 0.18f, h * 0.56f)
        lineTo(w * 0.46f, h * 0.56f)
        lineTo(w * 0.36f, h)
        lineTo(w * 0.82f, h * 0.40f)
        lineTo(w * 0.54f, h * 0.40f)
        close()
    }
    drawPath(path, color)
} // draw_bolt

private fun DrawScope.draw_down_arrow(color: Color, progress: Float = 1f) {
    val w = size.width; val h = size.height
    drawRoundRect(
        color = color.copy(alpha = 0.14f),
        topLeft = Offset(w * 0.12f, h * 0.74f),
        size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.10f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.06f)
    )
    drawRoundRect(
        color = color.copy(alpha = 0.45f),
        topLeft = Offset(w * 0.12f, h * 0.74f),
        size = androidx.compose.ui.geometry.Size(w * 0.76f * progress.coerceIn(0f, 1f), h * 0.10f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.06f)
    )
    drawLine(color, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h * 0.85f), strokeWidth = h * 0.14f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.5f, h * 0.85f), Offset(w * 0.22f, h * 0.55f), strokeWidth = h * 0.14f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.5f, h * 0.85f), Offset(w * 0.78f, h * 0.55f), strokeWidth = h * 0.14f, cap = StrokeCap.Round)
} // draw_down_arrow

private fun DrawScope.draw_session_transfer(color: Color, progress: Float, charging: Boolean) {
    if (charging) draw_bolt(color, progress) else draw_down_arrow(color, progress)
}

private fun DrawScope.draw_capacity_glyph(color: Color, progress: Float) {
    val w = size.width; val h = size.height
    val body = Rect(w * 0.08f, h * 0.28f, w * 0.78f, h * 0.72f)
    drawRoundRect(
        color = color,
        topLeft = Offset(body.left, body.top),
        size = androidx.compose.ui.geometry.Size(body.width, body.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.12f),
        style = Stroke(width = h * 0.10f)
    )
    drawRoundRect(
        color = color.copy(alpha = 0.45f),
        topLeft = Offset(body.left + h * 0.10f, body.top + h * 0.10f),
        size = androidx.compose.ui.geometry.Size(
            (body.width - h * 0.24f) * progress.coerceIn(0f, 1f),
            body.height - h * 0.20f
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.06f)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.78f, h * 0.38f),
        size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.24f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.04f)
    )
} // draw_capacity_glyph

private fun DrawScope.draw_thermometer(color: Color, progress: Float) {
    val w = size.width; val h = size.height
    val stemX = w * 0.5f
    val fillTop = h * (0.74f - 0.40f * progress.coerceIn(0f, 1f))
    drawRoundRect(
        color = color,
        topLeft = Offset(stemX - w * 0.08f, h * 0.12f),
        size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.62f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
        style = Stroke(width = h * 0.10f)
    )
    drawCircle(color, radius = h * 0.16f, center = Offset(stemX, h * 0.82f))
    drawLine(color.copy(alpha = 0.22f), Offset(stemX, h * 0.22f), Offset(stemX, h * 0.74f), strokeWidth = h * 0.14f, cap = StrokeCap.Round)
    drawLine(color, Offset(stemX, fillTop), Offset(stemX, h * 0.74f), strokeWidth = h * 0.14f, cap = StrokeCap.Round)
} // draw_thermometer

private fun DrawScope.draw_leaf(color: Color, active: Boolean) {
    val w = size.width; val h = size.height
    val path = Path().apply {
        moveTo(w * 0.10f, h * 0.88f)
        cubicTo(w * 0.08f, h * 0.36f, w * 0.40f, h * 0.06f, w * 0.92f, h * 0.10f)
        cubicTo(w * 0.90f, h * 0.60f, w * 0.62f, h * 0.92f, w * 0.10f, h * 0.88f)
        close()
    }
    drawPath(path, if (active) color.copy(alpha = 0.92f) else color.copy(alpha = 0.28f))
    drawLine(color.copy(alpha = if (active) 0.7f else 0.4f), Offset(w * 0.12f, h * 0.86f), Offset(w * 0.86f, h * 0.14f), strokeWidth = h * 0.06f, cap = StrokeCap.Round)
    drawCircle(
        color = if (active) color else color.copy(alpha = 0.22f),
        radius = h * 0.08f,
        center = Offset(w * 0.84f, h * 0.22f)
    )
} // draw_leaf

private fun DrawScope.draw_pulse(color: Color, progress: Float) {
    val w = size.width; val h = size.height
    val amplitude = (0.14f + 0.26f * progress.coerceIn(0f, 1f)) * h
    val path = Path().apply {
        moveTo(0f, h * 0.5f)
        lineTo(w * 0.25f, h * 0.5f)
        lineTo(w * 0.35f, h * 0.5f - amplitude)
        lineTo(w * 0.50f, h * 0.5f + amplitude)
        lineTo(w * 0.65f, h * 0.5f)
        lineTo(w, h * 0.5f)
    }
    drawPath(path, color, style = Stroke(width = h * 0.14f, cap = StrokeCap.Round))
} // draw_pulse

private fun DrawScope.draw_level_change(color: Color, progress: Float) {
    val w = size.width; val h = size.height
    drawRoundRect(
        color = color.copy(alpha = 0.18f),
        topLeft = Offset(w * 0.10f, h * 0.28f),
        size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.16f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.10f, h * 0.28f),
        size = androidx.compose.ui.geometry.Size(w * 0.64f * progress.coerceIn(0f, 1f), h * 0.16f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
    )
    drawRoundRect(
        color = color.copy(alpha = 0.34f),
        topLeft = Offset(w * 0.18f, h * 0.56f),
        size = androidx.compose.ui.geometry.Size(w * 0.72f * progress.coerceIn(0.12f, 1f), h * 0.14f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
    )
} // draw_level_change

internal fun battery_level_color(level: Int, charging: Boolean, full: Boolean = false): Color = when {
    full || level >= 100 -> Color(0xFF4DA3FF)
    charging -> Color(0xFF34C759)
    else -> {
        val clamped = level.coerceIn(0, 100) / 100f
        when {
            clamped >= 0.60f -> lerp_color(
                Color(0xFF34C759),
                Color(0xFF4DA3FF),
                ((clamped - 0.60f) / 0.40f).coerceIn(0f, 1f)
            )
            clamped >= 0.25f -> lerp_color(
                Color(0xFFFFC857),
                Color(0xFF34C759),
                ((clamped - 0.25f) / 0.35f).coerceIn(0f, 1f)
            )
            else -> lerp_color(
                Color(0xFFFF453A),
                Color(0xFFFFC857),
                (clamped / 0.25f).coerceIn(0f, 1f)
            )
        }
    }
}


fun androidx.compose.ui.graphics.drawscope.DrawScope.draw_frosted_glass_noise(intensity: Float = 0.05f) { }

private fun initial_visible_battery_insights(pool: List<BatteryInsight>): List<BatteryInsight> =
    pool.take(BATTERY_VISIBLE_INSIGHT_SLOTS)

private fun pick_replacement_battery_insight(
    pool: List<BatteryInsight>,
    visible: List<BatteryInsight>,
    replace_index: Int
): BatteryInsight? {
    val current = visible.getOrNull(replace_index)
    val blocked = visible.filterIndexed { index, _ -> index != replace_index }.toSet()
    val candidates = pool.filter { it !in blocked && it != current }
    if (candidates.isEmpty()) return null
    return candidates[Random.nextInt(candidates.size)]
}

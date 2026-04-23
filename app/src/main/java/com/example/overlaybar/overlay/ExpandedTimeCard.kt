//// app/src/main/java/com/example/overlaybar/overlay/ExpandedTimeCard.kt
//// Created 2026-04-22
//// expandedtimecard module


/// Imports


package com.example.overlaybar.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay


/// Functions


@Composable
internal fun expanded_time_card(
    now_millis: Long,
    active_timer_end_millis: Long?,
    timer_set_at: Long?,
    weather_lat: Float,
    weather_lon: Float,
    text_color: Color,
    fontFamily: FontFamily,
    fontScale: Float,
    use_24_hour_time: Boolean,
    onAddEightMinuteTimer: () -> Unit,
    onSubtractEightMinuteTimer: () -> Unit,
    onAddTenMinuteTimer: () -> Unit,
    onSubtractTenMinuteTimer: () -> Unit
) {
    val sun_times = remember(now_millis, weather_lat, weather_lon) {
        compute_next_sun_events(weather_lat, weather_lon, now_millis)
    }
    val remaining_timer_millis = active_timer_end_millis?.let { (it - now_millis).coerceAtLeast(0L) }
    val timer_running = remaining_timer_millis != null && remaining_timer_millis > 0L

    // Stopwatch state — collected here since it's only needed in this card
    val sw_running by AccessibilityOverlayService.stopwatchRunningFlow.collectAsState()
    val sw_start   by AccessibilityOverlayService.stopwatchStartMillisFlow.collectAsState()
    val sw_accumulated by AccessibilityOverlayService.stopwatchAccumulatedMillisFlow.collectAsState()
    var sw_now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sw_running) {
        if (sw_running) {
            while (true) {
                sw_now = System.currentTimeMillis()
                delay(100L)
            }
        }
    }
    val sw_elapsed = sw_accumulated + if (sw_running && sw_start != null) (sw_now - sw_start!!) else 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // ── Sun section ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("Sun", color = text_color.copy(alpha = 0.72f), fontSize = (11f * fontScale).sp, fontFamily = fontFamily)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                time_info_column("Next Sunrise", sun_times?.sunrise, text_color, fontFamily, fontScale, use_24_hour_time, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                time_info_column("Next Sunset", sun_times?.sunset, text_color, fontFamily, fontScale, use_24_hour_time, Modifier.weight(1f))
            }
        }

        fading_section_divider(text_color)

        // ── Timer section ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 0.dp, top = 6.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val timer_start_label = if (timer_running && timer_set_at != null) format_clock_time(timer_set_at, use_24_hour_time) else ""
            val timer_end_label = active_timer_end_millis
                ?.takeIf { timer_running }
                ?.let { format_clock_time(it, use_24_hour_time) }
                .orEmpty()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Timer", color = text_color.copy(alpha = 0.72f), fontSize = (11f * fontScale).sp, fontFamily = fontFamily)
                Spacer(Modifier.weight(1f))
                if (timer_start_label.isNotEmpty()) {
                    Text(
                        timer_start_label,
                        color = text_color.copy(alpha = 0.45f),
                        fontSize = (10f * fontScale).sp,
                        fontFamily = fontFamily,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    if (timer_running) format_timer_clock(remaining_timer_millis ?: 0L) else "00:00",
                    color = text_color,
                    fontSize = (17f * fontScale).sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier.padding(start = 6.dp).width(34.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (timer_end_label.isNotEmpty()) {
                        Text(
                            timer_end_label,
                            color = text_color.copy(alpha = 0.45f),
                            fontSize = (10f * fontScale).sp,
                            fontFamily = fontFamily,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("8 min", color = text_color, fontSize = (18f * fontScale).sp, fontFamily = fontFamily, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    timer_icon_button("-", text_color, fontFamily, fontScale, onSubtractEightMinuteTimer)
                    timer_icon_button("+", text_color, fontFamily, fontScale, onAddEightMinuteTimer)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("10 min", color = text_color, fontSize = (18f * fontScale).sp, fontFamily = fontFamily, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    timer_icon_button("-", text_color, fontFamily, fontScale, onSubtractTenMinuteTimer)
                    timer_icon_button("+", text_color, fontFamily, fontScale, onAddTenMinuteTimer)
                }
            }
        }

        fading_section_divider(text_color)

        // ── Stopwatch section ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 0.dp, top = 4.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stopwatch", color = text_color.copy(alpha = 0.72f), fontSize = (11f * fontScale).sp, fontFamily = fontFamily)
                Spacer(Modifier.weight(1f))
                Text(
                    format_stopwatch(sw_elapsed),
                    color = text_color,
                    fontSize = (17f * fontScale).sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Reset (only when stopped with accumulated time)
                Box(modifier = Modifier.weight(1f)) {
                    if (!sw_running && sw_elapsed > 0L) {
                        stopwatch_action_button("Reset", text_color, fontFamily, fontScale) { stopwatch_reset() }
                    }
                }
                stopwatch_action_button(
                    label = if (sw_running) "Stop" else "Start",
                    text_color = text_color,
                    fontFamily = fontFamily,
                    fontScale = fontScale
                ) { if (sw_running) stopwatch_pause() else stopwatch_start() }
            }
        }
    }
}

private fun stopwatch_start() {
    AccessibilityOverlayService.stopwatchStartMillisFlow.value = System.currentTimeMillis()
    AccessibilityOverlayService.stopwatchRunningFlow.value = true
} // stopwatch_start

private fun stopwatch_pause() {
    val start = AccessibilityOverlayService.stopwatchStartMillisFlow.value ?: return
    AccessibilityOverlayService.stopwatchAccumulatedMillisFlow.value += (System.currentTimeMillis() - start)
    AccessibilityOverlayService.stopwatchRunningFlow.value = false
} // stopwatch_pause

private fun stopwatch_reset() {
    AccessibilityOverlayService.stopwatchRunningFlow.value = false
    AccessibilityOverlayService.stopwatchStartMillisFlow.value = null
    AccessibilityOverlayService.stopwatchAccumulatedMillisFlow.value = 0L
} // stopwatch_reset

@Composable
private fun fading_section_divider(text_color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to text_color.copy(alpha = 0f),
                            0.14f to text_color.copy(alpha = 0.08f),
                            0.30f to text_color.copy(alpha = 0.24f),
                            0.50f to text_color.copy(alpha = 0.38f),
                            0.70f to text_color.copy(alpha = 0.24f),
                            0.86f to text_color.copy(alpha = 0.08f),
                            1.0f to text_color.copy(alpha = 0f)
                        )
                    )
                )
            }
    )
} // fading_section_divider

@Composable
private fun time_info_column(
    label: String,
    event: SunEvent?,
    text_color: Color,
    fontFamily: FontFamily,
    fontScale: Float,
    use_24_hour_time: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = text_color.copy(alpha = 0.70f), fontSize = (10f * fontScale).sp, fontFamily = fontFamily)
        Text(
            event?.time_millis?.let { format_clock_time(it, use_24_hour_time) } ?: "--",
            color = text_color,
            fontSize = (15f * fontScale).sp,
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun timer_icon_button(
    label: String,
    text_color: Color,
    fontFamily: FontFamily,
    fontScale: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(text_color.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                .border(1.dp, text_color.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                .size(width = 28.dp, height = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = text_color, fontSize = (14f * fontScale).sp, fontFamily = fontFamily, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun stopwatch_action_button(
    label: String,
    text_color: Color,
    fontFamily: FontFamily,
    fontScale: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(76.dp)
            .height(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(text_color.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                .border(1.dp, text_color.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = text_color, fontSize = (13f * fontScale).sp, fontFamily = fontFamily, fontWeight = FontWeight.Medium)
        }
    }
}

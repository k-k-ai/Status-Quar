//// app/src/main/java/com/example/overlaybar/overlay/WeatherCards.kt
//// Created 2026-04-22
//// weathercards module


/// Imports


package com.example.overlaybar.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.overlaybar.data.ElementSettings
import com.example.overlaybar.data.WEATHER_BACKDROP_SUNNY
import com.example.overlaybar.data.WEIGHT_BOLD
import com.example.overlaybar.data.WEIGHT_NORMAL
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay


/// Functions


internal fun compact_weather_wind_label(wind_mph: Float, gust_mph: Float?, is_refreshing: Boolean): String {
    if (wind_mph <= 0f) return if (is_refreshing) "..." else "--"
    val steady = wind_mph.toInt()
    val gust = gust_mph?.takeIf { it > wind_mph + 1f }?.toInt()
    return if (gust != null) "$steady↑${gust - steady}" else "${steady}mph"
} // compact_weather_wind_label

private fun should_marquee_weather_label(label: String): Boolean =
    label.length > 12 || label.contains('&') || label.contains(' ')

private fun live_weather_condition_label(code: Int, wind_mph: Float, gust_mph: Float?, is_refreshing: Boolean): String {
    if (is_refreshing && wind_mph <= 0f) return "Updating..."
    return weather_short_label(code, wind_mph.takeIf { it > 0f }, gust_mph)
} // live_weather_condition_label

@Composable
private fun remember_live_condition_pulse(
    enabled: Boolean,
    update_token: Long,
    interval_millis: Long = 60_000L,
    visible_millis: Long = 3_000L
): Boolean {
    var show_pulse by remember(enabled, update_token) { mutableStateOf(false) }

    LaunchedEffect(enabled, update_token) {
        show_pulse = false
        if (!enabled) return@LaunchedEffect
        if (update_token > 0L) {
            show_pulse = true
            delay(visible_millis)
            show_pulse = false
        }
        while (true) {
            delay(interval_millis)
            show_pulse = true
            delay(visible_millis)
            show_pulse = false
        }
    }

    return show_pulse
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun animated_weather_condition_text(
    label: String,
    color: Color,
    fontSizeSp: Float,
    fontWeight: FontWeight,
    fontFamily: FontFamily,
    fontStyle: FontStyle,
    maxWidth: Dp,
    fixed_width: Dp? = null,
    textAlign: TextAlign = TextAlign.Start,
    alpha: Float = 1f
) {
    AnimatedContent(
        targetState = label,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 320, delayMillis = 80)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 260))
        },
        label = "animated_weather_condition_text"
    ) { current_label ->
        Text(
            current_label,
            modifier = Modifier
                .then(
                    if (fixed_width != null) Modifier.width(fixed_width) else Modifier.widthIn(min = 44.dp, max = maxWidth)
                )
                .then(
                    if (should_marquee_weather_label(current_label)) {
                        Modifier.basicMarquee(
                            initialDelayMillis = 900,
                            repeatDelayMillis = 1200
                        )
                    } else {
                        Modifier
                    }
                ),
            color = color.copy(alpha = alpha),
            fontSize = fontSizeSp.sp,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            fontStyle = fontStyle,
            textAlign = textAlign,
            letterSpacing = 0.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun compact_live_weather_content(
    temp_label: String,
    wind_label: String,
    condition_label: String,
    show_condition_pulse: Boolean,
    color: Color,
    temp_fontSize_sp: Float,
    detail_fontSize_sp: Float,
    condition_fontSize_sp: Float,
    fontWeight: FontWeight,
    fontFamily: FontFamily,
    fontStyle: FontStyle,
    alpha: Float = 0.9f
) {
    val base_alpha by animateFloatAsState(
        targetValue = if (show_condition_pulse) 0f else 1f,
        animationSpec = tween(durationMillis = 280),
        label = "compact_live_weather_base_alpha"
    )
    val condition_alpha by animateFloatAsState(
        targetValue = if (show_condition_pulse) 1f else 0f,
        animationSpec = tween(durationMillis = 340),
        label = "compact_live_weather_condition_alpha"
    )

    Layout(
        content = {
            Row(
                modifier = Modifier.graphicsLayer { this.alpha = base_alpha },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    temp_label,
                    color = color,
                    fontSize = temp_fontSize_sp.sp,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    fontStyle = fontStyle,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
                Text(
                    wind_label,
                    color = color.copy(alpha = alpha),
                    fontSize = detail_fontSize_sp.sp,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    fontStyle = fontStyle,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
            Text(
                condition_label,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (should_marquee_weather_label(condition_label)) {
                            Modifier.basicMarquee(
                                initialDelayMillis = 900,
                                repeatDelayMillis = 1200
                            )
                        } else {
                            Modifier
                        }
                    )
                    .graphicsLayer { this.alpha = condition_alpha },
                color = color.copy(alpha = alpha),
                fontSize = condition_fontSize_sp.sp,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                fontStyle = fontStyle,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    ) { measurables, constraints ->
        val base_placeable = measurables[0].measure(constraints)
        val condition_placeable = measurables[1].measure(
            constraints.copy(
                minWidth = base_placeable.width,
                maxWidth = base_placeable.width
            )
        )
        val width = base_placeable.width
        val height = maxOf(base_placeable.height, condition_placeable.height)
        layout(width, height) {
            base_placeable.placeRelative(0, (height - base_placeable.height) / 2)
            condition_placeable.placeRelative(0, (height - condition_placeable.height) / 2)
        }
    }
}

internal fun expanded_weather_wind_label(wind_mph: Float?, gust_mph: Float?): String {
    val steady_mph = wind_mph?.takeIf { it > 0f } ?: return "--"
    val steady = steady_mph.toInt()
    val gust = gust_mph?.takeIf { it > steady_mph + 1f }?.toInt()
    return if (gust != null) "${steady}mph gusts ${gust}mph" else "${steady}mph"
} // expanded_weather_wind_label

internal fun weather_location_debug_line(lat: Float, lon: Float): String? {
    if (lat == 0f && lon == 0f) return null
    return String.format(Locale.US, "%.2f, %.2f", lat, lon)
} // weather_location_debug_line

@Composable
internal fun expanded_gif_card(uri: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .clip(RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
} // expanded_gif_card

@Composable
internal fun weather_module(
    settings: ElementSettings,
    mode: Int,
    color: Color,
    scale: Float,
    family: FontFamily,
    temp_f: Float,
    wind_mph: Float,
    gust_mph: Float?,
    condition_code: Int,
    is_refreshing: Boolean,
    weather_updated_at: Long
) {
    val weight = if (settings.weight == WEIGHT_NORMAL) FontWeight.Normal else if (settings.weight == WEIGHT_BOLD) FontWeight.Bold else FontWeight.Black
    val style = if (settings.italic) FontStyle.Italic else FontStyle.Normal
    val temp_str = when {
        temp_f > 0f -> "${temp_f.toInt()}°"
        is_refreshing -> "..."
        else -> "--°"
    }
    val wind_str = compact_weather_wind_label(wind_mph, gust_mph, is_refreshing)
    val condition_label = live_weather_condition_label(condition_code, wind_mph, gust_mph, is_refreshing)
    val should_pulse_condition = false
    val show_condition_pulse = remember_live_condition_pulse(
        enabled = should_pulse_condition && condition_label.isNotBlank(),
        update_token = weather_updated_at
    )
    Box(modifier = Modifier.testTag("overlay_weather").graphicsLayer { scaleX = settings.sizeScale; scaleY = settings.sizeScale }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            when (mode) {
                0 -> {
                    compact_live_weather_content(
                        temp_label = temp_str,
                        wind_label = wind_str,
                        condition_label = condition_label,
                        show_condition_pulse = show_condition_pulse,
                        color = color,
                        temp_fontSize_sp = 12f * scale,
                        detail_fontSize_sp = 11f * scale,
                        condition_fontSize_sp = 11.5f * scale,
                        fontWeight = weight,
                        fontFamily = family,
                        fontStyle = style,
                        alpha = 0.9f
                    )
                }
                1 -> Text(temp_str, color = color, fontSize = (12f * scale).sp, fontWeight = weight, fontFamily = family, fontStyle = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                2 -> {
                    compact_live_weather_content(
                        temp_label = temp_str,
                        wind_label = wind_str,
                        condition_label = condition_label,
                        show_condition_pulse = show_condition_pulse,
                        color = color,
                        temp_fontSize_sp = 12f * scale,
                        detail_fontSize_sp = 11f * scale,
                        condition_fontSize_sp = 11.5f * scale,
                        fontWeight = weight,
                        fontFamily = family,
                        fontStyle = style,
                        alpha = 0.9f
                    )
                }
                3 -> {
                    animated_weather_condition_text(
                        label = condition_label,
                        color = color,
                        fontSizeSp = 11.5f * scale,
                        fontWeight = weight,
                        fontFamily = family,
                        fontStyle = style,
                        maxWidth = 136.dp,
                        alpha = 1f
                    )
                }
            }
        }
    }
}

@Composable
internal fun hourly_weather_flare(
    code: Int,
    display_hour: Int,
    wind_mph: Float?,
    weather_lat: Float,
    weather_lon: Float,
    day_of_year: Int,
    modifier: Modifier = Modifier
) {
    val backdrop = resolve_backdrop_from_code(code, wind_mph ?: 0f, display_hour)
    val solar_brightness = if (weather_lat != 0f || weather_lon != 0f) {
        solar_brightness(weather_lat, weather_lon, display_hour, 0, day_of_year)
    } else if (display_hour in 6..18) {
        1f
    } else {
        0.15f
    }
    val flare_progress = if (backdrop == WEATHER_BACKDROP_SUNNY) 0.92f else 0.5f
    val coverage = wmo_cloud_coverage(code)
    Box(
        modifier = modifier
            .weather_flare_surface(
                backdrop = backdrop,
                solar_brightness = solar_brightness,
                clear_wayhead_progress = flare_progress,
                cloud_coverage = coverage,
                fog_overlay_alpha = fog_overlay_alpha(code, solar_brightness),
                wind_overlay_strength = wind_overlay_strength(wind_mph ?: 0f)
            )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun hourly_forecast_card(
    temps: List<Float>,
    codes: List<Int>,
    winds: List<Float>,
    current_hour: Int,
    current_wind_mph: Float,
    gust_mph: Float?,
    weather_lat: Float,
    weather_lon: Float,
    text_color: Color,
    fontFamily: FontFamily,
    fontScale: Float,
    use_24_hour: Boolean,
    is_refreshing: Boolean,
    location_source: String,
    fetched_at_millis: Long,
    now_millis: Long
) {
    if (temps.isEmpty()) return
    val rows = (0 until minOf(temps.size, codes.size))
        .take(5)
    val day_of_year = Calendar.getInstance().apply { timeInMillis = now_millis }.get(Calendar.DAY_OF_YEAR)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .clip(RectangleShape),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    weather_status_line(location_source, fetched_at_millis, now_millis, is_refreshing),
                    color = text_color.copy(alpha = 0.72f),
                    fontSize = (10f * fontScale).sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
                weather_location_debug_line(weather_lat, weather_lon)?.let { coords ->
                    Text(
                        coords,
                        color = text_color.copy(alpha = 0.5f),
                        fontSize = (9f * fontScale).sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                }
            }
        }
        items(
            items = rows.toList(),
            key = { offset ->
                val display_hour = (current_hour + offset) % 24
                val code = codes.getOrElse(offset) { 0 }
                val wind = winds.getOrNull(offset)?.toInt() ?: -1
                "$display_hour:$code:$wind"
            }
        ) { offset ->
            val display_hour = (current_hour + offset) % 24
            val is_current = offset == 0
            val code = codes.getOrElse(offset) { 0 }
            val wind_mph = winds.getOrNull(offset)
            val current_row_wind = if (is_current) {
                current_wind_mph.takeIf { it > 0f } ?: wind_mph?.takeIf { it > 0f }
            } else {
                wind_mph?.takeIf { it > 0f }
            }
            val current_gust_mph = gust_mph?.takeIf { is_current && current_row_wind != null && it > current_row_wind + 1f }
            val show_current_gust = current_gust_mph != null
            val current_wind_text = current_row_wind?.let { "${it.toInt()}mph" } ?: "--"
            val hourly_label = weather_short_label(
                code,
                if (is_current) current_row_wind ?: wind_mph else wind_mph,
                if (is_current) current_gust_mph else null
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (is_current) Modifier.background(text_color.copy(alpha = 0.12f), RoundedCornerShape(16.dp)) else Modifier)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    hour_label(display_hour, use_24_hour),
                    modifier = Modifier.width(52.dp),
                    color = text_color.copy(alpha = if (is_current) 1f else 0.7f),
                    fontSize = (12f * fontScale).sp,
                    fontFamily = fontFamily,
                    fontWeight = if (is_current) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 0.sp
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    hourly_weather_flare(
                        code = code,
                        display_hour = display_hour,
                        wind_mph = wind_mph,
                        weather_lat = weather_lat,
                        weather_lon = weather_lon,
                        day_of_year = day_of_year,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        hourly_label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .then(
                                if (should_marquee_weather_label(hourly_label)) {
                                    Modifier.basicMarquee(
                                        initialDelayMillis = 900,
                                        repeatDelayMillis = 1200
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        color = text_color.copy(alpha = if (is_current) 1f else 0.8f),
                        fontSize = (11f * fontScale).sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }

                Box(
                    modifier = Modifier.width(84.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (show_current_gust) {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                current_wind_text,
                                color = text_color.copy(alpha = 1f),
                                fontSize = (11f * fontScale).sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = fontFamily,
                                letterSpacing = 0.sp
                            )
                            Text(
                                "gusts ${current_gust_mph?.toInt()}mph",
                                color = text_color.copy(alpha = 0.66f),
                                fontSize = (9f * fontScale).sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = fontFamily,
                                letterSpacing = 0.sp
                            )
                        }
                    } else {
                        Text(
                            expanded_weather_wind_label(current_row_wind, null),
                            color = text_color.copy(alpha = if (is_current) 1f else 0.75f),
                            fontSize = (11f * fontScale).sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = fontFamily,
                            textAlign = TextAlign.End,
                            letterSpacing = 0.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    "${temps.getOrElse(offset) { 0f }.toInt()}°",
                    modifier = Modifier.width(40.dp),
                    color = text_color.copy(alpha = if (is_current) 1f else 0.85f),
                    fontSize = (14f * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.End,
                    letterSpacing = 0.sp
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "Weather Debugging",
                    color = text_color.copy(alpha = 0.5f),
                    fontSize = (9f * fontScale).sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
                Text(
                    "temps: ${temps.joinToString()}",
                    color = text_color.copy(alpha = 0.4f),
                    fontSize = (8f * fontScale).sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.sp
                )
                Text(
                    "codes: ${codes.joinToString()}",
                    color = text_color.copy(alpha = 0.4f),
                    fontSize = (8f * fontScale).sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.sp
                )
                Text(
                    "winds: ${winds.joinToString()}",
                    color = text_color.copy(alpha = 0.4f),
                    fontSize = (8f * fontScale).sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.sp
                )
                Text(
                    "currWind: $current_wind_mph | gust: $gust_mph",
                    color = text_color.copy(alpha = 0.4f),
                    fontSize = (8f * fontScale).sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

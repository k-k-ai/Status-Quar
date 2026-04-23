//// app/src/main/java/com/example/overlaybar/overlay/WeatherLabels.kt
//// Created 2026-04-22
//// weatherlabels module


/// Imports


package com.example.overlaybar.overlay

import com.example.overlaybar.data.WEATHER_LOCATION_SOURCE_CACHE
import com.example.overlaybar.data.WEATHER_LOCATION_SOURCE_DEVICE
import com.example.overlaybar.data.WEATHER_LOCATION_SOURCE_LOCATION_OFF
import com.example.overlaybar.data.WEATHER_LOCATION_SOURCE_PERMISSION
import com.example.overlaybar.data.WEATHER_LOCATION_SOURCE_UNAVAILABLE
import com.example.overlaybar.data.WEATHER_LOCATION_SOURCE_ZIP
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


/// Functions


internal fun hour_label(hour: Int, use_24_hour: Boolean): String {
    if (use_24_hour) return String.format("%02d:00", hour)
    return when (hour) {
        0 -> "12AM"; 12 -> "12PM"
        in 1..11 -> "${hour}AM"
        else -> "${hour - 12}PM"
    }
} // hour_label

internal fun weather_short_label(code: Int, wind_mph: Float? = null, gust_mph: Float? = null): String {
    val base = base_weather_label(code)
    val modifiers = buildList {
        if (is_fog_code(code)) add("Foggy")
        wind_descriptor(wind_mph, gust_mph)?.takeIf { code <= 3 }?.let { add(it) }
    }
    return if (modifiers.isEmpty()) base else "$base & ${modifiers.joinToString(" & ")}"
}

internal fun wind_descriptor(wind_mph: Float?, gust_mph: Float? = null): String? {
    val w = wind_mph ?: return null
    if (w < 8f) return null
    val spread = (gust_mph ?: w) - w
    return when {
        w >= 25f -> "Blustery"
        spread >= 10f -> "Gusty"
        w >= 15f -> "Windy"
        else -> "Breezy"
    }
}

internal fun base_weather_label(code: Int): String = when (code) {
    0 -> "Clear"
    1 -> "Sparsely Cloudy"
    2 -> "Partly Cloudy"
    3, 45, 48 -> "Overcast"
    51, 53, 55 -> "Drizzle"
    61, 63, 65 -> "Rainy"
    71, 73, 75 -> "Snowy"
    77 -> "Sleet"
    80, 81, 82 -> "Showers"
    85, 86 -> "Flurries"
    95 -> "Stormy"
    96, 99 -> "Hail"
    else -> "Cloudy"
}

internal fun weather_status_line(source: String, fetched_at_millis: Long, now_millis: Long, is_refreshing: Boolean): String {
    val sourceLabel = weather_source_label(source)
    return when {
        is_refreshing -> "Updating $sourceLabel..."
        fetched_at_millis <= 0L -> "$sourceLabel pending"
        else -> "$sourceLabel • ${relative_age_label(now_millis - fetched_at_millis)}"
    }
} // weather_status_line

internal fun weather_source_label(source: String): String = when (source) {
    WEATHER_LOCATION_SOURCE_DEVICE -> "System"
    WEATHER_LOCATION_SOURCE_CACHE -> "Cached system"
    WEATHER_LOCATION_SOURCE_ZIP -> "ZIP"
    WEATHER_LOCATION_SOURCE_PERMISSION -> "Need location"
    WEATHER_LOCATION_SOURCE_LOCATION_OFF -> "Location off"
    WEATHER_LOCATION_SOURCE_UNAVAILABLE -> "No fix yet"
    else -> "Weather"
}

internal fun relative_age_label(age_millis: Long): String {
    val age_seconds = (age_millis / 1000L).coerceAtLeast(0L)
    return when {
        age_seconds < 10L -> "just now"
        age_seconds < 60L -> "${age_seconds}s ago"
        age_seconds < 3600L -> "${age_seconds / 60L}m ago"
        else -> "${age_seconds / 3600L}h ago"
    }
} // relative_age_label

internal fun parse_hourly_floats(s: String): List<Float> =
    if (s.isBlank()) emptyList()
    else s.split(",").mapNotNull { it.trim().toFloatOrNull() }

internal fun parse_hourly_ints(s: String): List<Int> =
    if (s.isBlank()) emptyList()
    else s.split(",").mapNotNull { it.trim().toIntOrNull() }

internal fun next_clock_tick_delay_ms(now_millis: Long, high_frequency: Boolean): Long {
    val tick_size_ms = if (high_frequency) 1_000L else 60_000L
    return (tick_size_ms - (now_millis % tick_size_ms)).coerceAtLeast(1L)
} // next_clock_tick_delay_ms

internal fun get_current_time(now_millis: Long, use_24_hour: Boolean): String {
    val pattern = if (use_24_hour) "HH:mm" else "h:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(now_millis))
} // get_current_time

internal fun get_current_date(now_millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = now_millis }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val suffix = when { day in 11..13 -> "th"; day % 10 == 1 -> "st"; day % 10 == 2 -> "nd"; day % 10 == 3 -> "rd"; else -> "th" }
    return "${SimpleDateFormat("EEE, MMM", Locale.getDefault()).format(cal.time)} $day$suffix"
} // get_current_date

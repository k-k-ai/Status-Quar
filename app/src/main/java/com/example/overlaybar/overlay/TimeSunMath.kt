//// app/src/main/java/com/example/overlaybar/overlay/TimeSunMath.kt
//// Created 2026-04-22
//// timesunmath module


/// Imports


package com.example.overlaybar.overlay

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class SunEvent(val time_millis: Long, val day_offset: Int)
internal data class SunTimes(val sunrise: SunEvent, val sunset: SunEvent)


/// Functions


internal fun compute_next_sun_events(lat: Float, lon: Float, now_millis: Long): SunTimes? {
    if (lat == 0f && lon == 0f) return null
    val today = compute_sun_times_for_day(lat, lon, now_millis, 0) ?: return null
    val tomorrow = compute_sun_times_for_day(lat, lon, now_millis, 1) ?: return null
    return SunTimes(
        sunrise = if (today.sunrise.time_millis > now_millis) today.sunrise else tomorrow.sunrise,
        sunset = if (today.sunset.time_millis > now_millis) today.sunset else tomorrow.sunset
    )
} // compute_next_sun_events

internal fun compute_sun_times_for_day(lat: Float, lon: Float, base_millis: Long, day_offset: Int): SunTimes? {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = base_millis
        add(Calendar.DAY_OF_YEAR, day_offset)
    }
    val day_of_year = calendar.get(Calendar.DAY_OF_YEAR)
    val timezone = java.util.TimeZone.getDefault()
    val offset_hours = timezone.getOffset(calendar.timeInMillis) / 3_600_000.0
    val lat_rad = Math.toRadians(lat.toDouble())
    val decl = Math.toRadians(-23.45 * Math.cos(Math.toRadians(360.0 / 365.0 * (day_of_year + 10.0))))
    val cos_hour_angle = ((Math.sin(Math.toRadians(-0.833)) - Math.sin(lat_rad) * Math.sin(decl)) /
        (Math.cos(lat_rad) * Math.cos(decl))).coerceIn(-1.0, 1.0)
    val hour_angle_degrees = Math.toDegrees(Math.acos(cos_hour_angle))
    val solar_noon_hours = 12.0 + offset_hours - lon / 15.0
    val sunrise_hours = solar_noon_hours - hour_angle_degrees / 15.0
    val sunset_hours = solar_noon_hours + hour_angle_degrees / 15.0
    return SunTimes(
        sunrise = SunEvent(day_hour_to_millis(calendar, sunrise_hours), day_offset),
        sunset = SunEvent(day_hour_to_millis(calendar, sunset_hours), day_offset)
    )
} // compute_sun_times_for_day

internal fun day_hour_to_millis(dayCalendar: Calendar, hours: Double): Long {
    val normalized_hours = ((hours % 24.0) + 24.0) % 24.0
    return Calendar.getInstance().apply {
        timeInMillis = dayCalendar.timeInMillis
        set(Calendar.HOUR_OF_DAY, normalized_hours.toInt())
        set(Calendar.MINUTE, ((normalized_hours % 1.0) * 60.0).toInt())
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
} // day_hour_to_millis

internal fun format_clock_time(time_millis: Long, use_24_hour_time: Boolean): String {
    val pattern = if (use_24_hour_time) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(time_millis))
} // format_clock_time

internal fun format_relative_event(event_millis: Long, now_millis: Long, day_offset: Int): String {
    val delta_minutes = ((event_millis - now_millis) / 60_000L).toInt()
    return when {
        day_offset > 0 -> ""
        delta_minutes > 0 -> "${delta_minutes / 60}h ${delta_minutes % 60}m"
        delta_minutes < 0 -> "${(-delta_minutes) / 60}h ${(-delta_minutes) % 60}m ago"
        else -> "now"
    }
} // format_relative_event

internal fun format_timer_remaining(remaining_millis: Long): String {
    val total_seconds = (remaining_millis / 1000L).coerceAtLeast(0L)
    val minutes = total_seconds / 60L
    val seconds = total_seconds % 60L
    return String.format("%02d:%02d remaining", minutes, seconds)
} // format_timer_remaining

internal fun format_timer_clock(remaining_millis: Long): String {
    val total_seconds = (remaining_millis / 1000L).coerceAtLeast(0L)
    val minutes = total_seconds / 60L
    val seconds = total_seconds % 60L
    return String.format("%02d:%02d", minutes, seconds)
} // format_timer_clock

internal fun format_stopwatch(elapsed_millis: Long): String {
    val e = elapsed_millis.coerceAtLeast(0L)
    val tenths = (e / 100L) % 10L
    val total_seconds = e / 1000L
    val seconds = total_seconds % 60L
    val total_minutes = total_seconds / 60L
    val minutes = total_minutes % 60L
    val hours = total_minutes / 60L
    return if (hours > 0L)
        String.format("%d:%02d:%02d.%d", hours, minutes, seconds, tenths)
    else
        String.format("%02d:%02d.%d", minutes, seconds, tenths)
} // format_stopwatch

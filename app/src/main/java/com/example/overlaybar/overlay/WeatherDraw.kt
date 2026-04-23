//// app/src/main/java/com/example/overlaybar/overlay/WeatherDraw.kt
//// Created 2026-04-22
//// weatherdraw module


/// Imports


package com.example.overlaybar.overlay

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.overlaybar.data.ElementSettings
import com.example.overlaybar.data.WEATHER_BACKDROP_CLOUDY
import com.example.overlaybar.data.WEATHER_BACKDROP_RAINY
import com.example.overlaybar.data.WEATHER_BACKDROP_SNOWY
import com.example.overlaybar.data.WEATHER_BACKDROP_SUNNY
import com.example.overlaybar.data.WEATHER_BACKDROP_THUNDER
import com.example.overlaybar.data.WEATHER_BACKDROP_WINDY


/// Functions


internal fun parse_hex_color(hex: String): Color? {
    if (hex.isBlank()) return null
    val clean = hex.trim().removePrefix("#")
    return try {
        when (clean.length) {
            6 -> Color(android.graphics.Color.parseColor("#FF$clean"))
            8 -> {
                val r = clean.substring(0, 2)
                val g = clean.substring(2, 4)
                val b = clean.substring(4, 6)
                val a = clean.substring(6, 8)
                Color(android.graphics.Color.parseColor("#$a$r$g$b"))
            }
            else -> null
        }
    } catch (e: Exception) { null }
} // parse_hex_color

internal fun lerp_color(a: Color, b: Color, t: Float): Color = Color(
    (a.red   + (b.red   - a.red)   * t).coerceIn(0f, 1f),
    (a.green + (b.green - a.green) * t).coerceIn(0f, 1f),
    (a.blue  + (b.blue  - a.blue)  * t).coerceIn(0f, 1f),
    (a.alpha + (b.alpha - a.alpha) * t).coerceIn(0f, 1f)
)

// coverage: 0.0 = barely any cloud (1 thin bunch), 1.0 = full overcast (3 dense bunches)
// Tiers roughly: 0.15=mainly clear, 0.40=partly cloudy, 0.65=mostly cloudy, 0.85=cloudy, 1.0=overcast
internal fun DrawScope.draw_cloud_bunches(coverage: Float = 1f, clear_wayhead_progress: Float = 0.5f) {
    val t = coverage.coerceIn(0f, 1f)
    val alpha = 0.10f + (0.22f - 0.10f) * t
    val c = Color.White.copy(alpha = alpha)

    val h = size.height
    val cx = clear_wayhead_center_x(size.width, h, clear_wayhead_progress)

    // Main cloud head
    val main_r = h * 0.35f
    val main_y = h * 0.5f
    drawCircle(c, radius = main_r, center = Offset(cx, main_y))
    drawCircle(c, radius = h * 0.28f, center = Offset(cx - h * 0.3f, main_y + h * 0.1f))
    drawCircle(c, radius = h * 0.28f, center = Offset(cx + h * 0.3f, main_y + h * 0.1f))

    // Smaller puff satellites depending on coverage
    if (t > 0.3f) {
        drawCircle(c, radius = h * 0.22f, center = Offset(cx - h * 0.7f, main_y + h * 0.15f))
        drawCircle(c, radius = h * 0.22f, center = Offset(cx + h * 0.7f, main_y + h * 0.15f))
    }
    if (t > 0.6f) {
        drawCircle(c, radius = h * 0.18f, center = Offset(cx - h * 1.1f, main_y + h * 0.2f))
        drawCircle(c, radius = h * 0.18f, center = Offset(cx + h * 1.1f, main_y + h * 0.2f))
    }
}

internal fun wmo_cloud_coverage(code: Int): Float = when (code) {
    0     -> 0.0f   // clear sky
    1     -> 0.15f  // mainly clear
    2     -> 0.40f  // partly cloudy
    3     -> 1.0f   // overcast
    45, 48 -> 0.65f // fog — moderately cloudy look
    else  -> 0.85f  // anything else mapping to CLOUDY backdrop
}

internal fun DrawScope.draw_sunny_glow(solar_brightness: Float, clear_wayhead_progress: Float, scale: Float = 1f) {
    val h = size.height
    val cx = clear_wayhead_center_x(size.width, h, clear_wayhead_progress)
    val cy = h * 0.5f
    val night = (1f - solar_brightness).coerceIn(0f, 1f)
    val outer = lerp_color(Color(0xFFFFF5B0), Color(0xFFD7E3FF), night)
    val mid = lerp_color(Color(0xFFFFD860), Color(0xFFAFC4F6), night)
    val core = lerp_color(Color(0xFFFFE880), Color(0xFFE8EEFF), night)
    val rim = lerp_color(Color(0xFFFFF0A0), Color(0xFFF2F5FF), night)
    val disc = lerp_color(Color(0xFFFFF7C8), Color(0xFFFFFFFF), night)
    val s = scale
    drawCircle(outer.copy(alpha = 0.07f), radius = h * 1.56f * s, center = Offset(cx, cy))
    drawCircle(mid.copy(alpha = 0.10f), radius = h * 1.24f * s, center = Offset(cx, cy))
    drawCircle(core.copy(alpha = 0.14f), radius = h * 0.98f * s, center = Offset(cx, cy))
    drawCircle(outer.copy(alpha = 0.11f), radius = h * 1.38f * s, center = Offset(cx, cy))
    drawCircle(mid.copy(alpha = 0.16f), radius = h * 1.06f * s, center = Offset(cx, cy))
    drawCircle(core.copy(alpha = 0.22f), radius = h * 0.78f * s, center = Offset(cx, cy))
    drawCircle(rim.copy(alpha = 0.40f), radius = h * 0.40f * s, center = Offset(cx, cy))
    drawOval(
        color = disc.copy(alpha = 0.44f),
        topLeft = Offset(cx - h * 0.43f * s, cy - h * 0.31f * s),
        size = Size(h * 0.86f * s, h * 0.62f * s)
    )
    drawLine(
        rim.copy(alpha = 0.28f),
        start = Offset(cx, cy - h * 0.17f * s),
        end = Offset(cx, cy - h * 0.50f * s),
        strokeWidth = h * 0.06f * s
    )
    drawCircle(disc.copy(alpha = 0.38f), radius = h * 0.12f * s, center = Offset(cx, cy - h * 0.50f * s))
    drawCircle(disc.copy(alpha = 0.28f), radius = h * 0.09f * s, center = Offset(cx - h * 0.14f * s, cy - h * 0.34f * s))
    drawCircle(disc.copy(alpha = 0.28f), radius = h * 0.09f * s, center = Offset(cx + h * 0.14f * s, cy - h * 0.34f * s))
    drawLine(
        rim.copy(alpha = 0.24f),
        start = Offset(cx, cy + h * 0.17f * s),
        end = Offset(cx, cy + h * 0.50f * s),
        strokeWidth = h * 0.06f * s
    )
    drawCircle(disc.copy(alpha = 0.34f), radius = h * 0.12f * s, center = Offset(cx, cy + h * 0.50f * s))
    drawCircle(disc.copy(alpha = 0.24f), radius = h * 0.09f * s, center = Offset(cx - h * 0.14f * s, cy + h * 0.34f * s))
    drawCircle(disc.copy(alpha = 0.24f), radius = h * 0.09f * s, center = Offset(cx + h * 0.14f * s, cy + h * 0.34f * s))
}

internal fun clear_wayhead_center_x(width: Float, height: Float, progress: Float): Float {
    val side_inset = height * 0.40f
    val min_x = side_inset
    val max_x = width - side_inset
    return min_x + (max_x - min_x) * progress
} // clear_wayhead_center_x

internal fun normalize_signed_degrees(value: Float): Float {
    var normalized = value % 360f
    if (normalized > 180f) normalized -= 360f
    if (normalized < -180f) normalized += 360f
    return normalized
} // normalize_signed_degrees

internal fun clear_wayhead_progress(compass_heading_degrees: Float?, edge_sign: Float): Float {
    val relative_north = compass_heading_degrees?.let { normalize_signed_degrees(-it) } ?: 90f
    val visible_arc = 105f
    val clamped = when {
        relative_north > visible_arc -> visible_arc
        relative_north < -visible_arc -> -visible_arc
        kotlin.math.abs(relative_north) > 170f -> visible_arc * edge_sign
        else -> relative_north
    }
    return ((clamped + visible_arc) / (visible_arc * 2f)).coerceIn(0f, 1f)
} // clear_wayhead_progress

internal fun DrawScope.draw_rain_streaks() {
    val c = Color(0xFFADD8F0).copy(alpha = 0.13f)
    val spacing = 10.dp.toPx()
    var x = -spacing * 0.5f
    while (x < size.width + spacing) {
        drawLine(c,
            start = Offset(x, -size.height * 0.1f),
            end   = Offset(x - size.height * 0.65f, size.height * 1.1f),
            strokeWidth = 0.8.dp.toPx()
        )
        x += spacing
    }
} // draw_rain_streaks

internal fun DrawScope.draw_snow_crystals() {
    val c = Color(0xFFCCE8FF).copy(alpha = 0.22f)
    val arm = 2.8.dp.toPx()
    val positions = listOf(
        0.09f to 0.28f, 0.22f to 0.72f, 0.37f to 0.24f,
        0.51f to 0.68f, 0.65f to 0.32f, 0.79f to 0.70f, 0.93f to 0.38f
    )
    positions.forEach { (px, py) ->
        val ox = size.width * px
        val oy = size.height * py
        drawCircle(c, radius = 1.6.dp.toPx(), center = Offset(ox, oy))
        drawLine(c, Offset(ox - arm, oy), Offset(ox + arm, oy), strokeWidth = 0.7.dp.toPx())
        drawLine(c, Offset(ox, oy - arm), Offset(ox, oy + arm), strokeWidth = 0.7.dp.toPx())
    }
} // draw_snow_crystals

internal fun DrawScope.draw_thunder_bolt() {
    val w = size.width; val h = size.height
    val main = Color(0xFFFFE040).copy(alpha = 0.30f)
    drawLine(main, Offset(w * 0.64f, h * 0.04f), Offset(w * 0.42f, h * 0.50f), strokeWidth = 2.5.dp.toPx())
    drawLine(main, Offset(w * 0.56f, h * 0.50f), Offset(w * 0.33f, h * 0.96f), strokeWidth = 2.5.dp.toPx())
    val dim = Color(0xFFFFE040).copy(alpha = 0.10f)
    drawLine(dim, Offset(w * 0.24f, h * 0.04f), Offset(w * 0.12f, h * 0.50f), strokeWidth = 1.5.dp.toPx())
    drawLine(dim, Offset(w * 0.17f, h * 0.50f), Offset(w * 0.06f, h * 0.96f), strokeWidth = 1.5.dp.toPx())
} // draw_thunder_bolt

internal fun DrawScope.draw_wind_streaks() {
    draw_wind_streaks(1f)
} // draw_wind_streaks

internal fun DrawScope.draw_wind_streaks(strength: Float) {
    data class Streak(val y: Float, val x0: Float, val x1: Float, val alpha: Float)
    val t = strength.coerceIn(0f, 1f)
    val streaks = listOf(
        Streak(0.22f, 0.00f, 0.58f, 0.16f),
        Streak(0.40f, 0.04f, 0.88f, 0.14f),
        Streak(0.55f, 0.00f, 0.42f, 0.12f),
        Streak(0.70f, 0.08f, 0.76f, 0.15f),
        Streak(0.82f, 0.00f, 0.52f, 0.10f),
    )
    val visibleCount = (2 + (streaks.size - 2) * t).toInt().coerceIn(2, streaks.size)
    streaks.take(visibleCount).forEach { s ->
        drawLine(
            Color.White.copy(alpha = s.alpha * (0.45f + 0.55f * t)),
            start = Offset(size.width * s.x0, size.height * s.y),
            end   = Offset(size.width * s.x1, size.height * s.y),
            strokeWidth = (0.7f + 0.5f * t).dp.toPx()
        )
    }
} // draw_wind_streaks

internal fun DrawScope.draw_fog_bands(alpha: Float, solar_brightness: Float) {
    val t = alpha.coerceIn(0f, 1f)
    val night = (1f - solar_brightness).coerceIn(0f, 1f)
    val mist = lerp_color(Color(0xFFE6EEF6), Color(0xFFC9D6EA), night)
    val bands = listOf(
        Triple(0.28f, 0.86f, 0.18f),
        Triple(0.48f, 1.08f, 0.24f),
        Triple(0.70f, 0.94f, 0.16f)
    )
    bands.forEach { (centerY, widthScale, heightScale) ->
        val band_height = size.height * heightScale
        drawRoundRect(
            color = mist.copy(alpha = t * 0.22f),
            topLeft = Offset(size.width * (1f - widthScale) * 0.5f, size.height * centerY - band_height * 0.5f),
            size = Size(size.width * widthScale, band_height),
            cornerRadius = CornerRadius(band_height, band_height)
        )
    }
} // draw_fog_bands

internal fun resolve_backdrop_from_code(code: Int, wind_mph: Float, hour_of_day: Int): Int {
    return when {
        else -> when (code) {
            0                                     -> WEATHER_BACKDROP_SUNNY
            1, 2, 3                               -> WEATHER_BACKDROP_CLOUDY
            45, 48                                -> WEATHER_BACKDROP_CLOUDY
            51, 53, 55, 61, 63, 65, 80, 81, 82   -> WEATHER_BACKDROP_RAINY
            71, 73, 75, 85, 86                    -> WEATHER_BACKDROP_SNOWY
            95, 96, 99                            -> WEATHER_BACKDROP_THUNDER
            else                                  -> WEATHER_BACKDROP_CLOUDY
        }
    }
} // resolve_backdrop_from_code

internal fun is_fog_code(code: Int): Boolean = code == 45 || code == 48

internal fun fog_overlay_alpha(code: Int, solar_brightness: Float): Float {
    if (!is_fog_code(code)) return 0f
    val night_boost = 1f + (1f - solar_brightness).coerceIn(0f, 1f) * 0.18f
    val base_alpha = if (code == 48) 0.46f else 0.32f
    return (base_alpha * night_boost).coerceIn(0f, 0.54f)
} // fog_overlay_alpha

internal fun wind_overlay_strength(wind_mph: Float): Float {
    if (wind_mph < 14f) return 0f
    return ((wind_mph - 14f) / 16f).coerceIn(0f, 1f)
} // wind_overlay_strength

internal fun resolve_weather_colors(backdrop: Int, is_immersive: Boolean, ws: ElementSettings, solar_brightness: Float): Pair<Color?, Color?> {
    if (!is_immersive) return parse_hex_color(ws.pillColor) to parse_hex_color(ws.pillStrokeColor)
    // SUNNY blends from warm-gold (day) to deep navy (night) to avoid jarring dimmed-gold at night
    if (backdrop == WEATHER_BACKDROP_SUNNY) {
        val night = (1f - solar_brightness).coerceIn(0f, 1f)
        return lerp_color(Color(0xFFEAB830), Color(0xFF223860), night) to
               lerp_color(Color(0xCCFFE880), Color(0xBBAAC8FF), night)
    }
    val (fill, stroke) = when (backdrop) {
        WEATHER_BACKDROP_RAINY   -> Color(0xFF1A3355) to Color(0xBB80C8FF)
        WEATHER_BACKDROP_SNOWY   -> Color(0xFF28406A) to Color(0xBBBDD8FF)
        WEATHER_BACKDROP_THUNDER -> Color(0xFF0F1038) to Color(0xCCB080FF)
        WEATHER_BACKDROP_WINDY   -> Color(0xFF3A6A88) to Color(0xAAACD8FF)
        else                     -> Color(0xFF4A7898) to Color(0xAAFFFFFF)
    }
    return fill.withSolarBrightness(solar_brightness) to stroke.withSolarBrightness(solar_brightness)
} // resolve_weather_colors

internal fun Color.withSolarBrightness(factor: Float): Color {
    val f = (0.30f + 0.70f * factor).coerceIn(0.30f, 1f)
    return Color(red * f, green * f, blue * f, alpha)
} // withSolarBrightness

internal fun solar_brightness(lat: Float, lon: Float, hour_of_day: Int, minute_of_hour: Int, day_of_year: Int): Float {
    val decl = Math.toRadians(-23.45 * Math.cos(Math.toRadians(360.0 / 365.0 * (day_of_year + 10))))
    val utc_offset_hours = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3_600_000.0
    val solar_hour = hour_of_day + minute_of_hour / 60.0 - utc_offset_hours + lon / 15.0
    val hour_angle = Math.toRadians((solar_hour - 12.0) * 15.0)
    val lat_rad = Math.toRadians(lat.toDouble())
    val sin_elev = Math.sin(lat_rad) * Math.sin(decl) + Math.cos(lat_rad) * Math.cos(decl) * Math.cos(hour_angle)
    val elev_deg = Math.toDegrees(Math.asin(sin_elev.coerceIn(-1.0, 1.0))).toFloat()
    return when {
        elev_deg >= 45f -> 1.00f
        elev_deg >= 0f  -> 0.30f + 0.70f * (elev_deg / 45f)
        elev_deg >= -6f -> 0.15f + 0.15f * ((elev_deg + 6f) / 6f)
        else           -> 0.15f
    }
} // solar_brightness

// Returns a color to tint the pill stroke based on feels-like temperature, or null in the comfort zone.
internal fun temp_tint_color(temp_f: Float, wind_mph: Float): Color? {
    val feels_like = if (temp_f <= 50f && wind_mph >= 3f) {
        35.74f + 0.6215f * temp_f -
            35.75f * Math.pow(wind_mph.toDouble(), 0.16).toFloat() +
            0.4275f * temp_f * Math.pow(wind_mph.toDouble(), 0.16).toFloat()
    } else {
        temp_f
    }
    return when {
        feels_like <= 20f -> Color(0xDD6699FF)   // very cold
        feels_like <= 35f -> Color(0xCC88AAFF)   // cold
        feels_like <= 52f -> Color(0xBBAAC8FF)   // chilly
        feels_like <= 70f -> null                 // comfortable
        feels_like <= 85f -> Color(0xCCFFBB33)   // warm — more saturated gold
        else             -> Color(0xDDFF8833)   // hot — stronger orange
    }
} // temp_tint_color

internal fun Modifier.weather_flare_surface(
    backdrop: Int,
    solar_brightness: Float = 1f,
    clear_wayhead_progress: Float = 0.5f,
    cloud_coverage: Float = 1f,
    fog_overlay_alpha: Float = 0f,
    wind_overlay_strength: Float = 0f
): Modifier = this.drawBehind {
    when (backdrop) {
        WEATHER_BACKDROP_CLOUDY  -> draw_cloud_bunches(cloud_coverage)
        WEATHER_BACKDROP_SUNNY   -> draw_sunny_glow(solar_brightness, clear_wayhead_progress, scale = 0.52f)
        WEATHER_BACKDROP_RAINY   -> draw_rain_streaks()
        WEATHER_BACKDROP_SNOWY   -> draw_snow_crystals()
        WEATHER_BACKDROP_THUNDER -> draw_thunder_bolt()
        WEATHER_BACKDROP_WINDY   -> draw_wind_streaks()
    }
    if (fog_overlay_alpha > 0f) draw_fog_bands(fog_overlay_alpha, solar_brightness)
    if (wind_overlay_strength > 0f && backdrop != WEATHER_BACKDROP_WINDY) draw_wind_streaks(wind_overlay_strength)
}

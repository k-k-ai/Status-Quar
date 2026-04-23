//// app/src/main/java/com/example/overlaybar/data/Prefs.kt
//// Created 2026-04-22
//// prefs module


/// Imports


package com.example.overlaybar.data

import androidx.compose.runtime.Immutable

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map


/// Symbols


// Extension property for DataStore - snake_case for user-defined
val Context.data_store by preferencesDataStore("status_bar_prefs")

/**
 * Preference keys for all settings
 * Using object for namespace organization
 */
object PrefsKeys {
    // Global settings
    val ENABLED = booleanPreferencesKey("enabled")
    val FONT_SCALE = floatPreferencesKey("fontScale")
    val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
    val SHOW_DATE = booleanPreferencesKey("show_date")
    val SHOW_BATTERY = booleanPreferencesKey("show_battery")
    val SHOW_GIF = booleanPreferencesKey("show_gif")
    val SHOW_WEATHER = booleanPreferencesKey("show_weather")
    val WEATHER_MODE = intPreferencesKey("weather_mode")
    val WEATHER_BACKDROP = intPreferencesKey("weather_backdrop")
    val GIF_URI = stringPreferencesKey("gif_uri")
    val THEME_MODE = intPreferencesKey("theme_mode")
    val FONT_FAMILY   = intPreferencesKey("fontFamily")
    val GLOBAL_ITALIC_MODE = intPreferencesKey("global_italic_mode")
    val LEGACY_GLOBAL_ITALIC = booleanPreferencesKey("global_italic")

    // Time module
    val TIME_ALIGNMENT = intPreferencesKey("time_alignment")
    val TIME_OFFSET_PX = intPreferencesKey("time_offset_px")
    val TIME_SIZE_SCALE = floatPreferencesKey("time_size_scale")
    val TIME_PILL_SCALE = floatPreferencesKey("time_pill_scale")
    val TIME_WEIGHT = intPreferencesKey("time_weight")
    val TIME_ITALIC = booleanPreferencesKey("time_italic")
    val TIME_ORDER = intPreferencesKey("time_order")
    val TIME_PILL_COLOR = stringPreferencesKey("time_pill_color")
    val TIME_PILL_STROKE = stringPreferencesKey("time_pill_stroke")

    // Date module
    val DATE_ALIGNMENT = intPreferencesKey("date_alignment")
    val DATE_OFFSET_PX = intPreferencesKey("date_offset_px")
    val DATE_SIZE_SCALE = floatPreferencesKey("date_size_scale")
    val DATE_PILL_SCALE = floatPreferencesKey("date_pill_scale")
    val DATE_WEIGHT = intPreferencesKey("date_weight")
    val DATE_ITALIC = booleanPreferencesKey("date_italic")
    val DATE_ORDER = intPreferencesKey("date_order")
    val DATE_PILL_COLOR = stringPreferencesKey("date_pill_color")
    val DATE_PILL_STROKE = stringPreferencesKey("date_pill_stroke")

    // Battery module
    val BATTERY_ALIGNMENT = intPreferencesKey("battery_alignment")
    val BATTERY_OFFSET_PX = intPreferencesKey("battery_offset_px")
    val BATTERY_SIZE_SCALE = floatPreferencesKey("battery_size_scale")
    val BATTERY_PILL_SCALE = floatPreferencesKey("battery_pill_scale")
    val BATTERY_WEIGHT = intPreferencesKey("battery_weight")
    val BATTERY_ITALIC = booleanPreferencesKey("battery_italic")
    val BATTERY_ORDER = intPreferencesKey("battery_order")
    val BATTERY_PILL_COLOR = stringPreferencesKey("battery_pill_color")
    val BATTERY_PILL_STROKE = stringPreferencesKey("battery_pill_stroke")

    // GIF module
    val GIF_ALIGNMENT = intPreferencesKey("gif_alignment")
    val GIF_OFFSET_PX = intPreferencesKey("gif_offset_px")
    val GIF_SIZE_SCALE = floatPreferencesKey("gif_size_scale")
    val GIF_PILL_SCALE = floatPreferencesKey("gif_pill_scale")
    val GIF_ORDER = intPreferencesKey("gif_order")
    val GIF_PILL_COLOR = stringPreferencesKey("gif_pill_color")
    val GIF_PILL_STROKE = stringPreferencesKey("gif_pill_stroke")

    // Weather module
    val WEATHER_ALIGNMENT = intPreferencesKey("weather_alignment")
    val WEATHER_OFFSET_PX = intPreferencesKey("weather_offset_px")
    val WEATHER_SIZE_SCALE = floatPreferencesKey("weather_size_scale")
    val WEATHER_PILL_SCALE = floatPreferencesKey("weather_pill_scale")
    val WEATHER_WEIGHT = intPreferencesKey("weather_weight")
    val WEATHER_ITALIC = booleanPreferencesKey("weather_italic")
    val WEATHER_ORDER = intPreferencesKey("weather_order")
    val WEATHER_PILL_COLOR = stringPreferencesKey("weather_pill_color")
    val WEATHER_PILL_STROKE = stringPreferencesKey("weather_pill_stroke")
    val WEATHER_ZIP = stringPreferencesKey("weather_zip")
    val WEATHER_LOCATION_MODE = intPreferencesKey("weather_location_mode")
    val WEATHER_LOCATION_SOURCE = stringPreferencesKey("weather_location_source")
    val WEATHER_TEMP_F = floatPreferencesKey("weather_temp_f")
    val WEATHER_WIND_MPH = floatPreferencesKey("weather_wind_mph")
    val WEATHER_GUST_MPH = floatPreferencesKey("weather_gust_mph")
    val WEATHER_CODE = intPreferencesKey("weather_code")
    val WEATHER_FETCHED_AT = longPreferencesKey("weather_fetched_at")
    val WEATHER_LAT = floatPreferencesKey("weather_lat")
    val WEATHER_LON = floatPreferencesKey("weather_lon")
    val HOURLY_TEMPS_F = stringPreferencesKey("hourly_temps_f")
    val HOURLY_WCODES = stringPreferencesKey("hourly_wcodes")
    val HOURLY_WINDS_MPH = stringPreferencesKey("hourly_winds_mph")

    // Layout
    val ELEMENT_PADDING_DP       = intPreferencesKey("element_padding_dp")
    val LEFT_LANE_CLEARANCE_DP   = intPreferencesKey("left_lane_clearance_dp")
    val RIGHT_LANE_CLEARANCE_DP  = intPreferencesKey("right_lane_clearance_dp")
    val LEFT_VERTICAL_OFFSET_DP  = intPreferencesKey("left_vertical_offset_dp")
    val RIGHT_VERTICAL_OFFSET_DP = intPreferencesKey("right_vertical_offset_dp")

    // Overlay appearance
    val SHOW_CAPSULES = booleanPreferencesKey("show_capsules")
    val MERGED_LANES  = booleanPreferencesKey("merged_lanes")
    val USE_24_HOUR_TIME = booleanPreferencesKey("use_24_hour_time")
} // prefs_keys

// Alignment constants
const val ALIGN_LEFT = 0
const val ALIGN_RIGHT = 1

// Theme mode constants
const val THEME_AUTO = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2

// Font weight constants
const val WEIGHT_NORMAL = 400
const val WEIGHT_BOLD = 700
const val WEIGHT_BLACK = 900

// Font family constants
const val FONT_APTOS_SANS  = 0
const val FONT_APTOS_SERIF = 1
const val FONT_SYSTEM      = 2

// House-style pill scale
const val PILL_BASELINE_SIZE_SCALE = 1.1f


/// Types


@Immutable
data class ElementSettings(
    val alignment: Int   = ALIGN_LEFT,
    val offsetPx: Int    = 0,
    val pillScale: Float = 1.0f,
    val sizeScale: Float = 1.0f,
    val pillColor: String = "",
    val pillStrokeColor: String = "",
    val weight: Int      = WEIGHT_BLACK,
    val italic: Boolean  = false,
    val order: Int       = 0
) // element_settings

class Prefs(private val context: Context) {

    val enabled: Flow<Boolean> = context.data_store.data.map { it[PrefsKeys.ENABLED] ?: true }
    val fontScale: Flow<Float> = context.data_store.data.map { it[PrefsKeys.FONT_SCALE] ?: 1.0f }
    val animation_speed: Flow<Float> = context.data_store.data.map { it[PrefsKeys.ANIMATION_SPEED] ?: 1.0f }
    val show_date: Flow<Boolean> = context.data_store.data.map { it[PrefsKeys.SHOW_DATE] ?: true }
    val show_battery: Flow<Boolean> = context.data_store.data.map { it[PrefsKeys.SHOW_BATTERY] ?: true }
    val show_gif: Flow<Boolean> = context.data_store.data.map { it[PrefsKeys.SHOW_GIF] ?: true }
    val show_weather: Flow<Boolean> = context.data_store.data.map { it[PrefsKeys.SHOW_WEATHER] ?: true }
    val weather_mode: Flow<Int> = context.data_store.data.map { it[PrefsKeys.WEATHER_MODE] ?: 0 }
    val weather_backdrop: Flow<Int> = context.data_store.data.map { it[PrefsKeys.WEATHER_BACKDROP] ?: WEATHER_BACKDROP_LIVE }
    val gif_uri: Flow<String?> = context.data_store.data.map { it[PrefsKeys.GIF_URI] }
    val theme_mode: Flow<Int> = context.data_store.data.map { it[PrefsKeys.THEME_MODE] ?: THEME_AUTO }
    val fontFamily_choice: Flow<Int> = context.data_store.data.map { it[PrefsKeys.FONT_FAMILY] ?: FONT_APTOS_SANS }
    val global_italic_mode: Flow<Int> = context.data_store.data.map {
        resolve_global_italic_mode(it[PrefsKeys.GLOBAL_ITALIC_MODE], it[PrefsKeys.LEGACY_GLOBAL_ITALIC])
    }

    val time_settings: Flow<ElementSettings> = context.data_store.data.map {
        ElementSettings(
            alignment = it[PrefsKeys.TIME_ALIGNMENT] ?: ALIGN_RIGHT,
            offsetPx  = it[PrefsKeys.TIME_OFFSET_PX]  ?: 16,
            pillScale = it[PrefsKeys.TIME_PILL_SCALE] ?: 0.8f,
            sizeScale = it[PrefsKeys.TIME_SIZE_SCALE] ?: 1.0f,
            pillColor = it[PrefsKeys.TIME_PILL_COLOR] ?: "",
            pillStrokeColor = it[PrefsKeys.TIME_PILL_STROKE] ?: "",
            weight    = it[PrefsKeys.TIME_WEIGHT]      ?: WEIGHT_BOLD,
            italic    = it[PrefsKeys.TIME_ITALIC]      ?: false,
            order     = it[PrefsKeys.TIME_ORDER]       ?: 1
        )
    }

    val date_settings: Flow<ElementSettings> = context.data_store.data.map {
        ElementSettings(
            alignment = it[PrefsKeys.DATE_ALIGNMENT] ?: ALIGN_RIGHT,
            offsetPx  = it[PrefsKeys.DATE_OFFSET_PX]  ?: 0,
            pillScale = it[PrefsKeys.DATE_PILL_SCALE] ?: 0.8f,
            sizeScale = it[PrefsKeys.DATE_SIZE_SCALE] ?: 1.0f,
            pillColor = it[PrefsKeys.DATE_PILL_COLOR] ?: "",
            pillStrokeColor = it[PrefsKeys.DATE_PILL_STROKE] ?: "",
            weight    = it[PrefsKeys.DATE_WEIGHT]      ?: WEIGHT_BOLD,
            italic    = it[PrefsKeys.DATE_ITALIC]      ?: false,
            order     = it[PrefsKeys.DATE_ORDER]       ?: 1
        )
    }

    val battery_settings: Flow<ElementSettings> = context.data_store.data.map {
        ElementSettings(
            alignment = it[PrefsKeys.BATTERY_ALIGNMENT] ?: ALIGN_RIGHT,
            offsetPx  = it[PrefsKeys.BATTERY_OFFSET_PX]  ?: 20,
            pillScale = it[PrefsKeys.BATTERY_PILL_SCALE] ?: 0.8f,
            sizeScale = it[PrefsKeys.BATTERY_SIZE_SCALE] ?: 1.0f,
            pillColor = it[PrefsKeys.BATTERY_PILL_COLOR] ?: "",
            pillStrokeColor = it[PrefsKeys.BATTERY_PILL_STROKE] ?: "",
            weight    = it[PrefsKeys.BATTERY_WEIGHT]      ?: WEIGHT_BOLD,
            italic    = it[PrefsKeys.BATTERY_ITALIC]      ?: false,
            order     = it[PrefsKeys.BATTERY_ORDER]       ?: 1
        )
    }

    val gif_settings: Flow<ElementSettings> = context.data_store.data.map {
        ElementSettings(
            alignment = it[PrefsKeys.GIF_ALIGNMENT] ?: ALIGN_LEFT,
            offsetPx  = it[PrefsKeys.GIF_OFFSET_PX]  ?: 0,
            pillScale = it[PrefsKeys.GIF_PILL_SCALE] ?: 0.8f,
            sizeScale = it[PrefsKeys.GIF_SIZE_SCALE] ?: 2.0f,
            pillColor = it[PrefsKeys.GIF_PILL_COLOR] ?: "ffffff00",
            pillStrokeColor = it[PrefsKeys.GIF_PILL_STROKE] ?: "ffffff00",
            weight    = WEIGHT_NORMAL,
            italic    = false,
            order     = it[PrefsKeys.GIF_ORDER] ?: 0
        )
    }

    val weather_settings: Flow<ElementSettings> = context.data_store.data.map {
        ElementSettings(
            alignment = it[PrefsKeys.WEATHER_ALIGNMENT] ?: ALIGN_LEFT,
            offsetPx  = it[PrefsKeys.WEATHER_OFFSET_PX]  ?: 320,
            pillScale = it[PrefsKeys.WEATHER_PILL_SCALE] ?: 0.8f,
            sizeScale = it[PrefsKeys.WEATHER_SIZE_SCALE] ?: 1.0f,
            pillColor = it[PrefsKeys.WEATHER_PILL_COLOR] ?: "",
            pillStrokeColor = it[PrefsKeys.WEATHER_PILL_STROKE] ?: "",
            weight    = it[PrefsKeys.WEATHER_WEIGHT] ?: WEIGHT_BOLD,
            italic    = it[PrefsKeys.WEATHER_ITALIC] ?: false,
            order     = it[PrefsKeys.WEATHER_ORDER] ?: 4
        )
    }
    val weather_zip: Flow<String> = context.data_store.data.map { it[PrefsKeys.WEATHER_ZIP] ?: "14228" }
    val weather_location_mode: Flow<Int> = context.data_store.data.map { it[PrefsKeys.WEATHER_LOCATION_MODE] ?: WEATHER_LOCATION_MODE_SYSTEM }
    val weather_location_source: Flow<String> = context.data_store.data.map { it[PrefsKeys.WEATHER_LOCATION_SOURCE] ?: WEATHER_LOCATION_SOURCE_UNKNOWN }
    val weather_temp_f: Flow<Float> = context.data_store.data.map { it[PrefsKeys.WEATHER_TEMP_F] ?: 0f }
    val weather_wind_mph: Flow<Float> = context.data_store.data.map { it[PrefsKeys.WEATHER_WIND_MPH] ?: 0f }
    val weather_gust_mph: Flow<Float> = context.data_store.data.map { it[PrefsKeys.WEATHER_GUST_MPH] ?: 0f }
    val weather_code: Flow<Int> = context.data_store.data.map { it[PrefsKeys.WEATHER_CODE] ?: 0 }
    val weather_fetched_at: Flow<Long> = context.data_store.data.map { it[PrefsKeys.WEATHER_FETCHED_AT] ?: 0L }
    val weather_lat: Flow<Float> = context.data_store.data.map { it[PrefsKeys.WEATHER_LAT] ?: 0f }
    val weather_lon: Flow<Float> = context.data_store.data.map { it[PrefsKeys.WEATHER_LON] ?: 0f }
    val hourly_temps_f: Flow<String> = context.data_store.data.map { it[PrefsKeys.HOURLY_TEMPS_F] ?: "" }
    val hourly_wcodes: Flow<String> = context.data_store.data.map { it[PrefsKeys.HOURLY_WCODES] ?: "" }
    val hourly_winds_mph: Flow<String> = context.data_store.data.map { it[PrefsKeys.HOURLY_WINDS_MPH] ?: "" }

    val element_padding_dp: Flow<Int> = context.data_store.data.map { it[PrefsKeys.ELEMENT_PADDING_DP] ?: 0 }
    val left_lane_clearance_dp: Flow<Int> = context.data_store.data.map { it[PrefsKeys.LEFT_LANE_CLEARANCE_DP] ?: 0 }
    val right_lane_clearance_dp: Flow<Int> = context.data_store.data.map { it[PrefsKeys.RIGHT_LANE_CLEARANCE_DP] ?: 0 }
    val left_vertical_offset_dp: Flow<Int> = context.data_store.data.map { it[PrefsKeys.LEFT_VERTICAL_OFFSET_DP] ?: 0 }
    val right_vertical_offset_dp: Flow<Int> = context.data_store.data.map { it[PrefsKeys.RIGHT_VERTICAL_OFFSET_DP] ?: 0 }
    val show_capsules: Flow<Boolean> = context.data_store.data.map { it[PrefsKeys.SHOW_CAPSULES] ?: true }
    val merged_lanes: Flow<Boolean> = context.data_store.data.map { it[PrefsKeys.MERGED_LANES] ?: false }
    val use_24_hour_time: Flow<Boolean> = context.data_store.data.map { it[PrefsKeys.USE_24_HOUR_TIME] ?: false }

    val overlay_settings: Flow<OverlaySettingsSnapshot> = combine(
        enabled, fontScale, animation_speed, show_date, show_battery, show_gif, gif_uri, theme_mode,
        global_italic_mode, fontFamily_choice, time_settings, date_settings, battery_settings,
        gif_settings, show_weather, weather_mode, weather_settings,
        element_padding_dp, left_lane_clearance_dp, right_lane_clearance_dp,
        left_vertical_offset_dp, right_vertical_offset_dp, show_capsules, merged_lanes,
        weather_zip, weather_location_mode, weather_location_source, weather_temp_f, weather_wind_mph, weather_backdrop, weather_code,
        weather_fetched_at, weather_lat, weather_lon, hourly_temps_f, hourly_wcodes, hourly_winds_mph, use_24_hour_time,
        weather_gust_mph
    ) { values ->
        OverlaySettingsSnapshot(
            enabled = values[0] as Boolean,
            fontScale = values[1] as Float,
            animationSpeed = values[2] as Float,
            showDate = values[3] as Boolean,
            showBattery = values[4] as Boolean,
            showGif = values[5] as Boolean,
            gifUri = values[6] as String?,
            themeMode = values[7] as Int,
            globalItalicMode = values[8] as Int,
            fontFamilyChoice = values[9] as Int,
            timeSettings = values[10] as ElementSettings,
            dateSettings = values[11] as ElementSettings,
            batterySettings = values[12] as ElementSettings,
            gifSettings = values[13] as ElementSettings,
            showWeather = values[14] as Boolean,
            weatherMode = values[15] as Int,
            weatherSettings = values[16] as ElementSettings,
            elementPaddingDp = values[17] as Int,
            leftLaneClearanceDp = values[18] as Int,
            rightLaneClearanceDp = values[19] as Int,
            leftVerticalOffsetDp = values[20] as Int,
            rightVerticalOffsetDp = values[21] as Int,
            showCapsules = values[22] as Boolean,
            mergedLanes = values[23] as Boolean,
            weatherZip = values[24] as String,
            weatherLocationMode = values[25] as Int,
            weatherLocationSource = values[26] as String,
            weatherTempF = values[27] as Float,
            weatherWindMph = values[28] as Float,
            weatherBackdrop = values[29] as Int,
            weatherCode = values[30] as Int,
            weatherFetchedAt = values[31] as Long,
            weatherLat = values[32] as Float,
            weatherLon = values[33] as Float,
            hourlyTempsF = values[34] as String,
            hourlyWCodes = values[35] as String,
            hourlyWindsMph = values[36] as String,
            use24HourTime = values[37] as Boolean,
            weatherGustMph = (values[38] as Float).takeIf { it > 0f }
        )
    }

    suspend fun set_enabled(value: Boolean) { context.data_store.edit { it[PrefsKeys.ENABLED] = value } }
    suspend fun set_fontScale(value: Float) { context.data_store.edit { it[PrefsKeys.FONT_SCALE] = value.coerceIn(0.8f, 1.5f) } }
    suspend fun set_animation_speed(value: Float) { context.data_store.edit { it[PrefsKeys.ANIMATION_SPEED] = value.coerceIn(0.05f, 2.5f) } }
    suspend fun set_show_date(value: Boolean) { context.data_store.edit { it[PrefsKeys.SHOW_DATE] = value } }
    suspend fun set_show_battery(value: Boolean) { context.data_store.edit { it[PrefsKeys.SHOW_BATTERY] = value } }
    suspend fun set_show_gif(value: Boolean) { context.data_store.edit { it[PrefsKeys.SHOW_GIF] = value } }
    suspend fun set_show_weather(value: Boolean) { context.data_store.edit { it[PrefsKeys.SHOW_WEATHER] = value } }
    suspend fun set_weather_mode(value: Int) { context.data_store.edit { it[PrefsKeys.WEATHER_MODE] = value } }
    suspend fun set_weather_backdrop(value: Int) { context.data_store.edit { it[PrefsKeys.WEATHER_BACKDROP] = value } }
    suspend fun set_gif_uri(value: String?) {
        context.data_store.edit { if (value != null) it[PrefsKeys.GIF_URI] = value else it.remove(PrefsKeys.GIF_URI) }
    }
    suspend fun set_theme_mode(value: Int) { context.data_store.edit { it[PrefsKeys.THEME_MODE] = value.coerceIn(0, 2) } }
    suspend fun set_fontFamily_choice(value: Int) { context.data_store.edit { it[PrefsKeys.FONT_FAMILY] = value.coerceIn(0, 2) } }
    suspend fun set_global_italic_mode(value: Int) {
        context.data_store.edit {
            it[PrefsKeys.GLOBAL_ITALIC_MODE] = value.coerceIn(ITALIC_DEFAULT, ITALIC_FORCE_OFF)
            it.remove(PrefsKeys.LEGACY_GLOBAL_ITALIC)
        }
    }

    suspend fun set_time_alignment(value: Int) { context.data_store.edit { it[PrefsKeys.TIME_ALIGNMENT] = value } }
    suspend fun set_time_offset_px(value: Int) { context.data_store.edit { it[PrefsKeys.TIME_OFFSET_PX] = value } }
    suspend fun set_time_pill_scale(value: Float) { context.data_store.edit { it[PrefsKeys.TIME_PILL_SCALE] = value } }
    suspend fun set_time_size_scale(value: Float) { context.data_store.edit { it[PrefsKeys.TIME_SIZE_SCALE] = value } }
    suspend fun set_time_weight(value: Int) { context.data_store.edit { it[PrefsKeys.TIME_WEIGHT] = value } }
    suspend fun set_time_italic(value: Boolean) { context.data_store.edit { it[PrefsKeys.TIME_ITALIC] = value } }
    suspend fun set_time_order(value: Int) { context.data_store.edit { it[PrefsKeys.TIME_ORDER] = value } }
    suspend fun set_time_pill_color(value: String) { context.data_store.edit { it[PrefsKeys.TIME_PILL_COLOR] = value } }
    suspend fun set_time_pill_stroke(value: String) { context.data_store.edit { it[PrefsKeys.TIME_PILL_STROKE] = value } }

    suspend fun set_date_alignment(value: Int) { context.data_store.edit { it[PrefsKeys.DATE_ALIGNMENT] = value } }
    suspend fun set_date_offset_px(value: Int) { context.data_store.edit { it[PrefsKeys.DATE_OFFSET_PX] = value } }
    suspend fun set_date_pill_scale(value: Float) { context.data_store.edit { it[PrefsKeys.DATE_PILL_SCALE] = value } }
    suspend fun set_date_size_scale(value: Float) { context.data_store.edit { it[PrefsKeys.DATE_SIZE_SCALE] = value } }
    suspend fun set_date_weight(value: Int) { context.data_store.edit { it[PrefsKeys.DATE_WEIGHT] = value } }
    suspend fun set_date_italic(value: Boolean) { context.data_store.edit { it[PrefsKeys.DATE_ITALIC] = value } }
    suspend fun set_date_order(value: Int) { context.data_store.edit { it[PrefsKeys.DATE_ORDER] = value } }
    suspend fun set_date_pill_color(value: String) { context.data_store.edit { it[PrefsKeys.DATE_PILL_COLOR] = value } }
    suspend fun set_date_pill_stroke(value: String) { context.data_store.edit { it[PrefsKeys.DATE_PILL_STROKE] = value } }

    suspend fun set_battery_alignment(value: Int) { context.data_store.edit { it[PrefsKeys.BATTERY_ALIGNMENT] = value } }
    suspend fun set_battery_offset_px(value: Int) { context.data_store.edit { it[PrefsKeys.BATTERY_OFFSET_PX] = value } }
    suspend fun set_battery_pill_scale(value: Float) { context.data_store.edit { it[PrefsKeys.BATTERY_PILL_SCALE] = value } }
    suspend fun set_battery_size_scale(value: Float) { context.data_store.edit { it[PrefsKeys.BATTERY_SIZE_SCALE] = value } }
    suspend fun set_battery_weight(value: Int) { context.data_store.edit { it[PrefsKeys.BATTERY_WEIGHT] = value } }
    suspend fun set_battery_italic(value: Boolean) { context.data_store.edit { it[PrefsKeys.BATTERY_ITALIC] = value } }
    suspend fun set_battery_order(value: Int) { context.data_store.edit { it[PrefsKeys.BATTERY_ORDER] = value } }
    suspend fun set_battery_pill_color(value: String) { context.data_store.edit { it[PrefsKeys.BATTERY_PILL_COLOR] = value } }
    suspend fun set_battery_pill_stroke(value: String) { context.data_store.edit { it[PrefsKeys.BATTERY_PILL_STROKE] = value } }

    suspend fun set_gif_alignment(value: Int) { context.data_store.edit { it[PrefsKeys.GIF_ALIGNMENT] = value } }
    suspend fun set_gif_offset_px(value: Int) { context.data_store.edit { it[PrefsKeys.GIF_OFFSET_PX] = value } }
    suspend fun set_gif_pill_scale(value: Float) { context.data_store.edit { it[PrefsKeys.GIF_PILL_SCALE] = value } }
    suspend fun set_gif_size_scale(value: Float) { context.data_store.edit { it[PrefsKeys.GIF_SIZE_SCALE] = value } }
    suspend fun set_gif_order(value: Int) { context.data_store.edit { it[PrefsKeys.GIF_ORDER] = value } }
    suspend fun set_gif_pill_color(value: String) { context.data_store.edit { it[PrefsKeys.GIF_PILL_COLOR] = value } }
    suspend fun set_gif_pill_stroke(value: String) { context.data_store.edit { it[PrefsKeys.GIF_PILL_STROKE] = value } }

    suspend fun set_weather_alignment(value: Int) { context.data_store.edit { it[PrefsKeys.WEATHER_ALIGNMENT] = value } }
    suspend fun set_weather_offset_px(value: Int) { context.data_store.edit { it[PrefsKeys.WEATHER_OFFSET_PX] = value } }
    suspend fun set_weather_pill_scale(value: Float) { context.data_store.edit { it[PrefsKeys.WEATHER_PILL_SCALE] = value } }
    suspend fun set_weather_size_scale(value: Float) { context.data_store.edit { it[PrefsKeys.WEATHER_SIZE_SCALE] = value } }
    suspend fun set_weather_weight(value: Int) { context.data_store.edit { it[PrefsKeys.WEATHER_WEIGHT] = value } }
    suspend fun set_weather_italic(value: Boolean) { context.data_store.edit { it[PrefsKeys.WEATHER_ITALIC] = value } }
    suspend fun set_weather_order(value: Int) { context.data_store.edit { it[PrefsKeys.WEATHER_ORDER] = value } }
    suspend fun set_weather_pill_color(value: String) { context.data_store.edit { it[PrefsKeys.WEATHER_PILL_COLOR] = value } }
    suspend fun set_weather_pill_stroke(value: String) { context.data_store.edit { it[PrefsKeys.WEATHER_PILL_STROKE] = value } }
    suspend fun set_weather_zip(value: String) { context.data_store.edit { it[PrefsKeys.WEATHER_ZIP] = value } }
    suspend fun set_weather_location_mode(value: Int) { context.data_store.edit { it[PrefsKeys.WEATHER_LOCATION_MODE] = value } }
    suspend fun set_weather_location_source(value: String) { context.data_store.edit { it[PrefsKeys.WEATHER_LOCATION_SOURCE] = value } }
    suspend fun set_weather_temp_f(value: Float) { context.data_store.edit { it[PrefsKeys.WEATHER_TEMP_F] = value } }
    suspend fun set_weather_wind_mph(value: Float) { context.data_store.edit { it[PrefsKeys.WEATHER_WIND_MPH] = value } }
    suspend fun set_weather_code(value: Int) { context.data_store.edit { it[PrefsKeys.WEATHER_CODE] = value } }
    suspend fun set_weather_fetched_at(value: Long) { context.data_store.edit { it[PrefsKeys.WEATHER_FETCHED_AT] = value } }
    suspend fun set_weather_lat(value: Float) { context.data_store.edit { it[PrefsKeys.WEATHER_LAT] = value } }
    suspend fun set_weather_lon(value: Float) { context.data_store.edit { it[PrefsKeys.WEATHER_LON] = value } }
    suspend fun set_hourly_temps_f(value: String) { context.data_store.edit { it[PrefsKeys.HOURLY_TEMPS_F] = value } }
    suspend fun set_hourly_wcodes(value: String) { context.data_store.edit { it[PrefsKeys.HOURLY_WCODES] = value } }
    suspend fun set_hourly_winds_mph(value: String) { context.data_store.edit { it[PrefsKeys.HOURLY_WINDS_MPH] = value } }
    suspend fun set_weather_snapshot(
        locationSource: String,
        lat: Float,
        lon: Float,
        tempF: Float,
        windMph: Float,
        gustMph: Float?,
        code: Int,
        hourlyTempsF: String,
        hourlyWCodes: String,
        hourlyWindsMph: String,
        fetchedAt: Long
    ) {
        context.data_store.edit {
            it[PrefsKeys.WEATHER_LOCATION_SOURCE] = locationSource
            it[PrefsKeys.WEATHER_LAT] = lat
            it[PrefsKeys.WEATHER_LON] = lon
            it[PrefsKeys.WEATHER_TEMP_F] = tempF
            it[PrefsKeys.WEATHER_WIND_MPH] = windMph
            if (gustMph != null) it[PrefsKeys.WEATHER_GUST_MPH] = gustMph else it.remove(PrefsKeys.WEATHER_GUST_MPH)
            it[PrefsKeys.WEATHER_CODE] = code
            it[PrefsKeys.HOURLY_TEMPS_F] = hourlyTempsF
            it[PrefsKeys.HOURLY_WCODES] = hourlyWCodes
            it[PrefsKeys.HOURLY_WINDS_MPH] = hourlyWindsMph
            it[PrefsKeys.WEATHER_FETCHED_AT] = fetchedAt
        } // edit
    } // set_weather_snapshot

    suspend fun set_element_padding_dp(value: Int) { context.data_store.edit { it[PrefsKeys.ELEMENT_PADDING_DP] = value } }
    suspend fun set_left_lane_clearance_dp(value: Int) { context.data_store.edit { it[PrefsKeys.LEFT_LANE_CLEARANCE_DP] = value } }
    suspend fun set_right_lane_clearance_dp(value: Int) { context.data_store.edit { it[PrefsKeys.RIGHT_LANE_CLEARANCE_DP] = value } }
    suspend fun set_left_vertical_offset_dp(value: Int) { context.data_store.edit { it[PrefsKeys.LEFT_VERTICAL_OFFSET_DP] = value } }
    suspend fun set_right_vertical_offset_dp(value: Int) { context.data_store.edit { it[PrefsKeys.RIGHT_VERTICAL_OFFSET_DP] = value } }
    suspend fun set_show_capsules(value: Boolean) { context.data_store.edit { it[PrefsKeys.SHOW_CAPSULES] = value } }
    suspend fun set_merged_lanes(value: Boolean) { context.data_store.edit { it[PrefsKeys.MERGED_LANES] = value } }
    suspend fun set_use_24_hour_time(value: Boolean) { context.data_store.edit { it[PrefsKeys.USE_24_HOUR_TIME] = value } }

    suspend fun reset_time_settings() { context.data_store.edit { it.remove(PrefsKeys.TIME_ALIGNMENT); it.remove(PrefsKeys.TIME_OFFSET_PX); it.remove(PrefsKeys.TIME_SIZE_SCALE); it.remove(PrefsKeys.TIME_PILL_SCALE); it.remove(PrefsKeys.TIME_WEIGHT); it.remove(PrefsKeys.TIME_ITALIC); it.remove(PrefsKeys.TIME_ORDER); it.remove(PrefsKeys.TIME_PILL_COLOR); it.remove(PrefsKeys.TIME_PILL_STROKE) } }
    suspend fun reset_date_settings() { context.data_store.edit { it.remove(PrefsKeys.DATE_ALIGNMENT); it.remove(PrefsKeys.DATE_OFFSET_PX); it.remove(PrefsKeys.DATE_SIZE_SCALE); it.remove(PrefsKeys.DATE_PILL_SCALE); it.remove(PrefsKeys.DATE_WEIGHT); it.remove(PrefsKeys.DATE_ITALIC); it.remove(PrefsKeys.DATE_ORDER); it.remove(PrefsKeys.DATE_PILL_COLOR); it.remove(PrefsKeys.DATE_PILL_STROKE) } }
    suspend fun reset_battery_settings() { context.data_store.edit { it.remove(PrefsKeys.BATTERY_ALIGNMENT); it.remove(PrefsKeys.BATTERY_OFFSET_PX); it.remove(PrefsKeys.BATTERY_SIZE_SCALE); it.remove(PrefsKeys.BATTERY_PILL_SCALE); it.remove(PrefsKeys.BATTERY_WEIGHT); it.remove(PrefsKeys.BATTERY_ITALIC); it.remove(PrefsKeys.BATTERY_ORDER); it.remove(PrefsKeys.BATTERY_PILL_COLOR); it.remove(PrefsKeys.BATTERY_PILL_STROKE) } }
    suspend fun reset_gif_settings() { context.data_store.edit { it.remove(PrefsKeys.GIF_ALIGNMENT); it.remove(PrefsKeys.GIF_OFFSET_PX); it.remove(PrefsKeys.GIF_SIZE_SCALE); it.remove(PrefsKeys.GIF_PILL_SCALE); it.remove(PrefsKeys.GIF_ORDER); it.remove(PrefsKeys.GIF_PILL_COLOR); it.remove(PrefsKeys.GIF_PILL_STROKE) } }
    suspend fun reset_weather_settings() { context.data_store.edit { it.remove(PrefsKeys.WEATHER_ALIGNMENT); it.remove(PrefsKeys.WEATHER_OFFSET_PX); it.remove(PrefsKeys.WEATHER_SIZE_SCALE); it.remove(PrefsKeys.WEATHER_PILL_SCALE); it.remove(PrefsKeys.WEATHER_WEIGHT); it.remove(PrefsKeys.WEATHER_ITALIC); it.remove(PrefsKeys.WEATHER_ORDER); it.remove(PrefsKeys.WEATHER_PILL_COLOR); it.remove(PrefsKeys.WEATHER_PILL_STROKE); it.remove(PrefsKeys.WEATHER_BACKDROP); it.remove(PrefsKeys.WEATHER_CODE) } }
    suspend fun reset_all_settings() { context.data_store.edit { it.clear() } }
} // prefs

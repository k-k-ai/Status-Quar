//// app/src/main/java/com/example/overlaybar/data/OverlaySettings.kt
//// Created 2026-04-22
//// overlaysettings module


/// Imports


package com.example.overlaybar.data

import androidx.compose.runtime.Immutable


/// Types


const val ITALIC_DEFAULT = 0
const val ITALIC_FORCE_ON = 1
const val ITALIC_FORCE_OFF = 2

enum class OverlayElementId {
    TIME,
    DATE,
    BATTERY,
    GIF,
    WEATHER
} // overlay_element_id

enum class SettingsTreeGroup {
    OVERLAY,
    DISPLAY,
    APPEARANCE,
    LAYOUT,
    ELEMENTS,
    TIME,
    DATE,
    BATTERY,
    GIF
} // settings_tree_group

const val WEATHER_BACKDROP_CLOUDY  = 0
const val WEATHER_BACKDROP_SUNNY   = 1
const val WEATHER_BACKDROP_RAINY   = 2
const val WEATHER_BACKDROP_SNOWY   = 3
const val WEATHER_BACKDROP_THUNDER = 4
const val WEATHER_BACKDROP_WINDY   = 5
const val WEATHER_BACKDROP_LIVE    = -1

const val WEATHER_LOCATION_MODE_SYSTEM = 0
const val WEATHER_LOCATION_MODE_ZIP = 1

const val WEATHER_LOCATION_CACHE_MAX_AGE_MILLIS = 60 * 60 * 1000L

const val WEATHER_LOCATION_SOURCE_DEVICE = "device"
const val WEATHER_LOCATION_SOURCE_CACHE = "cache"
const val WEATHER_LOCATION_SOURCE_ZIP = "zip"
const val WEATHER_LOCATION_SOURCE_PERMISSION = "permission"
const val WEATHER_LOCATION_SOURCE_LOCATION_OFF = "location_off"
const val WEATHER_LOCATION_SOURCE_UNAVAILABLE = "unavailable"
const val WEATHER_LOCATION_SOURCE_UNKNOWN = "unknown"


/// Functions


fun should_use_cached_system_weather_location(
    mode: Int,
    cachedFromSystem: Boolean,
    ageMillis: Long,
    maxAgeMillis: Long = WEATHER_LOCATION_CACHE_MAX_AGE_MILLIS
): Boolean {
    return mode == WEATHER_LOCATION_MODE_SYSTEM &&
        cachedFromSystem &&
        ageMillis in 0..maxAgeMillis
} // should_use_cached_system_weather_location

@Immutable
data class OverlaySettingsSnapshot(
    val enabled: Boolean = true,
    val fontScale: Float = 1.0f,
    val animationSpeed: Float = 1.0f,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val showGif: Boolean = true,
    val showWeather: Boolean = true,
    val weatherMode: Int = 0,
    val weatherBackdrop: Int = WEATHER_BACKDROP_LIVE,
    val gifUri: String? = null,
    val themeMode: Int = THEME_AUTO,
    val globalItalicMode: Int = ITALIC_DEFAULT,
    val fontFamilyChoice: Int = FONT_APTOS_SANS,
    val timeSettings: ElementSettings = ElementSettings(
        alignment = ALIGN_RIGHT,
        pillScale = 0.8f,
        weight = WEIGHT_BOLD,
        offsetPx = 16,
        order = 1
    ),
    val dateSettings: ElementSettings = ElementSettings(
        alignment = ALIGN_RIGHT,
        pillScale = 0.8f,
        weight = WEIGHT_BOLD,
        offsetPx = 0,
        order = 1
    ),
    val batterySettings: ElementSettings = ElementSettings(
        alignment = ALIGN_RIGHT,
        pillScale = 0.8f,
        weight = WEIGHT_BOLD,
        offsetPx = 20,
        order = 1
    ),
    val gifSettings: ElementSettings = ElementSettings(
        alignment = ALIGN_LEFT,
        pillScale = 0.8f,
        sizeScale = 2.0f,
        weight = WEIGHT_NORMAL,
        pillColor = "ffffff00",
        pillStrokeColor = "ffffff00",
        order = 0
    ),
    val weatherSettings: ElementSettings = ElementSettings(
        alignment = ALIGN_LEFT,
        pillScale = 0.8f,
        weight = WEIGHT_BOLD,
        offsetPx = 320,
        order = 4
    ),
    val elementPaddingDp: Int = 0,
    val leftLaneClearanceDp: Int = 0,
    val rightLaneClearanceDp: Int = 0,
    val leftVerticalOffsetDp: Int = 0,
    val rightVerticalOffsetDp: Int = 0,
    val showCapsules: Boolean = true,
    val mergedLanes: Boolean = false,
    val use24HourTime: Boolean = false,
    val weatherZip: String = "14228",
    val weatherLocationMode: Int = WEATHER_LOCATION_MODE_SYSTEM,
    val weatherLocationSource: String = WEATHER_LOCATION_SOURCE_UNKNOWN,
    val weatherTempF: Float = 0f,
    val weatherWindMph: Float = 0f,
    val weatherGustMph: Float? = null,
    val weatherCode: Int = 0,
    val weatherFetchedAt: Long = 0L,
    val weatherLat: Float = 0f,
    val weatherLon: Float = 0f,
    val hourlyTempsF: String = "",
    val hourlyWCodes: String = "",
    val hourlyWindsMph: String = ""
) {
    val resolvedTimeSettings: ElementSettings
        get() = timeSettings.resolved(globalItalicMode)

    val resolvedDateSettings: ElementSettings
        get() = dateSettings.resolved(globalItalicMode)

    val resolvedBatterySettings: ElementSettings
        get() = batterySettings.resolved(globalItalicMode)

    fun isElementVisible(elementId: OverlayElementId): Boolean = when (elementId) {
        OverlayElementId.TIME -> true
        OverlayElementId.DATE -> showDate
        OverlayElementId.BATTERY -> showBattery
        OverlayElementId.GIF -> showGif
        OverlayElementId.WEATHER -> showWeather
    } // is_element_visible

    fun orderedElementsForSide(alignment: Int): List<OverlayElementId> =
        buildList {
            if (resolvedTimeSettings.alignment == alignment) {
                add(resolvedTimeSettings.order to OverlayElementId.TIME)
            }
            if (showDate && resolvedDateSettings.alignment == alignment) {
                add(resolvedDateSettings.order to OverlayElementId.DATE)
            }
            if (showBattery && resolvedBatterySettings.alignment == alignment) {
                add(resolvedBatterySettings.order to OverlayElementId.BATTERY)
            }
            if (showGif && gifUri != null && gifSettings.alignment == alignment) {
                add(gifSettings.order to OverlayElementId.GIF)
            }
        }.sortedBy { it.first }.map { it.second } // build_list
} // overlay_settings_snapshot

fun OverlaySettingsSnapshot.elementSettings(elementId: OverlayElementId): ElementSettings = when (elementId) {
    OverlayElementId.TIME -> timeSettings
    OverlayElementId.DATE -> dateSettings
    OverlayElementId.BATTERY -> batterySettings
    OverlayElementId.GIF -> gifSettings
        OverlayElementId.WEATHER -> weatherSettings
} // element_settings

fun OverlaySettingsSnapshot.withElementSettings(
    elementId: OverlayElementId,
    settings: ElementSettings
): OverlaySettingsSnapshot = when (elementId) {
    OverlayElementId.TIME -> copy(timeSettings = settings)
    OverlayElementId.DATE -> copy(dateSettings = settings)
    OverlayElementId.BATTERY -> copy(batterySettings = settings)
    OverlayElementId.GIF -> copy(gifSettings = settings)
            OverlayElementId.WEATHER -> copy(weatherSettings = settings)
} // with_element_settings

fun OverlaySettingsSnapshot.clearanceDpFor(alignment: Int): Int =
    if (alignment == ALIGN_LEFT) leftLaneClearanceDp else rightLaneClearanceDp

fun OverlaySettingsSnapshot.verticalOffsetDpFor(alignment: Int): Int =
    if (alignment == ALIGN_LEFT) leftVerticalOffsetDp else rightVerticalOffsetDp

@Immutable
data class SettingsUiState(
    val settings: OverlaySettingsSnapshot = OverlaySettingsSnapshot(),
    val accessibilityServiceEnabled: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val batteryLevel: Int = 72,
    val isCharging: Boolean = false,
    val expandedGroups: Set<SettingsTreeGroup> = default_expanded_groups(),
    val selectedElementId: OverlayElementId = OverlayElementId.TIME
) {
    val needsAccessibilitySetup: Boolean
        get() = !accessibilityServiceEnabled

    fun isExpanded(group: SettingsTreeGroup): Boolean = expandedGroups.contains(group)
}

// One UI 8.5: open calmer. Only the slab the user most likely needs first
// (Overlay — accessibility setup status) starts expanded; everything else is
// collapsed and reveals on tap.
fun default_expanded_groups(): Set<SettingsTreeGroup> = setOf(
    SettingsTreeGroup.OVERLAY
)

fun resolve_global_italic_mode(storedMode: Int?, legacyGlobalItalic: Boolean?): Int = when (storedMode) {
    ITALIC_DEFAULT,
    ITALIC_FORCE_ON,
    ITALIC_FORCE_OFF -> storedMode
    else -> if (legacyGlobalItalic == true) ITALIC_FORCE_ON else ITALIC_DEFAULT
}

fun resolve_italic(globalItalicMode: Int, elementItalic: Boolean): Boolean = when (globalItalicMode) {
    ITALIC_FORCE_ON -> true
    ITALIC_FORCE_OFF -> false
    else -> elementItalic
}

fun resolve_dark_theme(themeMode: Int, systemDarkTheme: Boolean): Boolean = when (themeMode) {
    THEME_LIGHT -> false
    THEME_DARK -> true
    else -> systemDarkTheme
}

fun ElementSettings.resolved(globalItalicMode: Int): ElementSettings =
    copy(italic = resolve_italic(globalItalicMode, italic))

fun theme_mode_label(themeMode: Int): String = when (themeMode) {
    THEME_LIGHT -> "Light"
    THEME_DARK -> "Dark"
    else -> "Auto"
}

fun fontFamily_label(fontFamily: Int): String = when (fontFamily) {
    FONT_APTOS_SERIF -> "Aptos Serif"
    FONT_SYSTEM -> "System"
    else -> "Aptos"
}

fun italic_mode_label(globalItalicMode: Int): String = when (globalItalicMode) {
    ITALIC_FORCE_ON -> "Always on"
    ITALIC_FORCE_OFF -> "Always off"
    else -> "Per element"
}

fun overlay_group_summary(uiState: SettingsUiState): String = when {
    !uiState.accessibilityServiceEnabled -> "Setup required"
    uiState.settings.enabled -> "Ready and visible"
    else -> "Service active, overlay hidden"
}

fun display_group_summary(settings: OverlaySettingsSnapshot): String {
    val visibleItems = buildList {
        add("Time")
        if (settings.showDate) add("Date")
        if (settings.showBattery) add("Battery")
        if (settings.showGif && settings.gifUri != null) add("GIF")
    }
    return visibleItems.joinToString(separator = " • ")
}

fun appearance_group_summary(settings: OverlaySettingsSnapshot): String =
    "${theme_mode_label(settings.themeMode)} • ${fontFamily_label(settings.fontFamilyChoice)} • ${italic_mode_label(settings.globalItalicMode)} • Anim ${String.format("%.2f", settings.animationSpeed)}x"

fun layout_group_summary(settings: OverlaySettingsSnapshot): String =
    "Gap ${settings.elementPaddingDp} dp • L clear ${settings.leftLaneClearanceDp} dp • R clear ${settings.rightLaneClearanceDp} dp"

fun elements_group_summary(settings: OverlaySettingsSnapshot): String =
    "${settings.orderedElementsForSide(ALIGN_LEFT).size} left • ${settings.orderedElementsForSide(ALIGN_RIGHT).size} right"

fun element_summary(settings: ElementSettings): String {
    val side = if (settings.alignment == ALIGN_LEFT) "Left lane" else "Right lane"
    val weight = when (settings.weight) {
        WEIGHT_NORMAL -> "Normal"
        WEIGHT_BOLD -> "Bold"
        else -> "Black"
    }
    val italic = if (settings.italic) "Italic" else "Regular"
    return "$side • ${settings.order + 1} • Pill ${String.format("%.1f", settings.pillScale)}x • $weight • $italic"
}

fun signed_dp(value: Int): String = "${if (value >= 0) "+" else ""}$value dp"

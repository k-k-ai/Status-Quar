package com.example.overlaybar

import com.example.overlaybar.data.ALIGN_LEFT
import com.example.overlaybar.data.ALIGN_RIGHT
import com.example.overlaybar.data.ElementSettings
import com.example.overlaybar.data.ITALIC_DEFAULT
import com.example.overlaybar.data.ITALIC_FORCE_OFF
import com.example.overlaybar.data.ITALIC_FORCE_ON
import com.example.overlaybar.data.OverlayElementId
import com.example.overlaybar.data.OverlaySettingsSnapshot
import com.example.overlaybar.data.layout_group_summary
import com.example.overlaybar.data.resolve_global_italic_mode
import com.example.overlaybar.data.resolve_italic
import com.example.overlaybar.data.should_use_cached_system_weather_location
import com.example.overlaybar.data.WEATHER_LOCATION_CACHE_MAX_AGE_MILLIS
import com.example.overlaybar.data.WEATHER_LOCATION_MODE_SYSTEM
import com.example.overlaybar.data.WEATHER_LOCATION_MODE_ZIP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlaySettingsTest {

    @Test
    fun legacy_global_italic_true_maps_to_force_on() {
        assertEquals(
            ITALIC_FORCE_ON,
            resolve_global_italic_mode(storedMode = null, legacyGlobalItalic = true)
        )
    }

    @Test
    fun missing_global_italic_defaults_to_per_element() {
        assertEquals(
            ITALIC_DEFAULT,
            resolve_global_italic_mode(storedMode = null, legacyGlobalItalic = null)
        )
        assertEquals(
            ITALIC_DEFAULT,
            resolve_global_italic_mode(storedMode = null, legacyGlobalItalic = false)
        )
    }

    @Test
    fun italic_resolution_respects_force_modes() {
        assertTrue(resolve_italic(ITALIC_FORCE_ON, elementItalic = false))
        assertFalse(resolve_italic(ITALIC_FORCE_OFF, elementItalic = true))
        assertTrue(resolve_italic(ITALIC_DEFAULT, elementItalic = true))
    }

    @Test
    fun ordered_elements_for_side_returns_visible_elements_in_order() {
        val settings = OverlaySettingsSnapshot(
            showDate = true,
            showBattery = true,
            showGif = true,
            gifUri = "content://gif",
            timeSettings = ElementSettings(alignment = ALIGN_LEFT, order = 2),
            dateSettings = ElementSettings(alignment = ALIGN_LEFT, order = 0),
            batterySettings = ElementSettings(alignment = ALIGN_LEFT, order = 1),
            gifSettings = ElementSettings(alignment = ALIGN_RIGHT, order = 0)
        )

        assertEquals(
            listOf(OverlayElementId.DATE, OverlayElementId.BATTERY, OverlayElementId.TIME),
            settings.orderedElementsForSide(ALIGN_LEFT)
        )
        assertEquals(
            listOf(OverlayElementId.GIF),
            settings.orderedElementsForSide(ALIGN_RIGHT)
        )
    }

    @Test
    fun layout_group_summary_reports_lane_clearance() {
        val settings = OverlaySettingsSnapshot(
            elementPaddingDp = 12,
            leftLaneClearanceDp = 118,
            rightLaneClearanceDp = 24
        )

        assertEquals(
            "Gap 12 dp • L clear 118 dp • R clear 24 dp",
            layout_group_summary(settings)
        )
    }

    @Test
    fun cached_system_location_is_only_reused_for_system_mode_and_system_origin() {
        assertTrue(
            should_use_cached_system_weather_location(
                mode = WEATHER_LOCATION_MODE_SYSTEM,
                cachedFromSystem = true,
                ageMillis = WEATHER_LOCATION_CACHE_MAX_AGE_MILLIS
            )
        )
        assertFalse(
            should_use_cached_system_weather_location(
                mode = WEATHER_LOCATION_MODE_SYSTEM,
                cachedFromSystem = false,
                ageMillis = 5_000L
            )
        )
        assertFalse(
            should_use_cached_system_weather_location(
                mode = WEATHER_LOCATION_MODE_ZIP,
                cachedFromSystem = true,
                ageMillis = 5_000L
            )
        )
    }
}

//// app/src/main/java/com/example/overlaybar/SettingsViewModel.kt
//// Created 2026-04-22
//// settingsviewmodel module


/// Imports


package com.example.overlaybar

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.overlaybar.data.OverlayElementId
import com.example.overlaybar.data.Prefs
import com.example.overlaybar.data.SettingsTreeGroup
import com.example.overlaybar.data.SettingsUiState
import com.example.overlaybar.data.default_expanded_groups
import com.example.overlaybar.overlay.AccessibilityOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/// Types


sealed interface SettingsAction {
    data class ToggleGroup(val group: SettingsTreeGroup) : SettingsAction
    data class SetOverlayEnabled(val value: Boolean) : SettingsAction
    data class SetShowDate(val value: Boolean) : SettingsAction
    data class SetShowBattery(val value: Boolean) : SettingsAction
    data class SetShowGif(val value: Boolean) : SettingsAction
    data class SetShowWeather(val value: Boolean) : SettingsAction
    data class CommitWeatherMode(val value: Int) : SettingsAction
    data class SetThemeMode(val value: Int) : SettingsAction
    data class SetFontFamily(val value: Int) : SettingsAction
    data class SetGlobalItalicMode(val value: Int) : SettingsAction
    data class SetShowCapsules(val value: Boolean) : SettingsAction
    data class SetMergedLanes(val value: Boolean) : SettingsAction
    data class SetUse24HourTime(val value: Boolean) : SettingsAction
    data class SelectElement(val elementId: OverlayElementId) : SettingsAction
    data class CommitFontScale(val value: Float) : SettingsAction
    data class CommitAnimationSpeed(val value: Float) : SettingsAction
    data class CommitElementPadding(val value: Int) : SettingsAction
    data class CommitLeftLaneClearance(val value: Int) : SettingsAction
    data class CommitRightLaneClearance(val value: Int) : SettingsAction
    data class CommitLeftVerticalOffset(val value: Int) : SettingsAction
    data class CommitRightVerticalOffset(val value: Int) : SettingsAction
    data class SetGifUri(val value: String?) : SettingsAction
    data class SetElementAlignment(val elementId: OverlayElementId, val value: Int) : SettingsAction
    data class CommitElementOffset(val elementId: OverlayElementId, val value: Int) : SettingsAction
    data class CommitElementPillScale(val elementId: OverlayElementId, val value: Float) : SettingsAction
    data class CommitElementSize(val elementId: OverlayElementId, val value: Float) : SettingsAction
    data class CommitElementPillColor(val elementId: OverlayElementId, val value: String) : SettingsAction
    data class CommitElementStrokeColor(val elementId: OverlayElementId, val value: String) : SettingsAction
    data class SetElementWeight(val elementId: OverlayElementId, val value: Int) : SettingsAction
    data class SetElementItalic(val elementId: OverlayElementId, val value: Boolean) : SettingsAction
    data class SetElementOrder(val elementId: OverlayElementId, val value: Int) : SettingsAction
    data class ResetElement(val elementId: OverlayElementId) : SettingsAction
    data class SetWeatherZip(val value: String) : SettingsAction
    data class SetWeatherLocationMode(val value: Int) : SettingsAction
    data class CommitWeatherBackdrop(val value: Int) : SettingsAction
    data object RefreshWeather : SettingsAction
    data object ResetAll : SettingsAction
    data object RefreshSystemState : SettingsAction
} // settings_action

class SettingsViewModel(
    private val appContext: Context,
    private val prefs: Prefs = Prefs(appContext)
) : ViewModel() {

    private val accessibility_service_enabled = MutableStateFlow(false)
    private val has_location_permission = MutableStateFlow(false)
    private val battery_level = MutableStateFlow(72)
    private val is_charging = MutableStateFlow(false)
    private val expanded_groups = MutableStateFlow(default_expanded_groups())
    private val selected_element = MutableStateFlow(OverlayElementId.TIME)
    private var battery_receiver_registered = false

    private val battery_receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            update_battery_state(intent)
        } // on_receive
    } // battery_receiver

    val ui_state: StateFlow<SettingsUiState> = combine(
        prefs.overlay_settings,
        accessibility_service_enabled,
        has_location_permission,
        battery_level,
        is_charging,
        expanded_groups,
        selected_element
    ) { values ->
        @Suppress("UNCHECKED_CAST")
            SettingsUiState(
                settings = values[0] as com.example.overlaybar.data.OverlaySettingsSnapshot,
                accessibilityServiceEnabled = values[1] as Boolean,
                hasLocationPermission = values[2] as Boolean,
                batteryLevel = values[3] as Int,
                isCharging = values[4] as Boolean,
                expandedGroups = values[5] as Set<SettingsTreeGroup>,
                selectedElementId = values[6] as OverlayElementId
            )
        }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    init {
        register_battery_receiver()
        refresh_system_state()
        viewModelScope.launch {
            val zip = prefs.weather_zip.first()
            if (zip.length == 5) {
                val age = System.currentTimeMillis() - prefs.weather_fetched_at.first()
                if (age > 30 * 60 * 1000L) AccessibilityOverlayService.weatherRefreshRequests.tryEmit(Unit)
            }
        } // launch
    } // init

    fun on_action(action: SettingsAction) {
        when (action) {
            is SettingsAction.ToggleGroup -> toggle_group(action.group)
            is SettingsAction.SetOverlayEnabled -> launch_edit { prefs.set_enabled(action.value) }
            is SettingsAction.SetShowDate -> launch_edit { prefs.set_show_date(action.value) }
            is SettingsAction.SetShowBattery -> launch_edit { prefs.set_show_battery(action.value) }
            is SettingsAction.SetShowGif -> launch_edit { prefs.set_show_gif(action.value) }
            is SettingsAction.SetShowWeather -> launch_edit { prefs.set_show_weather(action.value) }
            is SettingsAction.CommitWeatherMode -> launch_edit { prefs.set_weather_mode(action.value) }
            is SettingsAction.SetWeatherZip -> launch_edit {
                prefs.set_weather_zip(action.value)
                if (prefs.weather_location_mode.first() == com.example.overlaybar.data.WEATHER_LOCATION_MODE_ZIP) {
                    AccessibilityOverlayService.weatherRefreshRequests.emit(Unit)
                }
            }
            is SettingsAction.SetWeatherLocationMode -> launch_edit {
                prefs.set_weather_location_mode(action.value)
                AccessibilityOverlayService.weatherRefreshRequests.emit(Unit)
            }
            is SettingsAction.CommitWeatherBackdrop -> launch_edit { prefs.set_weather_backdrop(action.value) }
            SettingsAction.RefreshWeather -> viewModelScope.launch {
                AccessibilityOverlayService.weatherRefreshRequests.emit(Unit)
            }
            is SettingsAction.SetThemeMode -> launch_edit { prefs.set_theme_mode(action.value) }
            is SettingsAction.SetFontFamily -> launch_edit { prefs.set_fontFamily_choice(action.value) }
            is SettingsAction.SetGlobalItalicMode -> launch_edit { prefs.set_global_italic_mode(action.value) }
            is SettingsAction.SetShowCapsules -> launch_edit { prefs.set_show_capsules(action.value) }
            is SettingsAction.SetMergedLanes -> launch_edit { prefs.set_merged_lanes(action.value) }
            is SettingsAction.SetUse24HourTime -> launch_edit { prefs.set_use_24_hour_time(action.value) }
            is SettingsAction.SelectElement -> selected_element.value = action.elementId
            is SettingsAction.CommitFontScale -> launch_edit { prefs.set_fontScale(action.value) }
            is SettingsAction.CommitAnimationSpeed -> launch_edit { prefs.set_animation_speed(action.value) }
            is SettingsAction.CommitElementPadding -> launch_edit { prefs.set_element_padding_dp(action.value) }
            is SettingsAction.CommitLeftLaneClearance -> launch_edit { prefs.set_left_lane_clearance_dp(action.value) }
            is SettingsAction.CommitRightLaneClearance -> launch_edit { prefs.set_right_lane_clearance_dp(action.value) }
            is SettingsAction.CommitLeftVerticalOffset -> launch_edit { prefs.set_left_vertical_offset_dp(action.value) }
            is SettingsAction.CommitRightVerticalOffset -> launch_edit { prefs.set_right_vertical_offset_dp(action.value) }
            is SettingsAction.SetGifUri -> launch_edit {
                prefs.set_gif_uri(action.value)
                prefs.set_show_gif(action.value != null)
            }
            is SettingsAction.SetElementAlignment -> update_alignment(action.elementId, action.value)
            is SettingsAction.CommitElementOffset -> update_offset(action.elementId, action.value)
            is SettingsAction.CommitElementPillScale -> update_pill_scale(action.elementId, action.value)
            is SettingsAction.CommitElementSize -> update_size_scale(action.elementId, action.value)
            is SettingsAction.CommitElementPillColor -> update_pill_color(action.elementId, action.value)
            is SettingsAction.CommitElementStrokeColor -> update_pill_stroke_color(action.elementId, action.value)
            is SettingsAction.SetElementWeight -> update_weight(action.elementId, action.value)
            is SettingsAction.SetElementItalic -> update_italic(action.elementId, action.value)
            is SettingsAction.SetElementOrder -> update_order(action.elementId, action.value)
            is SettingsAction.ResetElement -> reset_element(action.elementId)
            SettingsAction.ResetAll -> launch_edit { prefs.reset_all_settings() }
            SettingsAction.RefreshSystemState -> refresh_system_state()
        } // when
    } // on_action

    private fun toggle_group(group: SettingsTreeGroup) {
        expanded_groups.update { current ->
            if (group in current) current - group else current + group
        } // update
    } // toggle_group

    private fun refresh_system_state() {
        accessibility_service_enabled.value = is_accessibility_service_enabled(appContext)
        has_location_permission.value = has_weather_location_permission(appContext)
        update_battery_state(appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
    } // refresh_system_state

    private fun register_battery_receiver() {
        if (battery_receiver_registered) return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(battery_receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(battery_receiver, filter)
        }
        battery_receiver_registered = true
        update_battery_state(appContext.registerReceiver(null, filter))
    } // register_battery_receiver

    private fun update_battery_state(intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        if (level >= 0 && scale > 0) {
            battery_level.value = (level * 100 / scale.toFloat()).toInt().coerceIn(0, 100)
        }
        is_charging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    } // update_battery_state

    private fun is_accessibility_service_enabled(context: Context): Boolean {
        val accessibility_enabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1
        if (!accessibility_enabled) return false

        val enabled_services = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val target_service = ComponentName(
            context,
            AccessibilityOverlayService::class.java
        ).flattenToString()

        return enabled_services.split(':').any { it.equals(target_service, ignoreCase = true) }
    } // is_accessibility_service_enabled

    private fun has_weather_location_permission(context: Context): Boolean {
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        return coarse == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            fine == android.content.pm.PackageManager.PERMISSION_GRANTED
    } // has_weather_location_permission

    private fun update_alignment(elementId: OverlayElementId, value: Int) = launch_edit {
        when (elementId) {
            OverlayElementId.TIME -> prefs.set_time_alignment(value)
            OverlayElementId.DATE -> prefs.set_date_alignment(value)
            OverlayElementId.BATTERY -> prefs.set_battery_alignment(value)
            OverlayElementId.GIF -> prefs.set_gif_alignment(value)
            OverlayElementId.WEATHER -> prefs.set_weather_alignment(value)
        } // when
    } // update_alignment

    private fun update_offset(elementId: OverlayElementId, value: Int) = launch_edit {
        when (elementId) {
            OverlayElementId.TIME -> prefs.set_time_offset_px(value)
            OverlayElementId.DATE -> prefs.set_date_offset_px(value)
            OverlayElementId.BATTERY -> prefs.set_battery_offset_px(value)
            OverlayElementId.GIF -> prefs.set_gif_offset_px(value)
            OverlayElementId.WEATHER -> prefs.set_weather_offset_px(value)
        } // when
    } // update_offset

    private fun update_pill_color(elementId: OverlayElementId, value: String) = launch_edit {
        when (elementId) {
            OverlayElementId.TIME -> prefs.set_time_pill_color(value)
            OverlayElementId.DATE -> prefs.set_date_pill_color(value)
            OverlayElementId.BATTERY -> prefs.set_battery_pill_color(value)
            OverlayElementId.GIF -> prefs.set_gif_pill_color(value)
            OverlayElementId.WEATHER -> prefs.set_weather_pill_color(value)
        } // when
    } // update_pill_color

    private fun update_pill_stroke_color(elementId: OverlayElementId, value: String) = launch_edit {
        when (elementId) {
            OverlayElementId.TIME -> prefs.set_time_pill_stroke(value)
            OverlayElementId.DATE -> prefs.set_date_pill_stroke(value)
            OverlayElementId.BATTERY -> prefs.set_battery_pill_stroke(value)
            OverlayElementId.GIF -> prefs.set_gif_pill_stroke(value)
            OverlayElementId.WEATHER -> prefs.set_weather_pill_stroke(value)
        } // when
    } // update_pill_stroke_color

    private fun update_size_scale(elementId: OverlayElementId, value: Float) = launch_edit {
        when (elementId) {
            OverlayElementId.TIME -> prefs.set_time_size_scale(value)
            OverlayElementId.DATE -> prefs.set_date_size_scale(value)
            OverlayElementId.BATTERY -> prefs.set_battery_size_scale(value)
            OverlayElementId.GIF -> prefs.set_gif_size_scale(value)
            OverlayElementId.WEATHER -> prefs.set_weather_size_scale(value)
        } // when
    } // update_size_scale

    private fun update_pill_scale(elementId: OverlayElementId, value: Float) = launch_edit {
        when (elementId) {
            OverlayElementId.TIME -> prefs.set_time_pill_scale(value)
            OverlayElementId.DATE -> prefs.set_date_pill_scale(value)
            OverlayElementId.BATTERY -> prefs.set_battery_pill_scale(value)
            OverlayElementId.GIF -> prefs.set_gif_pill_scale(value)
            OverlayElementId.WEATHER -> prefs.set_weather_pill_scale(value)
        } // when
    } // update_pill_scale

    private fun update_weight(elementId: OverlayElementId, value: Int) = launch_edit {
        when (elementId) {
            OverlayElementId.TIME -> prefs.set_time_weight(value)
            OverlayElementId.DATE -> prefs.set_date_weight(value)
            OverlayElementId.BATTERY -> prefs.set_battery_weight(value)
            OverlayElementId.GIF -> Unit
            OverlayElementId.WEATHER -> prefs.set_weather_weight(value)
        } // when
    } // update_weight

    private fun update_italic(elementId: OverlayElementId, value: Boolean) = launch_edit {
        when (elementId) {
            OverlayElementId.TIME -> prefs.set_time_italic(value)
            OverlayElementId.DATE -> prefs.set_date_italic(value)
            OverlayElementId.BATTERY -> prefs.set_battery_italic(value)
            OverlayElementId.GIF -> Unit
            OverlayElementId.WEATHER -> prefs.set_weather_italic(value)
        } // when
    } // update_italic

    private fun update_order(elementId: OverlayElementId, value: Int) = launch_edit {
        when (elementId) {
            OverlayElementId.TIME -> prefs.set_time_order(value)
            OverlayElementId.DATE -> prefs.set_date_order(value)
            OverlayElementId.BATTERY -> prefs.set_battery_order(value)
            OverlayElementId.GIF -> prefs.set_gif_order(value)
            OverlayElementId.WEATHER -> prefs.set_weather_order(value)
        } // when
    } // update_order

    private fun reset_element(elementId: OverlayElementId) = launch_edit {
        when (elementId) {
            OverlayElementId.TIME -> prefs.reset_time_settings()
            OverlayElementId.DATE -> prefs.reset_date_settings()
            OverlayElementId.BATTERY -> prefs.reset_battery_settings()
            OverlayElementId.GIF -> prefs.reset_gif_settings()
            OverlayElementId.WEATHER -> prefs.reset_weather_settings()
        } // when
    } // reset_element

    private fun launch_edit(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    } // launch_edit

    override fun onCleared() {
        if (battery_receiver_registered) {
            runCatching { appContext.unregisterReceiver(battery_receiver) }
            battery_receiver_registered = false
        }
        super.onCleared()
    } // on_cleared
} // settings_view_model

class SettingsViewModelFactory(
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(appContext.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    } // create
} // settings_view_model_factory

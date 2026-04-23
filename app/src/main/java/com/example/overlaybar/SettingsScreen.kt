//// app/src/main/java/com/example/overlaybar/SettingsScreen.kt
//// Created 2026-04-22
//// settingsscreen module


/// Imports


package com.example.overlaybar

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.overlaybar.ui.theme.*
import com.example.overlaybar.data.*
import kotlin.math.roundToInt


/// Symbols


private val page_card_shape = OneUi.ShapeCard
private val action_shape = OneUi.ShapePill


/// UI Components


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settings_screen(
    settings: OverlaySettingsSnapshot,
    selected_element_id: OverlayElementId,
    needs_accessibility_setup: Boolean,
    has_location_permission: Boolean,
    on_action: (SettingsAction) -> Unit,
    on_open_accessibility_settings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var show_reset_dialog by rememberSaveable { mutableStateOf(false) }
    val gif_picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            on_action(SettingsAction.SetGifUri(it.toString()))
        }
    }
    val location_permission_launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        on_action(SettingsAction.RefreshSystemState)
        on_action(SettingsAction.RefreshWeather)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(settings_screen_background())
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(
            start = OneUi.ScreenHorizontalPadding,
            top = OneUi.ScreenTopBreath,
            end = OneUi.ScreenHorizontalPadding,
            bottom = OneUi.ScreenBottomBreath
        ),
        verticalArrangement = Arrangement.spacedBy(OneUi.CardOuterSpacing)
    ) {
        item(key = "page_header", contentType = "header") {
            settings_page_header()
        }
        item(key = "accessibility_banner", contentType = "banner") {
            accessibility_banner(
                needs_setup = needs_accessibility_setup,
                on_open = on_open_accessibility_settings,
                on_refresh = { on_action(SettingsAction.RefreshSystemState) }
            )
        }
        item(key = "placement_label", contentType = "label") {
            item_header("Quar", Modifier.padding(start = OneUi.SpaceS, bottom = OneUi.SpaceXS))
        }
        item(key = "placement_card", contentType = "placement") {
            placement_card(
                settings = settings,
                selected_element_id = selected_element_id,
                on_action = on_action
            )
        }
        item(key = "selected_element_label_${selected_element_id.name}", contentType = "label") {
            val label = when (selected_element_id) {
                OverlayElementId.TIME -> "Time"
                OverlayElementId.DATE -> "Date"
                OverlayElementId.BATTERY -> "Battery"
                OverlayElementId.GIF -> "GIF"
                OverlayElementId.WEATHER -> "Weather"
            }
            item_header(label, Modifier.padding(start = OneUi.SpaceS, bottom = OneUi.SpaceXS))
        }
        item(key = "selected_element_editor_${selected_element_id.name}", contentType = "editor") {
            selected_element_editor_card(
                settings = settings,
                has_location_permission = has_location_permission,
                element_id = selected_element_id,
                on_action = on_action,
                on_pick_gif = { gif_picker.launch(arrayOf("image/gif")) },
                on_request_location_permission = {
                    location_permission_launcher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
            )
        }
        item(key = "appearance_label", contentType = "label") {
            item_header("Appearance", Modifier.padding(start = OneUi.SpaceS, bottom = OneUi.SpaceXS))
        }
        item(key = "appearance_card", contentType = "appearance") {
            global_appearance_card(settings, on_action)
        }
        item(key = "export_row", contentType = "action") {
            export_tuning_row(on_export = { copy_to_clipboard(context, build_tuning_snapshot(settings)) })
        }
        item(key = "reset_row", contentType = "action") {
            reset_row(on_confirm = { show_reset_dialog = true })
        }
        item(key = "version_footer", contentType = "footer") {
            version_footer()
        }
    }

    if (show_reset_dialog) {
        AlertDialog(
            onDismissRequest = { show_reset_dialog = false },
            title = { Text("Reset all settings?") },
            text = { Text("This clears theme, layout, typography, GIF, and element preferences.") },
            confirmButton = {
                Button(onClick = { show_reset_dialog = false; on_action(SettingsAction.ResetAll) }, shape = action_shape) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { show_reset_dialog = false }) { Text("Cancel") } }
        )
    }
} // settings_screen

@Composable
private fun settings_page_header() {
    val screen_height = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(screen_height / 2)
            .padding(bottom = OneUi.SpaceL),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Status Quar",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Normal
        )
    }
} // settings_page_header

@Composable
private fun accessibility_banner(needs_setup: Boolean, on_open: () -> Unit, on_refresh: () -> Unit) {
    oneui_card {
        Column(Modifier.fillMaxWidth().padding(OneUi.CardInternalPadding), verticalArrangement = Arrangement.spacedBy(OneUi.SpaceM)) {
            Text(if (needs_setup) "Setup required" else "Accessibility", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (needs_setup) {
                Text("Enable service to allow the Quar to draw over all apps", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OneUi.SpaceS), verticalAlignment = Alignment.CenterVertically) {
                oneui_pill_button("Accessibility toggle", onClick = on_open, emphasized = true)
                oneui_pill_button("Refresh", onClick = on_refresh)
            }
        }
    }
} // accessibility_banner

@Composable
private fun placement_card(settings: OverlaySettingsSnapshot, selected_element_id: OverlayElementId, on_action: (SettingsAction) -> Unit) {
    val selected_settings = settings.elementSettings(selected_element_id)
    val selected_visible = settings.isElementVisible(selected_element_id)
    oneui_card(modifier = Modifier.testTag("canvas_card")) {
        Column(Modifier.padding(OneUi.CardInternalPadding), verticalArrangement = Arrangement.spacedBy(OneUi.SpaceM)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Status bar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                oneui_switch(checked = settings.enabled, on_checked_change = { on_action(SettingsAction.SetOverlayEnabled(it)) })
            }
            element_picker_row(selected_element_id = selected_element_id, on_select = { on_action(SettingsAction.SelectElement(it)) })
            if (selected_visible) {
                commit_slider_row("placement_${selected_element_id.name.lowercase()}_offset", "Edge inset", "", selected_settings.offsetPx.toFloat(), 0f..320f, 0, { "${it.toInt()} px" }, { on_action(SettingsAction.CommitElementOffset(selected_element_id, it.toInt())) }, 0.dp)
                commit_slider_row("placement_${selected_element_id.name.lowercase()}_pill_scale", "Pill scale", "", selected_settings.pillScale, 0.5f..2.0f, 14, { "${String.format("%.1f", it)}x" }, { on_action(SettingsAction.CommitElementPillScale(selected_element_id, it)) }, 0.dp)
                commit_slider_row("placement_${selected_element_id.name.lowercase()}_size_scale", "Content size", "", selected_settings.sizeScale, 0.5f..2.0f, 14, { "${String.format("%.1f", it)}x" }, { on_action(SettingsAction.CommitElementSize(selected_element_id, it)) }, 0.dp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OneUi.SpaceS)) {
                    color_hex_input("Fill", selected_settings.pillColor, { on_action(SettingsAction.CommitElementPillColor(selected_element_id, it)) }, Modifier.weight(1f))
                    color_hex_input("Stroke", selected_settings.pillStrokeColor, { on_action(SettingsAction.CommitElementStrokeColor(selected_element_id, it)) }, Modifier.weight(1f))
                }
            }
        }
    }
} // placement_card

@Composable
 private fun selected_element_editor_card(
    settings: OverlaySettingsSnapshot,
    has_location_permission: Boolean,
    element_id: OverlayElementId,
    on_action: (SettingsAction) -> Unit,
    on_pick_gif: () -> Unit,
    on_request_location_permission: () -> Unit
 ) {
    val focus_manager = LocalFocusManager.current
    val (element_settings, label, supports_weight, supports_italic) = when (element_id) {
        OverlayElementId.TIME -> element_panel_spec(settings.timeSettings, "Time", true, true)
        OverlayElementId.DATE -> element_panel_spec(settings.dateSettings, "Date", true, true)
        OverlayElementId.BATTERY -> element_panel_spec(settings.batterySettings, "Battery", true, true)
        OverlayElementId.GIF -> element_panel_spec(settings.gifSettings, "GIF", false, false)
        OverlayElementId.WEATHER -> element_panel_spec(settings.weatherSettings, "Weather", false, false)
    }
    val visible = settings.isElementVisible(element_id)
    oneui_card {
        Column(modifier = Modifier.padding(vertical = OneUi.SpaceM).testTag("editor_${element_id.name.lowercase()}")) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = OneUi.CardInternalPadding, vertical = OneUi.SpaceS), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                oneui_pill_button("Reset", onClick = { on_action(SettingsAction.ResetElement(element_id)) })
            }
            settings_divider()
            if (element_id == OverlayElementId.TIME) {
                tree_switch_row("24-hour time", "", settings.use24HourTime, { on_action(SettingsAction.SetUse24HourTime(it)) })
                settings_divider()
            }
            if (element_id != OverlayElementId.TIME) {
                tree_switch_row("Show in overlay", "", visible, { when(element_id) {
                    OverlayElementId.DATE -> on_action(SettingsAction.SetShowDate(it))
                    OverlayElementId.BATTERY -> on_action(SettingsAction.SetShowBattery(it))
                    OverlayElementId.GIF -> on_action(SettingsAction.SetShowGif(it))
                    OverlayElementId.WEATHER -> on_action(SettingsAction.SetShowWeather(it))
                    else -> {}
                }})
                settings_divider()
            }
            if (element_id == OverlayElementId.GIF) {
                gif_asset_row(settings.gifUri != null, on_pick_gif, { on_action(SettingsAction.SetGifUri(null)) })
                settings_divider()
            }
            if (element_id == OverlayElementId.WEATHER) {
                var zip_draft by rememberSaveable(settings.weatherZip) { mutableStateOf(settings.weatherZip) }
                val using_system_location = settings.weatherLocationMode == WEATHER_LOCATION_MODE_SYSTEM
                val active_source_label = weather_location_source_label(settings.weatherLocationSource)
                segmented_setting_row(
                    "Weather location",
                    "",
                    settings.weatherLocationMode,
                    listOf(
                        WEATHER_LOCATION_MODE_SYSTEM to "System",
                        WEATHER_LOCATION_MODE_ZIP to "ZIP"
                    ),
                    { on_action(SettingsAction.SetWeatherLocationMode(it)) }
                )
                settings_divider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = OneUi.CardInternalPadding, vertical = OneUi.SpaceS),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(active_source_label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (!using_system_location || has_location_permission) {
                        oneui_pill_button("Refresh", onClick = { on_action(SettingsAction.RefreshWeather) })
                    }
                }
                if (using_system_location && !has_location_permission) {
                    settings_divider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = OneUi.CardInternalPadding, vertical = OneUi.SpaceS),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Uses ZIP fallback", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        oneui_pill_button("Grant location", onClick = on_request_location_permission, emphasized = true)
                    }
                }
                settings_divider()
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = OneUi.CardInternalPadding, vertical = OneUi.SpaceS), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OneUi.SpaceS)) {
                    OutlinedTextField(
                        value = zip_draft,
                        onValueChange = { if (it.length <= 5 && it.all { c -> c.isDigit() }) zip_draft = it },
                        label = { Text("ZIP code") },
                        supportingText = null,
                        singleLine = true,
                        isError = zip_draft.isNotEmpty() && zip_draft.length < 5,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (zip_draft.length == 5) {
                                    on_action(SettingsAction.SetWeatherZip(zip_draft))
                                }
                                focus_manager.clearFocus()
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    oneui_pill_button(
                        "Save ZIP",
                        onClick = { on_action(SettingsAction.SetWeatherZip(zip_draft)) },
                        enabled = zip_draft.length == 5
                    )
                }
                settings_divider()
                segmented_setting_row("Weather mode", "", settings.weatherMode, listOf(0 to "Immersive", 1 to "Temp", 2 to "+ Wind", 3 to "Simple"), { on_action(SettingsAction.CommitWeatherMode(it)) })
                settings_divider()
                item_header("Backdrop (Dev)", Modifier.padding(horizontal = OneUi.CardInternalPadding))
                chip_ribbon(
                    options = listOf(
                        WEATHER_BACKDROP_LIVE to "Live",
                        WEATHER_BACKDROP_SUNNY to "Sunny",
                        WEATHER_BACKDROP_CLOUDY to "Cloudy",
                        WEATHER_BACKDROP_RAINY to "Rainy",
                        WEATHER_BACKDROP_SNOWY to "Snowy",
                        WEATHER_BACKDROP_THUNDER to "Thunder"
                    ),
                    selected = settings.weatherBackdrop,
                    on_selected = { on_action(SettingsAction.CommitWeatherBackdrop(it)) },
                    modifier = Modifier.padding(horizontal = OneUi.CardInternalPadding)
                )
                settings_divider()
            }
            segmented_setting_row("Order in lane", "", element_settings.order, listOf(0 to "1", 1 to "2", 2 to "3", 3 to "4"), { on_action(SettingsAction.SetElementOrder(element_id, it)) })
            settings_divider()
            segmented_setting_row("Lane", "", element_settings.alignment, listOf(ALIGN_LEFT to "Left", ALIGN_RIGHT to "Right"), { on_action(SettingsAction.SetElementAlignment(element_id, it)) })
            if (supports_weight) {
                settings_divider()
                segmented_setting_row("Font weight", "", element_settings.weight, listOf(WEIGHT_NORMAL to "Normal", WEIGHT_BOLD to "Bold", WEIGHT_BLACK to "Black"), { on_action(SettingsAction.SetElementWeight(element_id, it)) })
            }
            if (supports_italic) {
                settings_divider()
                tree_switch_row("Italic", "", element_settings.italic, { on_action(SettingsAction.SetElementItalic(element_id, it)) }, settings.globalItalicMode == ITALIC_DEFAULT)
            }
        }
    }
} // selected_element_editor_card

@Composable
private fun item_header(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, modifier = modifier.padding(vertical = 8.dp))
} // item_header

@Composable
private fun setting_label_block(
    label: String,
    summary: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(OneUi.SpaceXS)) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        if (summary.isNotBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} // setting_label_block


/// Helper Functions


private fun weather_location_source_label(source: String): String = when (source) {
    WEATHER_LOCATION_SOURCE_DEVICE -> "Using system location"
    WEATHER_LOCATION_SOURCE_CACHE -> "Using cached system location"
    WEATHER_LOCATION_SOURCE_ZIP -> "Using ZIP location"
    WEATHER_LOCATION_SOURCE_PERMISSION -> "System selected, waiting for location permission"
    WEATHER_LOCATION_SOURCE_LOCATION_OFF -> "System selected, but device location is off"
    WEATHER_LOCATION_SOURCE_UNAVAILABLE -> "System selected, but no usable location fix yet"
    else -> "Waiting for first weather refresh"
} // weather_location_source_label

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun chip_ribbon(options: List<Pair<Int, String>>, selected: Int, on_selected: (Int) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Surface(
                modifier = Modifier.clickable { on_selected(value) },
                shape = OneUi.ShapeChip,
                color = if (isSelected) oneui_selected_fill() else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (oneui_dark()) 0.62f else 0.72f),
                border = if (isSelected) null else border(1.dp, MaterialTheme.colorScheme.outlineVariant, OneUi.ShapeChip)
            ) {
                Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
} // chip_ribbon

private fun border(width: Dp, color: Color, shape: androidx.compose.ui.graphics.Shape) = androidx.compose.foundation.BorderStroke(width, color)

@Composable
private fun global_appearance_card(settings: OverlaySettingsSnapshot, on_action: (SettingsAction) -> Unit) {
    var advanced_open by rememberSaveable { mutableStateOf(false) }
    oneui_card {
        Column(modifier = Modifier.padding(vertical = OneUi.SpaceM)) {
            Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = OneUi.CardInternalPadding, vertical = OneUi.SpaceS))
            settings_divider()
            segmented_setting_row("Theme", "", settings.themeMode, listOf(THEME_AUTO to "Auto", THEME_LIGHT to "Light", THEME_DARK to "Dark"), { on_action(SettingsAction.SetThemeMode(it)) })
            settings_divider()
            segmented_setting_row("Font family", "", settings.fontFamilyChoice, listOf(FONT_APTOS_SANS to "Aptos", FONT_APTOS_SERIF to "Serif", FONT_SYSTEM to "System"), { on_action(SettingsAction.SetFontFamily(it)) })
            settings_divider()
            tree_switch_row("Solid lane backdrops", "", settings.showCapsules, { on_action(SettingsAction.SetShowCapsules(it)) })
            settings_divider()
            tree_switch_row("Merge lane into one pill", "", settings.mergedLanes, { on_action(SettingsAction.SetMergedLanes(it)) })
            settings_divider()
            commit_slider_row("fontScale", "Font scale", "", settings.fontScale, 0.8f..1.5f, 6, { "${String.format("%.1f", it)}x" }, { on_action(SettingsAction.CommitFontScale(it)) })
            settings_divider()
            commit_slider_row("animation_speed", "Animation speed", "", settings.animationSpeed, 0.05f..2.5f, 48, { "${String.format("%.2f", it)}x" }, { on_action(SettingsAction.CommitAnimationSpeed(it)) })
            settings_divider()
            commit_slider_row("element_spacing", "Lane spacing", "", settings.elementPaddingDp.toFloat(), 0f..64f, 0, { "${it.toInt()} dp" }, { on_action(SettingsAction.CommitElementPadding(it.toInt())) })
            settings_divider()
            advanced_layout_expander(advanced_open, { advanced_open = !advanced_open })
            AnimatedVisibility(visible = advanced_open, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    commit_slider_row("left_lane_clearance", "Left clearance", "", settings.leftLaneClearanceDp.toFloat(), 0f..240f, 23, { "${it.toInt()} dp" }, { on_action(SettingsAction.CommitLeftLaneClearance(it.toInt())) })
                    settings_divider()
                    commit_slider_row("right_lane_clearance", "Right clearance", "", settings.rightLaneClearanceDp.toFloat(), 0f..240f, 23, { "${it.toInt()} dp" }, { on_action(SettingsAction.CommitRightLaneClearance(it.toInt())) })
                    settings_divider()
                    commit_slider_row("left_vertical_offset", "Left trim", "", settings.leftVerticalOffsetDp.toFloat(), -30f..30f, 0, { signed_dp(it.toInt()) }, { on_action(SettingsAction.CommitLeftVerticalOffset(it.toInt())) })
                    settings_divider()
                    commit_slider_row("right_vertical_offset", "Right trim", "", settings.rightVerticalOffsetDp.toFloat(), -30f..30f, 0, { signed_dp(it.toInt()) }, { on_action(SettingsAction.CommitRightVerticalOffset(it.toInt())) })
                }
            }
        }
    }
} // global_appearance_card

@Composable
private fun advanced_layout_expander(open: Boolean, on_toggle: () -> Unit) {
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "adv_rot")
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = on_toggle).padding(horizontal = OneUi.CardInternalPadding, vertical = OneUi.RowVerticalPadding), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Advanced layout", style = MaterialTheme.typography.titleMedium)
        }
        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.rotate(rotation))
    }
} // advanced_layout_expander

@Composable
private fun reset_row(on_confirm: () -> Unit) {
    oneui_card {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = on_confirm).padding(horizontal = OneUi.CardInternalPadding, vertical = OneUi.SpaceL), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Reset all settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            }
            Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.error)
        }
    }
} // reset_row

@Composable
private fun export_tuning_row(on_export: () -> Unit) {
    oneui_card {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = on_export).padding(horizontal = OneUi.CardInternalPadding, vertical = OneUi.SpaceL), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Copy tuning snapshot", style = MaterialTheme.typography.titleMedium)
            }
            Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
} // export_tuning_row

@Composable
private fun version_footer() {
    Text(
        text = "Version ${BuildConfig.VERSION_NAME}",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OneUi.SpaceM),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
} // version_footer

@Composable
private fun oneui_dark(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.20f // oneui_dark

@Composable
private fun settings_screen_background(): Brush = if (oneui_dark()) {
    Brush.verticalGradient(
        listOf(
            OneUIScreenBackgroundDark,
            Color(0xFF050505)
        )
    )
} else {
    Brush.verticalGradient(
        listOf(
            OneUIScreenBackgroundLight,
            Color(0xFFF5F2FA),
            Color(0xFFEAF7F8)
        )
    )
} // settings_screen_background

@Composable
private fun oneui_card(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = page_card_shape,
        color = if (oneui_dark()) OneUICardDark else OneUICardLight,
        tonalElevation = 0.dp,
        shadowElevation = if (oneui_dark()) 0.dp else 1.dp,
        border = border(OneUi.HairlineStroke, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (oneui_dark()) 0.25f else 0.45f), page_card_shape),
        content = content
    )
} // oneui_card

@Composable
private fun oneui_selected_fill(): Color =
    if (oneui_dark()) OneUIAccentMutedBlue.copy(alpha = 0.38f) else OneUIAccentMutedBlue.copy(alpha = 0.32f) // oneui_selected_fill

@Composable
private fun oneui_pill_button(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    destructive: Boolean = false
) {
    val dark = oneui_dark()
    val fill = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (dark) 0.34f else 0.48f)
        destructive -> MaterialTheme.colorScheme.error.copy(alpha = if (dark) 0.18f else 0.12f)
        emphasized -> oneui_selected_fill()
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (dark) 0.72f else 0.86f)
    }
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(OneUi.ShapePill)
            .clickable(enabled = enabled, onClick = onClick),
        shape = OneUi.ShapePill,
        color = fill,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = border(OneUi.HairlineStroke, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f), OneUi.ShapePill)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                fontWeight = FontWeight.Medium
            )
        }
    }
} // oneui_pill_button

@Composable
private fun oneui_switch(
    checked: Boolean,
    on_checked_change: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val dark = oneui_dark()
    val track_target = when {
        checked && dark -> OneUISwitchTrackOnDark
        checked -> OneUISwitchTrackOnLight
        dark -> OneUISwitchTrackOffDark
        else -> OneUISwitchTrackOffLight
    }.copy(alpha = if (enabled) 1f else 0.45f)
    val track_color by animateColorAsState(
        targetValue = track_target,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "oneui_switch_track"
    )
    val thumb_offset by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "oneui_switch_thumb"
    )

    Box(
        modifier = Modifier
            .width(54.dp)
            .height(32.dp)
            .clip(OneUi.ShapePill)
            .background(track_color)
            .clickable(enabled = enabled) { on_checked_change(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumb_offset)
                .size(28.dp)
                .clip(OneUi.ShapePill)
                .background(OneUISwitchThumb)
                .border(OneUi.HairlineStroke, Color.Black.copy(alpha = 0.05f), OneUi.ShapePill)
        )
    }
} // oneui_switch

@Composable
private fun oneui_segmented_control(
    selected_value: Int,
    options: List<Pair<Int, String>>,
    on_selected: (Int) -> Unit
) {
    Surface(
        shape = OneUi.ShapePill,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (oneui_dark()) 0.70f else 0.82f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = border(OneUi.HairlineStroke, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), OneUi.ShapePill)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OneUi.SpaceXS),
            horizontalArrangement = Arrangement.spacedBy(OneUi.SpaceXS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (value, title) ->
                val selected = selected_value == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(OneUi.ShapePill)
                        .background(if (selected) oneui_selected_fill() else Color.Transparent)
                        .clickable { on_selected(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
} // oneui_segmented_control

@Composable
private fun oneui_slider(
    value: Float,
    on_value_change: (Float) -> Unit,
    on_value_change_finished: () -> Unit,
    value_range: ClosedFloatingPointRange<Float>
) {
    val density = LocalDensity.current
    var track_width_px by remember { mutableFloatStateOf(1f) }
    var drag_fraction by remember { mutableFloatStateOf(slider_fraction(value, value_range)) }
    val target_fraction = slider_fraction(value, value_range)
    LaunchedEffect(value) {
        drag_fraction = target_fraction
    }
    val animated_fraction by animateFloatAsState(
        targetValue = drag_fraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "oneui_slider_fraction"
    )
    val draggable_state = rememberDraggableState { delta ->
        if (track_width_px > 0f) {
            val next_fraction = (drag_fraction + delta / track_width_px).coerceIn(0f, 1f)
            drag_fraction = next_fraction
            on_value_change(slider_value(next_fraction, value_range))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .draggable(
                state = draggable_state,
                orientation = Orientation.Horizontal,
                onDragStopped = { on_value_change_finished() }
            )
            .pointerInput(value_range) {
                detectTapGestures { offset ->
                    val next_fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    drag_fraction = next_fraction
                    on_value_change(slider_value(next_fraction, value_range))
                    on_value_change_finished()
                }
            }
    ) {
        val thumb_size = 14.dp
        val track_height = 8.dp
        val thumb_travel = with(density) { (maxWidth - thumb_size).toPx() }
        val thumb_offset = with(density) { (thumb_travel * animated_fraction).toDp() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(track_height)
                .align(Alignment.Center)
                .clip(OneUi.ShapePill)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (oneui_dark()) 0.78f else 0.92f))
                .onSizeChanged { track_width_px = it.width.toFloat().coerceAtLeast(1f) }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.18f)
                    .height(track_height)
                    .background(OneUIAccentMutedPink.copy(alpha = 0.34f), OneUi.ShapePill)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated_fraction.coerceIn(0f, 1f))
                    .height(track_height)
                    .background(OneUIAccentMutedBlue.copy(alpha = if (oneui_dark()) 0.82f else 0.72f), OneUi.ShapePill)
            )
        }
        Box(
            modifier = Modifier
                .offset(x = thumb_offset)
                .size(thumb_size)
                .align(Alignment.CenterStart)
                .clip(OneUi.ShapePill)
                .background(OneUISwitchThumb)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f), OneUi.ShapePill)
        )
    }
} // oneui_slider

private fun slider_fraction(value: Float, value_range: ClosedFloatingPointRange<Float>): Float {
    val span = value_range.endInclusive - value_range.start
    if (span <= 0f) return 0f
    return ((value - value_range.start) / span).coerceIn(0f, 1f)
} // slider_fraction

private fun slider_value(fraction: Float, value_range: ClosedFloatingPointRange<Float>): Float =
    value_range.start + (value_range.endInclusive - value_range.start) * fraction.coerceIn(0f, 1f)

private fun snap_slider_value(value: Float, value_range: ClosedFloatingPointRange<Float>, steps: Int): Float {
    if (steps <= 0) return value.coerceIn(value_range.start, value_range.endInclusive)
    val intervals = steps + 1
    val fraction = slider_fraction(value, value_range)
    return slider_value((fraction * intervals).roundToInt() / intervals.toFloat(), value_range)
} // snap_slider_value

private fun build_tuning_snapshot(settings: OverlaySettingsSnapshot): String {
    val s = settings
    fun esc(v: String): String = v.replace("\\", "\\\\").replace("\"", "\\\"")
    fun elementJson(label: String, e: ElementSettings): String = buildString {
        append("    \"$label\": {\"alignment\": \"${if(e.alignment==ALIGN_LEFT) "left" else "right"}\", \"order\": ${e.order}, \"offsetPx\": ${e.offsetPx}, \"pillScale\": ${String.format("%.3f", e.pillScale)}, \"sizeScale\": ${String.format("%.3f", e.sizeScale)}, \"weight\": ${e.weight}, \"italic\": ${e.italic}, \"pillColor\": \"${esc(e.pillColor)}\", \"pillStrokeColor\": \"${esc(e.pillStrokeColor)}\"}")
    }
    return buildString {
        append("{\n  \"global\": {\"theme\": ${s.themeMode}, \"font\": ${s.fontFamilyChoice}, \"fontScale\": ${s.fontScale}, \"animationSpeed\": ${String.format("%.3f", s.animationSpeed)}, \"capsules\": ${s.showCapsules}, \"merged\": ${s.mergedLanes}, \"use24h\": ${s.use24HourTime}, \"leftTrim\": ${s.leftVerticalOffsetDp}, \"rightTrim\": ${s.rightVerticalOffsetDp}},\n")
        append("  \"weather\": {\"mode\": ${s.weatherMode}, \"backdrop\": ${s.weatherBackdrop}},\n")
        append("  \"elements\": {\n").append(elementJson("time", s.timeSettings)).append(",\n").append(elementJson("date", s.dateSettings)).append(",\n").append(elementJson("battery", s.batterySettings)).append(",\n").append(elementJson("gif", s.gifSettings)).append(",\n").append(elementJson("weather", s.weatherSettings)).append("\n  }\n}")
    }
} // build_tuning_snapshot

private fun copy_to_clipboard(context: Context, payload: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let {
        it.setPrimaryClip(ClipData.newPlainText("Status Quar tuning", payload))
        Toast.makeText(context, "Tuning snapshot copied", Toast.LENGTH_SHORT).show()
    }
} // copy_to_clipboard

@Composable
private fun element_picker_row(selected_element_id: OverlayElementId, on_select: (OverlayElementId) -> Unit) {
    val entries = listOf(OverlayElementId.TIME to "Time", OverlayElementId.DATE to "Date", OverlayElementId.BATTERY to "Battery", OverlayElementId.GIF to "GIF", OverlayElementId.WEATHER to "Weather")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = OneUi.ShapeInner,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (oneui_dark()) 0.70f else 0.82f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(OneUi.SpaceXS),
            horizontalArrangement = Arrangement.spacedBy(OneUi.SpaceXS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            entries.forEach { (id, label) ->
                val selected = id == selected_element_id
                Surface(
                    shape = OneUi.ShapeChip,
                    color = if (selected) oneui_selected_fill() else Color.Transparent,
                    border = if (selected) null else border(OneUi.HairlineStroke, MaterialTheme.colorScheme.outlineVariant, OneUi.ShapeChip),
                    modifier = Modifier
                        .weight(1f)
                        .clip(OneUi.ShapeChip)
                        .clickable { on_select(id) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = OneUi.SpaceM),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
} // element_picker_row

private data class element_panel_spec(val settings: ElementSettings, val label: String, val supports_weight: Boolean, val supports_italic: Boolean)

@Composable
private fun gif_asset_row(hasGif: Boolean, on_pick_gif: () -> Unit, on_remove_gif: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = OneUi.CardInternalPadding, vertical = OneUi.SpaceM), verticalArrangement = Arrangement.spacedBy(OneUi.SpaceS)) {
        Text("GIF asset", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(OneUi.SpaceS)) {
            oneui_pill_button(if (hasGif) "Change GIF" else "Choose GIF", onClick = on_pick_gif, emphasized = true)
            if (hasGif) oneui_pill_button("Remove", onClick = on_remove_gif)
        }
    }
} // gif_asset_row

@Composable
private fun tree_switch_row(label: String, summary: String, checked: Boolean, on_checked_change: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(checked, enabled, Role.Switch, on_checked_change)
            .padding(horizontal = OneUi.CardInternalPadding, vertical = OneUi.RowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(OneUi.SpaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        setting_label_block(
            label = label,
            summary = summary,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        oneui_switch(checked = checked, on_checked_change = on_checked_change, enabled = enabled)
    }
} // tree_switch_row

@Composable
private fun segmented_setting_row(label: String, summary: String, selected_value: Int, options: List<Pair<Int, String>>, on_selected: (Int) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = OneUi.CardInternalPadding, vertical = OneUi.SpaceM),
        verticalArrangement = Arrangement.spacedBy(OneUi.SpaceS)
    ) {
        setting_label_block(label = label, summary = summary)
        oneui_segmented_control(selected_value, options, on_selected)
    }
} // segmented_setting_row

@Composable
private fun commit_slider_row(key: String, label: String, summary: String, value: Float, value_range: ClosedFloatingPointRange<Float>, steps: Int, value_formatter: (Float) -> String, on_value_commit: (Float) -> Unit, horizontal_padding: Dp = OneUi.CardInternalPadding) {
    var draft_value by rememberSaveable(key) { mutableFloatStateOf(value) }
    var is_editing by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!is_editing) draft_value = value }
    Column(modifier = Modifier.padding(horizontal = horizontal_padding, vertical = OneUi.SpaceM), verticalArrangement = Arrangement.spacedBy(OneUi.SpaceXS)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            setting_label_block(
                label = label,
                summary = summary,
                modifier = Modifier.weight(1f)
            )
            Text(
                value_formatter(draft_value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        oneui_slider(
            value = draft_value,
            on_value_change = { is_editing = true; draft_value = it },
            on_value_change_finished = {
                is_editing = false
                on_value_commit(snap_slider_value(draft_value, value_range, steps))
            },
            value_range = value_range
        )
    }
} // commit_slider_row

@Composable
private fun color_hex_input(label: String, hex_value: String, on_value_change: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus_manager = LocalFocusManager.current
    val normalized_external = normalize_hex_input(hex_value)
    var draft_value by rememberSaveable(label) { mutableStateOf(normalized_external) }
    var is_focused by remember { mutableStateOf(false) }

    LaunchedEffect(normalized_external) {
        if (!is_focused && draft_value != normalized_external) {
            draft_value = normalized_external
        }
    }

    val is_valid = is_valid_hex_color(draft_value)
    val preview_color = preview_hex_color(draft_value)

    fun commit_draft() {
        val committed_value = when {
            draft_value.isEmpty() -> ""
            is_valid -> draft_value
            else -> normalized_external
        }
        draft_value = committed_value
        if (committed_value != normalized_external) {
            on_value_change(committed_value)
        }
    }

    OutlinedTextField(
        value = draft_value,
        onValueChange = { draft_value = normalize_hex_input(it) },
        label = { Text(label) },
        singleLine = true,
        shape = OneUi.ShapeInner,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (oneui_dark()) 0.72f else 0.82f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (oneui_dark()) 0.60f else 0.70f),
            focusedBorderColor = oneui_selected_fill(),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.onFocusChanged { state ->
            val was_focused = is_focused
            is_focused = state.isFocused
            if (was_focused && !state.isFocused) {
                commit_draft()
            }
        },
        prefix = { Text("#") },
        trailingIcon = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(OneUi.ShapePill)
                    .background(preview_color ?: MaterialTheme.colorScheme.surfaceVariant)
                    .border(OneUi.HairlineStroke, MaterialTheme.colorScheme.outlineVariant, OneUi.ShapePill)
            )
        },
        supportingText = {
            Text("Hexadecimal #RRGGBBAA")
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                commit_draft()
                focus_manager.clearFocus()
            }
        )
    )
} // color_hex_input

private fun normalize_hex_input(raw: String): String =
    raw
        .removePrefix("#")
        .filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
        .take(8)
        .uppercase() // normalize_hex_input

private fun is_valid_hex_color(value: String): Boolean = value.length == 6 || value.length == 8 // is_valid_hex_color

private fun preview_hex_color(value: String): Color? {
    if (!is_valid_hex_color(value)) return null
    val normalized = if (value.length == 6) "${value}FF" else value
    val parsed = normalized.toLongOrNull(16) ?: return null
    val red = ((parsed shr 24) and 0xFF).toInt()
    val green = ((parsed shr 16) and 0xFF).toInt()
    val blue = ((parsed shr 8) and 0xFF).toInt()
    val alpha = (parsed and 0xFF).toInt()
    return Color(red = red, green = green, blue = blue, alpha = alpha)
} // preview_hex_color

@Composable
private fun settings_divider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = OneUi.DividerInset), thickness = OneUi.DividerThickness, color = MaterialTheme.colorScheme.outlineVariant)
} // settings_divider
package com.example.overlaybar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.overlaybar.data.OverlaySettingsSnapshot
import com.example.overlaybar.data.SettingsTreeGroup
import com.example.overlaybar.data.SettingsUiState
import com.example.overlaybar.ui.theme.OverlaybarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disabled_service_shows_setup_required_state() {
        composeRule.setContent {
            OverlaybarTheme(themeMode = OverlaySettingsSnapshot().themeMode) {
                SettingsScreen(
                    uiState = SettingsUiState(accessibilityServiceEnabled = false),
                    onAction = {},
                    onOpenAccessibilitySettings = {}
                )
            }
        }

        composeRule.onNodeWithText("Setup required").assertExists()
    }

    @Test
    fun clicking_elements_group_expands_nested_controls() {
        composeRule.setContent {
            var uiState by mutableStateOf(SettingsUiState())

            OverlaybarTheme(themeMode = uiState.settings.themeMode) {
                SettingsScreen(
                    uiState = uiState,
                    onAction = { action ->
                        if (action is SettingsAction.ToggleGroup) {
                            val nextGroups = if (action.group in uiState.expandedGroups) {
                                uiState.expandedGroups - action.group
                            } else {
                                uiState.expandedGroups + action.group
                            }
                            uiState = uiState.copy(expandedGroups = nextGroups)
                        }
                    },
                    onOpenAccessibilitySettings = {}
                )
            }
        }

        composeRule.onNodeWithText("Time").assertDoesNotExist()
        composeRule.onNodeWithTag("group_elements").performClick()
        composeRule.onNodeWithText("Time").assertExists()
    }

    @Test
    fun preview_hides_battery_when_setting_is_off() {
        composeRule.setContent {
            OverlaybarTheme(themeMode = OverlaySettingsSnapshot().themeMode) {
                SettingsScreen(
                    uiState = SettingsUiState(
                        settings = OverlaySettingsSnapshot(showBattery = false)
                    ),
                    onAction = {},
                    onOpenAccessibilitySettings = {}
                )
            }
        }

        composeRule.onNodeWithTag("overlay_battery").assertDoesNotExist()
    }
}

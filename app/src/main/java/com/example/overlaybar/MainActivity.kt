//// app/src/main/java/com/example/overlaybar/MainActivity.kt
//// Created 2026-04-22
//// mainactivity module


/// Imports


package com.example.overlaybar

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.overlaybar.data.resolve_dark_theme
import com.example.overlaybar.ui.theme.OverlaybarTheme


/// Types


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings_view_model: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(applicationContext)
            )
            val ui_state = settings_view_model.ui_state.collectAsStateWithLifecycle()
            val battery_debug_state = settings_view_model.battery_debug_state.collectAsStateWithLifecycle()
            val settings = ui_state.value.settings
            val theme_mode = settings.themeMode
            val dark_theme = resolve_dark_theme(theme_mode, isSystemInDarkTheme())

            OverlaybarTheme(theme_mode = theme_mode) {
                configure_system_bars(window = window, dark_theme = dark_theme)
                LifecycleResumeEffect(Unit) {
                    settings_view_model.on_action(SettingsAction.RefreshSystemState)
                    onPauseOrDispose { }
                } // lifecycle_resume_effect

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    settings_screen(
                        settings = settings,
                        battery_debug_state = battery_debug_state.value,
                        selected_element_id = ui_state.value.selectedElementId,
                        needs_accessibility_setup = ui_state.value.needsAccessibilitySetup,
                        has_location_permission = ui_state.value.hasLocationPermission,
                        on_action = settings_view_model::on_action,
                        on_open_accessibility_settings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } // surface
            } // overlaybar_theme
        } // set_content
    } // on_create
} // main_activity


/// Functions


@Composable
private fun configure_system_bars(
    window: Window,
    dark_theme: Boolean
) {
    val view = LocalView.current
    val navigation_bar_color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
    if (view.isInEditMode) return

    SideEffect {
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = navigation_bar_color.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark_theme
            isAppearanceLightNavigationBars = !dark_theme
        } // apply
    } // side_effect
} // configure_system_bars

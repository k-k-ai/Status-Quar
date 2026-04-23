package com.example.overlaybar.overlay

import android.accessibilityservice.AccessibilityService
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.location.LocationManager
import android.os.Build
import android.os.BatteryManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.example.overlaybar.BuildConfig
import com.example.overlaybar.data.*
import com.example.overlaybar.data.OverlayElementId
import com.example.overlaybar.data.OverlaySettingsSnapshot
import com.example.overlaybar.data.Prefs
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

class AccessibilityOverlayService : AccessibilityService() {

    private data class LatLon(val lat: Double, val lon: Double)

    private data class WeatherSnapshot(
        val tempF: Float,
        val windMph: Float,
        val gustMph: Float? = null,
        val code: Int,
        val hourlyTemps: String,
        val hourlyCodes: String,
        val hourlyWindsMph: String
    )

    private data class StationObservationCandidate(
        val tempF: Float,
        val windMph: Float?,
        val gustMph: Float?,
        val summary: String,
        val ageMinutes: Long,
        val distanceMiles: Double
    )

    private data class ResolvedWeatherLocation(
        val location: LatLon,
        val source: String
    )

    companion object {
        val stopwatchRunningFlow = MutableStateFlow(false)
        val stopwatchStartMillisFlow = MutableStateFlow<Long?>(null)
        val stopwatchAccumulatedMillisFlow = MutableStateFlow(0L)
        val batterySnapshotFlow = MutableStateFlow(BatterySnapshot())

        private const val OVERLAY_FADE_HEIGHT_DP = 52
        private const val TOUCH_EDGE_GESTURE_GUTTER_DP = 24
        private const val TIMER_ALARM_ACTION  = "com.example.overlaybar.TIMER_EXPIRED"
        private const val TIMER_ALARM_REQUEST = 0x74696D65 // "time"
        
        // Modular expansion system:
        // 1. BarUi reports screen bounds for all interactive pills here
        val interactiveRegionsFlow = MutableStateFlow<Map<OverlayElementId, android.graphics.Rect>>(emptyMap())
        // 2. Tapping a touch window (or pill) sets this to the active element
        val expandedElementFlow = MutableStateFlow<OverlayElementId?>(null)
        val expandedCardBoundsFlow = MutableStateFlow<android.graphics.Rect?>(null)
        val activeTimerEndMillisFlow = MutableStateFlow<Long?>(null)
        val timerSetAtMillisFlow    = MutableStateFlow<Long?>(null)
        val timerAlarmActiveFlow      = MutableStateFlow(false)
        val alarmSuppressPulseFlow    = MutableStateFlow(0L)
        val weatherRefreshRequests  = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val weatherRefreshInFlightFlow = MutableStateFlow(false)
        val compassHeadingDegreesFlow = MutableStateFlow<Float?>(null)

        // Legacy/Direct access (will be migrated or kept as aliases if needed)
        val pillBoundsFlow = MutableStateFlow<android.graphics.Rect?>(null)
        val weatherExpandedFlow = MutableStateFlow(false)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var prefs: Prefs
    private var wm: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var batteryReceiverRegistered = false
    
    // Single transparent dispatcher view that routes taps to the best-matching pill region.
    private var touchOverlayViews: List<View> = emptyList()
    private var touchOverlayParams: List<WindowManager.LayoutParams> = emptyList()
    private var currentTouchRects: Map<OverlayElementId, android.graphics.Rect> = emptyMap()
    private var currentExpandedTouchId: OverlayElementId? = null

    private var timerAlarmReceiverRegistered = false
    private val timerAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val firedFor = intent.getLongExtra("end_millis", -1L)
            if (firedFor <= 0L) return
            fire_timer_alarm_if_current(firedFor)
        }
    }

    private var alarmVibrator: android.os.Vibrator? = null
    private var cachedWeatherLocation: LatLon? = null
    private var cachedWeatherLocationAtMillis: Long = 0L
    private var sensorManager: SensorManager? = null
    private var compassSensor: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var smoothedHeadingDegrees: Float? = null
    private val compassListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val normalized = normalize_heading_degrees(azimuthDegrees)
            val previous = smoothedHeadingDegrees
            val smoothed = if (previous == null) {
                normalized
            } else {
                normalize_heading_degrees(previous + shortest_heading_delta(previous, normalized) * 0.18f)
            }
            smoothedHeadingDegrees = smoothed
            compassHeadingDegreesFlow.value = smoothed
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f

            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val mahRemaining = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER).takeIf { it != Int.MIN_VALUE }?.let { it / 1000 }
            } else null
            val currentMa = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).takeIf { it != Int.MIN_VALUE }?.let { it / 1000 }
            } else null

            val currentLevel = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt().coerceIn(0, 100) else 100
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            batterySnapshotFlow.value = batterySnapshotFlow.value.copy(
                level = currentLevel,
                charging = isCharging,
                full = status == BatteryManager.BATTERY_STATUS_FULL,
                plug_type = plugType,
                temperature_c = temperature,
                mah_remaining = mahRemaining,
                current_ma = currentMa
            )
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        register_battery_receiver_if_needed()
        register_timer_alarm_receiver()
        start_compass_updates()
        scope.launch { show_overlay() }
        scope.launch { start_weather_refresh_loop() }
        scope.launch {
            weatherRefreshRequests.collectLatest {
                val zip = prefs.weather_zip.first()
                if (zip.length == 5 && zip.all { ch -> ch.isDigit() }) {
                    fetch_weather(zip)
                }
            }
        }
        scope.launch { manage_touch_overlay() }
        scope.launch { watch_timer_expiry() }
        scope.launch { watch_alarm_dismissed() }
        scope.launch {
            expandedElementFlow.collect { expandedId ->
                weatherExpandedFlow.value = (expandedId == OverlayElementId.WEATHER)
                update_overlay_interactivity(expandedId != null)
            }
        }
        scope.launch {
            weatherExpandedFlow.collect { isExpanded ->
                if (!isExpanded && expandedElementFlow.value == OverlayElementId.WEATHER) {
                    expandedElementFlow.value = null
                }
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        hide_overlay()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        hide_overlay()
        if (batteryReceiverRegistered) {
            runCatching { unregisterReceiver(batteryReceiver) }
            batteryReceiverRegistered = false
        }
        if (timerAlarmReceiverRegistered) {
            runCatching { unregisterReceiver(timerAlarmReceiver) }
            timerAlarmReceiverRegistered = false
        }
        stop_compass_updates()
        scope.cancel()
        weatherRefreshInFlightFlow.value = false
        compassHeadingDegreesFlow.value = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    private fun register_battery_receiver_if_needed() {
        if (batteryReceiverRegistered) return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(batteryReceiver, filter)
        }
        batteryReceiverRegistered = true
        registerReceiver(null, filter)?.let { batteryReceiver.onReceive(this, it) }
    }

    private fun register_timer_alarm_receiver() {
        if (timerAlarmReceiverRegistered) return
        val filter = IntentFilter(TIMER_ALARM_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerAlarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(timerAlarmReceiver, filter)
        }
        timerAlarmReceiverRegistered = true
    }

    private fun start_compass_updates() {
        if (compassSensor != null) return
        val manager = getSystemService(SENSOR_SERVICE) as? SensorManager ?: return
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: return
        sensorManager = manager
        compassSensor = sensor
        manager.registerListener(compassListener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun stop_compass_updates() {
        sensorManager?.unregisterListener(compassListener)
        sensorManager = null
        compassSensor = null
        smoothedHeadingDegrees = null
    }

    private fun normalize_heading_degrees(value: Float): Float {
        var normalized = value % 360f
        if (normalized < 0f) normalized += 360f
        return normalized
    }

    private fun shortest_heading_delta(from: Float, to: Float): Float {
        var delta = (to - from) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    private fun show_overlay() {
        if (overlayView != null) return

        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

            setContent {
                val settings by prefs.overlay_settings.collectAsState(initial = OverlaySettingsSnapshot())
                val battery by batterySnapshotFlow.collectAsState()

                if (settings.enabled) {
                    status_bar_overlay(
                        config = settings,
                        battery_snapshot = battery,
                        fade_height_dp = OVERLAY_FADE_HEIGHT_DP
                    )
                }
            }
        }

        val screenHeight = resources.displayMetrics.heightPixels
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (screenHeight * 0.75f).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            create_overlay_flags(touchable = false),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 0
        }

        wm?.addView(composeView, lp)
        overlayView = composeView
        overlayLayoutParams = lp

        scope.launch {
            expandedCardBoundsFlow.collect { bounds ->
                update_overlay_window_bounds(bounds)
            }
        }

        owner.onCreate()
        owner.onStart()
        owner.onResume()
    }

    private fun update_overlay_window_bounds(bounds: android.graphics.Rect?) {
        val wm = wm ?: return
        val view = overlayView ?: return
        val lp = overlayLayoutParams ?: return
        val screenHeight = resources.displayMetrics.heightPixels
        
        // If not expanded, make it small height to avoid blocking swipes.
        if (expandedElementFlow.value == null) {
            lp.height = (80 * resources.displayMetrics.density).toInt()
        } else {
            lp.height = (screenHeight * 0.75f).toInt()
        }
        
        runCatching { wm.updateViewLayout(view, lp) }
    }

    private fun create_overlay_flags(touchable: Boolean): Int {
        return (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    }

    private fun update_overlay_interactivity(touchable: Boolean) {
        val wm = wm ?: return
        val view = overlayView ?: return
        val lp = overlayLayoutParams ?: return
        val updatedFlags = create_overlay_flags(touchable)
        if (lp.flags == updatedFlags) {
            // Even if flags didn't change, we might need to update bounds (height) based on expansion state
            update_overlay_window_bounds(null)
            return
        }
        lp.flags = updatedFlags
        update_overlay_window_bounds(null)
    }

    private fun schedule_timer_alarm(endMillis: Long) {
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(TIMER_ALARM_ACTION).setPackage(packageName)
            .putExtra("end_millis", endMillis)
        val pi = PendingIntent.getBroadcast(
            this, TIMER_ALARM_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, endMillis, pi)
        }
    }

    private fun cancel_timer_alarm() {
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            this, TIMER_ALARM_REQUEST,
            Intent(TIMER_ALARM_ACTION).setPackage(packageName),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        am.cancel(pi)
    }

    private fun fire_timer_alarm_if_current(endMillis: Long) {
        val current = activeTimerEndMillisFlow.value ?: return
        if (current != endMillis || timerAlarmActiveFlow.value) return
        cancel_timer_alarm()
        activeTimerEndMillisFlow.value = null
        timerSetAtMillisFlow.value = null
        timerAlarmActiveFlow.value = true
        start_alarm_vibration()
    }

    private suspend fun watch_timer_expiry() {
        activeTimerEndMillisFlow.collectLatest { endMillis ->
            if (endMillis == null) {
                cancel_timer_alarm()
                return@collectLatest
            }

            schedule_timer_alarm(endMillis)

            // Keep an in-process expiry path while the overlay service is alive so active edits
            // like +10 / -8 do not wait on inexact alarm delivery.
            val remainingMillis = endMillis - System.currentTimeMillis()
            if (remainingMillis > 0L) delay(remainingMillis)
            fire_timer_alarm_if_current(endMillis)
        }
    }

    private suspend fun watch_alarm_dismissed() {
        timerAlarmActiveFlow.collect { active ->
            if (!active) stop_alarm_vibration()
        }
    }

    private fun start_alarm_vibration() {
        @Suppress("DEPRECATION")
        val vib = getSystemService(VIBRATOR_SERVICE) as? android.os.Vibrator ?: return
        alarmVibrator = vib
        // double-tap at visual pulse peak: wait ~820ms (near 900ms visual peak), buzz 80ms,
        // gap 120ms, buzz 80ms, wait 700ms — total 1800ms matches the 900ms visual tween cycle
        val timings = longArrayOf(820, 80, 120, 80, 700)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(android.os.VibrationEffect.createWaveform(timings, 0))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(timings, 0)
        }
    }

    private fun stop_alarm_vibration() {
        alarmVibrator?.cancel()
        alarmVibrator = null
    }

    private fun hide_overlay() {
        lifecycleOwner?.apply {
            onPause()
            onStop()
            onDestroy()
        }
        overlayView?.let { view ->
            runCatching { wm?.removeViewImmediate(view) }
        }
        overlayView = null
        overlayLayoutParams = null
        lifecycleOwner = null
        remove_all_touch_windows()
        interactiveRegionsFlow.value = emptyMap()
        expandedElementFlow.value = null
        expandedCardBoundsFlow.value = null
        pillBoundsFlow.value = null
        weatherExpandedFlow.value = false
        timerAlarmActiveFlow.value = false
        stop_alarm_vibration()
    }

    private suspend fun start_weather_refresh_loop() {
        val interval = 10 * 60 * 1000L // 10 minutes
        while (true) {
            val zip = prefs.weather_zip.first()
            if (zip.length == 5 && zip.all { it.isDigit() }) {
                val age = System.currentTimeMillis() - prefs.weather_fetched_at.first()
                if (age >= interval) fetch_weather(zip)
            }
            delay(interval)
        }
    }

    private suspend fun fetch_weather(zip: String) {
        withContext(Dispatchers.IO) {
            weatherRefreshInFlightFlow.value = true
            try {
                val requestedMode = prefs.weather_location_mode.first()
                val resolvedLocation = resolve_weather_location(zip) ?: return@withContext
                if (prefs.weather_location_mode.first() != requestedMode) return@withContext
                val location = resolvedLocation.location

                val googleApiKey = BuildConfig.GOOGLE_WEATHER_API_KEY.trim()
                val weather = fetch_google_weather(location, googleApiKey)
                    ?: fetch_nws_weather(location)
                    ?: fetch_open_meteo_weather(location)
                    ?: return@withContext
                val stabilizedWeather = reconcile_weather_snapshot(weather)
                if (prefs.weather_location_mode.first() != requestedMode) return@withContext

                prefs.set_weather_snapshot(
                    locationSource = resolvedLocation.source,
                    lat = location.lat.toFloat(),
                    lon = location.lon.toFloat(),
                    tempF = stabilizedWeather.tempF,
                    windMph = stabilizedWeather.windMph,
                    gustMph = stabilizedWeather.gustMph,
                    code = stabilizedWeather.code,
                    hourlyTempsF = stabilizedWeather.hourlyTemps,
                    hourlyWCodes = stabilizedWeather.hourlyCodes,
                    hourlyWindsMph = stabilizedWeather.hourlyWindsMph,
                    fetchedAt = System.currentTimeMillis()
                )
            } catch (_: Exception) { /* network/parse error — silently ignore */ }
            finally { weatherRefreshInFlightFlow.value = false }
        }
    }

    private suspend fun resolve_weather_location(zip: String): ResolvedWeatherLocation? {
        val mode = prefs.weather_location_mode.first()
        val now = System.currentTimeMillis()
        val cached = cachedWeatherLocation
        if (mode == WEATHER_LOCATION_MODE_SYSTEM && cached != null && now - cachedWeatherLocationAtMillis <= 60 * 60 * 1000L) {
            return ResolvedWeatherLocation(cached, WEATHER_LOCATION_SOURCE_CACHE)
        }

        if (mode == WEATHER_LOCATION_MODE_SYSTEM) {
            fetch_current_device_location()?.let { current ->
                cachedWeatherLocation = current
                cachedWeatherLocationAtMillis = now
                return ResolvedWeatherLocation(current, WEATHER_LOCATION_SOURCE_DEVICE)
            }
            val fallbackSource = when {
                !has_weather_location_permission() -> WEATHER_LOCATION_SOURCE_PERMISSION
                !is_system_location_enabled() -> WEATHER_LOCATION_SOURCE_LOCATION_OFF
                else -> WEATHER_LOCATION_SOURCE_UNAVAILABLE
            }
            prefs.set_weather_location_source(fallbackSource)
        }

        val zipLocation = fetch_zip_location(zip) ?: return null
        cachedWeatherLocation = zipLocation
        cachedWeatherLocationAtMillis = now
        return ResolvedWeatherLocation(zipLocation, WEATHER_LOCATION_SOURCE_ZIP)
    }

    private fun fetch_zip_location(zip: String): LatLon? {
        val geoJson = JSONObject(read_json("https://api.zippopotam.us/us/$zip"))
        val place = geoJson.optJSONArray("places")?.optJSONObject(0) ?: return null
        val lat = place.optString("latitude").toDoubleOrNull() ?: return null
        val lon = place.optString("longitude").toDoubleOrNull() ?: return null
        return LatLon(lat = lat, lon = lon)
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetch_current_device_location(): LatLon? {
        if (!has_weather_location_permission()) return null

        val client = LocationServices.getFusedLocationProviderClient(this)
        val lastLocation = suspendCancellableCoroutine<android.location.Location?> { continuation ->
            client.lastLocation
                .addOnSuccessListener { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
        if (lastLocation != null) {
            return LatLon(lastLocation.latitude, lastLocation.longitude)
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxUpdateAgeMillis(30 * 60 * 1000L)
            .setDurationMillis(8_000L)
            .build()
        val tokenSource = CancellationTokenSource()

        val location = suspendCancellableCoroutine<android.location.Location?> { continuation ->
            val task = client.getCurrentLocation(request, tokenSource.token)
            task.addOnSuccessListener { result: android.location.Location? ->
                if (continuation.isActive) continuation.resume(result)
            }
            task.addOnFailureListener {
                if (continuation.isActive) continuation.resume(null)
            }
            continuation.invokeOnCancellation { tokenSource.cancel() }
        } ?: return null

        return LatLon(location.latitude, location.longitude)
    }

    private fun has_weather_location_permission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    private fun is_system_location_enabled(): Boolean {
        val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return manager.isLocationEnabled
    }

    private fun fetch_google_weather(location: LatLon, apiKey: String): WeatherSnapshot? {
        if (apiKey.isBlank()) return null
        val baseUrl = "https://weather.googleapis.com/v1"
        val query = "key=$apiKey&location.latitude=${location.lat}&location.longitude=${location.lon}&unitsSystem=IMPERIAL"

        val currentJson = JSONObject(read_json("$baseUrl/currentConditions:lookup?$query"))
        val current = currentJson.optJSONObject("currentConditions") ?: return null
        val tempF = current.optJSONObject("temperature")?.optFloat("degrees") ?: return null
        val windMph = current.optJSONObject("wind")?.optJSONObject("speed")?.optFloat("value") ?: return null
        val gustMph = current.optJSONObject("wind")?.optJSONObject("gust")?.optFloat("value")?.takeIf { it > 0f }
        val code = google_condition_to_code(current.optJSONObject("weatherCondition")?.optString("type").orEmpty())

        val hourlyJson = JSONObject(read_json("$baseUrl/forecast/hours:lookup?$query&pageSize=24"))
        val hours = hourlyJson.optJSONArray("forecastHours")
        val hourlyTemps = mutableListOf<String>()
        val hourlyCodes = mutableListOf<String>()
        val hourlyWinds = mutableListOf<String>()
        for (i in 0 until minOf(hours?.length() ?: 0, 24)) {
            val hour = hours?.optJSONObject(i) ?: continue
            hour.optJSONObject("temperature")?.optFloat("degrees")?.let { degrees ->
                hourlyTemps += degrees.toString()
            }
            hourlyCodes += google_condition_to_code(
                hour.optJSONObject("weatherCondition")?.optString("type").orEmpty()
            ).toString()
            hour.optJSONObject("wind")?.optJSONObject("speed")?.optFloat("value")?.let { mph ->
                hourlyWinds += mph.toString()
            }
        }

        return WeatherSnapshot(
            tempF = tempF,
            windMph = windMph,
            gustMph = gustMph,
            code = code,
            hourlyTemps = hourlyTemps.joinToString(","),
            hourlyCodes = hourlyCodes.joinToString(","),
            hourlyWindsMph = hourlyWinds.joinToString(",")
        )
    }

    private fun fetch_nws_weather(location: LatLon): WeatherSnapshot? {
        val headers = mapOf(
            "User-Agent" to "overlaybar/1.0 (local accessibility overlay)",
            "Accept" to "application/geo+json"
        )

        val points = JSONObject(read_json("https://api.weather.gov/points/${location.lat},${location.lon}", headers))
            .optJSONObject("properties") ?: return null
        val stationsUrl = points.optString("observationStations")
        val hourlyUrl = points.optString("forecastHourly")
        if (stationsUrl.isBlank() || hourlyUrl.isBlank()) return null

        val stations = JSONObject(read_json(stationsUrl, headers)).optJSONArray("features") ?: return null
        val hourly = JSONObject(read_json(hourlyUrl, headers))
            .optJSONObject("properties")
            ?.optJSONArray("periods") ?: JSONArray()
        val bestObservation = find_best_nws_observation(location, stations, headers)

        val firstHourly = hourly.optJSONObject(0)
        val tempF = bestObservation?.tempF
            ?: firstHourly?.let(::period_temperature_to_fahrenheit)
            ?: return null
        val windMph = bestObservation?.windMph?.takeIf { it > 0f }
            ?: firstHourly?.optString("windSpeed")?.let(::wind_speed_text_to_mph)
            ?: 0f
        val gustMph = bestObservation?.gustMph?.takeIf { it > 0f }
        val currentSummary = bestObservation?.summary?.ifBlank {
            firstHourly?.optString("shortForecast").orEmpty()
        } ?: firstHourly?.optString("shortForecast").orEmpty()
        val currentCode = description_to_code(currentSummary)

        val hourlyTemps = mutableListOf<Float>()
        val hourlyCodes = mutableListOf<Int>()
        val hourlyWinds = mutableListOf<Float>()
        for (i in 0 until minOf(hourly.length(), 24)) {
            val period = hourly.optJSONObject(i) ?: continue
            period_temperature_to_fahrenheit(period)?.let { hourlyTemps += it }
            hourlyCodes += period_to_code(period)
            period.optString("windSpeed").takeIf { it.isNotBlank() }?.let { windText ->
                wind_speed_text_to_mph(windText)?.let { hourlyWinds += it }
            }
        }
        val correctedTemps = bias_correct_hourly_temps(hourlyTemps, tempF)
        val correctedWinds = bias_correct_hourly_winds(hourlyWinds, windMph)
        val correctedCodes = stabilize_hourly_codes(hourlyCodes, currentCode)

        return WeatherSnapshot(
            tempF = tempF,
            windMph = windMph,
            gustMph = gustMph,
            code = currentCode,
            hourlyTemps = correctedTemps.joinToString(","),
            hourlyCodes = correctedCodes.joinToString(","),
            hourlyWindsMph = correctedWinds.joinToString(",")
        )
    }

    private fun find_best_nws_observation(
        location: LatLon,
        stations: JSONArray,
        headers: Map<String, String>
    ): StationObservationCandidate? {
        var best: StationObservationCandidate? = null
        for (i in 0 until minOf(stations.length(), 8)) {
            val station = stations.optJSONObject(i) ?: continue
            val stationUrl = station.optString("id")
            if (stationUrl.isBlank()) continue
            val coords = station.optJSONObject("geometry")?.optJSONArray("coordinates")
            val stationLon = coords?.optDoubleOrNull(0)
            val stationLat = coords?.optDoubleOrNull(1)
            val distanceMiles = if (stationLat != null && stationLon != null) {
                haversine_miles(location.lat, location.lon, stationLat, stationLon)
            } else {
                Double.MAX_VALUE
            }

            val observation = runCatching {
                JSONObject(read_json("$stationUrl/observations/latest", headers)).optJSONObject("properties")
            }.getOrNull() ?: continue

            val candidate = observation_to_candidate(observation, distanceMiles) ?: continue
            val candidateScore = candidate.ageMinutes * 2.0 + candidate.distanceMiles
            val bestScore = best?.let { it.ageMinutes * 2.0 + it.distanceMiles } ?: Double.MAX_VALUE
            if (candidateScore < bestScore) {
                best = candidate
            }
        }
        return best
    }

    private fun observation_to_candidate(
        observation: JSONObject,
        distanceMiles: Double
    ): StationObservationCandidate? {
        val timestamp = observation.optString("timestamp")
        if (timestamp.isBlank()) return null
        val ageMinutes = runCatching {
            java.time.Duration.between(Instant.parse(timestamp), Instant.now()).toMinutes()
        }.getOrNull() ?: return null
        if (ageMinutes !in 0..90) return null

        val tempF = observation.optJSONObject("temperature")?.let(::measurement_to_fahrenheit) ?: return null
        val windMph = observation.optJSONObject("windSpeed")?.let(::measurement_to_mph)
        val gustMph = observation.optJSONObject("windGust")?.let(::measurement_to_mph)
        return StationObservationCandidate(
            tempF = tempF,
            windMph = windMph,
            gustMph = gustMph,
            summary = observation.optString("textDescription"),
            ageMinutes = ageMinutes,
            distanceMiles = distanceMiles
        )
    }

    private fun fetch_open_meteo_weather(location: LatLon): WeatherSnapshot? {
        val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=${location.lat}&longitude=${location.lon}" +
            "&current=temperature_2m,wind_speed_10m,wind_gusts_10m,weather_code" +
            "&hourly=temperature_2m,weather_code,wind_speed_10m" +
            "&temperature_unit=fahrenheit&wind_speed_unit=mph&forecast_days=1&timezone=auto"
        val weatherJson = JSONObject(read_json(weatherUrl))
        val current = weatherJson.optJSONObject("current") ?: return null
        val hourly = weatherJson.optJSONObject("hourly") ?: return null

        val tempF = current.optFloat("temperature_2m") ?: return null
        val windMph = current.optFloat("wind_speed_10m") ?: return null
        val gustMph = current.optFloat("wind_gusts_10m")?.takeIf { it > 0f }
        val code = current.optInt("weather_code", 0)
        val currentTime = current.optString("time")
        val timeValues = hourly.optJSONArray("time")
        val startIndex = find_hourly_start_index(timeValues, currentTime)

        return WeatherSnapshot(
            tempF = tempF,
            windMph = windMph,
            gustMph = gustMph,
            code = code,
            hourlyTemps = json_numbers_to_csv(hourly.optJSONArray("temperature_2m"), startIndex),
            hourlyCodes = json_ints_to_csv(hourly.optJSONArray("weather_code"), startIndex),
            hourlyWindsMph = json_numbers_to_csv(hourly.optJSONArray("wind_speed_10m"), startIndex)
        )
    }

    private fun read_json(urlString: String, headers: Map<String, String> = emptyMap()): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }

        return try {
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("Weather fetch failed ($status)")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun measurement_to_fahrenheit(measurement: JSONObject): Float? {
        val value = measurement.optDoubleOrNull("value") ?: return null
        return when (measurement.optString("unitCode")) {
            "wmoUnit:degC" -> (value * 9.0 / 5.0 + 32.0).toFloat()
            "wmoUnit:degF" -> value.toFloat()
            else -> null
        }
    }

    private fun measurement_to_mph(measurement: JSONObject): Float? {
        val value = measurement.optDoubleOrNull("value") ?: return null
        return when (measurement.optString("unitCode")) {
            "wmoUnit:km_h-1" -> (value * 0.621371).toFloat()
            "wmoUnit:m_s-1" -> (value * 2.23694).toFloat()
            "wmoUnit:kn" -> (value * 1.15078).toFloat()
            "wmoUnit:mi_h-1" -> value.toFloat()
            else -> null
        }
    }

    private fun period_temperature_to_fahrenheit(period: JSONObject): Float? {
        val value = period.optDoubleOrNull("temperature") ?: return null
        return when (period.optString("temperatureUnit")) {
            "F" -> value.toFloat()
            "C" -> (value * 9.0 / 5.0 + 32.0).toFloat()
            else -> null
        }
    }

    private fun wind_speed_text_to_mph(text: String): Float? {
        val values = Regex("""(\d+(?:\.\d+)?)""").findAll(text)
            .mapNotNull { it.groupValues[1].toFloatOrNull() }
            .toList()
        if (values.isEmpty()) return null
        return values.average().toFloat()
    }

    private fun JSONArray.optDoubleOrNull(index: Int): Double? {
        if (index < 0 || index >= length() || isNull(index)) return null
        return optDouble(index)
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? {
        if (!has(name) || isNull(name)) return null
        return optDouble(name)
    }

    private fun JSONObject.optFloat(name: String): Float? = optDoubleOrNull(name)?.toFloat()

    private fun haversine_miles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMiles = 3958.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) *
            Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).let { it * it }
        return 2.0 * earthRadiusMiles * Math.asin(Math.sqrt(a))
    }

    private fun json_numbers_to_csv(values: JSONArray?, startIndex: Int = 0): String {
        if (values == null) return ""
        return mutableListOf<String>().apply {
            for (i in startIndex until values.length()) {
                if (!values.isNull(i)) add(values.optDouble(i).toString())
            }
        }.joinToString(",")
    }

    private fun json_ints_to_csv(values: JSONArray?, startIndex: Int = 0): String {
        if (values == null) return ""
        return mutableListOf<String>().apply {
            for (i in startIndex until values.length()) {
                if (!values.isNull(i)) add(values.optInt(i).toString())
            }
        }.joinToString(",")
    }

    private fun find_hourly_start_index(times: JSONArray?, currentTime: String): Int {
        if (times == null || currentTime.isBlank()) return 0
        for (i in 0 until times.length()) {
            if (times.optString(i) == currentTime) return i
        }
        return 0
    }

    private fun google_condition_to_code(type: String): Int {
        val normalized = type.lowercase(Locale.US)
        return when {
            "thunder" in normalized || "storm" in normalized -> 95
            "snow" in normalized || "flurr" in normalized || "blizzard" in normalized -> 71
            "sleet" in normalized || "ice" in normalized || "hail" in normalized || "freezing" in normalized -> 77
            "shower" in normalized -> 80
            "rain" in normalized || "drizzle" in normalized -> 61
            "fog" in normalized || "mist" in normalized || "haze" in normalized || "smoke" in normalized -> 45
            normalized == "clear" -> 0
            normalized == "mostly_clear" || normalized == "mostly_sunny" || normalized == "fair" -> 1
            "few_clouds" in normalized || "scattered_clouds" in normalized -> 1
            "partly_cloudy" in normalized || "partly_sunny" in normalized -> 2
            "mostly_cloudy" in normalized || "cloudy" in normalized || "overcast" in normalized -> 3
            else -> 1
        }
    }

    private fun description_to_code(description: String): Int {
        return description_to_code(description, null)
    }

    private fun description_to_code(description: String, precipChancePercent: Int?): Int {
        val normalized = description.lowercase(Locale.US)
        val precipChance = precipChancePercent ?: 100
        return when {
            "thunder" in normalized || "storm" in normalized -> 95
            "snow" in normalized || "flurr" in normalized || "blizzard" in normalized -> 71
            "sleet" in normalized || "ice pellets" in normalized || "hail" in normalized || "freezing rain" in normalized || "wintry mix" in normalized -> 77
            "showers" in normalized && precipChance >= 45 -> 80
            ("rain" in normalized || "drizzle" in normalized) && precipChance >= 35 -> 61
            "fog" in normalized || "mist" in normalized || "haze" in normalized || "smoke" in normalized -> 45
            "clear" in normalized || "sunny" in normalized -> 0
            ("few" in normalized || "scattered" in normalized || "isolated" in normalized) && "cloud" in normalized -> 1
            ("mostly" in normalized && "sun" in normalized) || ("mostly" in normalized && "clear" in normalized) -> 1
            ("partly" in normalized || "some" in normalized) && "cloud" in normalized -> 2
            ("mostly" in normalized || "considerable" in normalized) && "cloud" in normalized -> 3
            "cloud" in normalized || "overcast" in normalized -> 3
            else -> 1
        }
    }

    private fun period_to_code(period: JSONObject): Int {
        val chance = period.optJSONObject("probabilityOfPrecipitation")
            ?.optDoubleOrNull("value")
            ?.toInt()
        return description_to_code(period.optString("shortForecast"), chance)
    }

    private fun bias_correct_hourly_temps(values: List<Float>, currentTempF: Float): List<Float> {
        if (values.isEmpty()) return emptyList()
        val baseline = values.first()
        val anchorTemp = currentTempF.coerceIn(baseline - 8f, baseline + 10f)
        val trendScale = 0.45f
        val weights = floatArrayOf(1f, 0.95f, 0.8f, 0.35f, 0.1f)
        return values.mapIndexed { index, value ->
            val anchored = anchorTemp + (value - baseline) * trendScale
            val weight = weights.getOrElse(index) { 0f }
            (anchored * weight + value * (1f - weight)).coerceIn(-40f, 130f)
        }
    }

    private fun bias_correct_hourly_winds(values: List<Float>, currentWindMph: Float): List<Float> {
        if (values.isEmpty()) return emptyList()
        val smoothed = values.mapIndexed { index, value ->
            val previous = values.getOrElse(index - 1) { value }
            val next = values.getOrElse(index + 1) { value }
            ((previous + value * 2f + next) / 4f).coerceAtLeast(0f)
        }
        val anchorDelta = (currentWindMph - smoothed.first()).coerceIn(-6f, 6f)
        val weights = floatArrayOf(0.9f, 0.75f, 0.5f, 0.25f, 0.1f)
        return smoothed.mapIndexed { index, value ->
            val weight = weights.getOrElse(index) { 0f }
            (value + anchorDelta * weight).coerceIn(0f, 60f)
        }
    }

    private fun stabilize_hourly_codes(values: List<Int>, currentCode: Int): List<Int> {
        if (values.isEmpty()) return emptyList()
        return values.mapIndexed { index, code ->
            when {
                index == 0 -> currentCode
                else -> code
            }
        }
    }

    private fun reconcile_weather_snapshot(snapshot: WeatherSnapshot): WeatherSnapshot {
        val hourlyCodes = snapshot.hourlyCodes
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
        if (hourlyCodes.isEmpty()) return snapshot

        val correctedCurrent = reconcile_current_code(snapshot.code, hourlyCodes)
        val correctedHourly = stabilize_hourly_codes(hourlyCodes, correctedCurrent)
        return snapshot.copy(
            code = correctedCurrent,
            hourlyCodes = correctedHourly.joinToString(",")
        )
    }

    private fun reconcile_current_code(currentCode: Int, hourlyCodes: List<Int>): Int {
        val firstHourly = hourlyCodes.firstOrNull() ?: return currentCode
        val secondHourly = hourlyCodes.getOrNull(1) ?: firstHourly

        if (currentCode == 45 && firstHourly != 45) {
            return firstHourly
        }

        val imminentWet = firstHourly in setOf(61, 80, 95) && secondHourly in setOf(61, 80, 95)
        if (currentCode in 0..3 && imminentWet) {
            return if (95 in listOf(firstHourly, secondHourly)) 95 else firstHourly
        }

        return currentCode
    }

    private fun remove_all_touch_windows() {
        touchOverlayViews.forEach { view -> runCatching { wm?.removeViewImmediate(view) } }
        touchOverlayViews = emptyList()
        touchOverlayParams = emptyList()
        currentTouchRects = emptyMap()
        currentExpandedTouchId = null
    }

    private suspend fun manage_touch_overlay() {
        combine(interactiveRegionsFlow, expandedElementFlow, expandedCardBoundsFlow) { regions, expandedId, _ ->
            regions to expandedId
        }.collect { (regions, expandedId) ->
            update_touch_windows(regions, expandedId)
        }
    }

    private fun update_touch_windows(regions: Map<OverlayElementId, android.graphics.Rect>, expandedId: OverlayElementId?) {
        val wm = wm ?: return
        val touchRects = regions.mapValues { (id, rect) ->
            android.graphics.Rect(rect)
        }

        if (touchRects.isEmpty()) {
            remove_all_touch_windows()
            return
        }

        currentTouchRects = touchRects
        currentExpandedTouchId = expandedId

        val clusterRects = build_touch_clusters(
            rects = touchRects.values.toList(),
            expanded = expandedId != null,
            expandedRect = expandedCardBoundsFlow.value ?: expandedId?.let { touchRects[it] }
        )
        val existingViews = touchOverlayViews
        val existingParams = touchOverlayParams
        val updatedViews = mutableListOf<View>()
        val updatedParams = mutableListOf<WindowManager.LayoutParams>()

        clusterRects.forEachIndexed { index, clusterRect ->
            val view = existingViews.getOrNull(index) ?: View(this).apply {
                setBackgroundColor(0x01000000)
                isClickable = false
                var downX = 0f
                var downY = 0f
                var isSwipe = false
                setOnTouchListener { _, event ->
                    val tapX = event.rawX.toInt()
                    val tapY = event.rawY.toInt()
                    
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.rawX
                            downY = event.rawY
                            isSwipe = false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (!isSwipe && (event.rawY - downY > 15 * resources.displayMetrics.density)) {
                                isSwipe = true
                            }
                        }
                    }

                    val expandedTouchId = currentExpandedTouchId
                    val expandedRect = expandedCardBoundsFlow.value ?: expandedTouchId?.let { currentTouchRects[it] }
                    
                    if (isSwipe) {
                        // Pass touch to system (drawer)
                        return@setOnTouchListener false
                    }

                    if (expandedTouchId != null && expandedRect != null) {
                        if (expandedRect.contains(tapX, tapY)) {
                            false
                        } else {
                            if (event.action == MotionEvent.ACTION_UP) {
                                playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                expandedElementFlow.value = null
                            }
                            true
                        }
                    } else {
                        val tappedId = resolve_tapped_element(tapX, tapY, currentTouchRects, currentExpandedTouchId)
                        if (tappedId == null) {
                            false
                        } else {
                            if (event.action == MotionEvent.ACTION_UP) {
                                playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                if (tappedId == OverlayElementId.TIME && timerAlarmActiveFlow.value) {
                                    timerAlarmActiveFlow.value = false
                                    alarmSuppressPulseFlow.value = System.currentTimeMillis()
                                } else if (expandedElementFlow.value == tappedId) {
                                    expandedElementFlow.value = null
                                } else {
                                    expandedElementFlow.value = tappedId
                                }
                            }
                            true
                        }
                    }
                }
            }
            val lp = existingParams.getOrNull(index) ?: WindowManager.LayoutParams(
                clusterRect.width(),
                clusterRect.height(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.LEFT
            }

            lp.width = clusterRect.width()
            lp.height = clusterRect.height()
            lp.x = clusterRect.left
            lp.y = clusterRect.top

            if (view.parent == null) {
                runCatching { wm.addView(view, lp) }
            } else {
                runCatching { wm.updateViewLayout(view, lp) }
            }

            updatedViews += view
            updatedParams += lp
        }

        existingViews.drop(clusterRects.size).forEach { view ->
            runCatching { wm.removeViewImmediate(view) }
        }

        touchOverlayViews = updatedViews
        touchOverlayParams = updatedParams
    }

    private fun build_touch_clusters(
        rects: List<android.graphics.Rect>,
        expanded: Boolean,
        expandedRect: android.graphics.Rect? = null
    ): List<android.graphics.Rect> {
        if (rects.isEmpty()) return emptyList()
        if (expanded) {
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val cardRect = expandedRect ?: return listOf(
                android.graphics.Rect(0, 0, screenWidth, screenHeight)
            )
            val clampedCard = android.graphics.Rect(
                cardRect.left.coerceIn(0, screenWidth),
                cardRect.top.coerceIn(0, screenHeight),
                cardRect.right.coerceIn(0, screenWidth),
                cardRect.bottom.coerceIn(0, screenHeight)
            )
            val regions = mutableListOf<android.graphics.Rect>()
            if (clampedCard.top > 0) {
                regions += android.graphics.Rect(0, 0, screenWidth, clampedCard.top)
            }
            if (clampedCard.left > 0) {
                regions += android.graphics.Rect(0, clampedCard.top, clampedCard.left, clampedCard.bottom)
            }
            if (clampedCard.right < screenWidth) {
                regions += android.graphics.Rect(clampedCard.right, clampedCard.top, screenWidth, clampedCard.bottom)
            }
            if (clampedCard.bottom < screenHeight) {
                regions += android.graphics.Rect(0, clampedCard.bottom, screenWidth, screenHeight)
            }
            return regions.filter { it.width() > 0 && it.height() > 0 }
        }

        val sortedRects = rects.sortedBy { it.left }
        val clusters = mutableListOf<android.graphics.Rect>()
        val mergeGapPx = (24 * resources.displayMetrics.density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        val edgeGestureGutterPx = (TOUCH_EDGE_GESTURE_GUTTER_DP * resources.displayMetrics.density).toInt()

        sortedRects.forEach { rect ->
            val current = android.graphics.Rect(rect)
            val last = clusters.lastOrNull()
            if (last != null && current.left <= last.right + mergeGapPx) {
                last.union(current)
            } else {
                clusters += current
            }
        }
        return clusters.mapNotNull { rect ->
            val adjusted = android.graphics.Rect(rect)
            // Left/Right exclusion for side gestures
            if (adjusted.left <= edgeGestureGutterPx) {
                adjusted.left = edgeGestureGutterPx
            }
            if (adjusted.right >= screenWidth - edgeGestureGutterPx) {
                adjusted.right = screenWidth - edgeGestureGutterPx
            }
            // Top exclusion for status bar swipe down
            val topGutterPx = (4 * resources.displayMetrics.density).toInt()
            if (adjusted.top <= topGutterPx) {
                adjusted.top = topGutterPx
            }
            adjusted.takeIf { it.width() > 0 && it.height() > 0 }
        }
    }

    private fun resolve_tapped_element(
        tapX: Int,
        tapY: Int,
        touchRects: Map<OverlayElementId, android.graphics.Rect>,
        expandedId: OverlayElementId?
    ): OverlayElementId? {
        if (expandedId != null) {
            val expandedRect = touchRects[expandedId]
            if (expandedRect != null && expandedRect.contains(tapX, tapY)) {
                return expandedId
            }
        }

        return touchRects
            .filterValues { it.contains(tapX, tapY) }
            .minByOrNull { (_, rect) -> rect.width() * rect.height() }
            ?.key
    }

    private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val modelStore = ViewModelStore()
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val viewModelStore: ViewModelStore
            get() = modelStore

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        fun onCreate() {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun onStart() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun onResume() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun onPause() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        fun onStop() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        fun onDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            modelStore.clear()
        }
    }
}

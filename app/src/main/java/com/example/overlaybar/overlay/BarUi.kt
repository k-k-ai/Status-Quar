package com.example.overlaybar.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.overlaybar.R
import com.example.overlaybar.data.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay



private val aptos_sans_font_family = FontFamily(
    Font(resId = R.font.aptos_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.aptos_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.aptos_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.aptos_bold_italic, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(resId = R.font.aptos_black, weight = FontWeight.Black, style = FontStyle.Normal),
    Font(resId = R.font.aptos_black_italic, weight = FontWeight.Black, style = FontStyle.Italic)
)

private val aptos_serif_font_family = FontFamily(
    Font(resId = R.font.aptos_serif_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.aptos_serif_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.aptos_serif_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.aptos_serif_bold_italic, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(resId = R.font.aptos_serif_bold, weight = FontWeight.Black, style = FontStyle.Normal),
    Font(resId = R.font.aptos_serif_bold_italic, weight = FontWeight.Black, style = FontStyle.Italic)
)

private fun get_font_family(fontFamily: Int): FontFamily = when (fontFamily) {
    FONT_APTOS_SERIF -> aptos_serif_font_family
    FONT_SYSTEM -> FontFamily.Default
    else -> aptos_sans_font_family
}

private val ONEUI_DARK_LANE = Color(0xE1141820)
private val ONEUI_LIGHT_LANE = Color(0xF7F7F9FC)
private val ONEUI_DARK_LANE_BORDER = Color(0x24FFFFFF)
private val ONEUI_LIGHT_LANE_BORDER = Color(0x16000000)
private val GIF_SHAPE = RoundedCornerShape(13.dp)
private val EDITOR_HIGHLIGHT_SHAPE = RoundedCornerShape(16.dp)

data class OverlayPreviewEditor(
    val selectedElementId: OverlayElementId,
    val onElementBoundsChanged: (OverlayElementId, Rect) -> Unit
)

private const val VERTICAL_TRIM_BASELINE_DP = -28

private fun lerpDp(start: Dp, end: Dp, fraction: Float): Dp =
    (start.value + (end.value - start.value) * fraction).dp

@Composable
fun status_bar_overlay(
    config: OverlaySettingsSnapshot,
    battery_snapshot: BatterySnapshot,
    fade_height_dp: Int = 40,
    editor: OverlayPreviewEditor? = null,
    modifier: Modifier = Modifier
) {
    val use_dark = resolve_dark_theme(config.themeMode, isSystemInDarkTheme())
    val text_color = if (use_dark) Color(0xFFF4F6F8) else Color(0xFF16191D)
    val lane_fill = if (use_dark) ONEUI_DARK_LANE else ONEUI_LIGHT_LANE
    val lane_border = if (use_dark) ONEUI_DARK_LANE_BORDER else ONEUI_LIGHT_LANE_BORDER
    val selected_font_family = get_font_family(config.fontFamilyChoice)

    // Modular Expansion State
    val serviceExpandedId by AccessibilityOverlayService.expandedElementFlow.collectAsState()
    val activeTimerEndMillis by AccessibilityOverlayService.activeTimerEndMillisFlow.collectAsState()
    val timerSetAt           by AccessibilityOverlayService.timerSetAtMillisFlow.collectAsState()
    val timerAlarmActive       by AccessibilityOverlayService.timerAlarmActiveFlow.collectAsState()
    val alarmSuppressPulse     by AccessibilityOverlayService.alarmSuppressPulseFlow.collectAsState()
    val weatherRefreshInFlight by AccessibilityOverlayService.weatherRefreshInFlightFlow.collectAsState()
    val compassHeadingDegrees by AccessibilityOverlayService.compassHeadingDegreesFlow.collectAsState()
    val currentTimeMillis = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeTimerEndMillis) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTimeMillis.value = now
            delay(next_clock_tick_delay_ms(now, activeTimerEndMillis != null))
        }
    }

    val timeStr = remember(currentTimeMillis.value, config.use24HourTime) {
        get_current_time(currentTimeMillis.value, config.use24HourTime)
    }
    val cal = remember(currentTimeMillis.value) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis.value }
    }
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = cal.get(Calendar.MINUTE)
    val currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
    val hourlyTemps = remember(config.hourlyTempsF) { parse_hourly_floats(config.hourlyTempsF) }
    val hourlyCodes = remember(config.hourlyWCodes) { parse_hourly_ints(config.hourlyWCodes) }
    val hourlyWinds = remember(config.hourlyWindsMph) { parse_hourly_floats(config.hourlyWindsMph) }
    val liveBackdrop = remember(config.weatherBackdrop, config.weatherCode, config.weatherWindMph, currentHour, config.hourlyWCodes) {
        if (config.weatherBackdrop == WEATHER_BACKDROP_LIVE) {
            val effectiveCode = hourlyCodes.firstOrNull() ?: config.weatherCode
            resolve_backdrop_from_code(effectiveCode, config.weatherWindMph, currentHour)
        } else {
            config.weatherBackdrop
        }
    }
    val isWeatherImmersive = config.weatherMode == 0 || config.weatherMode == 3
    val solarBrightness: Float = if (config.weatherLat != 0f) {
        solar_brightness(config.weatherLat, config.weatherLon, currentHour, currentMinute, currentDayOfYear)
    } else 1f
    var clearWayheadEdgeSign by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(compassHeadingDegrees) {
        val relativeNorth = compassHeadingDegrees?.let { normalize_signed_degrees(-it) } ?: return@LaunchedEffect
        if (kotlin.math.abs(relativeNorth) < 170f) {
            clearWayheadEdgeSign = if (relativeNorth >= 0f) 1f else -1f
        }
    }
    val clearWayheadProgressTarget = remember(compassHeadingDegrees, clearWayheadEdgeSign) {
        clear_wayhead_progress(compassHeadingDegrees, clearWayheadEdgeSign)
    }
    val clearWayheadProgress by animateFloatAsState(
        targetValue = clearWayheadProgressTarget,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "clear_wayhead_progress"
    )
    val (weatherFill, weatherStrokeBase) = resolve_weather_colors(liveBackdrop, isWeatherImmersive, config.weatherSettings, solarBrightness)
    val tempTint = if (isWeatherImmersive && config.weatherTempF > 0f) temp_tint_color(config.weatherTempF, config.weatherWindMph) else null
    val weatherStroke = tempTint?.let { tint -> weatherStrokeBase?.let { b -> lerp_color(b, tint, 0.4f) } ?: tint } ?: weatherStrokeBase
    val weatherTextColor = Color.White
    val timerArcProgress: Float = run {
        val end = activeTimerEndMillis ?: return@run -1f
        val setAt = timerSetAt ?: return@run -1f
        val total = (end - setAt).toFloat()
        if (total <= 0f) return@run -1f
        val remaining = (end - currentTimeMillis.value).coerceAtLeast(0L).toFloat()
        (remaining / total).coerceIn(0f, 1f)
    }
    val alarmInfinite = rememberInfiniteTransition(label = "alarm_pulse")
    val rawAlarmPulse by alarmInfinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alarm_pulse_float"
    )
    val alarmPulse = if (timerAlarmActive) rawAlarmPulse else 0f
    val suppressGreenAlpha = remember { Animatable(0f) }
    LaunchedEffect(alarmSuppressPulse) {
        if (alarmSuppressPulse == 0L) return@LaunchedEffect
        suppressGreenAlpha.snapTo(0.82f)
        suppressGreenAlpha.animateTo(0f, tween(durationMillis = 600, easing = FastOutSlowInEasing))
    }
    val alarmSuppressFill: Color? = if (suppressGreenAlpha.value > 0f)
        Color(0xFF30D158).copy(alpha = suppressGreenAlpha.value) else null

    var localExpandedId by remember { mutableStateOf<OverlayElementId?>(null) }
    val activeExpandedId = if (editor == null) serviceExpandedId else localExpandedId
    var morphExpandedId by remember { mutableStateOf<OverlayElementId?>(null) }
    var morphSourceRect by remember { mutableStateOf<Rect?>(null) }
    var morphIsActive by remember { mutableStateOf(false) }
    val hiddenElementId = activeExpandedId ?: morphExpandedId

    // Warmup state - one invisible open/close to pre-JIT morph code and compile GPU shaders
    var warmupActive by remember { mutableStateOf(false) }
    var warmupDone by remember { mutableStateOf(false) }

    // Local bounds tracking for each pill
    var elementBoundsMap by remember { mutableStateOf<Map<OverlayElementId, Rect>>(emptyMap()) }
    val density = LocalDensity.current

    val buildLaneEntry: (OverlayElementId, ElementSettings, Color?, Color?, Int, Float, (@Composable () -> Unit)) -> OverlayLaneEntry = { id, s, fill, stroke, backdrop, alpha, content ->
        OverlayLaneEntry(
            id = id,
            order = s.order,
            pillScale = s.pillScale,
            offsetPx = s.offsetPx,
            customFill = fill,
            customStroke = stroke,
            backdropType = backdrop,
            alpha = alpha,
            onTap = if (editor != null) ({ localExpandedId = if (localExpandedId == id) null else id }) else null,
            content = content
        )
    }

    // Alarm pulse fill — blends base pill color toward red when alarm is ringing
    val alarmTimeFill: Color? = if (timerAlarmActive) {
        val base = parse_hex_color(config.resolvedTimeSettings.pillColor) ?: lane_fill
        lerp_color(base, Color(0xFFFF3B3B), alarmPulse * 0.75f)
    } else null

    val left_elements = buildList {
        val s = config.resolvedTimeSettings
        if (s.alignment == ALIGN_LEFT) {
            add(buildLaneEntry(OverlayElementId.TIME, s, alarmSuppressFill ?: alarmTimeFill ?: parse_hex_color(s.pillColor), parse_hex_color(s.pillStrokeColor), -1, if (hiddenElementId == OverlayElementId.TIME) 0f else 1f) {
                editable_overlay_element(OverlayElementId.TIME, editor, text_color) {
                    time_module(timeStr, s, text_color, config.fontScale, selected_font_family)
                }
            }.copy(timerArcProgress = timerArcProgress))
        }
        val ds = config.resolvedDateSettings
        if (config.showDate && ds.alignment == ALIGN_LEFT) {
            add(buildLaneEntry(OverlayElementId.DATE, ds, parse_hex_color(ds.pillColor), parse_hex_color(ds.pillStrokeColor), -1, if (hiddenElementId == OverlayElementId.DATE) 0f else 1f) {
                editable_overlay_element(OverlayElementId.DATE, editor, text_color) {
                    date_module(ds, text_color, config.fontScale, selected_font_family)
                }
            })
        }
        val bs = config.resolvedBatterySettings
        if (config.showBattery && bs.alignment == ALIGN_LEFT) {
            add(buildLaneEntry(OverlayElementId.BATTERY, bs, parse_hex_color(bs.pillColor), parse_hex_color(bs.pillStrokeColor), -1, if (hiddenElementId == OverlayElementId.BATTERY) 0f else 1f) {
                editable_overlay_element(OverlayElementId.BATTERY, editor, text_color) {
                    battery_module(battery_snapshot.level, battery_snapshot.charging, bs, text_color, config.fontScale, selected_font_family)
                }
            }.copy(battery_level_slice = if (!battery_snapshot.charging) battery_snapshot.level / 100f else -1f))
        }
        val gs = config.gifSettings
        if (config.showGif && config.gifUri != null && gs.alignment == ALIGN_LEFT) {
            add(buildLaneEntry(OverlayElementId.GIF, gs, parse_hex_color(gs.pillColor), parse_hex_color(gs.pillStrokeColor), -1, if (hiddenElementId == OverlayElementId.GIF) 0f else 1f) {
                editable_overlay_element(OverlayElementId.GIF, editor, text_color) {
                    gif_module(config.gifUri, gs, config.fontScale)
                }
            })
        }
        val ws = config.weatherSettings
        if (config.showWeather && ws.alignment == ALIGN_LEFT) {
            add(buildLaneEntry(OverlayElementId.WEATHER, ws, weatherFill, weatherStroke, if (isWeatherImmersive) liveBackdrop else -1, if (hiddenElementId == OverlayElementId.WEATHER) 0f else 1f) {
                editable_overlay_element(OverlayElementId.WEATHER, editor, text_color) {
                    weather_module(
                        settings = ws,
                        mode = config.weatherMode,
                        color = if (isWeatherImmersive) weatherTextColor else text_color,
                        scale = config.fontScale,
                        family = selected_font_family,
                        temp_f = config.weatherTempF,
                        wind_mph = config.weatherWindMph,
                        gust_mph = config.weatherGustMph,
                        condition_code = config.weatherCode,
                        is_refreshing = weatherRefreshInFlight,
                        weather_updated_at = config.weatherFetchedAt
                    )
                }
            })
        }
    }.sortedBy { it.order }

    val right_elements = buildList {
        val s = config.resolvedTimeSettings
        if (s.alignment != ALIGN_LEFT) {
            add(buildLaneEntry(OverlayElementId.TIME, s, alarmSuppressFill ?: alarmTimeFill ?: parse_hex_color(s.pillColor), parse_hex_color(s.pillStrokeColor), -1, if (hiddenElementId == OverlayElementId.TIME) 0f else 1f) {
                editable_overlay_element(OverlayElementId.TIME, editor, text_color) {
                    time_module(timeStr, s, text_color, config.fontScale, selected_font_family)
                }
            }.copy(timerArcProgress = timerArcProgress))
        }
        val ds = config.resolvedDateSettings
        if (config.showDate && ds.alignment != ALIGN_LEFT) {
            add(buildLaneEntry(OverlayElementId.DATE, ds, parse_hex_color(ds.pillColor), parse_hex_color(ds.pillStrokeColor), -1, if (hiddenElementId == OverlayElementId.DATE) 0f else 1f) {
                editable_overlay_element(OverlayElementId.DATE, editor, text_color) {
                    date_module(ds, text_color, config.fontScale, selected_font_family)
                }
            })
        }
        val bs = config.resolvedBatterySettings
        if (config.showBattery && bs.alignment != ALIGN_LEFT) {
            add(buildLaneEntry(OverlayElementId.BATTERY, bs, parse_hex_color(bs.pillColor), parse_hex_color(bs.pillStrokeColor), -1, if (hiddenElementId == OverlayElementId.BATTERY) 0f else 1f) {
                editable_overlay_element(OverlayElementId.BATTERY, editor, text_color) {
                    battery_module(battery_snapshot.level, battery_snapshot.charging, bs, text_color, config.fontScale, selected_font_family)
                }
            }.copy(battery_level_slice = if (!battery_snapshot.charging) battery_snapshot.level / 100f else -1f))
        }
        val gs = config.gifSettings
        if (config.showGif && config.gifUri != null && gs.alignment != ALIGN_LEFT) {
            add(buildLaneEntry(OverlayElementId.GIF, gs, parse_hex_color(gs.pillColor), parse_hex_color(gs.pillStrokeColor), -1, if (hiddenElementId == OverlayElementId.GIF) 0f else 1f) {
                editable_overlay_element(OverlayElementId.GIF, editor, text_color) {
                    gif_module(config.gifUri, gs, config.fontScale)
                }
            })
        }
        val ws = config.weatherSettings
        if (config.showWeather && ws.alignment != ALIGN_LEFT) {
            add(buildLaneEntry(OverlayElementId.WEATHER, ws, weatherFill, weatherStroke, if (isWeatherImmersive) liveBackdrop else -1, if (hiddenElementId == OverlayElementId.WEATHER) 0f else 1f) {
                editable_overlay_element(OverlayElementId.WEATHER, editor, text_color) {
                    weather_module(
                        settings = ws,
                        mode = config.weatherMode,
                        color = if (isWeatherImmersive) weatherTextColor else text_color,
                        scale = config.fontScale,
                        family = selected_font_family,
                        temp_f = config.weatherTempF,
                        wind_mph = config.weatherWindMph,
                        gust_mph = config.weatherGustMph,
                        condition_code = config.weatherCode,
                        is_refreshing = weatherRefreshInFlight,
                        weather_updated_at = config.weatherFetchedAt
                    )
                }
            })
        }
    }.sortedBy { it.order }
    val lane_entries = remember(left_elements, right_elements) { left_elements + right_elements }

    // Logic to update global interactive regions
    LaunchedEffect(elementBoundsMap) {
        if (editor == null) {
            val regions = elementBoundsMap.mapValues { (_, rect) ->
                android.graphics.Rect(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
            }
            AccessibilityOverlayService.interactiveRegionsFlow.value = regions
        }
    }
    LaunchedEffect(activeExpandedId, elementBoundsMap) {
        if (activeExpandedId != null) {
            elementBoundsMap[activeExpandedId]?.let { rect ->
                morphExpandedId = activeExpandedId
                morphSourceRect = rect
                morphIsActive = true
            }
        } else if (morphExpandedId != null) {
            morphIsActive = false
        }
    }
    LaunchedEffect(elementBoundsMap.isNotEmpty()) {
        if (warmupDone || elementBoundsMap.isEmpty()) return@LaunchedEffect
        delay(800L)
        if (activeExpandedId != null) return@LaunchedEffect  // user beat us to it
        warmupActive = true
        delay(700L)
        warmupActive = false
        delay(500L)
        warmupDone = true
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.TopStart) {
            // Status bar pills
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                overlay_lane(left_elements, true, config.elementPaddingDp, lane_fill, lane_border, config.mergedLanes, text_color, VERTICAL_TRIM_BASELINE_DP + config.leftVerticalOffsetDp, solarBrightness, clearWayheadProgress) { id, rect ->
                    elementBoundsMap = elementBoundsMap + (id to rect)
                }
                overlay_lane(right_elements, false, config.elementPaddingDp, lane_fill, lane_border, config.mergedLanes, text_color, VERTICAL_TRIM_BASELINE_DP + config.rightVerticalOffsetDp, solarBrightness, clearWayheadProgress) { id, rect ->
                    elementBoundsMap = elementBoundsMap + (id to rect)
                }
            }
        }

        // Generic Morphing Expansion Surface
        val expandedId = morphExpandedId
        val sourceRect = morphSourceRect

        if (expandedId != null && sourceRect != null) {
            val expandedEntry = lane_entries.firstOrNull { it.id == expandedId }
            // Expansion target config
            val targetWidth = if (expandedId == OverlayElementId.GIF) 324.dp else null
            val targetHeight = when (expandedId) {
                OverlayElementId.WEATHER -> 186.dp
                OverlayElementId.GIF -> 324.dp
                OverlayElementId.TIME -> 290.dp
                OverlayElementId.BATTERY -> 195.dp
                else -> 100.dp
            }
            val useWeatherColors = (expandedId == OverlayElementId.WEATHER && isWeatherImmersive)
            val fill = if (useWeatherColors) (weatherFill ?: lane_fill) else lane_fill
            val stroke = if (useWeatherColors) (weatherStroke ?: lane_border) else lane_border
            val contentColor = if (useWeatherColors) weatherTextColor else text_color
            val surfaceAlpha = when (expandedId) {
                OverlayElementId.GIF -> 0.34f
                OverlayElementId.TIME -> 0.995f
                OverlayElementId.WEATHER -> 0.94f
                else -> 0.98f
            }

            MorphingSurface(
                active = morphIsActive,
                sourceRect = sourceRect,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                animationSpeed = config.animationSpeed,
                fill = fill,
                stroke = stroke,
                pillScale = expandedEntry?.pillScale ?: 1f,
                backdropType = expandedEntry?.backdropType ?: -1,
                fillAlpha = surfaceAlpha,
                onClose = { if (editor == null) AccessibilityOverlayService.expandedElementFlow.value = null else localExpandedId = null },
                onTransitionFinished = {
                    if (!morphIsActive) {
                        AccessibilityOverlayService.expandedCardBoundsFlow.value = null
                        morphExpandedId = null
                        morphSourceRect = null
                    }
                },
                pillContent = expandedEntry?.content,
                onCardBoundsChanged = { rect ->
                    if (editor == null) {
                        AccessibilityOverlayService.expandedCardBoundsFlow.value = android.graphics.Rect(
                            rect.left.toInt(),
                            rect.top.toInt(),
                            rect.right.toInt(),
                            rect.bottom.toInt()
                        )
                    }
                }
            ) { progress, cardProgress ->
                // Cross-fade pill vs expanded content
                if (cardProgress < 0.32f) {
                    Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 1f - (cardProgress / 0.32f) }, contentAlignment = Alignment.Center) {
                        when (expandedId) {
                            OverlayElementId.WEATHER -> weather_module(config.weatherSettings, config.weatherMode, contentColor, config.fontScale * 0.9f, selected_font_family, config.weatherTempF, config.weatherWindMph, config.weatherGustMph, config.weatherCode, weatherRefreshInFlight, config.weatherFetchedAt)
                            OverlayElementId.TIME    -> time_module(timeStr, config.resolvedTimeSettings, contentColor, config.fontScale * 0.9f, selected_font_family)
                            OverlayElementId.BATTERY -> battery_module(battery_snapshot.level, battery_snapshot.charging, config.resolvedBatterySettings, contentColor, config.fontScale * 0.9f, selected_font_family)
                            else -> {}
                        }
                    }
                }
                if (cardProgress > 0.42f) {
                    Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = ((cardProgress - 0.42f) / 0.58f).coerceIn(0f, 1f) }) {
                        when (expandedId) {
                            OverlayElementId.WEATHER -> {
                                hourly_forecast_card(
                                    temps = hourlyTemps,
                                    codes = hourlyCodes,
                                    winds = hourlyWinds,
                                    current_hour = currentHour,
                                    current_wind_mph = config.weatherWindMph,
                                    gust_mph = config.weatherGustMph,
                                    weather_lat = config.weatherLat,
                                    weather_lon = config.weatherLon,
                                    text_color = contentColor,
                                    fontFamily = selected_font_family,
                                    fontScale = config.fontScale,
                                    use_24_hour = config.use24HourTime,
                                    is_refreshing = weatherRefreshInFlight,
                                    location_source = config.weatherLocationSource,
                                    fetched_at_millis = config.weatherFetchedAt,
                                    now_millis = currentTimeMillis.value
                                )
                            }
                            OverlayElementId.TIME -> {
                                expanded_time_card(
                                    now_millis = currentTimeMillis.value,
                                    active_timer_end_millis = activeTimerEndMillis,
                                    timer_set_at = timerSetAt,
                                    weather_lat = config.weatherLat,
                                    weather_lon = config.weatherLon,
                                    text_color = contentColor,
                                    fontFamily = selected_font_family,
                                    fontScale = config.fontScale,
                                    use_24_hour_time = config.use24HourTime,
                                    onAddEightMinuteTimer = { adjust_timer_duration_by(currentTimeMillis.value, activeTimerEndMillis, timerSetAt, 8 * 60 * 1000L) },
                                    onSubtractEightMinuteTimer = { adjust_timer_duration_by(currentTimeMillis.value, activeTimerEndMillis, timerSetAt, -8 * 60 * 1000L) },
                                    onAddTenMinuteTimer = { adjust_timer_duration_by(currentTimeMillis.value, activeTimerEndMillis, timerSetAt, 10 * 60 * 1000L) },
                                    onSubtractTenMinuteTimer = { adjust_timer_duration_by(currentTimeMillis.value, activeTimerEndMillis, timerSetAt, -10 * 60 * 1000L) }
                                )
                            }
                            OverlayElementId.GIF -> {
                                expanded_gif_card(config.gifUri)
                            }
                            OverlayElementId.BATTERY -> {
                                expanded_battery_card(
                                    battery_snapshot = battery_snapshot,
                                    text_color = contentColor,
                                    font_family = selected_font_family,
                                    font_scale = config.fontScale
                                )
                            }
                            else -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Detail view coming soon", color = contentColor, fontSize = 14.sp, fontFamily = selected_font_family)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Invisible warmup surface — runs once at startup to pre-JIT and pre-compile GPU shaders
        val warmupRect = elementBoundsMap[OverlayElementId.TIME] ?: elementBoundsMap.values.firstOrNull()
        if (!warmupDone && warmupRect != null) {
            Box(modifier = Modifier.graphicsLayer { alpha = 0.005f }) {
                MorphingSurface(
                    active = warmupActive,
                    sourceRect = warmupRect,
                    targetHeight = 290.dp,
                    animationSpeed = 2.5f,
                    fill = lane_fill,
                    stroke = lane_border,
                    fillAlpha = 0.97f,
                    onClose = {},
                    onTransitionFinished = {}
                ) { _, _ -> }
            }
        }
    }
}

@Composable
private fun MorphingSurface(
    active: Boolean,
    sourceRect: Rect,
    targetWidth: Dp? = null,
    targetHeight: Dp,
    animationSpeed: Float = 1f,
    fill: Color,
    stroke: Color,
    pillScale: Float = 1f,
    backdropType: Int = -1,
    fillAlpha: Float = 0.97f,
    onClose: () -> Unit,
    onTransitionFinished: () -> Unit = {},
    pillContent: (@Composable () -> Unit)? = null,
    onCardBoundsChanged: ((Rect) -> Unit)? = null,
    content: @Composable (progress: Float, cardProgress: Float) -> Unit
) {
    val progressAnim = remember { Animatable(0f) }
    val speed = animationSpeed.coerceIn(0.05f, 2.5f)
    LaunchedEffect(active, sourceRect) {
        if (active) {
            progressAnim.snapTo(0f)
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow * speed
                )
            )
        } else {
            progressAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow * speed
                )
            )
            onTransitionFinished()
        }
    }
    val animProgress = progressAnim.value

    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    val resolvedTargetWidth = targetWidth ?: (screenWidthDp - 36.dp)
    val targetX = ((screenWidthDp - resolvedTargetWidth) / 2f).coerceAtLeast(18.dp)
    val targetY = 35.dp

    val pillLeft = with(density) { sourceRect.left.toDp() }
    val pillRight = with(density) { sourceRect.right.toDp() }
    val pillTop = with(density) { sourceRect.top.toDp() }
    val pillBottom = with(density) { sourceRect.bottom.toDp() }
    val sourcePillWidth = (pillRight - pillLeft).coerceAtLeast(0.dp)
    val sourcePillHeight = (pillBottom - pillTop).coerceAtLeast(0.dp)
    val pillWidth = sourcePillWidth + (20.dp * pillScale)
    val pillHeight = (24.dp * pillScale).coerceAtLeast(sourcePillHeight * 0.92f)
    val pillCenterX = pillLeft + (sourcePillWidth / 2f)
    val pillCenterY = pillTop + (sourcePillHeight / 2f)
    val centeredPillLeft = targetX + ((resolvedTargetWidth - pillWidth) / 2f)
    val centeredPillTop = targetY + ((targetHeight - pillHeight) / 2f)
    val travelingPhase = (animProgress / 0.72f).coerceIn(0f, 1f)
    val cardProgress = ((animProgress - 0.30f) / 0.70f).coerceIn(0f, 1f)
    val shellMorphProgress = ((travelingPhase - 0.12f) / 0.88f).coerceIn(0f, 1f)
    val travelingPillWidth = lerpDp(pillWidth, resolvedTargetWidth * 0.86f, shellMorphProgress)
    val travelingPillHeight = lerpDp(pillHeight, targetHeight * 0.72f, shellMorphProgress)
    val travelingPillCenterX = lerpDp(pillCenterX, targetX + (resolvedTargetWidth / 2f), travelingPhase)
    val travelingPillCenterY = lerpDp(pillCenterY, targetY + (targetHeight / 2f), travelingPhase)
    val travelingPillRectLeft = travelingPillCenterX - (travelingPillWidth / 2f)
    val travelingPillRectRight = travelingPillCenterX + (travelingPillWidth / 2f)
    val travelingPillRectTop = travelingPillCenterY - (travelingPillHeight / 2f)
    val travelingPillRectBottom = travelingPillCenterY + (travelingPillHeight / 2f)
    val travelingPillAlpha = when {
        animProgress < 0.62f -> 1f
        else -> ((0.92f - animProgress) / 0.30f).coerceIn(0f, 1f)
    }
    val travelingPillContentAlpha = ((0.34f - animProgress) / 0.22f).coerceIn(0f, 1f)
    val travelingCorner = lerpDp(pillHeight / 2f, 32.dp, shellMorphProgress * 0.92f)
    val currentCorner = lerpDp(travelingPillHeight / 2f, 32.dp, cardProgress)

    Box(
        modifier = Modifier
            .offset(
                x = lerpDp(travelingPillRectLeft, targetX, cardProgress),
                y = lerpDp(travelingPillRectTop, targetY, cardProgress)
            )
            .size(
                width = lerpDp(travelingPillWidth, resolvedTargetWidth, cardProgress).coerceAtLeast(0.dp),
                height = lerpDp(travelingPillHeight, targetHeight, cardProgress).coerceAtLeast(0.dp)
            )
            .onGloballyPositioned { onCardBoundsChanged?.invoke(it.boundsInRoot()) }
            .graphicsLayer { alpha = cardProgress.coerceIn(0.01f, 1f) }
            .background(fill.copy(alpha = fill.alpha * fillAlpha), RoundedCornerShape(currentCorner))
            .border(1.5.dp, stroke, RoundedCornerShape(currentCorner))
    ) {
        content(animProgress, cardProgress)
    }
    if (pillContent != null && travelingPillAlpha > 0.01f) {
        Box(
            modifier = Modifier
                .offset(x = travelingPillRectLeft, y = travelingPillRectTop)
                .size(travelingPillWidth, travelingPillHeight)
                .graphicsLayer { alpha = travelingPillAlpha }
                .background(fill, RoundedCornerShape(travelingCorner))
                .border(1.5.dp, stroke, RoundedCornerShape(travelingCorner)),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.graphicsLayer { alpha = travelingPillContentAlpha }) {
                pillContent()
            }
        }
    }
}

@Composable
private fun editable_overlay_element(id: OverlayElementId, editor: OverlayPreviewEditor?, accent: Color, content: @Composable () -> Unit) {
    if (editor == null) { content(); return }
    val sel = editor.selectedElementId == id
    Box(modifier = Modifier.wrapContentSize(unbounded = true).onGloballyPositioned { editor.onElementBoundsChanged(id, it.boundsInRoot()) }.then(if (sel) Modifier.background(accent.copy(alpha = 0.09f), EDITOR_HIGHLIGHT_SHAPE).border(1.dp, accent.copy(alpha = 0.55f), EDITOR_HIGHLIGHT_SHAPE) else Modifier).padding(horizontal = if (sel) 6.dp else 0.dp, vertical = if (sel) 4.dp else 0.dp)) { content() }
}

@Composable
private fun time_module(time: String, settings: ElementSettings, color: Color, scale: Float, family: FontFamily) {
    val weight = if (settings.weight == WEIGHT_NORMAL) FontWeight.Normal else if (settings.weight == WEIGHT_BOLD) FontWeight.Bold else FontWeight.Black
    val style = if (settings.italic) FontStyle.Italic else FontStyle.Normal
    Box(modifier = Modifier.testTag("overlay_time").graphicsLayer { scaleX = settings.sizeScale; scaleY = settings.sizeScale }) {
        Text(time, color = color, fontSize = (13.5f * scale).sp, fontWeight = weight, fontStyle = style, fontFamily = family, letterSpacing = 0.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}

@Composable
private fun date_module(settings: ElementSettings, color: Color, scale: Float, family: FontFamily) {
    val weight = if (settings.weight == WEIGHT_NORMAL) FontWeight.Normal else if (settings.weight == WEIGHT_BOLD) FontWeight.Bold else FontWeight.Black
    val style = if (settings.italic) FontStyle.Italic else FontStyle.Normal
    Box(modifier = Modifier.testTag("overlay_date").graphicsLayer { scaleX = settings.sizeScale; scaleY = settings.sizeScale }) {
        Text(get_current_date(System.currentTimeMillis()), color = color, fontSize = (13.5f * scale).sp, fontWeight = weight, fontStyle = style, fontFamily = family, letterSpacing = 0.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}

@Composable
private fun battery_module(level: Int, charging: Boolean, settings: ElementSettings, color: Color, scale: Float, family: FontFamily) {
    val weight = if (settings.weight == WEIGHT_NORMAL) FontWeight.Normal else if (settings.weight == WEIGHT_BOLD) FontWeight.Bold else FontWeight.Black
    val style = if (settings.italic) FontStyle.Italic else FontStyle.Normal
    Box(modifier = Modifier.testTag("overlay_battery").graphicsLayer { scaleX = settings.sizeScale; scaleY = settings.sizeScale }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((4 * scale).dp)) {
            Text("$level%", color = color, fontSize = (11.5f * scale).sp, fontWeight = weight, fontStyle = style, fontFamily = family, letterSpacing = 0.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        }
    }
}

@Composable
private fun gif_module(uri: String?, settings: ElementSettings, scale: Float) {
    Box(modifier = Modifier.testTag("overlay_gif").graphicsLayer { scaleX = settings.sizeScale; scaleY = settings.sizeScale }) {
        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).build(), contentDescription = null, modifier = Modifier.size((16 * scale).dp).clip(GIF_SHAPE))
    }
}



private fun adjust_timer_duration_by(now: Long, currentStart: Long?, currentEnd: Long?, deltaMillis: Long) {
    val baseEnd = currentEnd ?: now
    val updatedEnd = baseEnd + deltaMillis
    if (updatedEnd <= now) {
        AccessibilityOverlayService.activeTimerEndMillisFlow.value = null
        AccessibilityOverlayService.timerSetAtMillisFlow.value = null
        AccessibilityOverlayService.timerAlarmActiveFlow.value = false
        return
    }

    AccessibilityOverlayService.timerAlarmActiveFlow.value = false
    AccessibilityOverlayService.activeTimerEndMillisFlow.value = updatedEnd
    AccessibilityOverlayService.timerSetAtMillisFlow.value = currentStart ?: now
}

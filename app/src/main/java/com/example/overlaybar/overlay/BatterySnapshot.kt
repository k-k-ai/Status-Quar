//// app/src/main/java/com/example/overlaybar/overlay/BatterySnapshot.kt
//// Created 2026-04-22
//// batterysnapshot module


/// Imports


package com.example.overlaybar.overlay

import android.os.BatteryManager
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale

data class BatterySnapshot(
    val level: Int = 100,
    val charging: Boolean = false,
    val full: Boolean = false,
    val power_save: Boolean = false,
    val plug_type: Int = 0,
    val mah_remaining: Int? = null,
    val current_ma: Int? = null,
    val temperature_c: Float? = null,
    // Capacity sources — design_mah = factory rating, full_mah = hardware/sysfs full charge,
    // learned_full_mah = app-derived estimate from persisted charge/discharge sessions.
    val design_mah: Int? = null,
    val full_mah: Int? = null,
    val learned_full_mah: Int? = null,
    val learned_sample_count: Int = 0,
    val direct_health: Int? = null,
    val cycles: Float? = null,
    val cycle_source: String? = null,

    // Session state — resets on plug/unplug transition
    val session_delta_mah: Int? = null,
    val session_delta_level: Int? = null,
    val session_duration_minutes: Int? = null,
    val session_charging: Boolean = false
) {
    // Internal-only ratio used for time-to-full math. Too noisy per snapshot
    // to surface as a capacity headline — voltage sag, temperature, and load
    // make any single reading swing ±10%.
    val estimated_total_mah: Int? get() {
        if (mah_remaining == null || level <= 4 || level >= 100) return null
        val raw = (mah_remaining * 100L / level).toInt()
        val ceiling = full_mah ?: learned_full_mah ?: design_mah
        return if (ceiling != null && raw > ceiling * 1.15) ceiling else raw
    }

    val resolved_full_mah: Int? get() = full_mah ?: learned_full_mah

    val displayed_total_mah: Int? get() = estimated_total_mah ?: resolved_full_mah ?: design_mah

    val displayed_mah_remaining: Int? get() {
        mah_remaining?.let { return it }
        val total = displayed_total_mah ?: return null
        return ((total * level.coerceIn(0, 100)) / 100f).roundToInt().coerceIn(0, total)
    }

    val session_percent_per_hour: Int? get() {
        val delta = session_delta_level ?: return null
        val minutes = session_duration_minutes ?: return null
        if (delta <= 0 || minutes < 3) return null
        return ((delta * 60f) / minutes).roundToInt().coerceAtLeast(1)
    }

    val learned_health_percent: Int? get() {
        if (learned_full_mah == null || design_mah == null || design_mah <= 0) return null
        return (learned_full_mah * 100L / design_mah).toInt().coerceIn(1, 100)
    }

    val cycle_estimated_health_percent: Int? get() {
        val cycle_count = cycles ?: return null
        val estimatedLoss = (cycle_count * 20f / 500f).toInt()
        return (100 - estimatedLoss).coerceIn(50, 100)
    }

    // Prefer direct/hardware-backed readings, then fall back to our persisted estimate.
    val health_percent: Int? get() {
        if (direct_health != null && direct_health in 1..100) return direct_health
        if (full_mah != null && design_mah != null && design_mah > 0) {
            return (full_mah * 100L / design_mah).toInt().coerceIn(1, 100)
        }
        return learned_health_percent ?: cycle_estimated_health_percent
    }

    val health_source: String? get() = when {
        direct_health != null -> "hardware reported"
        full_mah != null && design_mah != null -> "sysfs ratio"
        learned_full_mah != null && design_mah != null -> "estimated health"
        cycle_estimated_health_percent != null -> "estimated health"
        else -> null
    }

    val drain_ma: Int? get() {
        val c = current_ma ?: return null
        return if (!charging) abs(c).takeIf { it > 10 } else null
    }

    val charge_ma: Int? get() {
        val c = current_ma ?: return null
        return if (charging && !full) abs(c).takeIf { it > 10 } else null
    }

    val time_to_full_minutes: Int? get() {
        if (!charging || full) return null
        val total = displayed_total_mah
        val remaining = displayed_mah_remaining
        val rate = charge_ma
        if (total != null && remaining != null && rate != null && rate > 0) {
            val toFull = total - remaining
            if (toFull > 0) return (toFull * 60L / rate).toInt().coerceAtLeast(1)
        }
        val percentRate = session_percent_per_hour ?: return null
        val percentToFull = (100 - level).coerceAtLeast(0)
        if (percentToFull <= 0) return null
        return ((percentToFull * 60f) / percentRate).roundToInt().coerceAtLeast(1)
    }

    val time_remaining_minutes: Int? get() {
        if (charging) return null
        val remaining = displayed_mah_remaining
        val rate = drain_ma
        if (remaining != null && rate != null && rate > 0) {
            return (remaining * 60L / rate).toInt().coerceAtLeast(1)
        }
        val percentRate = session_percent_per_hour ?: return null
        val percentRemaining = level.coerceAtLeast(0)
        if (percentRemaining <= 0) return null
        return ((percentRemaining * 60f) / percentRate).roundToInt().coerceAtLeast(1)
    }

    val plug_label: String get() = when (plug_type) {
        BatteryManager.BATTERY_PLUGGED_AC       -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB      -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> ""
    }

    val state_label: String get() = when {
        full     -> "Full"
        charging -> "Charging"
        power_save -> "Power Save"
        else     -> "On battery"
    }

    val charge_speed_label: String? get() {
        val rate = charge_ma ?: return null
        return when {
            rate >= 2000 -> "Super fast"
            rate >= 1000 -> "Fast"
            rate >= 500  -> "Standard"
            else         -> "Slow"
        }
    }
}


/// Functions


internal fun format_duration_minutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h >= 24 -> "${h / 24}d ${h % 24}h"
        h > 0   -> "${h}h ${m}m"
        else    -> "${m}m"
    }
} // format_duration_minutes

internal fun format_mah(value: Int): String =
    if (value >= 1000) "${value / 1000},${"%03d".format(value % 1000)}" else "$value"

private fun format_cycle_count(value: Float): String {
    val rounded = kotlin.math.round(value)
    return if (kotlin.math.abs(value - rounded) < 0.05f) {
        "${rounded.toInt()} cycles"
    } else {
        String.format(Locale.US, "%.1f cycles", value)
    }
} // format_cycle_count

// --- Insights ---

data class BatteryInsight(
    val kind: InsightKind,
    val headline: String,
    val label: String
)

enum class InsightKind { CHARGED_IN, DRAINED_OUT, CAPACITY, SPEED, WARMTH, POWER_SAVE, RATE, LEVEL_DELTA }

internal fun compute_battery_insights(battery: BatterySnapshot): List<BatteryInsight> {
    val list = mutableListOf<BatteryInsight>()

    // mAh in — how much charge we've put in this plug session
    if (battery.session_charging && battery.session_delta_mah != null && battery.session_delta_mah > 20) {
        list += BatteryInsight(
            InsightKind.CHARGED_IN,
            "+${format_mah(battery.session_delta_mah)} mAh",
            "added since plugged in"
        )
    }

    // mAh out — how much drained since last unplug
    if (!battery.session_charging && battery.session_delta_mah != null && battery.session_delta_mah > 20) {
        list += BatteryInsight(
            InsightKind.DRAINED_OUT,
            "−${format_mah(battery.session_delta_mah)} mAh",
            "used since unplugged"
        )
    }

    battery.displayed_mah_remaining?.let { remainingMah ->
        list += BatteryInsight(
            InsightKind.CAPACITY,
            "${format_mah(remainingMah)} mAh",
            if (battery.charging) "charge currently stored" else "energy remaining"
        )
    }

    val runtimeMinutes = if (battery.charging) battery.time_to_full_minutes else battery.time_remaining_minutes
    if (runtimeMinutes != null) {
        list += BatteryInsight(
            InsightKind.RATE,
            format_duration_minutes(runtimeMinutes),
            if (battery.charging) "until full" else "estimated runtime"
        )
    }

    // Level delta this session
    if (battery.session_delta_level != null && abs(battery.session_delta_level) >= 2) {
        val dur = battery.session_duration_minutes
        val suffix = if (dur != null && dur > 0) " in ${format_duration_minutes(dur)}" else ""
        val sign = if (battery.session_charging) "+" else "−"
        list += BatteryInsight(
            InsightKind.LEVEL_DELTA,
            "$sign${abs(battery.session_delta_level)}%",
            if (battery.session_charging) "charged$suffix" else "drained$suffix"
        )
    }

    // Full charge capacity.
    battery.full_mah?.let { cap ->
        list += BatteryInsight(
            InsightKind.CAPACITY,
            "${format_mah(cap)} mAh",
            "full charge capacity"
        )
    } ?: battery.learned_full_mah?.let { cap ->
        val sampleLabel = battery.learned_sample_count
            .takeIf { it > 0 }
            ?.let { " · $it samples" }
            .orEmpty()
        list += BatteryInsight(
            InsightKind.CAPACITY,
            "${format_mah(cap)} mAh",
            "estimated full charge capacity$sampleLabel"
        )
    } ?: battery.design_mah?.let { cap ->
        list += BatteryInsight(
            InsightKind.CAPACITY,
            "${format_mah(cap)} mAh",
            "rated design capacity"
        )
    }

    val health_source = battery.health_source
    val healthValue = battery.health_percent
    if (healthValue != null && health_source != null) {
        list += BatteryInsight(
            InsightKind.CAPACITY,
            "$healthValue%",
            if (health_source == "estimated health") "estimated health" else "battery health"
        )
    } else if (battery.learned_sample_count in 1..2) {
        val sampleCount = battery.learned_sample_count
        list += BatteryInsight(
            InsightKind.CAPACITY,
            "Learning health",
            "$sampleCount capacity sample${if (sampleCount == 1) "" else "s"} captured"
        )
    }

    battery.design_mah
        ?.takeIf { battery.full_mah != null || battery.learned_full_mah != null }
        ?.let { designMah ->
            list += BatteryInsight(
                InsightKind.CAPACITY,
                "${format_mah(designMah)} mAh",
                "rated design capacity"
            )
        }

    // Cycles
    battery.cycles?.let { c ->
        list += BatteryInsight(
            InsightKind.CAPACITY,
            format_cycle_count(c),
            battery.cycle_source ?: "battery cycles"
        )
    }

    // Charge speed
    battery.charge_speed_label?.let { speed ->
        list += BatteryInsight(
            InsightKind.SPEED,
            speed,
            "charge speed · ${battery.plug_label.ifEmpty { "plugged" }}"
        )
    }

    // Warmth
    battery.temperature_c?.let { t ->
        val description = when {
            t >= 40f -> "Running hot"
            t >= 35f -> "Warm"
            t >= 25f -> null
            t >= 15f -> "Cool"
            else     -> "Cold"
        }
        if (description != null) {
            val temp_f = t * 9f / 5f + 32f
            list += BatteryInsight(
                InsightKind.WARMTH,
                description,
                "${temp_f.toInt()}°F · ${t.toInt()}°C"
            )
        }
    }

    // Power save state
    if (battery.power_save) {
        list += BatteryInsight(
            InsightKind.POWER_SAVE,
            "Power Save on",
            "background activity reduced"
        )
    } else {
        list += BatteryInsight(
            InsightKind.POWER_SAVE,
            "Power Save off",
            "running normally"
        )
    }

    // Live rate
    val rate_ma = if (battery.charging) battery.charge_ma else battery.drain_ma
    if (rate_ma != null && rate_ma > 0) {
        val arrow = if (battery.charging) "↑" else "↓"
        list += BatteryInsight(
            InsightKind.RATE,
            "$arrow ${format_mah(rate_ma)} mA",
            if (battery.charging) "charging" else "discharging"
        )
    } else {
        battery.session_percent_per_hour?.let { percentPerHour ->
            val arrow = if (battery.charging) "↑" else "↓"
            list += BatteryInsight(
                InsightKind.RATE,
                "$arrow $percentPerHour%/h",
                if (battery.charging) "charging pace" else "drain pace"
            )
        }
    }

    return list
} // compute_battery_insights

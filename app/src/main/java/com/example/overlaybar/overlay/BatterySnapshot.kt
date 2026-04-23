//// app/src/main/java/com/example/overlaybar/overlay/BatterySnapshot.kt
//// Created 2026-04-22
//// batterysnapshot module


/// Imports


package com.example.overlaybar.overlay

import android.os.BatteryManager
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale

data class BatteryDebugState(
    val enabled: Boolean = false,
    val level: Int = 42,
    val charging: Boolean = false,
    val full: Boolean = false,
    val powerSave: Boolean = false,
    val remainingMah: Int = 1450,
    val currentMa: Int = 620,
    val temperatureC: Float = 34f,
    val learnedFullMah: Int = 3250,
    val cycles: Float = 287f,
    val sessionDeltaLevel: Int = 6,
    val sessionDurationMinutes: Int = 92
) {
    fun apply_to(base: BatterySnapshot, nowMillis: Long = System.currentTimeMillis()): BatterySnapshot {
        val resolvedLevel = if (full) 100 else level.coerceIn(0, 100)
        val resolvedCapacity = learnedFullMah.coerceAtLeast(500)
        val resolvedRemaining = if (full) {
            resolvedCapacity
        } else {
            remainingMah.coerceIn(0, resolvedCapacity)
        }
        val resolvedSessionLevel = sessionDeltaLevel.coerceAtLeast(0)
        val resolvedSessionMah = ((resolvedCapacity * resolvedSessionLevel) / 100f).roundToInt().takeIf { it > 0 }
        return base.copy(
            level = resolvedLevel,
            charging = charging || full,
            full = full,
            power_save = powerSave,
            plug_type = if (charging || full) BatteryManager.BATTERY_PLUGGED_USB else 0,
            mah_remaining = resolvedRemaining,
            current_ma = currentMa.takeIf { it > 0 },
            temperature_c = temperatureC,
            full_mah = null,
            learned_full_mah = resolvedCapacity,
            learned_sample_count = 6,
            cycles = cycles.takeIf { it > 0f },
            cycle_source = "debug override",
            session_delta_mah = resolvedSessionMah,
            session_delta_level = resolvedSessionLevel.takeIf { it > 0 },
            session_duration_minutes = sessionDurationMinutes.takeIf { it > 0 },
            session_charging = charging || full,
            updated_at_millis = nowMillis
        )
    }
}

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
    val session_charging: Boolean = false,
    val updated_at_millis: Long = 0L
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

    val estimated_session_delta_mah: Int? get() {
        session_delta_mah?.let { return it }
        val total = displayed_total_mah ?: return null
        val deltaPercent = session_delta_level ?: return null
        if (deltaPercent <= 0) return null
        return ((total * deltaPercent) / 100f).roundToInt().takeIf { it > 0 }
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

    fun projected_mah_remaining(nowMillis: Long): Int? {
        val base = displayed_mah_remaining ?: return null
        if (full) return displayed_total_mah ?: base
        val rate = if (charging) charge_ma else drain_ma
        if (rate == null || rate <= 0 || updated_at_millis <= 0L || nowMillis <= updated_at_millis) {
            return base
        }
        val elapsedHours = (nowMillis - updated_at_millis).coerceAtLeast(0L) / 3_600_000f
        val deltaMah = (rate * elapsedHours).roundToInt()
        val projected = if (charging) base + deltaMah else base - deltaMah
        val total = displayed_total_mah
        return when {
            total != null -> projected.coerceIn(0, total)
            else -> projected.coerceAtLeast(0)
        }
    }

    fun projected_runtime_minutes(nowMillis: Long): Int? {
        if (full) return null
        val rate = if (charging) charge_ma else drain_ma
        if (rate != null && rate > 0) {
            val projectedRemaining = projected_mah_remaining(nowMillis) ?: return null
            return if (charging) {
                val total = displayed_total_mah ?: return null
                val toFull = (total - projectedRemaining).coerceAtLeast(0)
                if (toFull == 0) 0 else (toFull * 60L / rate).toInt().coerceAtLeast(1)
            } else {
                if (projectedRemaining == 0) 0 else (projectedRemaining * 60L / rate).toInt().coerceAtLeast(1)
            }
        }
        return if (charging) time_to_full_minutes else time_remaining_minutes
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

internal fun format_cycle_count(value: Float): String {
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
    val label: String,
    val primary_fraction: Float? = null,
    val secondary_fraction: Float? = null,
    val active: Boolean = false
)

enum class InsightKind { CHARGED_IN, DRAINED_OUT, CAPACITY, SPEED, WARMTH, POWER_SAVE, RATE, LEVEL_DELTA }

internal fun compute_battery_insights(battery: BatterySnapshot): List<BatteryInsight> {
    val primary = mutableListOf<BatteryInsight>()
    val extras = mutableListOf<BatteryInsight>()
    val nowMillis = System.currentTimeMillis()

    val health_source = battery.health_source
    val healthValue = battery.health_percent
    if (healthValue != null && health_source != null) {
        primary += BatteryInsight(
            InsightKind.CAPACITY,
            "$healthValue%",
            if (health_source == "estimated health") "estimated battery health" else "battery health",
            primary_fraction = (healthValue / 100f).coerceIn(0f, 1f)
        )
    } else {
        val sampleCount = battery.learned_sample_count
        primary += BatteryInsight(
            InsightKind.CAPACITY,
            if (sampleCount > 0) "Learning" else "Estimating",
            if (sampleCount > 0) "$sampleCount health sample${if (sampleCount == 1) "" else "s"} captured" else "battery health",
            primary_fraction = (sampleCount / 3f).coerceIn(0.08f, 0.66f)
        )
    }

    val sessionMah = battery.estimated_session_delta_mah?.takeIf { it > 0 }
    val sessionPct = battery.session_delta_level?.takeIf { it > 0 }
    val sessionLabel = if (battery.session_charging) "since plugged in" else "since unplugged"
    val sessionHeadline = when {
        sessionMah != null && sessionPct != null -> "${format_mah(sessionMah)} mAh · $sessionPct%"
        sessionMah != null -> "${format_mah(sessionMah)} mAh"
        sessionPct != null -> "$sessionPct%"
        else -> "0% · 0 mAh"
    }
    primary += BatteryInsight(
        if (battery.session_charging) InsightKind.CHARGED_IN else InsightKind.DRAINED_OUT,
        sessionHeadline,
        if (sessionMah != null || sessionPct != null) {
            if (battery.session_charging) "added $sessionLabel" else "used $sessionLabel"
        } else {
            "current session"
        },
        primary_fraction = ((sessionPct ?: 0) / 100f).coerceIn(0.04f, 1f),
        secondary_fraction = battery.displayed_total_mah
            ?.takeIf { capacity -> capacity > 0 && sessionMah != null }
            ?.let { capacity -> (sessionMah!! / capacity.toFloat()).coerceIn(0f, 1f) }
    )

    battery.temperature_c?.let { t ->
        val tempF = (t * 9f / 5f + 32f).roundToInt()
        val status = when {
            t >= 40f -> "running hot"
            t >= 35f -> "warm"
            t >= 15f -> "normal temp"
            else -> "cool"
        }
        primary += BatteryInsight(
            InsightKind.WARMTH,
            "$tempF°F",
            "${t.roundToInt()}°C · $status",
            primary_fraction = ((t - 10f) / 35f).coerceIn(0f, 1f)
        )
    } ?: run {
        primary += BatteryInsight(
            InsightKind.WARMTH,
            "—",
            "temperature unavailable",
            primary_fraction = 0.08f
        )
    }

    primary += BatteryInsight(
        InsightKind.POWER_SAVE,
        if (battery.power_save) "Active" else "Off",
        "low power mode",
        primary_fraction = if (battery.power_save) 1f else 0.18f,
        active = battery.power_save
    )

    val learnedOrMeasuredCapacity = battery.full_mah ?: battery.learned_full_mah
    val capacityInsight = learnedOrMeasuredCapacity?.let { cap ->
        val capacityLabel = if (battery.full_mah != null) {
            "measured full capacity"
        } else {
            val sampleLabel = battery.learned_sample_count
                .takeIf { it > 0 }
                ?.let { " · $it samples" }
                .orEmpty()
            "estimated full capacity$sampleLabel"
        }
        BatteryInsight(
            InsightKind.CAPACITY,
            "${format_mah(cap)} mAh",
            capacityLabel,
            primary_fraction = battery.design_mah
                ?.takeIf { it > 0 }
                ?.let { design -> (cap / design.toFloat()).coerceIn(0f, 1.15f) }
                ?: battery.health_percent?.let { it / 100f }
                ?: 0.5f
        )
    } ?: BatteryInsight(
        InsightKind.CAPACITY,
        "Awaiting data",
        "capacity estimate",
        primary_fraction = 0.08f
    )
    extras += capacityInsight

    val runtimeMinutes = battery.projected_runtime_minutes(nowMillis)
    extras += BatteryInsight(
        InsightKind.RATE,
        when {
            battery.full -> "Now"
            runtimeMinutes != null -> format_duration_minutes(runtimeMinutes)
            else -> "Awaiting"
        },
        when {
            battery.full -> "fully charged"
            battery.charging -> "until full"
            else -> "estimated runtime"
        },
        primary_fraction = battery.level.coerceIn(0, 100) / 100f
    )

    battery.charge_speed_label?.let { speed ->
        extras += BatteryInsight(
            InsightKind.SPEED,
            speed,
            "charge speed",
            primary_fraction = when (speed) {
                "Super fast" -> 1f
                "Fast" -> 0.72f
                "Standard" -> 0.45f
                else -> 0.25f
            }
        )
    } ?: run {
        extras += BatteryInsight(
            InsightKind.SPEED,
            if (battery.charging) "Connected" else "On battery",
            if (battery.charging) "power source" else "not charging",
            primary_fraction = if (battery.charging) 0.45f else 0.18f
        )
    }

    val rateMa = if (battery.charging) battery.charge_ma else battery.drain_ma
    if (rateMa != null && rateMa > 0) {
        extras += BatteryInsight(
            InsightKind.RATE,
            "${format_mah(rateMa)} mA",
            if (battery.charging) "charging now" else "draining now",
            primary_fraction = (rateMa / 3000f).coerceIn(0.08f, 1f)
        )
    } else if (battery.session_percent_per_hour != null) {
        val percentPerHour = battery.session_percent_per_hour
        if (percentPerHour != null) {
            extras += BatteryInsight(
                InsightKind.RATE,
                "$percentPerHour%/h",
                if (battery.charging) "charge pace" else "drain pace",
                primary_fraction = (percentPerHour / 20f).coerceIn(0.08f, 1f)
            )
        }
    } else {
        extras += BatteryInsight(
            InsightKind.RATE,
            "Awaiting",
            if (battery.charging) "charge pace" else "drain pace",
            primary_fraction = 0.08f
        )
    }

    val sessionDuration = battery.session_duration_minutes
    extras += BatteryInsight(
        InsightKind.LEVEL_DELTA,
        sessionDuration?.let(::format_duration_minutes) ?: "0m",
        "current session",
        primary_fraction = ((sessionDuration ?: 0) / (6f * 60f)).coerceIn(0.04f, 1f)
    )

    extras += BatteryInsight(
        InsightKind.CAPACITY,
        battery.projected_mah_remaining(nowMillis)?.let { "${format_mah(it)} mAh" } ?: "Awaiting",
        "stored charge",
        primary_fraction = battery.level.coerceIn(0, 100) / 100f
    )

    extras += BatteryInsight(
        InsightKind.SPEED,
        when {
            battery.plug_label.isNotEmpty() -> battery.plug_label
            battery.full -> "Charged"
            battery.charging -> "Connected"
            else -> "Battery"
        },
        when {
            battery.plug_label.isNotEmpty() -> "power source"
            battery.full -> "battery state"
            battery.charging -> "power source"
            else -> "running on battery"
        },
        primary_fraction = when {
            battery.full -> 1f
            battery.charging -> 0.52f
            else -> 0.18f
        }
    )

    return (primary + extras).distinct()
} // compute_battery_insights

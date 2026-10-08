package com.civic.app.safety.time

import com.civic.shared.model.TimeOfDay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

/**
 * Puts a moment into a [TimeOfDay] bucket using the sun at the report's position, so "dawn" means the same
 * light conditions in Guwahati in June (≈04:00 IST) and Bhuj in January (≈07:30 IST).
 *
 * Buckets on the local date: DAWN = civil dawn − 60 min → sunrise + 60 min, MORNING → 11:00, MIDDAY → 16:00,
 * EVENING → civil dusk, NIGHT → 21:00, LATE_NIGHT otherwise. Boundaries are forced to be non-decreasing, so a
 * bucket whose end falls before its start is simply empty. Without a position, or when the sun doesn't rise or
 * set that day, it falls back to [TimeOfDay.fallbackFor].
 */
object TimeOfDayClassifier {
    private const val DAWN_MARGIN_MIN = 60L

    /** Generous: covers single-zone countries like India (≤1 h) and China (≈3 h in the far west). */
    private const val MAX_ZONE_SUN_MISMATCH_HOURS = 3.5
    private val MIDDAY_START = LocalTime.of(11, 0)
    private val EVENING_START = LocalTime.of(16, 0)
    private val LATE_NIGHT_START = LocalTime.of(21, 0)
    private val ORDER = listOf(
        TimeOfDay.DAWN,
        TimeOfDay.MORNING,
        TimeOfDay.MIDDAY,
        TimeOfDay.EVENING,
        TimeOfDay.NIGHT,
        TimeOfDay.LATE_NIGHT,
    )

    fun classify(
        epochMillis: Long,
        latitude: Double?,
        longitude: Double?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): TimeOfDay {
        val local = Instant.ofEpochMilli(epochMillis).atZone(zone)
        fun fallback() = TimeOfDay.fallbackFor(local.hour * 60 + local.minute)
        if (latitude == null || longitude == null) return fallback()
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return fallback()
        // Sun times on the local date only make sense if the clock's zone roughly matches the position (not, say,
        // a phone on IST with a GPS fix in California); otherwise fixed hours are the safer guess.
        val zoneVsSunHours = local.offset.totalSeconds / 3600.0 - longitude / 15.0
        if (abs(Math.IEEEremainder(zoneVsSunHours, 24.0)) > MAX_ZONE_SUN_MISMATCH_HOURS) return fallback()
        val sun = SolarCalculator.sunTimes(latitude, longitude, local.toLocalDate(), zone)
        val civilDawn = sun.civilDawn ?: return fallback()
        val sunrise = sun.sunrise ?: return fallback()
        val civilDusk = sun.civilDusk ?: return fallback()

        fun clock(time: LocalTime): Long = ZonedDateTime.of(local.toLocalDate(), time, zone).toInstant().toEpochMilli()
        // Start of each bucket in ORDER; LATE_NIGHT also covers the hours before DAWN.
        val starts = longArrayOf(
            civilDawn.minusMinutes(DAWN_MARGIN_MIN).toInstant().toEpochMilli(),
            sunrise.plusMinutes(DAWN_MARGIN_MIN).toInstant().toEpochMilli(),
            clock(MIDDAY_START),
            clock(EVENING_START),
            civilDusk.toInstant().toEpochMilli(),
            clock(LATE_NIGHT_START),
        )
        for (i in 1 until starts.size) starts[i] = maxOf(starts[i], starts[i - 1])
        val bucket = starts.indexOfLast { epochMillis >= it }
        return if (bucket < 0) TimeOfDay.LATE_NIGHT else ORDER[bucket]
    }
}

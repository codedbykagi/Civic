package com.civic.shared.model

import kotlinx.serialization.Serializable

/**
 * Part of the day a report refers to. Some streets are only unsafe at certain times (e.g. deserted at dawn),
 * so safety zones and routes can be filtered by it.
 *
 * Across India dawn moves between roughly 04:00 and 07:35 IST with latitude and season, so the app classifies
 * by the sun when it knows where the report is (DAWN = civil dawn − 60 min → sunrise + 60 min, EVENING ends at
 * civil dusk). [fallbackStartMinute] gives fixed local-clock boundaries used when no position is known.
 * Stored by name, so new values must only be appended.
 */
@Serializable
enum class TimeOfDay(val displayName: String, val hint: String, val fallbackStartMinute: Int) {
    DAWN("Dawn", "around sunrise", 4 * 60),
    MORNING("Morning", "after sunrise – 11 am", 7 * 60),
    MIDDAY("Midday", "11 am – 4 pm", 11 * 60),
    EVENING("Evening", "4 pm – dusk", 16 * 60),
    NIGHT("Night", "dusk – 9 pm", 18 * 60 + 30),
    LATE_NIGHT("Late night", "9 pm – dawn", 21 * 60),
    ;

    companion object {
        /** Fixed-clock classification for [minuteOfDay] (0..1439, local time), used when the sun can't be computed. */
        fun fallbackFor(minuteOfDay: Int): TimeOfDay {
            require(minuteOfDay in 0 until 24 * 60) { "minuteOfDay out of range: $minuteOfDay" }
            // LATE_NIGHT wraps past midnight: anything before DAWN's start belongs to it.
            return entries.lastOrNull { minuteOfDay >= it.fallbackStartMinute } ?: LATE_NIGHT
        }
    }
}

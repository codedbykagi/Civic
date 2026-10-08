package com.civic.app.safety

import com.civic.app.safety.routing.AvoidZone
import com.civic.app.safety.zones.HeatZone
import com.civic.shared.model.GeoLocation
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.TimeOfDay

/**
 * How much a heat zone should matter when planning a walk *now*. Personal-safety reports count fully, civic
 * problems only a little (a pothole is not a threat; a broken streetlight is, after dark), and reports from the
 * same part of the day count more than reports from other times (a street deserted at dawn may be busy at noon).
 */
object ZoneRelevance {
    private val DARK = setOf(TimeOfDay.DAWN, TimeOfDay.NIGHT, TimeOfDay.LATE_NIGHT)

    fun categoryWeight(category: IssueCategory, now: TimeOfDay): Double = when (category) {
        IssueCategory.UNSAFE_WOMEN, IssueCategory.UNSAFE_CHILDREN -> 1.0
        IssueCategory.UNSAFE_GENERAL -> 0.8
        IssueCategory.STREETLIGHT -> if (now in DARK) 0.6 else 0.2
        else -> 0.25
    }

    /** 1.0 for the same part of the day, 0.5 for a neighbouring one (wrapping past midnight), else 0.2. */
    fun timeMatch(reported: TimeOfDay, now: TimeOfDay): Double {
        val n = TimeOfDay.entries.size
        val gap = Math.floorMod(reported.ordinal - now.ordinal, n).let { minOf(it, n - it) }
        return when (gap) {
            0 -> 1.0
            1 -> 0.5
            else -> 0.2
        }
    }

    fun relevance(zone: HeatZone, now: TimeOfDay): Double {
        if (zone.count == 0) return 0.0
        val category = zone.byCategory.entries.sumOf { (c, n) -> categoryWeight(c, now) * n } / zone.count
        val time = zone.byTimeOfDay.entries.sumOf { (t, n) -> timeMatch(t, now) * n } / zone.count
        return zone.intensity * category * time
    }

    fun toAvoidZones(zones: List<HeatZone>, now: TimeOfDay): List<AvoidZone> = zones.map { z ->
        AvoidZone(
            center = GeoLocation(z.latitude, z.longitude),
            radiusM = z.radiusM,
            relevance = relevance(z, now),
            label = "${z.kind.label} (${z.count})",
        )
    }
}

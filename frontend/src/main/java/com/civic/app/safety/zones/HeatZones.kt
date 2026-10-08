package com.civic.app.safety.zones

import com.civic.shared.model.IssueCategory
import com.civic.shared.model.SafetyTag
import com.civic.shared.model.TimeOfDay
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Who a zone is unsafe for. Each kind is clustered on its own so every zone has exactly one colour. */
enum class ZoneKind(val label: String) {
    WOMEN("Unsafe for women"),
    CHILDREN("Unsafe for children"),
    OTHER("Other reports"),
    ;

    companion object {
        fun of(category: IssueCategory): ZoneKind = when (category) {
            IssueCategory.UNSAFE_WOMEN -> WOMEN
            IssueCategory.UNSAFE_CHILDREN -> CHILDREN
            else -> OTHER
        }
    }
}

/** One located report, already reduced to what zoning needs (the caller resolves its effective [timeOfDay]). */
data class ZoneInput(
    val reportId: Long,
    val latitude: Double,
    val longitude: Double,
    val category: IssueCategory,
    val timeOfDay: TimeOfDay,
    val capturedAt: Long,
    val tags: Set<SafetyTag>,
)

/**
 * An aggregated area with reports of one [kind]. Zones, not single pins, are what the map shows for safety
 * reports, so one person's exact positions and times aren't exposed. Breakdown maps hold non-zero counts only,
 * in enum order.
 */
data class HeatZone(
    val key: String,
    val kind: ZoneKind,
    val latitude: Double,
    val longitude: Double,
    val radiusM: Double,
    val count: Int,
    /** Sum of the members' recency weights; drives [intensity]. */
    val weight: Double,
    /** 0..1, absolute rather than relative to the hottest zone, so a zone doesn't fade when another one grows. */
    val intensity: Double,
    val byTimeOfDay: Map<TimeOfDay, Int>,
    val byCategory: Map<IssueCategory, Int>,
    val tagCounts: Map<SafetyTag, Int>,
    val reportIds: List<Long>,
    val latestAt: Long,
) {
    /** Most frequent time of day; ties go to the earlier [TimeOfDay]. */
    val dominantTimeOfDay: TimeOfDay?
        get() = TimeOfDay.entries.filter { (byTimeOfDay[it] ?: 0) > 0 }.maxByOrNull { byTimeOfDay.getValue(it) }
}

/**
 * Clusters reports into [HeatZone]s: 200 m grid binning per [ZoneKind], then a greedy merge of neighbouring
 * cells that a grid line split. Older reports fade with a 30-day half-life and disappear after 180 days, so the
 * map reflects current conditions. The result is deterministic and independent of input order.
 */
object HeatZones {
    const val CELL_M = 200.0
    const val MAX_AGE_DAYS = 180
    const val HALF_LIFE_DAYS = 30.0

    private const val EARTH_RADIUS_M = 6_371_008.8
    private const val M_PER_DEG_LAT = 111_320.0
    private const val MS_PER_DAY = 86_400_000.0
    private const val MIN_RADIUS_M = 100.0
    private const val MAX_RADIUS_M = 300.0

    /** Canonical order: every sum below runs in it, so shuffled input gives bit-identical doubles. */
    private val INPUT_ORDER = compareBy<ZoneInput>(
        { it.reportId },
        { it.capturedAt },
        { it.latitude },
        { it.longitude },
        { it.category },
        { it.timeOfDay },
        { input -> input.tags.sorted().joinToString(",") },
    )

    /** Draw order: OTHER under WOMEN under CHILDREN, hottest last, so the strongest safety zones end up on top. */
    private val DRAW_ORDER = compareBy<HeatZone>(
        { drawRank(it.kind) },
        { it.weight },
        { it.latitude },
        { it.longitude },
        { it.key },
    )

    fun build(
        inputs: List<ZoneInput>,
        nowMillis: Long,
        timeFilter: TimeOfDay? = null,
        kinds: Set<ZoneKind> = ZoneKind.entries.toSet(),
    ): List<HeatZone> {
        val members = inputs.asSequence()
            .filter { timeFilter == null || it.timeOfDay == timeFilter }
            .filter { ZoneKind.of(it.category) in kinds }
            .filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
            .mapNotNull { input ->
                // A clock-skewed future timestamp counts as brand new rather than heavier than new.
                val ageDays = (nowMillis - input.capturedAt).coerceAtLeast(0L) / MS_PER_DAY
                if (ageDays > MAX_AGE_DAYS) null else Member(input, 0.5.pow(ageDays / HALF_LIFE_DAYS))
            }
            .sortedWith(compareBy(INPUT_ORDER) { it.input })
            .toList()
        return members.groupBy { ZoneKind.of(it.input.category) }
            .flatMap { (kind, ofKind) -> cluster(kind, ofKind) }
            .sortedWith(DRAW_ORDER)
    }

    /** Great-circle (haversine) distance in metres on the mean-radius sphere. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    private class Member(val input: ZoneInput, val weight: Double)

    private data class Cell(val row: Long, val col: Long)

    /** Members of one or more cells; [seed] is the heaviest cell, which names the zone. */
    private class Group(val seed: Cell, val members: MutableList<Member>) {
        var weight = 0.0
        var latitude = 0.0
        var longitude = 0.0

        init {
            recompute()
        }

        fun recompute() {
            weight = members.sumOf { it.weight }
            latitude = members.sumOf { it.input.latitude * it.weight } / weight
            longitude = members.sumOf { it.input.longitude * it.weight } / weight
        }
    }

    private fun cluster(kind: ZoneKind, members: List<Member>): List<HeatZone> {
        val cells = members.groupBy { cellOf(it.input.latitude, it.input.longitude) }
            .map { (cell, inCell) -> Group(cell, inCell.toMutableList()) }
            .sortedWith(
                compareByDescending<Group> { it.weight }
                    .thenBy { it.latitude }
                    .thenBy { it.longitude }
                    .thenBy { it.seed.row }
                    .thenBy { it.seed.col },
            )
        val zones = ArrayList<Group>()
        // Accepted zones by the cell holding their current centre, so the merge isn't quadratic.
        val index = HashMap<Cell, MutableList<Int>>()
        for (cell in cells) {
            val target = firstZoneWithin(zones, index, cell.latitude, cell.longitude)
            if (target < 0) {
                zones += cell
                index.getOrPut(cellOf(cell.latitude, cell.longitude)) { ArrayList() } += zones.lastIndex
            } else {
                val zone = zones[target]
                val before = cellOf(zone.latitude, zone.longitude)
                zone.members += cell.members
                zone.recompute()
                val after = cellOf(zone.latitude, zone.longitude)
                if (after != before) {
                    index.getValue(before).remove(target)
                    index.getOrPut(after) { ArrayList() } += target
                }
            }
        }
        return zones.map { toZone(kind, it) }
    }

    /** Lowest-index (first accepted) zone whose centre is within [CELL_M], or -1. */
    private fun firstZoneWithin(
        zones: List<Group>,
        index: Map<Cell, List<Int>>,
        latitude: Double,
        longitude: Double,
    ): Int {
        var first = -1
        // 200 m can span just over one grid step (grid metres vs. the haversine sphere), so look two cells out.
        val row = rowOf(latitude)
        for (r in row - 2..row + 2) {
            val col = floor(longitude / lonStep(r)).toLong()
            for (c in col - 2..col + 2) {
                for (i in index[Cell(r, c)] ?: continue) {
                    if (first in 0..<i) continue
                    val zone = zones[i]
                    if (distanceMeters(zone.latitude, zone.longitude, latitude, longitude) <= CELL_M) first = i
                }
            }
        }
        return first
    }

    private fun cellOf(latitude: Double, longitude: Double): Cell {
        val row = rowOf(latitude)
        return Cell(row, floor(longitude / lonStep(row)).toLong())
    }

    private fun rowOf(latitude: Double): Long = floor(latitude / (CELL_M / M_PER_DEG_LAT)).toLong()

    /** Longitude step per row keeps cells roughly square at any latitude (capped near the poles). */
    private fun lonStep(row: Long): Double {
        val rowCentre = Math.toRadians((row + 0.5) * (CELL_M / M_PER_DEG_LAT))
        return CELL_M / (M_PER_DEG_LAT * cos(rowCentre).coerceAtLeast(0.01))
    }

    private fun toZone(kind: ZoneKind, group: Group): HeatZone {
        val members = group.members.sortedWith(compareBy(INPUT_ORDER) { it.input })
        val weight = members.sumOf { it.weight }
        val count = members.size
        return HeatZone(
            key = "${kind.name}:${group.seed.row}:${group.seed.col}",
            kind = kind,
            latitude = members.sumOf { it.input.latitude * it.weight } / weight,
            longitude = members.sumOf { it.input.longitude * it.weight } / weight,
            radiusM = (100.0 * sqrt(count.toDouble())).coerceIn(MIN_RADIUS_M, MAX_RADIUS_M),
            count = count,
            weight = weight,
            intensity = 1.0 - exp(-weight / 4.0),
            byTimeOfDay = countsOf(TimeOfDay.entries, members.map { it.input.timeOfDay }),
            byCategory = countsOf(IssueCategory.entries, members.map { it.input.category }),
            tagCounts = countsOf(SafetyTag.entries, members.flatMap { it.input.tags }),
            reportIds = members.map { it.input.reportId },
            latestAt = members.maxOf { it.input.capturedAt },
        )
    }

    private fun <E> countsOf(order: List<E>, values: List<E>): Map<E, Int> {
        val counts = values.groupingBy { it }.eachCount()
        return order.filter { it in counts }.associateWith { counts.getValue(it) }
    }

    private fun drawRank(kind: ZoneKind): Int = when (kind) {
        ZoneKind.OTHER -> 0
        ZoneKind.WOMEN -> 1
        ZoneKind.CHILDREN -> 2
    }
}

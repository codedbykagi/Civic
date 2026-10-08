package com.civic.app.safety.zones

import com.civic.shared.model.IssueCategory
import com.civic.shared.model.SafetyTag
import com.civic.shared.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp
import kotlin.math.floor
import kotlin.random.Random

class HeatZonesTest {
    private val now = 1_791_547_200_000L // 2026-10-09T12:00:00Z
    private val dayMs = 86_400_000L
    private val delhiLat = 28.6139
    private val delhiLon = 77.2090
    private val metresPerDegLat = 111_320.0

    private fun input(
        id: Long,
        lat: Double = delhiLat,
        lon: Double = delhiLon,
        category: IssueCategory = IssueCategory.UNSAFE_WOMEN,
        timeOfDay: TimeOfDay = TimeOfDay.NIGHT,
        ageDays: Double = 0.0,
        tags: Set<SafetyTag> = emptySet(),
    ) = ZoneInput(id, lat, lon, category, timeOfDay, now - (ageDays * dayMs).toLong(), tags)

    @Test
    fun kindOfCategory() {
        assertEquals(ZoneKind.WOMEN, ZoneKind.of(IssueCategory.UNSAFE_WOMEN))
        assertEquals(ZoneKind.CHILDREN, ZoneKind.of(IssueCategory.UNSAFE_CHILDREN))
        assertEquals(ZoneKind.OTHER, ZoneKind.of(IssueCategory.UNSAFE_GENERAL))
        IssueCategory.CIVIC.forEach { assertEquals(ZoneKind.OTHER, ZoneKind.of(it)) }
    }

    @Test
    fun outputIsIndependentOfInputOrder() {
        val random = Random(42)
        val categories = IssueCategory.entries
        val inputs = (1L..300L).map { id ->
            input(
                id = id,
                // Spread over ~2 km so many cells, merges and grid-line splits happen.
                lat = delhiLat + random.nextDouble(-0.01, 0.01),
                lon = delhiLon + random.nextDouble(-0.01, 0.01),
                category = categories[random.nextInt(categories.size)],
                timeOfDay = TimeOfDay.entries[random.nextInt(TimeOfDay.entries.size)],
                ageDays = random.nextDouble(-2.0, 200.0),
                tags = SafetyTag.entries.filter { random.nextBoolean() }.toSet(),
            )
        }
        val expected = HeatZones.build(inputs, now)
        assertTrue(expected.size > 10)
        assertEquals(expected, HeatZones.build(inputs.reversed(), now))
        repeat(20) { seed -> assertEquals(expected, HeatZones.build(inputs.shuffled(Random(seed)), now)) }
        val safety = setOf(ZoneKind.WOMEN, ZoneKind.CHILDREN)
        val filtered = HeatZones.build(inputs, now, TimeOfDay.DAWN, safety)
        assertTrue(filtered.isNotEmpty())
        assertEquals(filtered, HeatZones.build(inputs.shuffled(Random(7)), now, TimeOfDay.DAWN, safety))
    }

    @Test
    fun reportsSplitByGridLineMerge() {
        // Straddle a row boundary so the two reports start in different 200 m cells.
        val dLat = HeatZones.CELL_M / metresPerDegLat
        val boundary = (floor(delhiLat / dLat) + 1) * dLat
        val a = input(1, lat = boundary - 90 / metresPerDegLat)
        val b = input(2, lat = boundary + 90 / metresPerDegLat)
        val distance = HeatZones.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        assertEquals(180.0, distance, 1.0)

        val zones = HeatZones.build(listOf(a, b), now)
        assertEquals(1, zones.size)
        assertEquals(2, zones.single().count)
        assertEquals(listOf(1L, 2L), zones.single().reportIds)
        assertEquals((a.latitude + b.latitude) / 2, zones.single().latitude, 1e-9)
    }

    @Test
    fun distantReportsStaySeparate() {
        val zones = HeatZones.build(listOf(input(1), input(2, lat = delhiLat + 500 / metresPerDegLat)), now)
        assertEquals(2, zones.size)
        assertTrue(zones.all { it.count == 1 })
    }

    @Test
    fun kindsAtTheSameSpotFormSeparateZones() {
        val inputs = listOf(
            input(1, category = IssueCategory.UNSAFE_WOMEN),
            input(2, category = IssueCategory.UNSAFE_CHILDREN),
            input(3, category = IssueCategory.UNSAFE_GENERAL),
            input(4, category = IssueCategory.STREETLIGHT),
        )
        val zones = HeatZones.build(inputs, now)
        assertEquals(listOf(ZoneKind.OTHER, ZoneKind.WOMEN, ZoneKind.CHILDREN), zones.map { it.kind })
        val other = zones.first()
        assertEquals(2, other.count)
        assertEquals(mapOf(IssueCategory.STREETLIGHT to 1, IssueCategory.UNSAFE_GENERAL to 1), other.byCategory)
        assertEquals(3, zones.map { it.key }.toSet().size)
    }

    @Test
    fun oldReportsAreDropped() {
        val zones = HeatZones.build(listOf(input(1, ageDays = 181.0), input(2, ageDays = 179.0)), now)
        assertEquals(listOf(2L), zones.single().reportIds)
        assertTrue(HeatZones.build(listOf(input(1, ageDays = 400.0)), now).isEmpty())
    }

    @Test
    fun weightHalvesEveryThirtyDays() {
        fun weightOf(ageDays: Double) = HeatZones.build(listOf(input(1, ageDays = ageDays)), now).single().weight
        assertEquals(1.0, weightOf(0.0), 1e-9)
        assertEquals(0.5, weightOf(30.0), 1e-9)
        assertEquals(0.25, weightOf(60.0), 1e-9)
        assertEquals(1.0, weightOf(-5.0), 1e-9) // future timestamp clamps to age 0

        val zone = HeatZones.build(listOf(input(1), input(2, ageDays = 30.0)), now).single()
        assertEquals(1.5, zone.weight, 1e-9)
        assertEquals(1 - exp(-1.5 / 4), zone.intensity, 1e-9)
        assertEquals(now, zone.latestAt)
    }

    @Test
    fun centreIsWeightedTowardsRecentReports() {
        val fresh = input(1, lon = delhiLon)
        val old = input(2, lon = delhiLon + 0.001, ageDays = 30.0) // ~98 m east, half the weight
        val zone = HeatZones.build(listOf(fresh, old), now).single()
        assertEquals(delhiLon + 0.001 / 3, zone.longitude, 1e-9)
    }

    @Test
    fun timeAndKindFilters() {
        val inputs = listOf(
            input(1, timeOfDay = TimeOfDay.DAWN),
            input(2, timeOfDay = TimeOfDay.NIGHT),
            input(3, category = IssueCategory.UNSAFE_CHILDREN, timeOfDay = TimeOfDay.DAWN),
            input(4, category = IssueCategory.POTHOLE, timeOfDay = TimeOfDay.DAWN),
        )
        val dawn = HeatZones.build(inputs, now, timeFilter = TimeOfDay.DAWN)
        assertEquals(listOf(4L, 1L, 3L), dawn.flatMap { it.reportIds })
        assertTrue(dawn.all { it.byTimeOfDay.keys == setOf(TimeOfDay.DAWN) })

        val women = HeatZones.build(inputs, now, kinds = setOf(ZoneKind.WOMEN))
        assertEquals(listOf(ZoneKind.WOMEN), women.map { it.kind })
        assertEquals(listOf(1L, 2L), women.single().reportIds)

        assertEquals(listOf(1L), HeatZones.build(inputs, now, TimeOfDay.DAWN, setOf(ZoneKind.WOMEN)).single().reportIds)
        assertTrue(HeatZones.build(inputs, now, TimeOfDay.MIDDAY).isEmpty())
        assertTrue(HeatZones.build(inputs, now, kinds = emptySet()).isEmpty())
    }

    @Test
    fun radiusGrowsWithCountAndIsClamped() {
        fun radiusFor(count: Int) = HeatZones.build((1L..count).map { input(it) }, now).single().radiusM
        assertEquals(100.0, radiusFor(1), 1e-9)
        assertEquals(100.0 * Math.sqrt(2.0), radiusFor(2), 1e-9)
        assertEquals(200.0, radiusFor(4), 1e-9)
        assertEquals(300.0, radiusFor(9), 1e-9)
        assertEquals(300.0, radiusFor(25), 1e-9)
    }

    @Test
    fun sortedForDrawingHottestSafetyZonesLast() {
        val far = 0.02 // ~2 km apart, so no merging
        val inputs = listOf(
            input(1, category = IssueCategory.UNSAFE_CHILDREN),
            input(2, category = IssueCategory.UNSAFE_CHILDREN, lat = delhiLat + far),
            input(3, category = IssueCategory.UNSAFE_CHILDREN, lat = delhiLat + far),
            input(4, category = IssueCategory.UNSAFE_WOMEN),
            input(5, category = IssueCategory.UNSAFE_WOMEN, lat = delhiLat + far, ageDays = 90.0),
            input(6, category = IssueCategory.GARBAGE),
            input(7, category = IssueCategory.FIRE, lat = delhiLat + far),
            input(8, category = IssueCategory.FIRE, lat = delhiLat + far),
        )
        val zones = HeatZones.build(inputs, now)
        val other = ZoneKind.OTHER
        val women = ZoneKind.WOMEN
        val children = ZoneKind.CHILDREN
        assertEquals(listOf(other, other, women, women, children, children), zones.map { it.kind })
        assertEquals(listOf(listOf(6L), listOf(7L, 8L)), zones.take(2).map { it.reportIds })
        assertEquals(listOf(listOf(5L), listOf(4L)), zones.slice(2..3).map { it.reportIds })
        assertEquals(listOf(listOf(1L), listOf(2L, 3L)), zones.takeLast(2).map { it.reportIds })
        for (i in 1 until zones.size) {
            if (zones[i].kind == zones[i - 1].kind) assertTrue(zones[i].weight >= zones[i - 1].weight)
        }
    }

    @Test
    fun breakdownsAndDominantTimeOfDay() {
        val inputs = listOf(
            input(3, timeOfDay = TimeOfDay.NIGHT, tags = setOf(SafetyTag.POOR_LIGHTING, SafetyTag.DESERTED)),
            input(1, timeOfDay = TimeOfDay.DAWN, tags = setOf(SafetyTag.POOR_LIGHTING)),
            input(2, timeOfDay = TimeOfDay.NIGHT, ageDays = 3.0),
            input(4, timeOfDay = TimeOfDay.DAWN),
        )
        val zone = HeatZones.build(inputs, now).single()
        assertEquals(listOf(1L, 2L, 3L, 4L), zone.reportIds)
        assertEquals(mapOf(TimeOfDay.DAWN to 2, TimeOfDay.NIGHT to 2), zone.byTimeOfDay)
        assertEquals(listOf(TimeOfDay.DAWN, TimeOfDay.NIGHT), zone.byTimeOfDay.keys.toList()) // enum order
        assertEquals(TimeOfDay.DAWN, zone.dominantTimeOfDay) // tie -> earlier enum value
        assertEquals(mapOf(SafetyTag.POOR_LIGHTING to 2, SafetyTag.DESERTED to 1), zone.tagCounts)
        assertEquals(mapOf(IssueCategory.UNSAFE_WOMEN to 4), zone.byCategory)

        val nightHeavy = HeatZones.build(inputs + input(5, timeOfDay = TimeOfDay.NIGHT), now).single()
        assertEquals(TimeOfDay.NIGHT, nightHeavy.dominantTimeOfDay)
        val empty = zone.copy(byTimeOfDay = emptyMap())
        assertEquals(null, empty.dominantTimeOfDay)
    }

    @Test
    fun keyIsStableWhenWeakerReportsJoin() {
        val base = listOf(input(1), input(2), input(3))
        val key = HeatZones.build(base, now).single().key
        val grown = HeatZones.build(base + input(9, lat = delhiLat + 60 / metresPerDegLat, ageDays = 60.0), now)
        assertEquals(key, grown.single().key)
        assertTrue(key.startsWith("WOMEN:"))
        val children = HeatZones.build(listOf(input(1, category = IssueCategory.UNSAFE_CHILDREN)), now).single()
        assertNotEquals(key, children.key)
    }

    @Test
    fun haversineDistance() {
        assertEquals(111_195.08, HeatZones.distanceMeters(0.0, 0.0, 1.0, 0.0), 0.01) // R·π/180
        assertEquals(0.0, HeatZones.distanceMeters(delhiLat, delhiLon, delhiLat, delhiLon), 0.0)
        // Delhi -> Mumbai, ~1,150 km great-circle.
        assertEquals(1_150_000.0, HeatZones.distanceMeters(delhiLat, delhiLon, 19.0760, 72.8777), 10_000.0)
    }

    @Test
    fun styleColoursAndAlpha() {
        assertEquals(0xFFBB5566.toInt(), ZoneStyle.fillColor(ZoneKind.WOMEN))
        assertEquals(0xFF882255.toInt(), ZoneStyle.outlineColor(ZoneKind.WOMEN))
        assertEquals(0xFF004488.toInt(), ZoneStyle.fillColor(ZoneKind.CHILDREN))
        assertEquals(0xFF7A5A00.toInt(), ZoneStyle.outlineColor(ZoneKind.OTHER))
        assertEquals(OutlineStyle.DASHED, ZoneStyle.outlineStyle(ZoneKind.CHILDREN))
        assertEquals(null, ZoneStyle.dashIntervalsDp(ZoneStyle.outlineStyle(ZoneKind.WOMEN)))
        ZoneKind.entries.forEach { assertEquals(0xFF, ZoneStyle.fillColor(it) ushr 24) }
        assertEquals(63, ZoneStyle.fillAlpha(1 - exp(-1.0 / 4))) // one fresh report
        assertEquals(38, ZoneStyle.fillAlpha(0.0))
        assertEquals(153, ZoneStyle.fillAlpha(1.0))
        assertEquals(153, ZoneStyle.fillAlpha(5.0))
    }
}

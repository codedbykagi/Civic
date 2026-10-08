package com.civic.app.safety.routing

import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.tan

class ExcludePolygonsTest {
    private val origin = geo(28.6, 77.15)
    private val destination = geo(28.6, 77.25)
    private val route = path(origin, destination)

    /** [count] zones of [radius] m spaced along the route, all crossed by it. */
    private fun zonesOnRoute(count: Int, radius: Double) = (1..count).map {
        AvoidZone(geo(28.6, 77.15 + 0.1 * it / (count + 1)), radius, relevance = 1.0, label = "z$it")
    }

    @Test
    fun octagonIsClosedAndCircumscribesTheCircle() {
        val center = geo(28.6, 77.2)
        val ring = ExcludePolygons.octagon(center, 100.0)

        assertEquals(9, ring.size)
        assertEquals(ring.first(), ring.last())
        ring.forEach { assertEquals(100.0 / cos(PI / 8), RouteScorer.distanceMeters(center, it), 0.01) }
        assertEquals(16 * 100.0 * tan(PI / 8), RouteScorer.lengthMeters(ring), 0.5)
    }

    @Test
    fun vertexBudgetCapsSmallZones() {
        val selection = ExcludePolygons.select(zonesOnRoute(20, 50.0), listOf(route), origin, destination)

        assertEquals(11, selection.rings.size)
        assertTrue(selection.vertexCount <= ExcludePolygons.MAX_VERTICES)
        assertTrue(selection.perimeterM <= ExcludePolygons.MAX_PERIMETER_M)
        assertEquals(9, selection.overBudget.size)
    }

    @Test
    fun perimeterBudgetCapsLargeZones() {
        val selection = ExcludePolygons.select(zonesOnRoute(6, 300.0), listOf(route), origin, destination)

        // Each 300 m octagon is ~1 988 m round, so only four fit under 10 km.
        assertEquals(4, selection.rings.size)
        assertTrue(selection.perimeterM <= ExcludePolygons.MAX_PERIMETER_M)
        assertTrue(selection.vertexCount <= ExcludePolygons.MAX_VERTICES)
        assertEquals(2, selection.overBudget.size)
    }

    @Test
    fun picksZonesByRelevanceTimesMetresInside() {
        // Priorities: major 0.9 × 200 m, minor 0.5 × 200 m, grazing 1.0 × ~65 m (passes 94 m from its centre).
        val minor = AvoidZone(geo(28.6, 77.18), 100.0, relevance = 0.5, label = "minor")
        val grazing = AvoidZone(geo(28.60085, 77.2), 100.0, relevance = 1.0, label = "grazing")
        val major = AvoidZone(geo(28.6, 77.22), 100.0, relevance = 0.9, label = "major")

        val selection = ExcludePolygons.select(
            listOf(minor, grazing, major), listOf(route), origin, destination, maxVertices = 18,
        )

        assertEquals(listOf(major, minor), selection.excluded)
        assertEquals(listOf(grazing), selection.overBudget)
    }

    @Test
    fun neverExcludesZonesAroundTheEndpoints() {
        val atStart = AvoidZone(origin, 100.0, 1.0, "start")
        val nearEnd = AvoidZone(RouteScorer.destinationPoint(destination, 270.0, 120.0), 100.0, 1.0, "end")
        val middle = AvoidZone(geo(28.6, 77.2), 100.0, 1.0, "middle")

        val selection = ExcludePolygons.select(listOf(atStart, nearEnd, middle), listOf(route), origin, destination)

        assertEquals(listOf(middle), selection.excluded)
        assertEquals(listOf(atStart, nearEnd), selection.containsEndpoint)
        selection.rings.flatten().forEach {
            assertTrue(RouteScorer.distanceMeters(it, origin) > 100.0)
            assertTrue(RouteScorer.distanceMeters(it, destination) > 100.0)
        }
    }

    @Test
    fun ignoresZonesFarFromEveryCandidate() {
        val far = AvoidZone(geo(28.65, 77.2), 100.0, 1.0, "far")
        val corridor = AvoidZone(geo(28.602, 77.2), 50.0, 1.0, "corridor")

        val selection = ExcludePolygons.select(listOf(far, corridor), listOf(route), origin, destination)

        assertEquals(listOf(corridor), selection.excluded)
        assertFalse(far in selection.overBudget)
    }

    @Test
    fun jsonIsLonLatWithSixDecimals() {
        val json = ExcludePolygons.toJson(listOf(listOf(geo(28.123456789, 77.987654321))))
        val pair = json[0].jsonArray[0].jsonArray

        assertEquals(77.987654, pair[0].jsonPrimitive.double, 0.0)
        assertEquals(28.123457, pair[1].jsonPrimitive.double, 0.0)
    }
}

package com.civic.app.safety.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteScorerTest {
    private val center = geo(28.6, 77.2)

    @Test
    fun haversineMatchesOneDegreeOfLatitude() {
        assertEquals(111_195.08, RouteScorer.distanceMeters(geo(10.0, 77.0), geo(11.0, 77.0)), 0.05)
        assertEquals(0.0, RouteScorer.distanceMeters(center, center), 0.0)
    }

    @Test
    fun lengthSumsSegments() {
        val route = listOf(geo(10.0, 77.0), geo(10.5, 77.0), geo(11.0, 77.0))
        assertEquals(111_195.08, RouteScorer.lengthMeters(route), 0.05)
        assertEquals(0.0, RouteScorer.lengthMeters(listOf(center)), 0.0)
    }

    @Test
    fun softWeightIsFlatInCoreAndFadesPastTheEdge() {
        assertEquals(1.0, RouteScorer.softWeight(0.0, 100.0), 1e-12)
        assertEquals(1.0, RouteScorer.softWeight(60.0, 100.0), 1e-12)
        assertEquals(0.5, RouteScorer.softWeight(95.0, 100.0), 1e-12)
        assertEquals(0.0, RouteScorer.softWeight(130.0, 100.0), 1e-12)
        assertEquals(0.0, RouteScorer.softWeight(500.0, 100.0), 1e-12)
    }

    @Test
    fun exposureInsideCoreIsRelevanceTimesLength() {
        val zone = AvoidZone(center, radiusM = 1000.0, relevance = 0.5, label = "big")
        val route = path(geo(28.6, 77.199), geo(28.6, 77.201))
        val length = RouteScorer.lengthMeters(route)

        assertEquals(0.5 * length, RouteScorer.exposure(route, listOf(zone)), length * 0.001)
    }

    @Test
    fun exposureOutsideIsZero() {
        val zone = AvoidZone(center, radiusM = 100.0, relevance = 1.0, label = "z")
        val farRoute = path(geo(28.61, 77.19), geo(28.61, 77.21))

        assertEquals(0.0, RouteScorer.exposure(farRoute, listOf(zone)), 0.0)
        assertEquals(0, RouteScorer.zonesCrossed(farRoute, listOf(zone)))
    }

    @Test
    fun exposureOnSoftEdgeIsPartial() {
        val zone = AvoidZone(center, radiusM = 100.0, relevance = 2.0, label = "z")
        val start = RouteScorer.destinationPoint(center, 0.0, 95.0)
        val route = listOf(start, RouteScorer.destinationPoint(start, 90.0, 2.0))

        // ~2 m of route at 95 m from the centre: weight 0.5, relevance 2.
        assertEquals(2.0, RouteScorer.exposure(route, listOf(zone)), 0.02)
    }

    @Test
    fun fullCrossingWeighsCoreAndSoftEdges() {
        val zone = AvoidZone(center, radiusM = 100.0, relevance = 1.0, label = "z")
        val route = path(geo(28.6, 77.19), geo(28.6, 77.21))

        // 120 m of core plus two 70 m fades averaging 0.5.
        assertEquals(190.0, RouteScorer.exposure(route, listOf(zone)), 2.0)
    }

    @Test
    fun zonesCrossedCountsZonesTheRouteEntersOnly() {
        val route = path(geo(28.6, 77.19), geo(28.6, 77.21))
        val entered = AvoidZone(RouteScorer.destinationPoint(center, 0.0, 50.0), 100.0, 1.0, "entered")
        val grazed = AvoidZone(RouteScorer.destinationPoint(center, 0.0, 120.0), 100.0, 1.0, "grazed")
        val irrelevant = AvoidZone(center, 100.0, 0.0, "irrelevant")

        assertEquals(1, RouteScorer.zonesCrossed(route, listOf(entered, grazed, irrelevant)))
        assertTrue(RouteScorer.exposure(route, listOf(grazed)) > 0.0)
    }

    @Test
    fun metersInsideIsTheChordLength() {
        val zone = AvoidZone(center, radiusM = 100.0, relevance = 1.0, label = "z")
        val route = path(geo(28.6, 77.19), geo(28.6, 77.21))
        assertEquals(200.0, RouteScorer.metersInside(route, zone), 10.0)
    }

    @Test
    fun costAddsLambdaTimesExposure() {
        val zone = AvoidZone(center, radiusM = 100.0, relevance = 1.0, label = "z")
        val route = path(geo(28.6, 77.19), geo(28.6, 77.21))
        val option = RouteScorer.score(RouteCandidate(route, 1955.0, 1500.0), listOf(zone), 8.0, RouteRole.SHORTEST)

        assertEquals(1955.0, option.distanceM, 0.0)
        assertEquals(1955.0 + 8.0 * option.exposure, option.cost, 1e-9)
        assertEquals(1, option.zonesCrossed)
        assertEquals(RouteRole.SHORTEST, option.role)
    }

    @Test
    fun overlapFractionDetectsSameAndParallelRoutes() {
        val route = path(geo(28.6, 77.19), geo(28.6, 77.21))
        val parallel = path(geo(28.602, 77.19), geo(28.602, 77.21))
        val firstHalf = path(geo(28.6, 77.19), geo(28.6, 77.2))

        assertEquals(1.0, RouteScorer.overlapFraction(route, route), 0.0)
        assertEquals(0.0, RouteScorer.overlapFraction(route, parallel), 0.0)
        assertEquals(0.5, RouteScorer.overlapFraction(route, firstHalf), 0.03)
        assertEquals(1.0, RouteScorer.overlapFraction(firstHalf, route), 0.0)
    }

    @Test
    fun resampleKeepsEndsAndSpacing() {
        val route = path(geo(28.6, 77.19), geo(28.6, 77.2))
        val samples = RouteScorer.resample(route, 25.0)

        assertEquals(route.first(), samples.first())
        assertEquals(route.last(), samples.last())
        samples.zipWithNext().dropLast(1).forEach { (a, b) ->
            assertEquals(25.0, RouteScorer.distanceMeters(a, b), 0.5)
        }
    }
}

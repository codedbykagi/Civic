package com.civic.app.safety.routing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeRoutePlannerTest {
    private val origin = geo(28.6, 77.2)
    private val destination = geo(28.6, 77.21)

    /** ~976 m straight east, through [zone]'s centre. */
    private val direct = path(origin, destination)

    /** ~1 310 m, loops 167 m north around [zone]. */
    private val detour =
        path(origin, geo(28.6, 77.203), geo(28.6015, 77.203), geo(28.6015, 77.207), geo(28.6, 77.207), destination)
    private val zone = AvoidZone(geo(28.6, 77.205), radiusM = 100.0, relevance = 1.0, label = "Unsafe for women")

    private fun RoutingResult.plan() = (this as? RoutingResult.Success)?.plan ?: error("Expected success, got $this")

    private fun valhallaOnly(vararg responses: HttpResponseData): FakeTransport {
        val queue = ArrayDeque(responses.toList())
        return FakeTransport { request ->
            check(request.method == "POST") { "Unexpected OSRM call ${request.url}" }
            queue.removeFirst()
        }
    }

    @Test
    fun picksTheLowerExposureAlternativeAsFewerReports() = runBlocking {
        val transport = valhallaOnly(valhallaOk(direct, detour), valhallaError(442))

        val plan = SafeRoutePlanner(transport).plan(origin, destination, listOf(zone)).plan()

        assertEquals(listOf(RouteRole.FEWER_REPORTS, RouteRole.SHORTEST), plan.options.map { it.role })
        val recommended = plan.recommended!!
        assertEquals(detour.size, recommended.points.size)
        assertEquals(0, recommended.zonesCrossed)
        assertEquals(0.0, recommended.exposure, 0.0)
        assertEquals(1, plan.shortest!!.zonesCrossed)
        assertTrue(plan.shortest!!.distanceM < recommended.distanceM)
        assertEquals("Valhalla", plan.providerName)
        assertEquals("Routing © OpenStreetMap contributors (ODbL) · Valhalla / FOSSGIS", plan.attribution)
        assertTrue(plan.notices.toString(), "Fewer-reports route is +330 m and avoids 1 reported zone" in plan.notices)
        assertFalse(plan.notices.any { "safe" in it.lowercase() || "nobody" in it })
    }

    @Test
    fun sendsOneExcludeRequestWhenARouteCrossesAHardZone() = runBlocking {
        val transport = valhallaOnly(valhallaOk(direct), valhallaOk(detour))

        val plan = SafeRoutePlanner(transport).plan(origin, destination, listOf(zone)).plan()

        assertEquals(2, transport.requests.size)
        assertFalse("exclude_polygons" in transport.requests[0].body!!)
        assertTrue("exclude_polygons" in transport.requests[1].body!!)
        assertEquals(RouteRole.FEWER_REPORTS, plan.options[0].role)
        assertEquals(0, plan.options[0].zonesCrossed)
        assertEquals(2, plan.options.size)
    }

    @Test
    fun skipsTheExcludeRequestWhenNotNeeded() = runBlocking {
        val farZone = zone.copy(center = geo(28.62, 77.25))
        val weakZone = zone.copy(relevance = 0.2)
        for (zones in listOf(emptyList(), listOf(farZone), listOf(weakZone))) {
            val transport = valhallaOnly(valhallaOk(direct))
            val plan = SafeRoutePlanner(transport).plan(origin, destination, zones).plan()

            assertEquals(1, transport.requests.size)
            assertEquals(RouteRole.FEWER_REPORTS, plan.options.single().role)
            assertSame(plan.recommended, plan.shortest)
        }
    }

    @Test
    fun weakZonesStillCountInScoringAndNotices() = runBlocking {
        val weakZone = zone.copy(relevance = 0.2)
        val plan = SafeRoutePlanner(valhallaOnly(valhallaOk(direct))).plan(origin, destination, listOf(weakZone)).plan()

        val only = plan.options.single()
        assertTrue(only.exposure > 0)
        assertEquals(only.distanceM + 8.0 * only.exposure, only.cost, 1e-6)
        assertTrue("No route avoids every reported zone" in plan.notices)
    }

    @Test
    fun noZonesSaysAbsenceOfReportsIsNotAbsenceOfRisk() = runBlocking {
        val transport = valhallaOnly(valhallaOk(direct, detour))
        val plan = SafeRoutePlanner(transport).plan(origin, destination, emptyList()).plan()

        assertEquals(RouteScorer.lengthMeters(direct), plan.recommended!!.distanceM, 0.01)
        assertTrue(plan.notices.any { it.startsWith("No reported zones along this route") })
    }

    @Test
    fun warnsWhenTheStartIsInsideAZoneAndDoesNotExcludeIt() = runBlocking {
        val atStart = zone.copy(center = origin)
        val transport = valhallaOnly(valhallaOk(direct))

        val plan = SafeRoutePlanner(transport).plan(origin, destination, listOf(atStart)).plan()

        assertEquals(1, transport.requests.size)
        assertTrue("Your start point is inside a reported zone" in plan.notices)
        assertFalse("No route avoids every reported zone" in plan.notices)
    }

    @Test
    fun dropsNearDuplicatesAndOverlongRoutes() = runBlocking {
        val wiggle = direct.mapIndexed { i, p -> if (i % 4 == 2) geo(p.latitude + 0.00005, p.longitude) else p }
        val overlong = path(origin, geo(28.61, 77.2), geo(28.61, 77.21), destination)
        val transport = valhallaOnly(valhallaOk(direct, wiggle, overlong), valhallaError(442))

        val plan = SafeRoutePlanner(transport).plan(origin, destination, listOf(zone)).plan()

        assertEquals(1, plan.options.size)
        assertEquals(direct.size, plan.options.single().points.size)
    }

    @Test
    fun fallsBackToOsrmWithDetoursWhenValhallaIsDown() = runBlocking {
        val transport = FakeTransport { request ->
            when {
                request.method == "POST" -> HttpResponseData(503, "<html>Service Unavailable</html>")
                else -> osrmCoordinates(request.url).let { coords ->
                    if (coords.size == 2) osrmOk(direct) else osrmOk(path(origin, coords[1], destination))
                }
            }
        }

        val plan = SafeRoutePlanner(transport).plan(origin, destination, listOf(zone)).plan()

        assertEquals("OSRM", plan.providerName)
        assertEquals("Routing © OpenStreetMap contributors (ODbL) · OSRM / FOSSGIS", plan.attribution)
        val gets = transport.requests.filter { it.method == "GET" }
        assertTrue(gets[0].url.endsWith("&alternatives=true"))
        // Both 60 m detours clear the zone, so the 150 m ones are never requested.
        assertEquals(3, gets.size)
        gets.drop(1).forEach { request ->
            assertFalse("alternatives" in request.url)
            val via = osrmCoordinates(request.url)[1]
            assertEquals(160.0, RouteScorer.distanceMeters(via, zone.center), 1.0)
        }
        assertEquals(RouteRole.FEWER_REPORTS, plan.options[0].role)
        assertEquals(0, plan.options[0].zonesCrossed)
        assertEquals(RouteRole.SHORTEST, plan.options[1].role)
    }

    @Test
    fun skipsDetourViasInsideOtherZones() = runBlocking {
        val north = AvoidZone(RouteScorer.destinationPoint(zone.center, 0.0, 160.0), 50.0, 1.0, "north")
        val transport = FakeTransport { request ->
            if (request.method == "POST") offline() else osrmOk(direct)
        }

        SafeRoutePlanner(transport).plan(origin, destination, listOf(zone, north)).plan()

        val vias = transport.requests.drop(2).map { osrmCoordinates(it.url)[1] }
        assertTrue(vias.isNotEmpty())
        assertTrue(vias.none { RouteScorer.distanceMeters(it, north.center) <= north.radiusM })
    }

    @Test
    fun failsWithAReadableMessageWhenEveryProviderIsDown() = runBlocking {
        val transport = FakeTransport { request ->
            if (request.method == "POST") offline() else HttpResponseData(503, "<html>down</html>")
        }

        val result = SafeRoutePlanner(transport).plan(origin, destination, listOf(zone))

        assertTrue(result is RoutingResult.Failure)
        assertEquals(SafeRoutePlanner.OFFLINE_MESSAGE, (result as RoutingResult.Failure).message)
        assertTrue(result.cause is RouterException)
    }

    @Test
    fun reportsNoRouteWhenProvidersFindNoPath() = runBlocking {
        val transport = FakeTransport { request ->
            if (request.method == "POST") valhallaError(442) else HttpResponseData(400, """{"code":"NoRoute"}""")
        }

        val result = SafeRoutePlanner(transport).plan(origin, destination, emptyList()) as RoutingResult.Failure

        assertEquals(SafeRoutePlanner.NO_ROUTE_MESSAGE, result.message)
        assertNull(result.cause)
    }
}

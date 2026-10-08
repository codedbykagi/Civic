package com.civic.app.safety.routing

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ValhallaRouterTest {
    private val url = "https://valhalla.test/route"
    private val origin = geo(28.6, 77.2)
    private val destination = geo(28.6, 77.21)
    private val direct = path(origin, destination)
    private val detour = path(origin, geo(28.6015, 77.203), geo(28.6015, 77.207), destination)

    @Test
    fun parsesMainTripAndAlternates() = runBlocking {
        val router = ValhallaRouter(FakeTransport { valhallaOk(direct, detour) }, url)

        val routes = router.route(origin, destination)

        assertEquals(2, routes.size)
        assertEquals(RouteScorer.lengthMeters(direct), routes[0].distanceM, 0.01)
        assertEquals(RouteScorer.lengthMeters(direct) / 1.3, routes[0].durationS, 0.01)
        assertEquals(direct.size, routes[0].points.size)
        direct.zip(routes[0].points).forEach { (want, got) ->
            assertEquals(want.latitude, got.latitude, 1e-6)
            assertEquals(want.longitude, got.longitude, 1e-6)
        }
        assertEquals(RouteScorer.lengthMeters(detour), routes[1].distanceM, 0.01)
    }

    @Test
    fun concatenatesLegsWithoutRepeatingTheJoint() = runBlocking {
        val first = path(origin, geo(28.6, 77.205))
        val second = path(geo(28.6, 77.205), destination)
        val router = ValhallaRouter(FakeTransport { HttpResponseData(200, valhallaBody(listOf(first, second))) }, url)

        val points = router.route(origin, destination).single().points

        assertEquals(first.size + second.size - 1, points.size)
        assertEquals(points.size, points.distinct().size)
    }

    @Test
    fun noPathIsAnEmptyListNotAnError() = runBlocking {
        val router = ValhallaRouter(FakeTransport { valhallaError(442) }, url)
        assertTrue(router.route(origin, destination).isEmpty())
    }

    @Test
    fun otherErrorsThrowWithTheirStatus() = runBlocking {
        val cases =
            listOf(valhallaError(171), valhallaError(0, status = 503), HttpResponseData(429, "Too Many Requests"))
        for (response in cases) {
            try {
                ValhallaRouter(FakeTransport { response }, url).route(origin, destination)
                fail("Expected RouterException for HTTP ${response.status}")
            } catch (e: RouterException) {
                assertEquals(response.status, e.httpStatus)
            }
        }
    }

    @Test
    fun malformedSuccessBodyThrows() = runBlocking {
        for (body in listOf("<html>busy</html>", "{}", """{"trip":{"legs":[]}}""")) {
            try {
                ValhallaRouter(FakeTransport { HttpResponseData(200, body) }, url).route(origin, destination)
                fail("Expected RouterException for $body")
            } catch (e: RouterException) {
                assertEquals(null, e.httpStatus)
            }
        }
    }

    @Test
    fun requestBodyMatchesTheValhallaApi() = runBlocking {
        val transport = FakeTransport { valhallaOk(direct) }
        val router = ValhallaRouter(transport, url)
        router.route(origin, destination)
        router.route(origin, destination, listOf(ExcludePolygons.octagon(geo(28.6, 77.205), 100.0)))

        val plain = Json.parseToJsonElement(transport.requests[0].body!!).jsonObject
        assertEquals("POST", transport.requests[0].method)
        assertEquals(url, transport.requests[0].url)
        assertEquals("pedestrian", plain.getValue("costing").jsonPrimitive.content)
        assertEquals(2, plain.getValue("alternates").jsonPrimitive.int)
        assertEquals("none", plain.getValue("directions_type").jsonPrimitive.content)
        val locations = plain.getValue("locations").jsonArray
        assertEquals("28.6", locations[0].jsonObject.getValue("lat").jsonPrimitive.content)
        assertEquals("77.21", locations[1].jsonObject.getValue("lon").jsonPrimitive.content)
        assertFalse("exclude_polygons" in plain)

        val excluded = Json.parseToJsonElement(transport.requests[1].body!!).jsonObject
        val ring = excluded.getValue("exclude_polygons").jsonArray.single().jsonArray
        assertEquals(9, ring.size)
        assertEquals(ring.first(), ring.last())
        assertTrue(ring[0].jsonArray[0].jsonPrimitive.content.startsWith("77.2"))
    }
}

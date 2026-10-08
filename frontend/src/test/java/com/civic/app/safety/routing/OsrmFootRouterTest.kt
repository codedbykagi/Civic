package com.civic.app.safety.routing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OsrmFootRouterTest {
    private val base = "https://osrm.test/routed-foot/route/v1/foot"
    private val origin = geo(28.6, 77.2)
    private val destination = geo(28.6, 77.21)
    private val direct = path(origin, destination)

    @Test
    fun buildsFootUrlsWithAlternativesOnlyWithoutVia() = runBlocking {
        val transport = FakeTransport { osrmOk(direct) }
        val router = OsrmFootRouter(transport, "$base/")
        router.route(origin, destination)
        router.route(origin, destination, via = geo(28.601234567, 77.205))

        assertEquals(
            "$base/77.200000,28.600000;77.210000,28.600000?overview=full&geometries=geojson&alternatives=true",
            transport.requests[0].url,
        )
        val viaUrl = transport.requests[1].url
        assertEquals(
            "$base/77.200000,28.600000;77.205000,28.601235;77.210000,28.600000?overview=full&geometries=geojson",
            viaUrl,
        )
        assertFalse("alternatives" in viaUrl)
        assertTrue(transport.requests.all { it.method == "GET" })
    }

    @Test
    fun parsesGeoJsonLonLatGeometry() = runBlocking {
        val detour = path(origin, geo(28.602, 77.205), destination)
        val routes = OsrmFootRouter(FakeTransport { osrmOk(direct, detour) }, base).route(origin, destination)

        assertEquals(2, routes.size)
        assertEquals(28.6, routes[0].points.first().latitude, 1e-9)
        assertEquals(77.2, routes[0].points.first().longitude, 1e-9)
        assertEquals(RouteScorer.lengthMeters(detour), routes[1].distanceM, 1e-6)
        assertEquals(detour.size, routes[1].points.size)
    }

    @Test
    fun noRouteIsAnEmptyList() = runBlocking {
        val noRoute = HttpResponseData(400, """{"code":"NoRoute","message":"Impossible route between points"}""")
        assertTrue(OsrmFootRouter(FakeTransport { noRoute }, base).route(origin, destination).isEmpty())
    }

    @Test
    fun httpAndBodyErrorsThrow() = runBlocking {
        val cases = listOf(
            HttpResponseData(429, "<html>Too many requests</html>"),
            HttpResponseData(400, """{"code":"InvalidUrl","message":"bad"}"""),
            HttpResponseData(200, """{"code":"Ok"}"""),
        )
        for (response in cases) {
            try {
                OsrmFootRouter(FakeTransport { response }, base).route(origin, destination)
                fail("Expected RouterException for $response")
            } catch (e: RouterException) {
                // expected
            }
        }
    }
}

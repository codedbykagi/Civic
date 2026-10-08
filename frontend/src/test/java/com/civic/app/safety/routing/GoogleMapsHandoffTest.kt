package com.civic.app.safety.routing

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class GoogleMapsHandoffTest {
    private val origin = geo(28.6, 77.2)
    private val destination = geo(28.61, 77.23)
    private val base = "https://www.google.com/maps/dir/?api=1&destination=28.610000,77.230000" +
        "&travelmode=walking&dir_action=navigate"
    private lateinit var savedLocale: Locale

    @Before
    fun useCommaDecimalLocale() {
        savedLocale = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
    }

    @After
    fun restoreLocale() = Locale.setDefault(savedLocale)

    private fun waypointsOf(url: String) =
        url.substringAfter("&waypoints=", "").takeIf { it.isNotEmpty() }?.split("%7C")?.map {
            val (lat, lng) = it.split(',').map(String::toDouble)
            geo(lat, lng)
        }.orEmpty()

    @Test
    fun withoutRouteItIsAPlainWalkingNavigationLink() {
        assertEquals(base, GoogleMapsHandoff.walkingNavigationUrl(destination, null))
    }

    @Test
    fun neverSendsAnOrigin() {
        val url = GoogleMapsHandoff.walkingNavigationUrl(destination, path(origin, geo(28.61, 77.2), destination))
        assertFalse("origin=" in url)
        assertTrue(url.startsWith("$base&waypoints="))
    }

    @Test
    fun pinsTheDetourWithUpToThreeEncodedWaypointsInOrder() {
        val route =
            path(origin, geo(28.6, 77.21), geo(28.605, 77.21), geo(28.605, 77.22), geo(28.61, 77.22), destination)

        val url = GoogleMapsHandoff.walkingNavigationUrl(destination, route)
        val waypoints = waypointsOf(url)

        assertEquals(3, waypoints.size)
        assertFalse("|" in url)
        val coordinate = "\\d+\\.\\d{6},\\d+\\.\\d{6}"
        assertTrue(Regex("waypoints=$coordinate(%7C$coordinate){2}$").containsMatchIn(url))
        val along = waypoints.map { w -> route.indexOfFirst { RouteScorer.distanceMeters(it, w) < 1.0 } }
        assertTrue("waypoints must lie on the route: $along", along.all { it >= 0 })
        assertEquals(along.sorted(), along)
        waypoints.forEach {
            assertTrue(RouteScorer.distanceMeters(it, origin) >= 100.0)
            assertTrue(RouteScorer.distanceMeters(it, destination) >= 100.0)
        }
    }

    @Test
    fun straightRoutesGetEvenlySpacedWaypoints() {
        val route = path(origin, geo(28.6, 77.23))
        val waypoints = waypointsOf(GoogleMapsHandoff.walkingNavigationUrl(geo(28.6, 77.23), route))

        assertEquals(3, waypoints.size)
        val length = RouteScorer.lengthMeters(route)
        waypoints.forEachIndexed { i, w ->
            assertEquals(length * (i + 1) / 4, RouteScorer.distanceMeters(origin, w), 1.0)
        }
    }

    @Test
    fun respectsWaypointLimitsAndUrlLength() {
        val zigzag = path(*Array(40) { i -> geo(28.6 + 0.004 * (i % 2), 77.2 + 0.004 * i) })
        val end = zigzag.last()

        assertEquals(1, waypointsOf(GoogleMapsHandoff.walkingNavigationUrl(end, zigzag, maxWaypoints = 1)).size)
        assertFalse("waypoints" in GoogleMapsHandoff.walkingNavigationUrl(end, zigzag, maxWaypoints = 0))
        val many = GoogleMapsHandoff.walkingNavigationUrl(end, zigzag, maxWaypoints = 50)
        assertEquals(GoogleMapsHandoff.MAX_WAYPOINTS, waypointsOf(many).size)
        assertTrue(many.length < GoogleMapsHandoff.MAX_URL_LENGTH)
    }

    @Test
    fun shortRoutesHaveNoWaypoints() {
        val near = geo(28.6, 77.2015)
        val url = GoogleMapsHandoff.walkingNavigationUrl(near, path(origin, near))
        assertEquals(base.replace("28.610000,77.230000", "28.600000,77.201500"), url)
    }
}

package com.civic.app.safety.routing

import com.civic.shared.model.GeoLocation
import java.util.Locale
import java.util.TreeSet

/**
 * Hands a route to the Google Maps app for walking turn-by-turn via a keyless Maps URL. Google's route data may not
 * be drawn on our OSM map, so we only pass our route as a few waypoints; Google may still recalculate between them,
 * which the UI should say.
 */
object GoogleMapsHandoff {
    const val MAX_URL_LENGTH = 2048

    /** Maps URLs accept up to 9 waypoints (only 3 in mobile browsers). */
    const val MAX_WAYPOINTS = 9

    /** Waypoints this close to the start or end add nothing and can confuse the turn-by-turn start. */
    const val END_CLEARANCE_M = 100.0

    private const val MIN_DEVIATION_M = 15.0
    private const val MIN_SPACING_M = 150.0

    /**
     * `https://www.google.com/maps/dir/?api=1&destination=…&travelmode=walking&dir_action=navigate[&waypoints=…]`.
     * There is deliberately no origin: navigation only starts by itself from the current location.
     */
    fun walkingNavigationUrl(destination: GeoLocation, route: List<GeoLocation>?, maxWaypoints: Int = 3): String {
        val base = "https://www.google.com/maps/dir/?api=1&destination=${format(destination)}" +
            "&travelmode=walking&dir_action=navigate"
        var waypoints = route?.let { pickWaypoints(it, destination, maxWaypoints.coerceIn(0, MAX_WAYPOINTS)) }.orEmpty()
        while (true) {
            val url = when {
                waypoints.isEmpty() -> base
                else -> waypoints.joinToString(separator = "%7C", prefix = "$base&waypoints=", transform = ::format)
            }
            if (url.length < MAX_URL_LENGTH || waypoints.isEmpty()) return url
            waypoints = waypoints.dropLast(1)
        }
    }

    /**
     * The route's turning points that deviate most from a straight walk (Douglas–Peucker order), which pin Google
     * to our path best; topped up with evenly spaced points on straight routes. Returned in walking order.
     */
    internal fun pickWaypoints(route: List<GeoLocation>, destination: GeoLocation, count: Int): List<GeoLocation> {
        if (count <= 0 || route.size < 2) return emptyList()
        val along = DoubleArray(route.size)
        for (i in 1 until route.size) along[i] = along[i - 1] + RouteScorer.distanceMeters(route[i - 1], route[i])
        val total = along.last()
        val ends = listOf(route.first(), route.last(), destination)
        fun eligible(point: GeoLocation, at: Double) = at >= END_CLEARANCE_M && total - at >= END_CLEARANCE_M &&
            ends.all { RouteScorer.distanceMeters(it, point) >= END_CLEARANCE_M }

        val anchors = TreeSet(listOf(0, route.lastIndex))
        val chosen = mutableListOf<Pair<Double, GeoLocation>>()
        while (chosen.size < count) {
            var best = -1
            var bestDeviation = MIN_DEVIATION_M
            for (i in 1 until route.lastIndex) {
                if (i in anchors || !eligible(route[i], along[i])) continue
                val deviation =
                    RouteScorer.distanceToSegment(route[i], route[anchors.lower(i)!!], route[anchors.higher(i)!!])
                if (deviation > bestDeviation) {
                    best = i
                    bestDeviation = deviation
                }
            }
            if (best < 0) break
            anchors += best
            chosen += along[best] to route[best]
        }
        for (k in 1..count) {
            if (chosen.size >= count) break
            val at = total * k / (count + 1)
            val point = RouteScorer.pointAlong(route, at)
            if (eligible(point, at) && chosen.none { RouteScorer.distanceMeters(it.second, point) < MIN_SPACING_M }) {
                chosen += at to point
            }
        }
        return chosen.sortedBy { it.first }.map { it.second }
    }

    private fun format(point: GeoLocation) = String.format(Locale.ROOT, "%.6f,%.6f", point.latitude, point.longitude)
}

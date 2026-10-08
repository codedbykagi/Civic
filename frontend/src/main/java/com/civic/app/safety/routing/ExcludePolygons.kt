package com.civic.app.safety.routing

import com.civic.shared.model.GeoLocation
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong

/** Zones chosen for one Valhalla request, plus the ones left out and why. */
data class ExcludeSelection(
    /** Closed octagons (first point repeated last), one per entry of [excluded]. */
    val rings: List<List<GeoLocation>>,
    val excluded: List<AvoidZone>,
    /** Zones around the start or destination: excluding them makes Valhalla fail (error 442), so warn instead. */
    val containsEndpoint: List<AvoidZone>,
    /** Zones on or near the routes that did not fit the server's vertex/perimeter budget. */
    val overBudget: List<AvoidZone>,
) {
    val vertexCount: Int get() = rings.sumOf { it.size }
    val perimeterM: Double get() = rings.sumOf { RouteScorer.lengthMeters(it) }
}

/**
 * Builds Valhalla `exclude_polygons` within the public server's hard limits (100 vertices: error 176; 10 km total
 * perimeter: error 167). Each zone becomes an octagon drawn around the outside of its circle, so the circle is
 * fully covered with 9 coordinates instead of osmdroid's 60.
 */
object ExcludePolygons {
    const val MAX_VERTICES = 100
    const val MAX_PERIMETER_M = 10_000.0

    /** 1 % headroom in case the server measures the ring slightly differently from our haversine. */
    const val PERIMETER_BUDGET_M = MAX_PERIMETER_M * 0.99
    const val SIDES = 8

    /** Zones this close to a candidate route (beyond their edge) may be excluded after the ones it crosses. */
    const val CORRIDOR_M = 300.0

    private val VERTEX_FACTOR = 1.0 / cos(PI / SIDES)

    /** Closed octagon circumscribing the circle: vertex radius r / cos(π/8), first vertex repeated last. */
    fun octagon(center: GeoLocation, radiusM: Double): List<GeoLocation> {
        val vertexRadius = radiusM * VERTEX_FACTOR
        val ring = (0 until SIDES).map { RouteScorer.destinationPoint(center, it * 360.0 / SIDES, vertexRadius) }
        return ring + ring.first()
    }

    /**
     * Picks zones by descending relevance × metres of the [candidates] inside them, then zones within
     * [CORRIDOR_M] of a candidate by relevance (so the new route is not pushed into a neighbouring zone), while
     * the budget allows. Zones whose octagon plus the GPS margin covers [origin] or [destination] are never chosen.
     */
    fun select(
        zones: List<AvoidZone>,
        candidates: List<List<GeoLocation>>,
        origin: GeoLocation,
        destination: GeoLocation,
        maxVertices: Int = MAX_VERTICES,
        maxPerimeterM: Double = PERIMETER_BUDGET_M,
    ): ExcludeSelection {
        val (atEndpoint, usable) = zones
            .filter { it.radiusM > 0 && it.relevance > 0 }
            .partition { touches(it, origin) || touches(it, destination) }
        val ranked = usable
            .map { zone -> zone to zone.relevance * candidates.sumOf { RouteScorer.metersInside(it, zone) } }
            .filter { (zone, priority) -> priority > 0 || candidates.any { nearCorridor(it, zone) } }
            .sortedWith(compareByDescending<Pair<AvoidZone, Double>> { it.second }
                .thenByDescending { it.first.relevance })
            .map { it.first }

        val rings = mutableListOf<List<GeoLocation>>()
        val excluded = mutableListOf<AvoidZone>()
        val overBudget = mutableListOf<AvoidZone>()
        var vertices = 0
        var perimeter = 0.0
        for (zone in ranked) {
            val ring = octagon(zone.center, zone.radiusM)
            val ringPerimeter = RouteScorer.lengthMeters(ring)
            if (vertices + ring.size <= maxVertices && perimeter + ringPerimeter <= maxPerimeterM) {
                rings += ring
                excluded += zone
                vertices += ring.size
                perimeter += ringPerimeter
            } else {
                overBudget += zone
            }
        }
        return ExcludeSelection(rings, excluded, atEndpoint, overBudget)
    }

    /** `[[[lon, lat], ...], ...]` with 6 decimals, as Valhalla expects. */
    fun toJson(rings: List<List<GeoLocation>>): JsonArray = buildJsonArray {
        for (ring in rings) {
            addJsonArray {
                for (point in ring) {
                    addJsonArray {
                        add(round6(point.longitude))
                        add(round6(point.latitude))
                    }
                }
            }
        }
    }

    private fun touches(zone: AvoidZone, point: GeoLocation) =
        RouteScorer.distanceMeters(zone.center, point) <= zone.radiusM * VERTEX_FACTOR + RouteScorer.GPS_MARGIN_M

    private fun nearCorridor(route: List<GeoLocation>, zone: AvoidZone) =
        route.isNotEmpty() && RouteScorer.distanceToPolyline(zone.center, route) <= zone.radiusM + CORRIDOR_M

    private fun round6(value: Double) = (value * 1e6).roundToLong() / 1e6
}

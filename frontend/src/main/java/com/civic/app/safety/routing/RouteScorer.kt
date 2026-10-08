package com.civic.app.safety.routing

import com.civic.shared.model.GeoLocation
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Scores routes by how much of the walk is spent near reported zones. Scoring happens on the phone for every
 * candidate because providers can only hard-exclude a few zones (or none, for OSRM), so the ranking is ours.
 * A zone counts fully within 0.6 × radius and fades to zero 30 m beyond its edge to allow for GPS error.
 */
object RouteScorer {
    const val EARTH_RADIUS_M = 6_371_008.8
    const val SAMPLE_STEP_M = 10.0
    const val GPS_MARGIN_M = 30.0
    const val CORE_FRACTION = 0.6
    const val OVERLAP_STEP_M = 25.0

    private const val DEG = PI / 180.0
    private const val METERS_PER_DEG_LAT = EARTH_RADIUS_M * DEG

    fun distanceMeters(a: GeoLocation, b: GeoLocation): Double {
        val dLat = (b.latitude - a.latitude) * DEG
        val dLng = (b.longitude - a.longitude) * DEG
        val h = sin(dLat / 2).let { it * it } +
            cos(a.latitude * DEG) * cos(b.latitude * DEG) * sin(dLng / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(h)))
    }

    fun lengthMeters(points: List<GeoLocation>): Double =
        (1 until points.size).sumOf { distanceMeters(points[it - 1], points[it]) }

    /** 1.0 in the zone's core, falling linearly to 0 at [radiusM] + [GPS_MARGIN_M]. */
    fun softWeight(distanceM: Double, radiusM: Double): Double {
        val core = CORE_FRACTION * radiusM
        val outer = radiusM + GPS_MARGIN_M
        return when {
            distanceM <= core -> 1.0
            distanceM >= outer -> 0.0
            else -> (outer - distanceM) / (outer - core)
        }
    }

    /** Σ relevance × softWeight × metres, sampled every [SAMPLE_STEP_M] along the route. */
    fun exposure(points: List<GeoLocation>, zones: List<AvoidZone>): Double =
        zonePasses(points, zones).sumOf { it.exposure }

    /** Zones (with positive relevance) the route comes within their radius of. */
    fun zonesCrossed(points: List<GeoLocation>, zones: List<AvoidZone>): Int =
        zones.count { it.relevance > 0 && crosses(points, it) }

    fun crosses(points: List<GeoLocation>, zone: AvoidZone): Boolean =
        points.isNotEmpty() && distanceToPolyline(zone.center, points) <= zone.radiusM

    /** Metres of the route strictly inside the zone's circle (no soft edge). */
    fun metersInside(points: List<GeoLocation>, zone: AvoidZone): Double {
        var inside = 0.0
        forEachSample(points) { p, len -> if (distanceMeters(p, zone.center) <= zone.radiusM) inside += len }
        return inside
    }

    /** Fraction of [a]'s 25 m-spaced samples lying within 25 m of [b]; used to drop near-duplicate routes. */
    fun overlapFraction(a: List<GeoLocation>, b: List<GeoLocation>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val samples = resample(a, OVERLAP_STEP_M)
        return samples.count { distanceToPolyline(it, b) <= OVERLAP_STEP_M }.toDouble() / samples.size
    }

    fun score(candidate: RouteCandidate, zones: List<AvoidZone>, lambda: Double, role: RouteRole): RouteOption {
        val distance = candidate.distanceM.takeIf { it > 0 && it.isFinite() } ?: lengthMeters(candidate.points)
        val exposure = exposure(candidate.points, zones)
        return RouteOption(
            points = candidate.points,
            distanceM = distance,
            durationS = candidate.durationS,
            exposure = exposure,
            zonesCrossed = zonesCrossed(candidate.points, zones),
            cost = distance + lambda * exposure,
            role = role,
        )
    }

    /** How a route passes one zone: its exposure there and the first/last sampled points within the soft edge. */
    internal class ZonePass(val zone: AvoidZone, val exposure: Double, val entry: GeoLocation, val exit: GeoLocation)

    internal fun zonePasses(points: List<GeoLocation>, zones: List<AvoidZone>): List<ZonePass> {
        val near = zonesNear(points, zones)
        if (near.isEmpty()) return emptyList()
        val exposure = DoubleArray(near.size)
        val entry = arrayOfNulls<GeoLocation>(near.size)
        val exit = arrayOfNulls<GeoLocation>(near.size)
        forEachSample(points) { p, len ->
            for (k in near.indices) {
                val zone = near[k]
                val weight = softWeight(distanceMeters(p, zone.center), zone.radiusM)
                if (weight > 0) {
                    exposure[k] += zone.relevance * weight * len
                    if (entry[k] == null) entry[k] = p
                    exit[k] = p
                }
            }
        }
        return near.indices.filter { exposure[it] > 0 }
            .map { ZonePass(near[it], exposure[it], entry[it]!!, exit[it]!!) }
    }

    /** Positive-relevance zones whose soft edge can reach the route's bounding box. */
    private fun zonesNear(points: List<GeoLocation>, zones: List<AvoidZone>): List<AvoidZone> {
        if (points.isEmpty()) return emptyList()
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLng = points.minOf { it.longitude }
        val maxLng = points.maxOf { it.longitude }
        val lngScale = max(cos(max(abs(minLat), abs(maxLat)) * DEG), 0.01)
        return zones.filter { zone ->
            val reachLat = (zone.radiusM + GPS_MARGIN_M) / METERS_PER_DEG_LAT
            val reachLng = reachLat / lngScale
            zone.relevance > 0 && zone.radiusM > 0 &&
                zone.center.latitude in (minLat - reachLat)..(maxLat + reachLat) &&
                zone.center.longitude in (minLng - reachLng)..(maxLng + reachLng)
        }
    }

    /** Calls [block] with the midpoint and length of every ≈[stepM] piece of the route. */
    private inline fun forEachSample(
        points: List<GeoLocation>,
        stepM: Double = SAMPLE_STEP_M,
        block: (GeoLocation, Double) -> Unit,
    ) {
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val length = distanceMeters(a, b)
            if (length <= 0.0) continue
            val pieces = max(1, ceil(length / stepM).toInt())
            val pieceLength = length / pieces
            for (j in 0 until pieces) block(interpolate(a, b, (j + 0.5) / pieces), pieceLength)
        }
    }

    /** Points every [stepM] metres along the route, always including both ends. */
    internal fun resample(points: List<GeoLocation>, stepM: Double): List<GeoLocation> {
        if (points.size < 2) return points
        val out = mutableListOf(points.first())
        var carried = 0.0
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val length = distanceMeters(a, b)
            var next = stepM - carried
            while (next <= length) {
                out += interpolate(a, b, next / length)
                next += stepM
            }
            carried = length - (next - stepM)
        }
        if (distanceMeters(out.last(), points.last()) > 0.5) out += points.last()
        return out
    }

    /** The point [distanceM] metres along the route, clamped to its ends. */
    internal fun pointAlong(points: List<GeoLocation>, distanceM: Double): GeoLocation {
        var walked = 0.0
        for (i in 1 until points.size) {
            val length = distanceMeters(points[i - 1], points[i])
            if (walked + length >= distanceM && length > 0) {
                return interpolate(points[i - 1], points[i], ((distanceM - walked) / length).coerceIn(0.0, 1.0))
            }
            walked += length
        }
        return points.last()
    }

    internal fun distanceToPolyline(p: GeoLocation, line: List<GeoLocation>): Double {
        if (line.size == 1) return distanceMeters(p, line[0])
        var best = Double.MAX_VALUE
        for (i in 1 until line.size) best = min(best, distanceToSegment(p, line[i - 1], line[i]))
        return best
    }

    /** Planar distance in a local equirectangular frame centred on [p]; accurate to well under 1 % below ~10 km. */
    internal fun distanceToSegment(p: GeoLocation, a: GeoLocation, b: GeoLocation): Double {
        val kx = cos(p.latitude * DEG) * METERS_PER_DEG_LAT
        val ax = (a.longitude - p.longitude) * kx
        val ay = (a.latitude - p.latitude) * METERS_PER_DEG_LAT
        val dx = (b.longitude - p.longitude) * kx - ax
        val dy = (b.latitude - p.latitude) * METERS_PER_DEG_LAT - ay
        val lengthSq = dx * dx + dy * dy
        val t = if (lengthSq == 0.0) 0.0 else (-(ax * dx + ay * dy) / lengthSq).coerceIn(0.0, 1.0)
        return hypot(ax + t * dx, ay + t * dy)
    }

    internal fun bearingDegrees(from: GeoLocation, to: GeoLocation): Double {
        val lat1 = from.latitude * DEG
        val lat2 = to.latitude * DEG
        val dLng = (to.longitude - from.longitude) * DEG
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        return (atan2(y, x) / DEG + 360.0) % 360.0
    }

    internal fun destinationPoint(from: GeoLocation, bearingDeg: Double, distanceM: Double): GeoLocation {
        val angular = distanceM / EARTH_RADIUS_M
        val bearing = bearingDeg * DEG
        val lat1 = from.latitude * DEG
        val lng1 = from.longitude * DEG
        val lat2 = asin(sin(lat1) * cos(angular) + cos(lat1) * sin(angular) * cos(bearing))
        val lng2 = lng1 + atan2(sin(bearing) * sin(angular) * cos(lat1), cos(angular) - sin(lat1) * sin(lat2))
        return GeoLocation(latitude = lat2 / DEG, longitude = ((lng2 / DEG + 540.0) % 360.0) - 180.0)
    }

    private fun interpolate(a: GeoLocation, b: GeoLocation, t: Double) = GeoLocation(
        latitude = a.latitude + (b.latitude - a.latitude) * t,
        longitude = a.longitude + (b.longitude - a.longitude) * t,
    )
}

package com.civic.app.safety.routing

import com.civic.shared.model.GeoLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Plans walking routes that keep away from user-reported zones ("fewer unsafe reports", never "safe": no reports
 * only means no data). Valhalla is asked first, then once more with the worst crossed zones hard-excluded; if it is
 * unreachable, OSRM foot plus via-point detours stands in. Every candidate is then re-scored on the phone with
 * [RouteScorer], because providers can only exclude a handful of zones and know nothing of their relevance.
 */
class SafeRoutePlanner(
    private val transport: HttpTransport,
    private val config: RoutingConfig = RoutingConfig(),
) {
    private val valhalla = ValhallaRouter(transport, config.valhallaUrl)
    private val osrm = OsrmFootRouter(transport, config.osrmFootBaseUrl)

    suspend fun plan(origin: GeoLocation, destination: GeoLocation, zones: List<AvoidZone>): RoutingResult {
        val active = zones.filter { it.relevance > 0 && it.radiusM > 0 }
        val (endpointZones, avoidable) = active.partition { isAt(it, origin) || isAt(it, destination) }
        val hard = avoidable.filter { it.relevance >= config.hardAvoidMinRelevance }

        var error: Throwable? = null
        val valhallaRoutes = attempt { fromValhalla(origin, destination, hard) }.onFailure { error = it }.getOrNull()
        if (!valhallaRoutes.isNullOrEmpty()) {
            return success(valhallaRoutes, Provider.VALHALLA, origin, destination, active, endpointZones)
        }
        val osrmRoutes = attempt { fromOsrm(origin, destination, active, hard) }.onFailure { error = it }.getOrNull()
        return when {
            !osrmRoutes.isNullOrEmpty() ->
                success(osrmRoutes, Provider.OSRM, origin, destination, active, endpointZones)
            // At least one provider answered "no path"; the other may simply have been unreachable.
            valhallaRoutes != null || osrmRoutes != null -> RoutingResult.Failure(NO_ROUTE_MESSAGE, error)
            else -> RoutingResult.Failure(OFFLINE_MESSAGE, error)
        }
    }

    private suspend fun success(
        candidates: List<RouteCandidate>,
        provider: Provider,
        origin: GeoLocation,
        destination: GeoLocation,
        active: List<AvoidZone>,
        endpointZones: List<AvoidZone>,
    ): RoutingResult = withContext(Dispatchers.Default) {
        RoutingResult.Success(buildPlan(candidates, provider, origin, destination, active, endpointZones))
    }

    /** Plain request, then one request with exclusions if a candidate crosses a zone worth avoiding outright. */
    private suspend fun fromValhalla(
        origin: GeoLocation,
        destination: GeoLocation,
        hard: List<AvoidZone>,
    ): List<RouteCandidate> {
        val plain = valhalla.route(origin, destination)
        if (plain.isEmpty() || hard.none { zone -> plain.any { RouteScorer.crosses(it.points, zone) } }) return plain
        val selection = withContext(Dispatchers.Default) {
            ExcludePolygons.select(hard, plain.map { it.points }, origin, destination)
        }
        if (selection.rings.isEmpty()) return plain
        // A failure or "no path" here only means there are no extra candidates.
        return plain + attempt { valhalla.route(origin, destination, selection.rings) }.getOrDefault(emptyList())
    }

    /** OSRM cannot exclude areas, so detour around the worst zone on the best route via points beside it. */
    private suspend fun fromOsrm(
        origin: GeoLocation,
        destination: GeoLocation,
        active: List<AvoidZone>,
        hard: List<AvoidZone>,
    ): List<RouteCandidate> {
        val base = osrm.route(origin, destination)
        if (base.isEmpty()) return base
        val pass = withContext(Dispatchers.Default) {
            val best = base.minBy { RouteScorer.score(it, active, config.lambda, RouteRole.ALTERNATIVE).cost }
            RouteScorer.zonePasses(best.points, hard).maxByOrNull { it.exposure }
        } ?: return base
        val zone = pass.zone
        val heading = if (RouteScorer.distanceMeters(pass.entry, pass.exit) >= 5.0) {
            RouteScorer.bearingDegrees(pass.entry, pass.exit)
        } else {
            RouteScorer.bearingDegrees(origin, destination)
        }
        val vias = DETOUR_MARGINS_M.flatMap { margin ->
            listOf(90.0, -90.0).map { side ->
                side to RouteScorer.destinationPoint(zone.center, heading + side, zone.radiusM + margin)
            }
        }.filter { (_, via) -> active.none { it !== zone && RouteScorer.distanceMeters(via, it.center) <= it.radiusM } }

        val detours = mutableListOf<RouteCandidate>()
        val clearedSides = mutableSetOf<Double>()
        for ((side, via) in vias.take(MAX_DETOUR_REQUESTS)) {
            if (side in clearedSides) continue
            val routes = attempt { osrm.route(origin, destination, via) }.getOrNull() ?: break
            val usable = routes.filterNot { doublesBack(it.points, via) }
            detours += usable
            if (usable.any { !RouteScorer.crosses(it.points, zone) }) clearedSides += side
        }
        return base + detours
    }

    private fun buildPlan(
        candidates: List<RouteCandidate>,
        provider: Provider,
        origin: GeoLocation,
        destination: GeoLocation,
        active: List<AvoidZone>,
        endpointZones: List<AvoidZone>,
    ): RoutePlan {
        val unique = mutableListOf<RouteCandidate>()
        for (candidate in candidates) {
            if (unique.none { isNearDuplicate(it.points, candidate.points) }) unique += candidate
        }
        val scored = unique.map { RouteScorer.score(it, active, config.lambda, RouteRole.ALTERNATIVE) }
        val shortestDistance = scored.minOf { it.distanceM }
        val limit = min(shortestDistance * config.maxDetourFactor, shortestDistance + config.maxExtraMeters)
        val kept = scored.filter { it.distanceM <= limit || it.distanceM == shortestDistance }

        val shortest = kept.minBy { it.distanceM }
        val best = kept.minWith(compareBy<RouteOption>({ it.cost }, { it.distanceM }))
        val options = buildList {
            add(best.copy(role = RouteRole.FEWER_REPORTS))
            if (shortest !== best) add(shortest.copy(role = RouteRole.SHORTEST))
            kept.filter { it !== best && it !== shortest }
                .filterNot { it.distanceM >= best.distanceM && it.exposure >= best.exposure }
                .sortedBy { it.cost }
                .forEach { add(it.copy(role = RouteRole.ALTERNATIVE)) }
        }.take(MAX_OPTIONS)

        return RoutePlan(
            options = options,
            providerName = provider.displayName,
            attribution = provider.attribution,
            notices = notices(options, origin, destination, active, endpointZones),
        )
    }

    private fun notices(
        options: List<RouteOption>,
        origin: GeoLocation,
        destination: GeoLocation,
        active: List<AvoidZone>,
        endpointZones: List<AvoidZone>,
    ): List<String> = buildList {
        if (endpointZones.any { isAt(it, origin) }) add("Your start point is inside a reported zone")
        if (endpointZones.any { isAt(it, destination) }) add("Your destination is inside a reported zone")
        val recommended = options.first()
        val avoidable = active - endpointZones.toSet()
        if (RouteScorer.zonesCrossed(recommended.points, avoidable) > 0) add("No route avoids every reported zone")
        options.firstOrNull { it.role == RouteRole.SHORTEST }?.let { shortest ->
            val avoided = shortest.zonesCrossed - recommended.zonesCrossed
            val gain = when {
                avoided == 1 -> "avoids 1 reported zone"
                avoided > 1 -> "avoids $avoided reported zones"
                else -> "spends less of the walk near reported zones"
            }
            add("Fewer-reports route is ${extraLength(recommended.distanceM - shortest.distanceM)} and $gain")
        }
        if (options.all { it.exposure == 0.0 }) {
            add("No reported zones along this route. That only means nobody has reported here yet.")
        }
    }

    private fun isNearDuplicate(a: List<GeoLocation>, b: List<GeoLocation>) =
        RouteScorer.overlapFraction(a, b) > DUPLICATE_OVERLAP && RouteScorer.overlapFraction(b, a) > DUPLICATE_OVERLAP

    /** True when the route walks into [via] and straight back out the same way (a dead-end spur). */
    private fun doublesBack(points: List<GeoLocation>, via: GeoLocation): Boolean {
        val samples = RouteScorer.resample(points, RouteScorer.OVERLAP_STEP_M)
        val turn = samples.indices.minByOrNull { RouteScorer.distanceMeters(samples[it], via) } ?: return false
        val before = samples.subList(max(0, turn - SPUR_SAMPLES), turn + 1)
        val after = samples.subList(min(samples.size, turn + 2), min(samples.size, turn + 2 + SPUR_SAMPLES))
        if (before.size < 3 || after.size < 2) return false
        return after.count { RouteScorer.distanceToPolyline(it, before) <= SPUR_TOLERANCE_M } >= after.size * 0.6
    }

    private fun isAt(zone: AvoidZone, point: GeoLocation) =
        RouteScorer.distanceMeters(zone.center, point) <= zone.radiusM + RouteScorer.GPS_MARGIN_M

    private fun extraLength(meters: Double): String = when {
        meters < 10 -> "about the same length"
        meters < 1000 -> "+${(meters / 10).roundToInt() * 10} m"
        else -> "+" + String.format(Locale.ROOT, "%.1f", meters / 1000) + " km"
    }

    /** Runs [block], turning failures (including internal timeouts) into a result unless we were cancelled. */
    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        currentCoroutineContext().ensureActive()
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private enum class Provider(val displayName: String, val attribution: String) {
        VALHALLA("Valhalla", "Routing © OpenStreetMap contributors (ODbL) · Valhalla / FOSSGIS"),
        OSRM("OSRM", "Routing © OpenStreetMap contributors (ODbL) · OSRM / FOSSGIS"),
    }

    companion object {
        const val OFFLINE_MESSAGE = "Couldn't reach the routing service. Check your internet connection and try again."
        const val NO_ROUTE_MESSAGE = "No walking route found between these points."
        const val MAX_OPTIONS = 3

        private val DETOUR_MARGINS_M = listOf(60.0, 150.0)
        private const val MAX_DETOUR_REQUESTS = 4
        private const val DUPLICATE_OVERLAP = 0.9
        private const val SPUR_SAMPLES = 6
        private const val SPUR_TOLERANCE_M = 8.0
    }
}

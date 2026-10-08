package com.civic.app.safety.routing

import com.civic.shared.model.GeoLocation

/**
 * A circle of user reports that walking routes should keep away from. [relevance] (0..1, may exceed 1) is
 * computed by the caller as zone intensity × category weight × time-of-day match, so the routing engine stays
 * independent of how reports are aggregated.
 */
data class AvoidZone(
    val center: GeoLocation,
    val radiusM: Double,
    val relevance: Double,
    val label: String,
)

/** Why a route is offered. Wording in the UI must say "fewer unsafe reports", never "safe". */
enum class RouteRole { FEWER_REPORTS, SHORTEST, ALTERNATIVE }

/**
 * A scored walking route. [exposure] is relevance-weighted metres near reported zones, [zonesCrossed] counts zones
 * the route enters, and [cost] = distance + λ × exposure is what the planner minimises.
 */
data class RouteOption(
    val points: List<GeoLocation>,
    val distanceM: Double,
    val durationS: Double,
    val exposure: Double,
    val zonesCrossed: Int,
    val cost: Double,
    val role: RouteRole,
)

/**
 * The routes to show, recommended first. [attribution] must be displayed (ODbL / FOSSGIS terms) and [notices] are
 * short user-readable explanations of trade-offs and limits.
 */
data class RoutePlan(
    val options: List<RouteOption>,
    val providerName: String,
    val attribution: String,
    val notices: List<String>,
) {
    /** The route with the fewest reports for its length, or the only route there is. */
    val recommended: RouteOption?
        get() = options.firstOrNull { it.role == RouteRole.FEWER_REPORTS } ?: options.singleOrNull()

    /** The shortest route; the recommended one when that is also the shortest. */
    val shortest: RouteOption?
        get() = options.firstOrNull { it.role == RouteRole.SHORTEST } ?: options.minByOrNull { it.distanceM }
}

sealed interface RoutingResult {
    data class Success(val plan: RoutePlan) : RoutingResult

    /** [message] is short and user-readable; [cause] is for logs. */
    data class Failure(val message: String, val cause: Throwable? = null) : RoutingResult
}

/**
 * Tunables for [SafeRoutePlanner]. Server URLs live here rather than being hardcoded because the FOSSGIS terms ask
 * apps not to hardcode their public servers. [lambda] converts exposure to "equivalent extra metres";
 * [maxDetourFactor] and [maxExtraMeters] cap how much longer an offered route may be than the shortest one; zones
 * with relevance ≥ [hardAvoidMinRelevance] are worth extra routing requests to avoid outright.
 */
data class RoutingConfig(
    val valhallaUrl: String = "https://valhalla1.openstreetmap.de/route",
    val osrmFootBaseUrl: String = "https://routing.openstreetmap.de/routed-foot/route/v1/foot",
    val lambda: Double = 8.0,
    val maxDetourFactor: Double = 1.5,
    val maxExtraMeters: Double = 1500.0,
    val hardAvoidMinRelevance: Double = 0.3,
)

/** A provider's route before scoring. */
data class RouteCandidate(
    val points: List<GeoLocation>,
    val distanceM: Double,
    val durationS: Double,
)

/** A routing provider could not answer: HTTP error or unexpected body. [httpStatus] is null for a bad 2xx body. */
class RouterException(
    message: String,
    val httpStatus: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

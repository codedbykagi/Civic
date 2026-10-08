package com.civic.app.safety.routing

import com.civic.shared.model.GeoLocation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

/**
 * Walking routes from the FOSSGIS OSRM foot server, the fallback when Valhalla is down or rate-limited. OSRM cannot
 * avoid areas, so the planner steers it with a via point instead. Never point this at router.project-osrm.org:
 * that demo server is car-only whatever profile the URL names.
 */
class OsrmFootRouter(private val transport: HttpTransport, private val baseUrl: String) {
    /**
     * Routes from [origin] to [destination], through [via] if given. Alternatives are only requested without a via
     * point (OSRM returns just one route then anyway). An empty list means OSRM found no route (NoRoute/NoSegment).
     * Throws [RouterException] for other HTTP errors or a malformed body; I/O errors propagate.
     */
    suspend fun route(origin: GeoLocation, destination: GeoLocation, via: GeoLocation? = null): List<RouteCandidate> {
        val response = transport.get(url(origin, destination, via))
        val code = codeOf(response.body)
        if (code in NO_ROUTE_CODES) return emptyList()
        if (response.status !in 200..299 || code != "Ok") {
            throw RouterException("OSRM HTTP ${response.status} ${code.orEmpty()}".trim(), response.status)
        }
        return parse(response.body)
    }

    internal fun url(origin: GeoLocation, destination: GeoLocation, via: GeoLocation?): String {
        val coordinates = listOfNotNull(origin, via, destination).joinToString(";") {
            String.format(Locale.ROOT, "%.6f,%.6f", it.longitude, it.latitude)
        }
        val alternatives = if (via == null) "&alternatives=true" else ""
        return "${baseUrl.trimEnd('/')}/$coordinates?overview=full&geometries=geojson$alternatives"
    }

    internal fun parse(body: String): List<RouteCandidate> = try {
        Json.parseToJsonElement(body).jsonObject.getValue("routes").jsonArray.mapNotNull { element ->
            val route = element.jsonObject
            val points = route.getValue("geometry").jsonObject.getValue("coordinates").jsonArray.map { pair ->
                val lonLat = pair.jsonArray
                GeoLocation(latitude = lonLat[1].jsonPrimitive.double, longitude = lonLat[0].jsonPrimitive.double)
            }
            points.takeIf { it.size >= 2 }?.let {
                RouteCandidate(
                    points = it,
                    distanceM = route.getValue("distance").jsonPrimitive.double,
                    durationS = route.getValue("duration").jsonPrimitive.double,
                )
            }
        }
    } catch (e: RuntimeException) {
        // A wrong JSON shape surfaces as IllegalArgument (incl. Serialization), NoSuchElement or IndexOutOfBounds.
        throw RouterException("Malformed OSRM response", cause = e)
    }

    private fun codeOf(body: String): String? = try {
        Json.parseToJsonElement(body).jsonObject["code"]?.jsonPrimitive?.contentOrNull
    } catch (e: RuntimeException) {
        null
    }

    private companion object {
        val NO_ROUTE_CODES = setOf("NoRoute", "NoSegment")
    }
}

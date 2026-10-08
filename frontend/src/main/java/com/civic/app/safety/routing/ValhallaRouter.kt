package com.civic.app.safety.routing

import com.civic.shared.model.GeoLocation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Pedestrian routes from a Valhalla `/route` endpoint. Valhalla is the primary provider because it is the only
 * keyless public router that can hard-exclude areas (`exclude_polygons`) and also returns up to 2 alternates.
 */
class ValhallaRouter(private val transport: HttpTransport, private val url: String) {
    /**
     * The main trip followed by its alternates. An empty list means Valhalla found no path (error 442, e.g. every
     * way out is excluded). Throws [RouterException] for other HTTP errors or a malformed body; I/O errors propagate.
     */
    suspend fun route(
        origin: GeoLocation,
        destination: GeoLocation,
        excludeRings: List<List<GeoLocation>> = emptyList(),
        alternates: Int = 2,
    ): List<RouteCandidate> {
        val response = transport.postJson(url, requestBody(origin, destination, excludeRings, alternates))
        if (response.status !in 200..299) {
            if (response.status == 400 && errorCode(response.body) == NO_PATH) return emptyList()
            throw RouterException("Valhalla HTTP ${response.status}", response.status)
        }
        return parse(response.body)
    }

    internal fun requestBody(
        origin: GeoLocation,
        destination: GeoLocation,
        excludeRings: List<List<GeoLocation>>,
        alternates: Int,
    ): String = buildJsonObject {
        putJsonArray("locations") {
            for (point in listOf(origin, destination)) {
                addJsonObject {
                    put("lat", point.latitude)
                    put("lon", point.longitude)
                }
            }
        }
        put("costing", "pedestrian")
        put("alternates", alternates)
        put("directions_type", "none")
        if (excludeRings.isNotEmpty()) put("exclude_polygons", ExcludePolygons.toJson(excludeRings))
    }.toString()

    internal fun parse(body: String): List<RouteCandidate> = try {
        val root = Json.parseToJsonElement(body).jsonObject
        val main = root["trip"]?.jsonObject ?: throw RouterException("Valhalla response has no trip")
        val alternates = root["alternates"]?.jsonArray?.mapNotNull { it.jsonObject["trip"]?.jsonObject }.orEmpty()
        (listOf(main) + alternates).mapNotNull(::parseTrip)
    } catch (e: RuntimeException) {
        // Wrong JSON shape surfaces as IllegalArgument (incl. SerializationException) or NoSuchElement.
        throw RouterException("Malformed Valhalla response", cause = e)
    }

    private fun parseTrip(trip: JsonObject): RouteCandidate? {
        val summary = trip.getValue("summary").jsonObject
        val metersPerUnit = if (trip["units"]?.jsonPrimitive?.content?.startsWith("mi") == true) 1609.344 else 1000.0
        val points = mutableListOf<GeoLocation>()
        for (leg in trip.getValue("legs").jsonArray) {
            val shape = PolylineCodec.decode(leg.jsonObject.getValue("shape").jsonPrimitive.content, precision = 6)
            points += if (points.isNotEmpty() && shape.firstOrNull() == points.last()) shape.drop(1) else shape
        }
        if (points.size < 2) return null
        return RouteCandidate(
            points = points,
            distanceM = summary.getValue("length").jsonPrimitive.double * metersPerUnit,
            durationS = summary.getValue("time").jsonPrimitive.double,
        )
    }

    private fun errorCode(body: String): Int? = try {
        Json.parseToJsonElement(body).jsonObject["error_code"]?.jsonPrimitive?.intOrNull
    } catch (e: RuntimeException) {
        null
    }

    private companion object {
        const val NO_PATH = 442
    }
}

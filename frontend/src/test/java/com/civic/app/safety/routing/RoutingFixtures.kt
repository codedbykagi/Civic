package com.civic.app.safety.routing

import com.civic.shared.model.GeoLocation
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max

/** Canned-response [HttpTransport] that records every request; [handler] may throw to simulate I/O failures. */
class FakeTransport(private val handler: (Request) -> HttpResponseData) : HttpTransport {
    data class Request(val method: String, val url: String, val body: String?)

    val requests = mutableListOf<Request>()

    override suspend fun get(url: String) = respond(Request("GET", url, null))

    override suspend fun postJson(url: String, json: String) = respond(Request("POST", url, json))

    private fun respond(request: Request): HttpResponseData {
        requests += request
        return handler(request)
    }
}

fun geo(lat: Double, lng: Double) = GeoLocation(latitude = lat, longitude = lng)

/** Polyline through [corners] with a vertex every ~0.0005° so shapes resemble real router output. */
fun path(vararg corners: GeoLocation): List<GeoLocation> = buildList {
    add(corners.first())
    for (i in 1 until corners.size) {
        val a = corners[i - 1]
        val b = corners[i]
        val steps = max(1, (max(abs(b.latitude - a.latitude), abs(b.longitude - a.longitude)) / 0.0005).toInt())
        for (s in 1..steps) {
            val t = s.toDouble() / steps
            add(geo(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t))
        }
    }
}

fun valhallaOk(main: List<GeoLocation>, vararg alternates: List<GeoLocation>) =
    HttpResponseData(200, valhallaBody(listOf(main), *alternates))

/** A Valhalla `/route` body; each element of [mainLegs] is one leg of the main trip. */
fun valhallaBody(mainLegs: List<List<GeoLocation>>, vararg alternates: List<GeoLocation>): String {
    fun trip(legs: List<List<GeoLocation>>) = buildJsonObject {
        val km = legs.sumOf { RouteScorer.lengthMeters(it) } / 1000
        putJsonArray("legs") {
            for (leg in legs) addJsonObject { put("shape", PolylineCodec.encode(leg, 6)) }
        }
        putJsonObject("summary") {
            put("length", km)
            put("time", km * 1000 / 1.3)
        }
        put("status", 0)
        put("units", "kilometers")
    }
    return buildJsonObject {
        put("trip", trip(mainLegs))
        if (alternates.isNotEmpty()) {
            putJsonArray("alternates") {
                for (alternate in alternates) addJsonObject { put("trip", trip(listOf(alternate))) }
            }
        }
    }.toString()
}

fun valhallaError(code: Int, status: Int = 400) = HttpResponseData(
    status,
    """{"error_code":$code,"error":"No path could be found for input","status_code":$status,"status":"Bad Request"}""",
)

fun osrmOk(vararg routes: List<GeoLocation>) = HttpResponseData(
    200,
    buildJsonObject {
        put("code", "Ok")
        putJsonArray("routes") {
            for (route in routes) {
                addJsonObject {
                    putJsonObject("geometry") {
                        put("type", "LineString")
                        putJsonArray("coordinates") {
                            for (p in route) {
                                addJsonArray {
                                    add(p.longitude)
                                    add(p.latitude)
                                }
                            }
                        }
                    }
                    put("distance", RouteScorer.lengthMeters(route))
                    put("duration", RouteScorer.lengthMeters(route) / 1.3)
                }
            }
        }
    }.toString(),
)

/** Coordinates in an OSRM route URL, as (lat, lng). */
fun osrmCoordinates(url: String): List<GeoLocation> =
    url.substringAfterLast('/').substringBefore('?').split(';').map {
        val (lng, lat) = it.split(',').map(String::toDouble)
        geo(lat, lng)
    }

fun offline(): Nothing = throw IOException("Unable to resolve host")

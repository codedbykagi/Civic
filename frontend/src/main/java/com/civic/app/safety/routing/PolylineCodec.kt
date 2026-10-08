package com.civic.app.safety.routing

import com.civic.shared.model.GeoLocation
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Google's encoded polyline algorithm. Valhalla returns shapes with precision 6, OSRM and Google with precision 5,
 * so the precision must match the source or every point is off by a factor of ten.
 */
object PolylineCodec {
    fun decode(encoded: String, precision: Int = 6): List<GeoLocation> {
        val factor = factorFor(precision)
        val points = ArrayList<GeoLocation>(encoded.length / 6)
        var index = 0
        var lat = 0L
        var lng = 0L

        fun nextValue(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                require(index < encoded.length) { "Truncated polyline" }
                val chunk = encoded[index++].code - 63
                require(chunk in 0..63) { "Invalid polyline character at ${index - 1}" }
                result = result or ((chunk and 0x1f).toLong() shl shift)
                shift += 5
                if (chunk < 0x20) break
            }
            return if (result and 1L != 0L) (result shr 1).inv() else result shr 1
        }

        while (index < encoded.length) {
            lat += nextValue()
            lng += nextValue()
            points += GeoLocation(latitude = lat / factor, longitude = lng / factor)
        }
        return points
    }

    fun encode(points: List<GeoLocation>, precision: Int = 6): String {
        val factor = factorFor(precision)
        val out = StringBuilder()
        var prevLat = 0L
        var prevLng = 0L
        for (point in points) {
            val lat = (point.latitude * factor).roundToLong()
            val lng = (point.longitude * factor).roundToLong()
            appendValue(out, lat - prevLat)
            appendValue(out, lng - prevLng)
            prevLat = lat
            prevLng = lng
        }
        return out.toString()
    }

    private fun appendValue(out: StringBuilder, value: Long) {
        var rest = if (value < 0) (value shl 1).inv() else value shl 1
        while (rest >= 0x20) {
            out.append(((0x20 or (rest and 0x1f).toInt()) + 63).toChar())
            rest = rest shr 5
        }
        out.append((rest.toInt() + 63).toChar())
    }

    private fun factorFor(precision: Int): Double {
        require(precision in 1..9) { "Unsupported polyline precision: $precision" }
        return 10.0.pow(precision)
    }
}

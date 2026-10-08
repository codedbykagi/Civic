package com.civic.app.safety.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/**
 * Sun events for one local date. A field is null when the sun never crosses that altitude on that day
 * (polar day or night), so callers must fall back to clock hours instead of guessing.
 */
data class SunTimes(
    val civilDawn: ZonedDateTime?,
    val sunrise: ZonedDateTime?,
    val sunset: ZonedDateTime?,
    val civilDusk: ZonedDateTime?,
)

/**
 * NOAA solar-calculator equations (Meeus-based, accurate to about a minute between ±72° latitude).
 * Runs offline on the device: dawn in India moves by roughly three hours with latitude and season, so the
 * time-of-day bucket of a report has to come from the sun at its position, not from fixed clock hours.
 */
object SolarCalculator {
    /** Sun's centre 50' below the horizon: refraction plus the solar disc's radius. */
    private const val SUNRISE_ZENITH = 90.833
    private const val CIVIL_TWILIGHT_ZENITH = 96.0
    private const val JD_UNIX_EPOCH = 2440587.5
    private const val JD_J2000 = 2451545.0
    private const val MS_PER_DAY = 86_400_000.0
    private const val ITERATIONS = 3

    fun sunTimes(latitude: Double, longitude: Double, date: LocalDate, zone: ZoneId): SunTimes {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
        val localNoon = julianDay(date.atTime(LocalTime.NOON).atZone(zone).toInstant())
        val noon = solarNoon(longitude, localNoon)
        fun at(zenith: Double, rising: Boolean) = event(latitude, noon, zenith, rising)?.let {
            ZonedDateTime.ofInstant(toInstant(it), zone)
        }
        return SunTimes(
            civilDawn = at(CIVIL_TWILIGHT_ZENITH, rising = true),
            sunrise = at(SUNRISE_ZENITH, rising = true),
            sunset = at(SUNRISE_ZENITH, rising = false),
            civilDusk = at(CIVIL_TWILIGHT_ZENITH, rising = false),
        )
    }

    /** Julian day of the solar noon closest to [nearJd], i.e. the one belonging to the requested local date. */
    private fun solarNoon(longitude: Double, nearJd: Double): Double {
        var noon = nearJd
        repeat(ITERATIONS) {
            val utcMidnight = floor(noon - 0.5) + 0.5
            noon = utcMidnight + (720.0 - 4.0 * longitude - sun(noon).equationOfTimeMin) / 1440.0
            while (noon - nearJd > 0.5) noon -= 1.0
            while (nearJd - noon > 0.5) noon += 1.0
        }
        return noon
    }

    /** Refines the event time by re-evaluating the sun's position at the previous estimate. */
    private fun event(latitude: Double, noon: Double, zenith: Double, rising: Boolean): Double? {
        val sign = if (rising) -1.0 else 1.0
        val noonEqTime = sun(noon).equationOfTimeMin
        var jd = noon
        repeat(ITERATIONS) {
            val position = sun(jd)
            val hourAngle = hourAngleDeg(latitude, position.declinationRad, zenith) ?: return null
            // Solar noon shifts slightly with the equation of time between noon and the event.
            val transit = noon + (noonEqTime - position.equationOfTimeMin) / 1440.0
            jd = transit + sign * 4.0 * hourAngle / 1440.0
        }
        return jd
    }

    private fun hourAngleDeg(latitude: Double, declinationRad: Double, zenith: Double): Double? {
        val lat = Math.toRadians(latitude)
        val cosH = cos(Math.toRadians(zenith)) / (cos(lat) * cos(declinationRad)) - tan(lat) * tan(declinationRad)
        // Outside [-1, 1] the sun stays above (polar day) or below (polar night) this zenith all day; NaN at the poles.
        return if (cosH in -1.0..1.0) Math.toDegrees(acos(cosH)) else null
    }

    private class SunPosition(val declinationRad: Double, val equationOfTimeMin: Double)

    private fun sun(jd: Double): SunPosition {
        val t = (jd - JD_J2000) / 36525.0
        val meanLong = (280.46646 + t * (36000.76983 + t * 0.0003032)).mod(360.0)
        val meanAnomaly = Math.toRadians(357.52911 + t * (35999.05029 - 0.0001537 * t))
        val eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
        val centre = sin(meanAnomaly) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            sin(2 * meanAnomaly) * (0.019993 - 0.000101 * t) +
            sin(3 * meanAnomaly) * 0.000289
        val omega = Math.toRadians(125.04 - 1934.136 * t)
        val apparentLong = Math.toRadians(meanLong + centre - 0.00569 - 0.00478 * sin(omega))
        val meanObliquity = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
        val obliquity = Math.toRadians(meanObliquity + 0.00256 * cos(omega))
        val declination = asin(sin(obliquity) * sin(apparentLong))
        val y = tan(obliquity / 2).let { it * it }
        val l0 = Math.toRadians(meanLong)
        val eqTime = 4.0 * Math.toDegrees(
            y * sin(2 * l0) - 2 * eccentricity * sin(meanAnomaly) +
                4 * eccentricity * y * sin(meanAnomaly) * cos(2 * l0) -
                0.5 * y * y * sin(4 * l0) - 1.25 * eccentricity * eccentricity * sin(2 * meanAnomaly),
        )
        return SunPosition(declination, eqTime)
    }

    private fun julianDay(instant: Instant): Double = instant.toEpochMilli() / MS_PER_DAY + JD_UNIX_EPOCH

    private fun toInstant(jd: Double): Instant = Instant.ofEpochSecond(((jd - JD_UNIX_EPOCH) * 86_400.0).roundToLong())
}

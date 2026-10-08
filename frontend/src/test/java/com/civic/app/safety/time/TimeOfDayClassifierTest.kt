package com.civic.app.safety.time

import com.civic.shared.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

class TimeOfDayClassifierTest {
    private val ist = ZoneId.of("Asia/Kolkata")
    private val delhiLat = 28.6139
    private val delhiLon = 77.2090
    private val june21 = LocalDate.of(2026, 6, 21)
    private val jan10 = LocalDate.of(2026, 1, 10)

    private fun assertNear(expected: LocalTime, actual: ZonedDateTime?, toleranceMin: Long = 3) {
        assertNotNull(actual)
        val diff = abs(Duration.between(expected, actual!!.toLocalTime()).toMinutes())
        assertTrue("expected ~$expected, got ${actual.toLocalTime()}", diff <= toleranceMin)
    }

    private fun millis(date: LocalDate, hour: Int, minute: Int, zone: ZoneId = ist): Long =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), zone).toInstant().toEpochMilli()

    private fun delhi(date: LocalDate, hour: Int, minute: Int): TimeOfDay =
        TimeOfDayClassifier.classify(millis(date, hour, minute), delhiLat, delhiLon, ist)

    @Test
    fun delhiMidsummerSunTimes() {
        val sun = SolarCalculator.sunTimes(delhiLat, delhiLon, june21, ist)
        assertNear(LocalTime.of(4, 57), sun.civilDawn)
        assertNear(LocalTime.of(5, 23), sun.sunrise)
        assertNear(LocalTime.of(19, 22), sun.sunset)
        assertNear(LocalTime.of(19, 48), sun.civilDusk)
        assertEquals(june21, sun.sunrise!!.toLocalDate())
    }

    @Test
    fun delhiWinterSunTimes() {
        val sun = SolarCalculator.sunTimes(delhiLat, delhiLon, jan10, ist)
        assertNear(LocalTime.of(6, 49), sun.civilDawn)
        assertNear(LocalTime.of(7, 15), sun.sunrise)
    }

    @Test
    fun dawnFollowsTheSunAcrossIndia() {
        // Guwahati's dawn is ~1.5 h earlier than Mumbai's on the same day, though both use IST.
        assertNear(LocalTime.of(4, 31), SolarCalculator.sunTimes(26.14, 91.74, june21, ist).sunrise)
        assertNear(LocalTime.of(6, 1), SolarCalculator.sunTimes(19.07, 72.88, june21, ist).sunrise)
        assertNear(LocalTime.of(7, 34), SolarCalculator.sunTimes(23.25, 69.67, jan10, ist).sunrise)
    }

    @Test
    fun junePreSunriseIsDawn() {
        assertEquals(TimeOfDay.DAWN, delhi(june21, 5, 30))
        assertEquals(TimeOfDay.DAWN, delhi(june21, 4, 0)) // civil dawn − 60 min ≈ 03:57
        assertEquals(TimeOfDay.LATE_NIGHT, delhi(june21, 3, 50))
        assertEquals(TimeOfDay.MORNING, delhi(june21, 6, 30)) // sunrise + 60 min ≈ 06:24
    }

    @Test
    fun januarySevenAmIsStillDawnUnlikeFixedHours() {
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fallbackFor(7 * 60))
        assertEquals(TimeOfDay.DAWN, delhi(jan10, 7, 0))
        assertEquals(TimeOfDay.DAWN, delhi(jan10, 8, 0)) // sunrise + 60 min ≈ 08:15
        assertEquals(TimeOfDay.MORNING, delhi(jan10, 8, 30))
    }

    @Test
    fun clockBoundedBuckets() {
        for (date in listOf(june21, jan10)) {
            assertEquals(TimeOfDay.MORNING, delhi(date, 10, 59))
            assertEquals(TimeOfDay.MIDDAY, delhi(date, 11, 0))
            assertEquals(TimeOfDay.MIDDAY, delhi(date, 12, 0))
            assertEquals(TimeOfDay.EVENING, delhi(date, 16, 0))
            assertEquals(TimeOfDay.NIGHT, delhi(date, 20, 59))
            assertEquals(TimeOfDay.LATE_NIGHT, delhi(date, 21, 0))
            assertEquals(TimeOfDay.LATE_NIGHT, delhi(date, 22, 30))
            assertEquals(TimeOfDay.LATE_NIGHT, delhi(date, 2, 0))
            assertEquals(TimeOfDay.LATE_NIGHT, delhi(date, 0, 0))
        }
    }

    @Test
    fun eveningEndsAtCivilDusk() {
        assertEquals(TimeOfDay.EVENING, delhi(june21, 19, 30)) // dusk ≈ 19:49
        assertEquals(TimeOfDay.NIGHT, delhi(june21, 20, 0))
        assertEquals(TimeOfDay.EVENING, delhi(jan10, 18, 0)) // dusk ≈ 18:08
        assertEquals(TimeOfDay.NIGHT, delhi(jan10, 18, 30))
    }

    @Test
    fun withoutPositionUsesFixedHours() {
        val at0530 = millis(june21, 5, 30)
        assertEquals(TimeOfDay.DAWN, TimeOfDayClassifier.classify(at0530, null, null, ist))
        assertEquals(TimeOfDay.DAWN, TimeOfDayClassifier.classify(at0530, delhiLat, null, ist))
        assertEquals(TimeOfDay.MORNING, TimeOfDayClassifier.classify(millis(jan10, 7, 0), null, null, ist))
        assertEquals(TimeOfDay.NIGHT, TimeOfDayClassifier.classify(millis(jan10, 19, 0), null, null, ist))
        assertEquals(TimeOfDay.LATE_NIGHT, TimeOfDayClassifier.classify(millis(jan10, 2, 0), null, null, ist))
        // Garbage coordinates fall back instead of throwing.
        assertEquals(TimeOfDay.DAWN, TimeOfDayClassifier.classify(at0530, 123.0, delhiLon, ist))
        assertEquals(TimeOfDay.DAWN, TimeOfDayClassifier.classify(at0530, Double.NaN, delhiLon, ist))
    }

    @Test
    fun polarDayAndNightFallBackWithoutCrashing() {
        val oslo = ZoneId.of("Europe/Oslo")
        val midnightSun = SolarCalculator.sunTimes(69.65, 18.96, june21, oslo)
        assertNull(midnightSun.sunrise)
        assertNull(midnightSun.civilDusk)
        val polarNight = SolarCalculator.sunTimes(69.65, 18.96, LocalDate.of(2026, 12, 21), oslo)
        assertNull(polarNight.sunrise)
        assertNotNull(polarNight.civilDawn) // civil twilight still happens around noon

        fun tromso(date: LocalDate, hour: Int) =
            TimeOfDayClassifier.classify(millis(date, hour, 0, oslo), 69.65, 18.96, oslo)
        assertEquals(TimeOfDay.LATE_NIGHT, tromso(june21, 23))
        assertEquals(TimeOfDay.MIDDAY, tromso(june21, 12))
        assertEquals(TimeOfDay.MIDDAY, tromso(LocalDate.of(2026, 12, 21), 12))
        // The poles themselves must not divide by zero.
        assertNull(SolarCalculator.sunTimes(90.0, 0.0, june21, ZoneId.of("UTC")).sunrise)
        assertNull(SolarCalculator.sunTimes(-90.0, 0.0, june21, ZoneId.of("UTC")).sunrise)
    }

    @Test
    fun duskBeforeFourEmptiesEvening() {
        val stockholm = ZoneId.of("Europe/Stockholm")
        val dec21 = LocalDate.of(2026, 12, 21)
        val dusk = SolarCalculator.sunTimes(59.33, 18.07, dec21, stockholm).civilDusk!!
        assertTrue("precondition: dusk before 16:00, was $dusk", dusk.toLocalTime() < LocalTime.of(16, 0))
        fun at(h: Int, m: Int) = TimeOfDayClassifier.classify(millis(dec21, h, m, stockholm), 59.33, 18.07, stockholm)
        assertEquals(TimeOfDay.MIDDAY, at(15, 55))
        assertEquals(TimeOfDay.NIGHT, at(16, 0))
        assertEquals(TimeOfDay.NIGHT, at(20, 59))
        assertEquals(TimeOfDay.LATE_NIGHT, at(21, 0))
    }

    @Test
    fun duskAfterNineEmptiesNight() {
        val stockholm = ZoneId.of("Europe/Stockholm")
        val sun = SolarCalculator.sunTimes(59.33, 18.07, june21, stockholm)
        val dusk = sun.civilDusk!!.toLocalTime()
        assertTrue("precondition: dusk after 21:00, was $dusk", dusk > LocalTime.of(21, 0))
        fun at(time: LocalTime) = TimeOfDayClassifier.classify(
            ZonedDateTime.of(june21, time, stockholm).toInstant().toEpochMilli(), 59.33, 18.07, stockholm,
        )
        assertEquals(TimeOfDay.EVENING, at(LocalTime.of(22, 0)))
        assertEquals(TimeOfDay.EVENING, at(dusk.minusMinutes(1)))
        assertEquals(TimeOfDay.LATE_NIGHT, at(dusk.plusMinutes(1)))
        for (hour in 0..23) assertTrue(at(LocalTime.of(hour, 30)) != TimeOfDay.NIGHT)
    }

    @Test
    fun lateSunriseEmptiesMorning() {
        val alaska = ZoneId.of("America/Anchorage")
        val dec21 = LocalDate.of(2026, 12, 21)
        val sunrise = SolarCalculator.sunTimes(64.84, -147.72, dec21, alaska).sunrise!!.toLocalTime()
        assertTrue("precondition: sunrise + 60 min after 11:00, was $sunrise", sunrise > LocalTime.of(10, 0))
        fun at(h: Int, m: Int) = TimeOfDayClassifier.classify(millis(dec21, h, m, alaska), 64.84, -147.72, alaska)
        assertEquals(TimeOfDay.DAWN, at(11, 30))
        assertEquals(TimeOfDay.MIDDAY, at(12, 30))
        for (hour in 0..23) assertTrue(at(hour, 0) != TimeOfDay.MORNING)
    }

    @Test
    fun sunTimesAreOrderedAndOnTheRequestedDate() {
        for (month in 1..12) {
            val date = LocalDate.of(2026, month, 15)
            val sun = SolarCalculator.sunTimes(delhiLat, delhiLon, date, ist)
            val times = listOf(sun.civilDawn!!, sun.sunrise!!, sun.sunset!!, sun.civilDusk!!)
            assertEquals(times.sorted(), times)
            times.forEach { assertEquals(date, it.toLocalDate()) }
        }
    }

    @Test
    fun clockZoneFarFromPositionFallsBackToFixedHours() {
        // A phone on IST with a (mock) fix in California: California's sun can't be read on India's clock.
        val mountainView = TimeOfDayClassifier.classify(millis(LocalDate.of(2026, 10, 9), 2, 27), 37.422, -122.084, ist)
        assertEquals(TimeOfDay.LATE_NIGHT, mountainView)
        // Same clock time in Delhi still uses the sun (and is also late night).
        assertEquals(TimeOfDay.LATE_NIGHT, delhi(LocalDate.of(2026, 10, 9), 2, 27))
    }
}

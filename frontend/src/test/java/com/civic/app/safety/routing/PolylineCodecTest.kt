package com.civic.app.safety.routing

import org.junit.Assert.assertEquals
import org.junit.Test

class PolylineCodecTest {
    private val googleExample = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"

    @Test
    fun decodesGooglesDocumentedExampleAtPrecision5() {
        val points = PolylineCodec.decode(googleExample, precision = 5)

        assertEquals(3, points.size)
        val expected = listOf(38.5 to -120.2, 40.7 to -120.95, 43.252 to -126.453)
        expected.zip(points).forEach { (want, got) ->
            assertEquals(want.first, got.latitude, 1e-9)
            assertEquals(want.second, got.longitude, 1e-9)
        }
    }

    @Test
    fun encodesGooglesDocumentedExample() {
        val points = listOf(geo(38.5, -120.2), geo(40.7, -120.95), geo(43.252, -126.453))
        assertEquals(googleExample, PolylineCodec.encode(points, precision = 5))
    }

    @Test
    fun roundTripsAtPrecision6() {
        val points =
            listOf(geo(28.613939, 77.209021), geo(28.6129, 77.2295), geo(-33.856784, 151.215297), geo(0.0, 0.0))

        val decoded = PolylineCodec.decode(PolylineCodec.encode(points, precision = 6), precision = 6)

        assertEquals(points.size, decoded.size)
        points.zip(decoded).forEach { (want, got) ->
            assertEquals(want.latitude, got.latitude, 1e-9)
            assertEquals(want.longitude, got.longitude, 1e-9)
        }
    }

    @Test
    fun wrongPrecisionScalesCoordinates() {
        val encoded6 = PolylineCodec.encode(listOf(geo(28.6, 77.2)), precision = 6)
        assertEquals(286.0, PolylineCodec.decode(encoded6, precision = 5).single().latitude, 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTruncatedInput() {
        PolylineCodec.decode(googleExample.dropLast(1), precision = 5)
    }
}

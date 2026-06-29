package com.elifoksas.earthquake

import com.elifoksas.earthquake.data.entity.Geojson
import com.elifoksas.earthquake.data.entity.Result
import com.elifoksas.earthquake.ui.EarthquakeFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class EarthquakeFilterTest {
    @Test
    fun appliesMagnitudeDepthAndDistanceTogether() {
        val near = earthquake("near", 4.0, 20.0, 41.0, 29.0)
        val deep = earthquake("deep", 4.0, 80.0, 41.0, 29.0)
        val far = earthquake("far", 4.0, 20.0, 39.0, 32.0)
        val weak = earthquake("weak", 1.0, 20.0, 41.0, 29.0)

        val result = EarthquakeFilter.apply(
            earthquakes = listOf(near, deep, far, weak),
            minimumMagnitude = 2.5,
            maximumDistanceKm = 100,
            maximumDepthKm = 50,
            userCoordinates = EarthquakeFilter.Coordinates(41.0, 29.0)
        )

        assertEquals(listOf("near"), result.map { it.title })
    }

    @Test
    fun ignoresDistanceRangeWhenLocationIsUnavailable() {
        val earthquake = earthquake("visible", 3.0, 20.0, 39.0, 32.0)

        val result = EarthquakeFilter.apply(
            earthquakes = listOf(earthquake),
            minimumMagnitude = 1.0,
            maximumDistanceKm = 25,
            maximumDepthKm = 300,
            userCoordinates = null
        )

        assertEquals(1, result.size)
    }

    @Test
    fun hidesResultsOutsideDistanceRangeWhenLocationIsAvailable() {
        val far = earthquake("far", 4.0, 20.0, 39.0, 32.0)
        val deep = earthquake("deep", 4.0, 400.0, 39.0, 32.0)

        val result = EarthquakeFilter.apply(
            earthquakes = listOf(far, deep),
            minimumMagnitude = 2.5,
            maximumDistanceKm = 25,
            maximumDepthKm = 300,
            userCoordinates = EarthquakeFilter.Coordinates(41.0, 29.0)
        )

        assertEquals(emptyList<String>(), result.map { it.title })
    }

    private fun earthquake(
        title: String,
        magnitude: Double,
        depth: Double,
        latitude: Double,
        longitude: Double
    ): Result {
        return Result(
            title = title,
            mag = magnitude,
            depth = depth,
            geojson = Geojson(coordinates = arrayListOf(longitude, latitude))
        )
    }
}

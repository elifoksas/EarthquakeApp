package com.elifoksas.earthquake.ui

import com.elifoksas.earthquake.data.entity.Result
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object EarthquakeFilter {
    data class Coordinates(val latitude: Double, val longitude: Double)

    fun apply(
        earthquakes: List<Result>,
        minimumMagnitude: Double,
        maximumDistanceKm: Int,
        maximumDepthKm: Int,
        userCoordinates: Coordinates?
    ): List<Result> {
        return earthquakes.filter { earthquake ->
            matchesMagnitude(earthquake, minimumMagnitude) &&
                matchesDepth(earthquake, maximumDepthKm) &&
            matchesDistance(earthquake, maximumDistanceKm, userCoordinates)
        }
    }

    private fun matchesMagnitude(earthquake: Result, minimumMagnitude: Double): Boolean {
        return (earthquake.mag ?: 0.0) >= minimumMagnitude
    }

    private fun matchesDepth(earthquake: Result, maximumDepthKm: Int): Boolean {
        if (maximumDepthKm == -1) return true
        val depth = earthquake.depth ?: return false
        return depth <= maximumDepthKm
    }

    private fun matchesDistance(
        earthquake: Result,
        maximumDistanceKm: Int,
        userCoordinates: Coordinates?
    ): Boolean {
        if (maximumDistanceKm == -1 || userCoordinates == null) return true
        val coordinates = earthquake.geojson?.coordinates ?: return false
        val longitude = coordinates.getOrNull(0) ?: return false
        val latitude = coordinates.getOrNull(1) ?: return false
        return distanceKm(
            userCoordinates,
            Coordinates(latitude = latitude, longitude = longitude)
        ) <= maximumDistanceKm
    }

    private fun distanceKm(from: Coordinates, to: Coordinates): Double {
        val earthRadiusKm = 6371.0
        val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
        val fromLatitude = Math.toRadians(from.latitude)
        val toLatitude = Math.toRadians(to.latitude)

        val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(fromLatitude) * cos(toLatitude) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}

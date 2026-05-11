package com.elifoksas.earthquake.ui.preference

object MagnitudePreference {
    const val KEY = "MinimumEarthquakeMagnitude"
    const val DEFAULT_STORED_VALUE = 10
    const val SCALE = 10.0

    fun toMagnitude(storedValue: Int): Double = storedValue / SCALE
}

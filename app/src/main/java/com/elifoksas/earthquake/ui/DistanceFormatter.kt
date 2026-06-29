package com.elifoksas.earthquake.ui

import com.elifoksas.earthquake.ui.preference.SettingsPreferences
import java.util.Locale

object DistanceFormatter {
    private const val KILOMETERS_PER_MILE = 1.609344

    fun format(distanceKilometers: Double, unit: String): String {
        val value = if (unit == SettingsPreferences.DISTANCE_UNIT_MILES) {
            distanceKilometers / KILOMETERS_PER_MILE
        } else {
            distanceKilometers
        }
        val suffix = if (unit == SettingsPreferences.DISTANCE_UNIT_MILES) "mi" else "km"
        return String.format(Locale.getDefault(), "%.0f %s", value, suffix)
    }
}

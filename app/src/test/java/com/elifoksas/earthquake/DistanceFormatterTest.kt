package com.elifoksas.earthquake

import com.elifoksas.earthquake.ui.DistanceFormatter
import com.elifoksas.earthquake.ui.preference.SettingsPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceFormatterTest {
    @Test
    fun formatsKilometersAndMiles() {
        assertEquals(
            "100 km",
            DistanceFormatter.format(100.0, SettingsPreferences.DISTANCE_UNIT_KILOMETERS)
        )
        assertEquals(
            "62 mi",
            DistanceFormatter.format(100.0, SettingsPreferences.DISTANCE_UNIT_MILES)
        )
    }
}

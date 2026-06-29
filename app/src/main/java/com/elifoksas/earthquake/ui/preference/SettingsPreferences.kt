package com.elifoksas.earthquake.ui.preference

import android.content.SharedPreferences

object SettingsPreferences {
    const val NOTIFICATIONS_KEY = "notifications"
    const val MAP_TYPE_KEY = "MapsType"
    const val DEFAULT_MAP_TYPE = "normal"

    const val DISTANCE_RANGE_KEY = "DistanceRangeKm"
    const val DEFAULT_DISTANCE_RANGE_KM = -1
    private const val DISTANCE_RANGE_DEFAULT_VERSION_KEY = "DistanceRangeDefaultVersion"
    private const val DISTANCE_RANGE_DEFAULT_VERSION = 2

    const val DEPTH_RANGE_KEY = "DepthRangeKm"
    const val DEFAULT_DEPTH_RANGE_KM = -1
    private const val DEPTH_RANGE_DEFAULT_VERSION_KEY = "DepthRangeDefaultVersion"
    private const val DEPTH_RANGE_DEFAULT_VERSION = 2

    const val DISTANCE_UNIT_KEY = "DistanceUnit"
    const val DISTANCE_UNIT_KILOMETERS = "km"
    const val DISTANCE_UNIT_MILES = "mi"
    const val DEFAULT_DISTANCE_UNIT = DISTANCE_UNIT_KILOMETERS

    const val QUIET_HOURS_ENABLED_KEY = "QuietHoursEnabled"
    const val QUIET_HOURS_START_MINUTES_KEY = "QuietHoursStartMinutes"
    const val QUIET_HOURS_END_MINUTES_KEY = "QuietHoursEndMinutes"
    const val DEFAULT_QUIET_HOURS_START_MINUTES = 22 * 60
    const val DEFAULT_QUIET_HOURS_END_MINUTES = 7 * 60

    const val ALL_RANGES = -1

    fun migrateRangeDefaults(preferences: SharedPreferences) {
        migrateRangeDefault(
            preferences = preferences,
            versionKey = DISTANCE_RANGE_DEFAULT_VERSION_KEY,
            currentVersion = DISTANCE_RANGE_DEFAULT_VERSION,
            preferenceKey = DISTANCE_RANGE_KEY,
            defaultValue = DEFAULT_DISTANCE_RANGE_KM
        )
        migrateRangeDefault(
            preferences = preferences,
            versionKey = DEPTH_RANGE_DEFAULT_VERSION_KEY,
            currentVersion = DEPTH_RANGE_DEFAULT_VERSION,
            preferenceKey = DEPTH_RANGE_KEY,
            defaultValue = DEFAULT_DEPTH_RANGE_KM
        )
    }

    private fun migrateRangeDefault(
        preferences: SharedPreferences,
        versionKey: String,
        currentVersion: Int,
        preferenceKey: String,
        defaultValue: Int
    ) {
        val storedVersion = preferences.getInt(versionKey, 0)
        if (storedVersion >= currentVersion) return

        preferences.edit()
            .putInt(preferenceKey, defaultValue)
            .putInt(versionKey, currentVersion)
            .apply()
    }
}

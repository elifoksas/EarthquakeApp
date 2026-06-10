package com.elifoksas.earthquake.data.entity

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class EmergencyItem(
    @DrawableRes val emergencyPicture: Int,
    @StringRes val emergencyName: Int,
    @StringRes val emergencyDescription: Int,
    val destination: EmergencyDestination
)

enum class EmergencyDestination {
    EMERGENCY_CALL,
    WHISTLE,
    INFORMATION,
    EMERGENCY_BAG
}

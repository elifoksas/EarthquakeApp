package com.elifoksas.earthquake.data.entity

import com.google.gson.annotations.SerializedName

data class AfadEvent(
    @SerializedName("eventID")
    val eventId: String? = null,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("latitude")
    val latitude: String? = null,
    @SerializedName("longitude")
    val longitude: String? = null,
    @SerializedName("depth")
    val depth: String? = null,
    @SerializedName("magnitude")
    val magnitude: String? = null,
    @SerializedName("province")
    val province: String? = null,
    @SerializedName("date")
    val date: String? = null
)

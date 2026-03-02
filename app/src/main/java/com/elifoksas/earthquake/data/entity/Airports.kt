package com.elifoksas.earthquake.data.entity

import com.google.gson.annotations.SerializedName


data class Airports(

    @SerializedName("distance")
    var distance: Double? = null,
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("code")
    var code: String? = null,
    @SerializedName("coordinates")
    var coordinates: Coordinates? = Coordinates()

)
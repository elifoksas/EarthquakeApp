package com.elifoksas.earthquake.data.entity

import com.google.gson.annotations.SerializedName


data class Coordinates(

    @SerializedName("type")
    var type: String? = null,
    @SerializedName("coordinates")
    var coordinates: ArrayList<Double> = arrayListOf()

)
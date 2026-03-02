package com.elifoksas.earthquake.data.entity

import com.google.gson.annotations.SerializedName


data class EpiCenter(

    @SerializedName("name")
    var name: String? = null,
    @SerializedName("cityCode")
    var cityCode: Int? = null,
    @SerializedName("population")
    var population: Int? = null

)
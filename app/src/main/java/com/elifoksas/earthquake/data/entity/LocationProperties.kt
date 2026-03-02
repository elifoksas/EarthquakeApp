package com.elifoksas.earthquake.data.entity

import com.google.gson.annotations.SerializedName
import com.elifoksas.earthquake.data.entity.Airports
import com.elifoksas.earthquake.data.entity.ClosestCities
import com.elifoksas.earthquake.data.entity.ClosestCity
import com.elifoksas.earthquake.data.entity.EpiCenter


data class LocationProperties(

    @SerializedName("closestCity")
    var closestCity: ClosestCity? = ClosestCity(),
    @SerializedName("epiCenter")
    var epiCenter: EpiCenter? = EpiCenter(),
    @SerializedName("closestCities")
    var closestCities: ArrayList<ClosestCities> = arrayListOf(),
    @SerializedName("airports")
    var airports: ArrayList<Airports> = arrayListOf()

)
package com.elifoksas.earthquake.data.entity

import com.google.gson.annotations.SerializedName
import com.elifoksas.earthquake.data.entity.Geojson
import com.elifoksas.earthquake.data.entity.LocationProperties


data class Result(

    @SerializedName("_id")
    var Id: String? = null,
    @SerializedName("earthquake_id")
    var earthquakeId: String? = null,
    @SerializedName("provider")
    var provider: String? = null,
    @SerializedName("title")
    var title: String? = null,
    @SerializedName(value = "date_time", alternate = ["date"])
    var date: String? = null,
    @SerializedName("mag")
    var mag: Double? = null,
    @SerializedName("depth")
    var depth: Double? = null,
    @SerializedName("geojson")
    var geojson: Geojson? = Geojson(),
    @SerializedName("location_properties")
    var locationProperties: LocationProperties? = LocationProperties(),
    @SerializedName("rev")
    var rev: String? = null,
    @SerializedName("created_at")
    var createdAt: Int? = null,
    @SerializedName("location_tz")
    var locationTz: String? = null

)

package com.elifoksas.earthquake.retrofit

import com.elifoksas.earthquake.data.entity.Earthquake
import retrofit2.http.GET


interface EarthquakeDao {

    @GET("deprem/kandilli/live")
    suspend fun getEarthquakes(): Earthquake
}
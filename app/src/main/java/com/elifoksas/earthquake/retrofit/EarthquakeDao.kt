package com.elifoksas.earthquake.retrofit

import com.elifoksas.earthquake.data.entity.AfadEvent
import com.elifoksas.earthquake.data.entity.Earthquake
import retrofit2.http.GET
import retrofit2.http.Query


interface EarthquakeDao {

    @GET("deprem/kandilli/live")
    suspend fun getEarthquakes(): Earthquake

    @GET("https://deprem.afad.gov.tr/apiv2/event/filter")
    suspend fun getAfadEarthquakes(
        @Query("start") start: String,
        @Query("end") end: String,
        @Query("orderby") orderBy: String = "timedesc",
        @Query("limit") limit: Int = 100
    ): List<AfadEvent>
}

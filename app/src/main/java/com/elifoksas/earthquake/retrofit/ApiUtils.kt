package com.elifoksas.earthquake.retrofit

class ApiUtils {
    companion object{
        val BASE_URL = "https://api.orhanaydogdu.com.tr/"


        fun getEarthquakeDao() : EarthquakeDao{
            return RetrofitClient.getClient(BASE_URL).create(EarthquakeDao::class.java)
        }
    }
}
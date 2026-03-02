package com.elifoksas.earthquake.data.datasource

import android.content.Context
import com.elifoksas.earthquake.data.entity.Earthquake
import com.elifoksas.earthquake.retrofit.EarthquakeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EarthquakeDataSource (val context: Context, var earthquakeDao: EarthquakeDao) {

    suspend fun getEarthquakes() : Earthquake = withContext(Dispatchers.IO) {
        return@withContext earthquakeDao.getEarthquakes()
    }
}
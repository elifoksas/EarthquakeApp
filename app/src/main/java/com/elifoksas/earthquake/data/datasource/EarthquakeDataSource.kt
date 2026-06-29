package com.elifoksas.earthquake.data.datasource

import android.content.Context
import com.elifoksas.earthquake.data.entity.ClosestCity
import com.elifoksas.earthquake.data.entity.Earthquake
import com.elifoksas.earthquake.data.entity.EpiCenter
import com.elifoksas.earthquake.data.entity.Geojson
import com.elifoksas.earthquake.data.entity.LocationProperties
import com.elifoksas.earthquake.data.entity.Metadata
import com.elifoksas.earthquake.data.entity.Result
import com.elifoksas.earthquake.retrofit.EarthquakeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EarthquakeDataSource (val context: Context, var earthquakeDao: EarthquakeDao) {

    suspend fun getEarthquakes() : Earthquake = withContext(Dispatchers.IO) {
        val primaryResponse = runCatching {
            earthquakeDao.getEarthquakes()
        }.getOrNull()

        if (primaryResponse?.result?.isNotEmpty() == true) {
            return@withContext primaryResponse
        }

        val afadResponse = runCatching {
            val (start, end) = getAfadDateRange()
            earthquakeDao.getAfadEarthquakes(start = start, end = end)
        }.getOrNull()

        if (!afadResponse.isNullOrEmpty()) {
            val earthquakes = afadResponse.mapNotNull { event ->
                val latitude = event.latitude?.toDoubleOrNull() ?: return@mapNotNull null
                val longitude = event.longitude?.toDoubleOrNull() ?: return@mapNotNull null

                Result(
                    earthquakeId = event.eventId,
                    provider = "afad",
                    title = event.location?.takeUnless { it.isBlank() } ?: event.province,
                    date = event.date?.replace('T', ' '),
                    mag = event.magnitude?.toDoubleOrNull(),
                    depth = event.depth?.toDoubleOrNull(),
                    geojson = Geojson(
                        type = "Point",
                        coordinates = arrayListOf(longitude, latitude)
                    ),
                    locationProperties = LocationProperties(
                        closestCity = ClosestCity(name = event.province),
                        epiCenter = EpiCenter(name = event.location)
                    )
                )
            }

            return@withContext Earthquake(
                status = true,
                httpStatus = 200,
                desc = "AFAD fallback",
                metadata = Metadata(total = earthquakes.size),
                result = ArrayList(earthquakes)
            )
        }

        return@withContext primaryResponse ?: Earthquake(
            status = false,
            desc = "Earthquake data is currently unavailable."
        )
    }

    private fun getAfadDateRange(): Pair<String, String> {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val end = Calendar.getInstance()
        val start = (end.clone() as Calendar).apply {
            add(Calendar.HOUR_OF_DAY, -48)
        }

        return formatter.format(start.time) to formatter.format(end.time)
    }
}

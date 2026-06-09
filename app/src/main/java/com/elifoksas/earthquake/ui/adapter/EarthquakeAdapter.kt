package com.elifoksas.earthquake.ui.adapter

import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elifoksas.earthquake.data.entity.Result
import com.elifoksas.earthquake.databinding.EarthquakeItemBinding
import com.elifoksas.earthquake.ui.MagnitudeStyle
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EarthquakeAdapter(
    private val mContext: Context,
    private val earthquakeList: List<Result>,
    private val listener: OnItemClickListener,
    private var userLocation: LatLng? = null
) : RecyclerView.Adapter<EarthquakeAdapter.HomePageItemHolder>() {

    inner class HomePageItemHolder(var item: EarthquakeItemBinding) : RecyclerView.ViewHolder(item.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomePageItemHolder {
        val binding = EarthquakeItemBinding.inflate(LayoutInflater.from(mContext), parent, false)
        return HomePageItemHolder(binding)
    }

    override fun getItemCount(): Int {
        return earthquakeList.size
    }

    override fun onBindViewHolder(holder: HomePageItemHolder, position: Int) {
        val binding = holder.item
        val result = earthquakeList[position]

        val formattedTime = formatToDisplayDate(result.date)
        val minutesPassed = calculateMinutesPassed(result.date)

        binding.countryTV.text = result.title?.takeUnless { it.isBlank() } ?: "-"
        binding.intensityTV.text = formatMagnitude(result.mag)
        MagnitudeStyle.applyBackground(binding.intensityTV, result.mag)
        binding.dateTimeTV.text = formattedTime
        binding.minutesPassedTV.text = minutesPassed
        binding.distanceTV.text = formatDistance(result)

        holder.itemView.setOnClickListener {
            listener.onItemClick(result)
        }
    }

    fun updateUserLocation(location: LatLng?) {
        if (userLocation == location) return

        userLocation = location
        notifyItemRangeChanged(0, itemCount)
    }

    private fun formatToDisplayDate(dateTime: String?): String {
        if (dateTime.isNullOrBlank() || dateTime.equals("null", ignoreCase = true)) {
            return "-"
        }

        return try {
            val outputFormat = SimpleDateFormat("dd MMM · HH:mm", Locale.ENGLISH)
            val date = parseApiDate(dateTime) ?: return "-"
            outputFormat.format(date)
        } catch (e: Exception) {
            "-"
        }
    }

    private fun calculateMinutesPassed(dateTime: String?): String {
        if (dateTime.isNullOrBlank() || dateTime.equals("null", ignoreCase = true)) {
            return "-"
        }

        return try {
            val currentDate = Date()
            val startDate = parseApiDate(dateTime) ?: return "-"
            val difference = currentDate.time - startDate.time
            val differenceInMinutes = kotlin.math.abs(difference / (60 * 1000))

            formatTimeDifference(differenceInMinutes)
        } catch (e: Exception) {
            "-"
        }
    }

    private fun formatMagnitude(magnitude: Double?): String {
        return magnitude?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
    }

    private fun formatDistance(result: Result): String {
        val currentLocation = userLocation ?: return "-"
        val latitude = result.geojson?.coordinates?.getOrNull(1) ?: return "-"
        val longitude = result.geojson?.coordinates?.getOrNull(0) ?: return "-"
        val distanceResult = FloatArray(1)

        Location.distanceBetween(
            currentLocation.latitude,
            currentLocation.longitude,
            latitude,
            longitude,
            distanceResult
        )

        return "${(distanceResult[0] / 1000).toLong()} km"
    }

    private fun parseApiDate(dateTime: String): Date? {
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy.MM.dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        patterns.forEach { pattern ->
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                val parsed = sdf.parse(dateTime)
                if (parsed != null) return parsed
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun formatTimeDifference(minutes: Long): String {
        if (minutes == 0L) return "now"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        val formattedString = StringBuilder()

        if (hours > 0) {
            formattedString.append("$hours h")
        }
        if (remainingMinutes > 0) {
            formattedString.append(" $remainingMinutes m")
        }

        return "${formattedString.toString().trim()} ago"
    }

    interface OnItemClickListener {
        fun onItemClick(item: Result)
    }
}

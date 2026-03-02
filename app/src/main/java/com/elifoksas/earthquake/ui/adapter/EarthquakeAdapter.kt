package com.elifoksas.earthquake.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elifoksas.earthquake.data.entity.Earthquake
import com.elifoksas.earthquake.data.entity.Result
import com.elifoksas.earthquake.databinding.EarthquakeItemBinding
import java.lang.Math.abs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EarthquakeAdapter (var mContext: Context, var earthquakeList: Earthquake, private val listener: OnItemClickListener) : RecyclerView.Adapter<EarthquakeAdapter.HomePageItemHolder>(){

    inner class HomePageItemHolder(var item: EarthquakeItemBinding) : RecyclerView.ViewHolder(item.root)

    var result : List<Result> = listOf()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomePageItemHolder {
        val binding = EarthquakeItemBinding.inflate(LayoutInflater.from(mContext), parent, false)
        return HomePageItemHolder(binding)
    }

    override fun getItemCount(): Int {
        return earthquakeList.result.size
    }

    override fun onBindViewHolder(holder: HomePageItemHolder, position: Int) {
        val earthquake = earthquakeList.result
        val binding = holder.item
        result = earthquake

        val formattedTime = formatToHourMinute(result[position].date)
        val minutesPassed = calculateMinutesPassed(result[position].date)

        binding.countryTV.text = result[position].title.toString()
        binding.intensityTV.text = result[position].mag.toString()
        binding.dateTimeTV.text = formattedTime
        binding.minutesPassedTV.text = minutesPassed

        holder.itemView.setOnClickListener {
            listener.onItemClick(result[position])
        }



    }

    private fun formatToHourMinute(dateTime: String?): String {
        if (dateTime.isNullOrBlank() || dateTime.equals("null", ignoreCase = true)) {
            return "-"
        }

        return try {
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
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
            val differenceInMinutes = Math.abs(difference / (60 * 1000))

            formatTimeDifference(differenceInMinutes)
        } catch (e: Exception) {
            "-"
        }
    }

    private fun parseApiDate(dateTime: String): Date? {
        val patterns = listOf("yyyy-MM-dd HH:mm:ss", "yyyy.MM.dd HH:mm:ss")
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

    private fun formatTimeDifference(minutes : Long) : String {

        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        val formattedString = StringBuilder()

        if (hours > 0) {
            formattedString.append("$hours h")
        }
        if (remainingMinutes > 0) {
            formattedString.append(" $remainingMinutes m")
        }

        return formattedString.toString().trim()

    }

    interface OnItemClickListener {
        fun onItemClick(item: Result)
    }


}

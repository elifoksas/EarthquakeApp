package com.techgeeksclub.earthquake.data.datasource

import android.content.Context
import com.techgeeksclub.earthquake.R
import com.techgeeksclub.earthquake.data.entity.EmergencyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmergencyDataSource (val context: Context) {

    suspend fun getEmergencyItems(): List<EmergencyItem> = withContext(Dispatchers.IO){
        val emergencyList = listOf(
            EmergencyItem(R.drawable.whistle,"Whistle"),
            EmergencyItem(R.drawable.exit,"What to Do During an Earthquake?"),
            EmergencyItem(R.drawable.emergency_call,"Emergency Numbers"),
            EmergencyItem(R.drawable.first_aid_bag,"Emergency Bag")
        )

        return@withContext emergencyList
    }
}
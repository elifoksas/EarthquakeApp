package com.elifoksas.earthquake.data.datasource

import android.content.Context
import com.elifoksas.earthquake.R
import com.elifoksas.earthquake.data.entity.EmergencyDestination
import com.elifoksas.earthquake.data.entity.EmergencyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmergencyDataSource (val context: Context) {

    suspend fun getEmergencyItems(): List<EmergencyItem> = withContext(Dispatchers.IO){
        val emergencyList = listOf(
            EmergencyItem(
                R.drawable.emergency_call,
                R.string.emergency_tool_call_title,
                R.string.emergency_tool_call_description,
                EmergencyDestination.EMERGENCY_CALL
            ),
            EmergencyItem(
                R.drawable.whistle,
                R.string.emergency_tool_whistle_title,
                R.string.emergency_tool_whistle_description,
                EmergencyDestination.WHISTLE
            ),
            EmergencyItem(
                R.drawable.exit,
                R.string.emergency_tool_information_title,
                R.string.emergency_tool_information_description,
                EmergencyDestination.INFORMATION
            ),
            EmergencyItem(
                R.drawable.first_aid_bag,
                R.string.emergency_tool_bag_title,
                R.string.emergency_tool_bag_description,
                EmergencyDestination.EMERGENCY_BAG
            )
        )

        return@withContext emergencyList
    }
}

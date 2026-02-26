package com.elifoksas.earthquake.data.repository

import android.content.Context
import com.elifoksas.earthquake.R
import com.elifoksas.earthquake.data.datasource.EmergencyDataSource
import com.elifoksas.earthquake.data.entity.EmergencyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmergencyRepository (var emergencyDataSource: EmergencyDataSource){

    suspend fun getEmergencyItems() : List<EmergencyItem> = emergencyDataSource.getEmergencyItems()
}
package com.elifoksas.earthquake.data.repository

import com.elifoksas.earthquake.data.datasource.InformationDataSource
import com.elifoksas.earthquake.data.entity.InformationItem

class InformationRepository(var informationDataSource: InformationDataSource){

    suspend fun getInformationItems() : List<InformationItem> = informationDataSource.getInformationItems()
}
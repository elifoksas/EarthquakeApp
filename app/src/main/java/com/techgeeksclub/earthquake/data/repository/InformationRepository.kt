package com.techgeeksclub.earthquake.data.repository

import com.techgeeksclub.earthquake.data.datasource.InformationDataSource
import com.techgeeksclub.earthquake.data.entity.InformationItem

class InformationRepository(var informationDataSource: InformationDataSource){

    suspend fun getInformationItems() : List<InformationItem> = informationDataSource.getInformationItems()
}
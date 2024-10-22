package com.techgeeksclub.earthquake.data.datasource

import com.techgeeksclub.earthquake.R
import com.techgeeksclub.earthquake.data.entity.InformationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InformationDataSource {

    suspend fun getInformationItems() : List<InformationItem> = withContext(Dispatchers.IO){
        val informationList = listOf(
            InformationItem(R.drawable.inf_zero,"Correct actions to take during an earthquake are critical to your safety. Here are some important tips."),
            InformationItem(R.drawable.inf_one,"1. Stay calm and do not panic."),
            InformationItem(R.drawable.inf_two,"2. Stay away from windows, mirrors, and exterior walls."),
            InformationItem(R.drawable.inf_three,"3. Move to a safe spot next to a sturdy furniture or an interior wall. "),
            InformationItem(R.drawable.inf_four,"4. Drop, Cover, and Hold On: Get under a sturdy piece of furniture and hold on. "),
            InformationItem(R.drawable.inf_five,"5. When the shaking stops, leave the building using the stairs."),
            InformationItem(R.drawable.inf_six,"6. Do not use the elevators."),
            InformationItem(R.drawable.inf_seven,"7. Get outside and go to an open gathering area away from buildings. Aftershocks may occur.")
        )

        return@withContext informationList
    }
}
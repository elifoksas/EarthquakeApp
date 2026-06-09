package com.elifoksas.earthquake.data.datasource

import com.elifoksas.earthquake.R
import com.elifoksas.earthquake.data.entity.InformationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InformationDataSource {

    suspend fun getInformationItems(): List<InformationItem> = withContext(Dispatchers.IO) {
        listOf(
            InformationItem(
                1,
                R.drawable.inf_one,
                "Stay calm",
                "Take a breath and avoid sudden movements."
            ),
            InformationItem(
                2,
                R.drawable.inf_two,
                "Move away from hazards",
                "Stay clear of windows, mirrors and exterior walls."
            ),
            InformationItem(
                3,
                R.drawable.inf_three,
                "Find a safer position",
                "Move beside sturdy furniture or toward an interior wall."
            ),
            InformationItem(
                4,
                R.drawable.inf_four,
                "Drop, cover and hold on",
                "Get under sturdy furniture and protect your head."
            ),
            InformationItem(
                5,
                R.drawable.inf_five,
                "Use the stairs",
                "When the shaking stops, leave the building carefully using the stairs."
            ),
            InformationItem(
                6,
                R.drawable.inf_six,
                "Avoid elevators",
                "Do not use an elevator during or immediately after an earthquake."
            ),
            InformationItem(
                7,
                R.drawable.inf_seven,
                "Move to an open area",
                "Stay away from buildings and be prepared for aftershocks."
            )
        )
    }
}

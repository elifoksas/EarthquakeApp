package com.elifoksas.earthquake.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.elifoksas.earthquake.data.entity.Earthquake
import com.elifoksas.earthquake.data.repository.EarthquakeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(var repository: EarthquakeRepository): ViewModel() {

    private val _earthquakes = MutableLiveData<Earthquake>()
    val earthquakes: LiveData<Earthquake> get() = _earthquakes

    init {
        loadEarthquakes()
    }

    private fun loadEarthquakes(){
        CoroutineScope(Dispatchers.Main).launch {
            val response = repository.getEarthquakes()
            _earthquakes.value = response
        }
    }

}
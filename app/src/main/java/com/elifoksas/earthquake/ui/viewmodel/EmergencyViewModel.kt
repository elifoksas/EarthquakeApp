package com.elifoksas.earthquake.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elifoksas.earthquake.data.entity.EmergencyItem
import com.elifoksas.earthquake.data.repository.EmergencyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyViewModel @Inject constructor( var repository: EmergencyRepository): ViewModel() {

    private val _emergencyItems = MutableLiveData<List<EmergencyItem>>()
    val emergencyItems: LiveData<List<EmergencyItem>> get() = _emergencyItems

    init {
        loadEmergencyItems()
    }

    private fun loadEmergencyItems() {
        viewModelScope.launch {
            val response = repository.getEmergencyItems()
            _emergencyItems.value = response
        }
    }
}
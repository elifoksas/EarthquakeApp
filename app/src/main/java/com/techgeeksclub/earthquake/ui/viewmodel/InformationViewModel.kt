package com.techgeeksclub.earthquake.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgeeksclub.earthquake.data.entity.InformationItem
import com.techgeeksclub.earthquake.data.repository.InformationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InformationViewModel  @Inject constructor(var repository: InformationRepository) : ViewModel(){

    private val _informationItems = MutableLiveData<List<InformationItem>>()
    val informationItems: LiveData<List<InformationItem>> get() = _informationItems

    init {
        loadInformationItems()
    }



    private fun loadInformationItems(){
        viewModelScope.launch {
            val response = repository.getInformationItems()
            _informationItems.value = response
        }
    }
}



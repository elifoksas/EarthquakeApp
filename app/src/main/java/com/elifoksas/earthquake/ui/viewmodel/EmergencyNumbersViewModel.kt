package com.elifoksas.earthquake.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.elifoksas.earthquake.data.entity.CountryEmergencyProfile
import com.elifoksas.earthquake.data.entity.PersonalEmergencyContact
import com.elifoksas.earthquake.data.repository.EmergencyNumbersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EmergencyNumbersViewModel @Inject constructor(
    private val repository: EmergencyNumbersRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<EmergencyNumbersUiState>()
    val uiState: LiveData<EmergencyNumbersUiState> = _uiState

    init {
        refresh()
    }

    fun selectCountry(isoCode: String?) {
        repository.setSelectedCountryIso(isoCode)
        refresh()
    }

    fun saveContact(
        id: String?,
        name: String,
        phoneNumber: String
    ): EmergencyNumbersRepository.ContactSaveResult {
        val result = repository.saveContact(id, name, phoneNumber)
        if (result == EmergencyNumbersRepository.ContactSaveResult.SAVED) {
            refresh()
        }
        return result
    }

    fun deleteContact(id: String) {
        repository.deleteContact(id)
        refresh()
    }

    private fun refresh() {
        val manualIsoCode = repository.getSelectedCountryIso()
        val automaticIsoCode = repository.resolveAutomaticCountryIso()
        val activeIsoCode = manualIsoCode ?: automaticIsoCode
        _uiState.value = EmergencyNumbersUiState(
            profiles = repository.getProfiles(),
            selectedProfile = repository.getProfile(activeIsoCode),
            automaticCountryIso = automaticIsoCode,
            isAutomatic = manualIsoCode == null,
            contacts = repository.getContacts()
        )
    }
}

data class EmergencyNumbersUiState(
    val profiles: List<CountryEmergencyProfile>,
    val selectedProfile: CountryEmergencyProfile?,
    val automaticCountryIso: String?,
    val isAutomatic: Boolean,
    val contacts: List<PersonalEmergencyContact>
)

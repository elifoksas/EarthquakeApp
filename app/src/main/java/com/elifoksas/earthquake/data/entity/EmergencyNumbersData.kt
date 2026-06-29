package com.elifoksas.earthquake.data.entity

data class EmergencyNumbersData(
    val generatedAt: String,
    val profiles: List<CountryEmergencyProfile>
)

data class CountryEmergencyProfile(
    val isoCode: String,
    val countryName: String,
    val services: List<EmergencyService>,
    val sourceUrl: String,
    val verifiedAt: String
)

data class EmergencyService(
    val number: String,
    val categories: List<String>
)

data class PersonalEmergencyContact(
    val id: String,
    val name: String,
    val phoneNumber: String
)

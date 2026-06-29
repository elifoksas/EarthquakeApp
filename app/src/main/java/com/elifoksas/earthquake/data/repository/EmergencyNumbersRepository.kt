package com.elifoksas.earthquake.data.repository

import android.content.Context
import android.telephony.TelephonyManager
import com.elifoksas.earthquake.R
import com.elifoksas.earthquake.data.entity.CountryEmergencyProfile
import com.elifoksas.earthquake.data.entity.EmergencyNumbersData
import com.elifoksas.earthquake.data.entity.PersonalEmergencyContact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale
import java.util.UUID

class EmergencyNumbersRepository(
    private val context: Context,
    private val gson: Gson
) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val emergencyData: EmergencyNumbersData by lazy {
        context.resources.openRawResource(R.raw.emergency_numbers).bufferedReader().use { reader ->
            gson.fromJson(reader, EmergencyNumbersData::class.java)
        }
    }

    fun getProfiles(): List<CountryEmergencyProfile> {
        return emergencyData.profiles.sortedBy { it.countryName }
    }

    fun getSelectedCountryIso(): String? {
        return preferences.getString(KEY_SELECTED_COUNTRY, null)
    }

    fun setSelectedCountryIso(isoCode: String?) {
        preferences.edit().apply {
            if (isoCode == null) {
                remove(KEY_SELECTED_COUNTRY)
            } else {
                putString(KEY_SELECTED_COUNTRY, isoCode.uppercase(Locale.US))
            }
        }.apply()
    }

    fun resolveAutomaticCountryIso(): String? {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val candidates = buildList {
            try {
                add(telephonyManager?.networkCountryIso)
            } catch (_: SecurityException) {
                add(null)
            }
            try {
                add(telephonyManager?.simCountryIso)
            } catch (_: SecurityException) {
                add(null)
            }
            add(Locale.getDefault().country)
        }

        return candidates
            .firstNotNullOfOrNull { candidate ->
                candidate
                    ?.trim()
                    ?.takeIf { it.length == 2 }
                    ?.uppercase(Locale.US)
            }
            ?.takeIf { isoCode -> emergencyData.profiles.any { it.isoCode == isoCode } }
    }

    fun getProfile(isoCode: String?): CountryEmergencyProfile? {
        return emergencyData.profiles.firstOrNull {
            it.isoCode.equals(isoCode, ignoreCase = true)
        }
    }

    fun getContacts(): List<PersonalEmergencyContact> {
        val json = preferences.getString(KEY_CONTACTS, null) ?: return emptyList()
        val type = object : TypeToken<List<PersonalEmergencyContact>>() {}.type
        return runCatching {
            gson.fromJson<List<PersonalEmergencyContact>>(json, type)
        }.getOrDefault(emptyList())
    }

    fun saveContact(
        id: String?,
        name: String,
        phoneNumber: String
    ): ContactSaveResult {
        val cleanName = name.trim()
        val cleanPhone = phoneNumber.trim()
        if (cleanName.isBlank() || normalizePhone(cleanPhone).isBlank()) {
            return ContactSaveResult.INVALID
        }

        val contacts = getContacts().toMutableList()
        val normalizedPhone = normalizePhone(cleanPhone)
        val duplicate = contacts.any { contact ->
            contact.id != id && normalizePhone(contact.phoneNumber) == normalizedPhone
        }
        if (duplicate) return ContactSaveResult.DUPLICATE

        if (id == null && contacts.size >= MAX_CONTACTS) {
            return ContactSaveResult.LIMIT_REACHED
        }

        val contact = PersonalEmergencyContact(
            id = id ?: UUID.randomUUID().toString(),
            name = cleanName,
            phoneNumber = cleanPhone
        )
        val existingIndex = contacts.indexOfFirst { it.id == id }
        if (existingIndex >= 0) {
            contacts[existingIndex] = contact
        } else {
            contacts.add(contact)
        }
        persistContacts(contacts)
        return ContactSaveResult.SAVED
    }

    fun deleteContact(id: String) {
        persistContacts(getContacts().filterNot { it.id == id })
    }

    private fun persistContacts(contacts: List<PersonalEmergencyContact>) {
        preferences.edit().putString(KEY_CONTACTS, gson.toJson(contacts)).apply()
    }

    private fun normalizePhone(phoneNumber: String): String {
        return phoneNumber.filter(Char::isDigit)
    }

    enum class ContactSaveResult {
        SAVED,
        INVALID,
        DUPLICATE,
        LIMIT_REACHED
    }

    companion object {
        const val MAX_CONTACTS = 3
        private const val PREFERENCES_NAME = "emergency_numbers_preferences"
        private const val KEY_SELECTED_COUNTRY = "selected_country"
        private const val KEY_CONTACTS = "personal_contacts"
    }
}

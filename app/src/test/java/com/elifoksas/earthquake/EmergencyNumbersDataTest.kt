package com.elifoksas.earthquake

import com.elifoksas.earthquake.data.entity.EmergencyNumbersData
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

class EmergencyNumbersDataTest {

    private val data: EmergencyNumbersData by lazy {
        val resource = sequenceOf(
            File("src/main/res/raw/emergency_numbers.json"),
            File("app/src/main/res/raw/emergency_numbers.json")
        ).first(File::exists)
        Gson().fromJson(resource.readText(), EmergencyNumbersData::class.java)
    }

    @Test
    fun profilesCoverEveryIsoCountryExactlyOnce() {
        val profileCodes = data.profiles.map { it.isoCode }
        assertEquals(profileCodes.size, profileCodes.toSet().size)
        assertTrue(profileCodes.containsAll(Locale.getISOCountries().toList()))
    }

    @Test
    fun servicesContainValidUniqueNumbersAndKnownCategories() {
        val knownCategories = setOf("GENERAL", "POLICE", "FIRE", "AMBULANCE")
        data.profiles.forEach { profile ->
            assertTrue(profile.isoCode.matches(Regex("[A-Z]{2}")))
            assertTrue(profile.countryName.isNotBlank())
            assertTrue(profile.sourceUrl.startsWith("https://"))
            assertTrue(profile.verifiedAt.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))

            val numbers = profile.services.map { it.number }
            assertEquals(
                "Duplicate number in ${profile.isoCode}",
                numbers.size,
                numbers.toSet().size
            )
            profile.services.forEach { service ->
                assertTrue(
                    "Invalid number ${service.number} in ${profile.isoCode}",
                    service.number.matches(Regex("[0-9*#]{2,6}"))
                )
                assertTrue(service.categories.isNotEmpty())
                assertTrue(service.categories.all(knownCategories::contains))
            }
        }
    }

    @Test
    fun verifiedOverridesAndMultiNumberRowsRemainCorrect() {
        val turkey = data.profiles.first { it.isoCode == "TR" }
        assertEquals(listOf("112"), turkey.services.map { it.number })
        assertEquals(
            setOf("GENERAL", "AMBULANCE", "FIRE", "POLICE"),
            turkey.services.single().categories.toSet()
        )

        val unitedKingdom = data.profiles.first { it.isoCode == "GB" }
        assertEquals(setOf("112", "999"), unitedKingdom.services.map { it.number }.toSet())
    }
}

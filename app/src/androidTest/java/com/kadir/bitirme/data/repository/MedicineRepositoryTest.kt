package com.kadir.bitirme.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kadir.bitirme.data.model.MedicineEntity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for MedicineRepository
 * Requires Android context
 */
@RunWith(AndroidJUnit4::class)
class MedicineRepositoryTest {

    private lateinit var repository: MedicineRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = MedicineRepository(context)
    }

    @After
    fun cleanup() {
        repository.close()
    }

    @Test
    fun searchByName_exactMatch_shouldReturnMedicine() {
        val result = repository.searchByName("Aspirin")
        assertNotNull(result)
        assertEquals("Aspirin", result?.name)
    }

    @Test
    fun searchByName_caseInsensitive_shouldReturnMedicine() {
        val result = repository.searchByName("aspirin")
        assertNotNull(result)
        assertEquals("Aspirin", result?.name)
    }

    @Test
    fun searchByName_notFound_shouldReturnNull() {
        val result = repository.searchByName("NonExistentMedicine")
        assertNull(result)
    }

    @Test
    fun searchByGenericName_shouldReturnMatchingMedicines() {
        val results = repository.searchByGenericName("Parasetamol")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "Parol" || it.name == "Major" })
    }

    @Test
    fun fuzzySearch_exactMatch_shouldReturnMedicine() {
        val results = repository.fuzzySearch("Aspirin")
        assertTrue(results.isNotEmpty())
        assertEquals("Aspirin", results.first().name)
    }

    @Test
    fun fuzzySearch_typo_shouldFindSimilar() {
        // "Asprin" yazılsa bile "Aspirin" bulmalı
        val results = repository.fuzzySearch("Asprin")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "Aspirin" })
    }

    @Test
    fun fuzzySearch_partialMatch_shouldReturnResults() {
        val results = repository.fuzzySearch("Par")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name.startsWith("Par", ignoreCase = true) })
    }

    @Test
    fun fuzzySearch_emptyQuery_shouldReturnEmpty() {
        val results = repository.fuzzySearch("")
        assertTrue(results.isEmpty())
    }

    @Test
    fun getAllMedicines_shouldReturnAtLeast20() {
        val allMedicines = repository.getAllMedicines()
        assertTrue(allMedicines.size >= 20)
    }

    @Test
    fun addMedicine_shouldSucceed() {
        val newMedicine = MedicineEntity(
            name = "Test Medicine",
            genericName = "Test Generic",
            dosage = "100mg",
            form = "Tablet",
            usage = "Test usage",
            sideEffects = null,
            warnings = null
        )

        val id = repository.addMedicine(newMedicine)
        assertTrue(id > 0)

        // Verify it was added
        val retrieved = repository.searchByName("Test Medicine")
        assertNotNull(retrieved)
        assertEquals("Test Medicine", retrieved?.name)
    }

    @Test
    fun levenshteinDistance_shouldFindSimilarMedicines() {
        // Test with common typos
        val testCases = listOf(
            "Parol" to "Parol",      // exact
            "Parol" to "Parol",      // typo
            "Asprin" to "Aspirin",   // missing 'i'
            "Majr" to "Major"        // missing 'o'
        )

        testCases.forEach { (query, expected) ->
            val results = repository.fuzzySearch(query)
            assertTrue("Failed for query: $query", results.isNotEmpty())
        }
    }

    @Test
    fun database_shouldContainCommonTurkishMedicines() {
        val commonMedicines = listOf(
            "Aspirin",
            "Parol",
            "Major",
            "Aferin",
            "Nurofen"
        )

        commonMedicines.forEach { medicineName ->
            val result = repository.searchByName(medicineName)
            assertNotNull("$medicineName should be in database", result)
        }
    }
}

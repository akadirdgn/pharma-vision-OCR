package com.kadir.bitirme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kadir.bitirme.data.model.ProcessedResult
import com.kadir.bitirme.data.repository.MedicineRepository
import com.kadir.bitirme.domain.processor.MedicineTextProcessor
import com.kadir.bitirme.domain.usecase.ProcessOcrTextUseCase
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for OCR processing accuracy
 * Tests the entire pipeline: OCR → Filtering → Database → Result
 */
@RunWith(AndroidJUnit4::class)
class OcrAccuracyTest {

    private lateinit var repository: MedicineRepository
    private lateinit var textProcessor: MedicineTextProcessor
    private lateinit var useCase: ProcessOcrTextUseCase

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        repository = MedicineRepository(context)
        textProcessor = MedicineTextProcessor()
        useCase = ProcessOcrTextUseCase(textProcessor, repository)
    }

    @After
    fun cleanup() {
        repository.close()
    }

    /**
     * Test cases simulating real OCR output from medicine boxes
     */
    private fun getRealWorldTestCases(): List<Pair<String, String>> {
        return listOf(
            // (OCR Text, Expected Medicine Name)
            """
                ASPIRIN
                500mg
                Film Kaplı Tablet
                SKT: 01.12.2025
                LOT: ABC123
                8690801234567
            """.trimIndent() to "Aspirin",

            """
                PAROL 500 MG
                Film Tablet
                Reçetesiz satılamaz
                SON KULLANMA TARİHİ: 15.06.2025
            """.trimIndent() to "Parol",

            """
                Major
                500mg tablet
                8690800987654
            """.trimIndent() to "Major",

            "AFERIN 400MG TABLET" to "Aferin",

            """
                Nurofen
                200 mg
                Tablet
                İbuprofen
            """.trimIndent() to "Nurofen",

            """
                VOLTAREN
                75mg Enjeksiyon
                Diklofenak
                8690805555555
                SKT 12/2025
            """.trimIndent() to "Voltaren",

            "Parol 500mg" to "Parol",
            
            "ASPIRIN 100 MG" to "Aspirin",

            """
                MAJEZIK
                25mg Film Tablet
                Deksketoprofen
            """.trimIndent() to "Majezik",

            "CALPOL 120mg/5ml Şurup" to "Calpol"
        )
    }

    @Test
    fun accuracy_test_realWorldScenarios() {
        val testCases = getRealWorldTestCases()
        var successCount = 0
        val results = mutableListOf<Triple<String, String, Boolean>>()

        testCases.forEach { (ocrText, expectedMedicine) ->
            val result = useCase.execute(ocrText)
            
            val success = when (result) {
                is ProcessedResult.Success -> {
                    result.medicine.name == expectedMedicine
                }
                else -> false
            }

            if (success) successCount++
            results.add(Triple(expectedMedicine, ocrText.take(50), success))
        }

        val accuracy = (successCount.toFloat() / testCases.size) * 100

        // Print results
        println("\n=== OCR Accuracy Test Results ===")
        println("Total Test Cases: ${testCases.size}")
        println("Successful Matches: $successCount")
        println("Accuracy: ${"%.2f".format(accuracy)}%")
        println("\nDetailed Results:")
        results.forEachIndexed { index, (expected, ocrSnippet, success) ->
            val status = if (success) "✓" else "✗"
            println("$status Test ${index + 1}: Expected '$expected', OCR: '${ocrSnippet}...'")
        }
        println("================================\n")

        // Assert 95% accuracy requirement
        assertTrue(
            "Accuracy (${"%.2f".format(accuracy)}%) is below 95% threshold",
            accuracy >= 95.0
        )
    }

    @Test
    fun processingTime_shouldBeUnder2Seconds() {
        val ocrText = """
            ASPIRIN 500mg
            Film Kaplı Tablet
            SKT: 01.12.2025
            8690801234567
        """.trimIndent()

        val result = useCase.execute(ocrText)

        when (result) {
            is ProcessedResult.Success -> {
                println("Processing Time: ${result.processingTimeMs}ms")
                assertTrue(
                    "Processing time (${result.processingTimeMs}ms) exceeds 2000ms",
                    result.processingTimeMs < 2000
                )
            }
            is ProcessedResult.NotFound -> {
                println("Processing Time: ${result.processingTimeMs}ms")
                assertTrue(
                    "Processing time (${result.processingTimeMs}ms) exceeds 2000ms",
                    result.processingTimeMs < 2000
                )
            }
            else -> fail("Unexpected result type")
        }
    }

    @Test
    fun averageProcessingTime_multipleRuns() {
        val ocrTexts = getRealWorldTestCases().map { it.first }
        val processingTimes = mutableListOf<Long>()

        ocrTexts.forEach { ocrText ->
            val result = useCase.execute(ocrText)
            when (result) {
                is ProcessedResult.Success -> processingTimes.add(result.processingTimeMs)
                is ProcessedResult.NotFound -> processingTimes.add(result.processingTimeMs)
                else -> {}
            }
        }

        val averageTime = processingTimes.average()
        val maxTime = processingTimes.maxOrNull() ?: 0L

        println("\n=== Processing Time Statistics ===")
        println("Average Processing Time: ${"%.2f".format(averageTime)}ms")
        println("Max Processing Time: ${maxTime}ms")
        println("Min Processing Time: ${processingTimes.minOrNull()}ms")
        println("===================================\n")

        assertTrue(
            "Average processing time (${"%.2f".format(averageTime)}ms) exceeds 1800ms",
            averageTime < 1800
        )
    }

    @Test
    fun fuzzySearch_shouldHandleTypos() {
        val typoTestCases = listOf(
            "Asprin" to "Aspirin",  // missing 'i'
            "Parol" to "Parol",      // typo
            "Majr" to "Major",       // missing 'o'
            "Nuroffen" to "Nurofen"  // extra 'f'
        )

        var successCount = 0

        typoTestCases.forEach { (typo, expected) ->
            val results = repository.fuzzySearch(typo)
            if (results.isNotEmpty() && results.first().name == expected) {
                successCount++
            }
        }

        val accuracy = (successCount.toFloat() / typoTestCases.size) * 100
        println("Typo Handling Accuracy: ${"%.2f".format(accuracy)}%")

        assertTrue(
            "Typo handling accuracy should be above 75%",
            accuracy >= 75.0
        )
    }
}

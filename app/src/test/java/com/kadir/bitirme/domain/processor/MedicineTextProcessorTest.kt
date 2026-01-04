package com.kadir.bitirme.domain.processor

import com.kadir.bitirme.data.model.MedicineInfo
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit test for MedicineTextProcessor
 * Tests the intelligent filtering algorithm
 */
class MedicineTextProcessorTest {

    private lateinit var processor: MedicineTextProcessor

    @Before
    fun setup() {
        processor = MedicineTextProcessor()
    }

    @Test
    fun `extractDosage should find mg values`() {
        val input = "ASPIRIN 500mg tablet"
        val result = processor.extractDosage(input)
        assertEquals("500mg", result)
    }

    @Test
    fun `extractDosage should find ml values`() {
        val input = "CALPOL 120mg/5ml şurup"
        val result = processor.extractDosage(input)
        assertNotNull(result)
        assertTrue(result!!.contains("mg") || result.contains("ml"))
    }

    @Test
    fun `extractDosage should handle space between number and unit`() {
        val input = "Parol 500 mg"
        val result = processor.extractDosage(input)
        assertEquals("500mg", result)
    }

    @Test
    fun `extractDosage should return null when no dosage found`() {
        val input = "ASPIRIN tablet"
        val result = processor.extractDosage(input)
        assertNull(result)
    }

    @Test
    fun `cleanIrrelevantText should remove barcodes`() {
        val input = "PAROL 8690800123456 500mg"
        val result = processor.cleanIrrelevantText(input)
        assertFalse(result.contains("8690800123456"))
        assertTrue(result.contains("PAROL"))
    }

    @Test
    fun `cleanIrrelevantText should remove dates`() {
        val input = "ASPIRIN 500mg SKT: 01.12.2025"
        val result = processor.cleanIrrelevantText(input)
        assertFalse(result.contains("01.12.2025"))
        assertTrue(result.contains("ASPIRIN"))
    }

    @Test
    fun `cleanIrrelevantText should remove lot numbers`() {
        val input = "PAROL 500mg LOT: ABC123"
        val result = processor.cleanIrrelevantText(input)
        assertFalse(result.contains("LOT"))
        assertFalse(result.contains("ABC123"))
    }

    @Test
    fun `extractMedicineName should extract uppercase medicine name`() {
        val input = "ASPIRIN 500mg film tablet"
        val result = processor.extractMedicineName(input)
        assertEquals("ASPIRIN", result)
    }

    @Test
    fun `extractMedicineName should extract capitalized medicine name`() {
        val input = "Parol 500mg tablet"
        val result = processor.extractMedicineName(input)
        assertEquals("Parol", result)
    }

    @Test
    fun `extractMedicineName should ignore dosage in name`() {
        val input = "ASPIRIN 500mg TABLET"
        val result = processor.extractMedicineName(input)
        assertEquals("ASPIRIN", result)
        assertNotEquals("500mg", result)
    }

    @Test
    fun `extractMedicineName should handle mixed case text`() {
        val input = "PAROL 500mg film kaplı tablet şurup"
        val result = processor.extractMedicineName(input)
        assertEquals("PAROL", result)
    }

    @Test
    fun `process should return MedicineInfo with name and dosage`() {
        val input = "ASPIRIN 500mg film kaplı tablet SKT: 01.12.2025 8690800123456"
        val result: MedicineInfo = processor.process(input)
        
        assertEquals("ASPIRIN", result.name)
        assertEquals("500mg", result.dosage)
        assertEquals(input, result.rawText)
    }

    @Test
    fun `process should handle text without dosage`() {
        val input = "PAROL tablet"
        val result = processor.process(input)
        
        assertEquals("PAROL", result.name)
        assertNull(result.dosage)
    }

    @Test
    fun `process should clean irrelevant information`() {
        val input = """
            PAROL 500mg
            SKT: 01.12.2025
            LOT: XYZ789
            8690801234567
            Reçetesiz satılamaz
        """.trimIndent()
        
        val result = processor.process(input)
        
        assertEquals("PAROL", result.name)
        assertEquals("500mg", result.dosage)
    }

    @Test
    fun `extractMedicineName should return empty for blank input`() {
        val result = processor.extractMedicineName("")
        assertEquals("", result)
    }

    @Test
    fun `cleanIrrelevantText should trim whitespace`() {
        val input = "  ASPIRIN 500mg  "
        val result = processor.cleanIrrelevantText(input)
        assertEquals("ASPIRIN 500mg", result)
    }

    @Test
    fun `extractMedicineName should find longest word when no uppercase`() {
        val input = "paracetamol 500mg tablet"
        val result = processor.extractMedicineName(input)
        // Should find 'paracetamol' as the longest word
        assertTrue(result.contains("paracetamol", ignoreCase = true))
    }
}

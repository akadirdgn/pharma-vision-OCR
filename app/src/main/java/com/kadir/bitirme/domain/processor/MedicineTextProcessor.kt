package com.kadir.bitirme.domain.processor

import android.util.Log
import com.kadir.bitirme.data.model.MedicineInfo

/**
 * OCR metnini işleyerek ilaç bilgilerini çıkarır
 * Akıllı filtreleme algoritması
 */
class MedicineTextProcessor {

    /**
     * Tüm OCR işleme pipeline'ı
     */
    fun process(ocrText: String): MedicineInfo {
        val cleanedText = cleanIrrelevantText(ocrText)
        val medicineName = extractMedicineName(cleanedText)
        val dosage = extractDosage(cleanedText)

        return MedicineInfo(
            name = medicineName,
            dosage = dosage,
            rawText = ocrText
        )
    }

    /**
     * Gereksiz metinleri temizler
     * - Barkod numaraları
     * - Tarih formatları
     * - Uzun sayı dizileri
     * - Yaygın uyarı metinleri
     */
    fun cleanIrrelevantText(ocrText: String): String {
        var cleaned = ocrText

        // Barkod numaralarını çıkar (8+ digit)
        cleaned = cleaned.replace(Regex("\\d{8,}"), "")

        // Tarih formatlarını çıkar (DD.MM.YYYY, DD/MM/YYYY)
        cleaned = cleaned.replace(Regex("\\d{2}[./]\\d{2}[./]\\d{4}"), "")

        // Son kullanma tarihi (SKT, EXP, Exp Date vb.)
        cleaned = cleaned.replace(Regex("(?i)(SKT|EXP|Exp\\s*Date)[:\\s]*\\d{2}[./]\\d{2,4}"), "")

        // Lot numaraları
        cleaned = cleaned.replace(Regex("(?i)(LOT|Lot\\s*No)[:\\s]*[A-Z0-9]+"), "")

        // Yaygın uyarı metinleri
        val commonPhrases = listOf(
            "reçetesiz satılamaz",
            "doktor reçetesi ile",
            "prospektüsü okuyunuz",
            "saklama koşulları",
            "üretim tarihi",
            "son kullanma",
            "eczacınıza danışınız",
            "kullanma talimatı"
        )

        commonPhrases.forEach { phrase ->
            cleaned = cleaned.replace(phrase, "", ignoreCase = true)
        }

        // Birden fazla boşluğu teke indir
        cleaned = cleaned.replace(Regex("\\s+"), " ")

        return cleaned.trim()
    }

    /**
     * İlaç adını ayıklar
     * Heuristics:
     * - Genelde büyük harfle yazılır
     * - En belirgin/uzun kelime
     * - Dosaj bilgisinden hemen önce gelir
     */
    fun extractMedicineName(cleanedText: String): String {
        if (cleanedText.isBlank()) return ""

        val words = cleanedText.split(Regex("\\s+"))
            .filter { it.length > 2 } // Çok kısa kelimeleri elendir

        if (words.isEmpty()) return cleanedText

        // 1. Strateji: Tamamı büyük harf olan kelimeler (örn: ASPIRIN)
        val allCapsWords = words.filter { word ->
            word.all { it.isUpperCase() || !it.isLetter() } && word.any { it.isLetter() }
        }
        if (allCapsWords.isNotEmpty()) {
            // Dosaj bilgisi içermeyenleri al
            val nonDosageWords = allCapsWords.filterNot { containsDosagePattern(it) }
            if (nonDosageWords.isNotEmpty()) {
                return nonDosageWords.maxByOrNull { it.length } ?: allCapsWords.first()
            }
        }

        // 2. Strateji: Başı büyük harf olan kelimeler (örn: Aspirin, Parol)
        val capitalizedWords = words.filter { word ->
            word.first().isUpperCase() && word.length > 3
        }.filterNot { containsDosagePattern(it) }

        if (capitalizedWords.isNotEmpty()) {
            return capitalizedWords.maxByOrNull { it.length } ?: capitalizedWords.first()
        }

        // 3. Strateji: En uzun kelime (son çare)
        val longestWord = words
            .filterNot { containsDosagePattern(it) }
            .maxByOrNull { it.length }

        return longestWord ?: words.firstOrNull() ?: ""
    }

    /**
     * Dozaj bilgisini çıkarır
     * Formatlar: 500mg, 10ml, 1g, 250mcg, 5 mg (boşluklu)
     */
    fun extractDosage(text: String): String? {
        val dosagePattern = Regex("\\d+\\s?(mg|ml|g|mcg|µg|cc|IU|iu)", RegexOption.IGNORE_CASE)
        val match = dosagePattern.find(text)
        return match?.value?.replace(" ", "")?.lowercase()
    }

    /**
     * Bir kelimenin dozaj bilgisi içerip içermediğini kontrol eder
     */
    private fun containsDosagePattern(word: String): Boolean {
        return Regex("\\d+(mg|ml|g|mcg|µg)", RegexOption.IGNORE_CASE).containsMatchIn(word)
    }

    companion object {
        private const val TAG = "MedicineTextProcessor"
    }
}

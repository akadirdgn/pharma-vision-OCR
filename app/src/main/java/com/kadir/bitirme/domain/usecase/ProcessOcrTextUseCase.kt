package com.kadir.bitirme.domain.usecase

import android.util.Log
import com.kadir.bitirme.data.model.ProcessedResult
import com.kadir.bitirme.data.repository.MedicineRepository
import com.kadir.bitirme.domain.processor.MedicineTextProcessor

/**
 * OCR metnini işleme use case
 * OCR → Filtreleme → Veritabanı Arama → TTS pipeline
 */
class ProcessOcrTextUseCase(
    private val textProcessor: MedicineTextProcessor,
    private val repository: MedicineRepository
) {

    /**
     * Ham OCR metnini işleyerek ilaç bilgisini çıkarır ve veritabanında arar
     * 
     * @param rawOcrText ML Kit'ten gelen ham metin
     * @return ProcessedResult ile sonuç ve seslendirilecek metin
     */
    fun execute(rawOcrText: String): ProcessedResult {
        val startTime = System.currentTimeMillis()

        return try {
            // 1. Boş metin kontrolü
            if (rawOcrText.isBlank()) {
                return ProcessedResult.Error(
                    message = "OCR metni boş",
                    speech = "Metin bulunamadı. Lütfen tekrar deneyin."
                )
            }

            Log.d(TAG, "Raw OCR Text: $rawOcrText")

            // 2. Metni işle (filtreleme ve bilgi çıkarma)
            val medicineInfo = textProcessor.process(rawOcrText)
            Log.d(TAG, "Extracted Name: ${medicineInfo.name}, Dosage: ${medicineInfo.dosage}")

            // 3. İlaç adı çıkarılamadıysa
            if (medicineInfo.name.isBlank()) {
                val processingTime = System.currentTimeMillis() - startTime
                return ProcessedResult.NotFound(
                    extractedName = "",
                    speech = "İlaç adı tespit edilemedi. Lütfen daha net bir görüntü ile tekrar deneyin.",
                    processingTimeMs = processingTime
                )
            }

            // 4. Veritabanında ara (fuzzy search)
            val matches = repository.fuzzySearch(medicineInfo.name)
            Log.d(TAG, "Database matches found: ${matches.size}")

            val processingTime = System.currentTimeMillis() - startTime

            // 5. Sonuç değerlendirmesi
            if (matches.isNotEmpty()) {
                val bestMatch = matches.first()
                
                // Dozaj bilgilerini karşılaştır
                val detectedDosage = medicineInfo.dosage
                val expectedDosage = bestMatch.dosage

                // TTS için konuşma metni oluştur
                val speech = buildSpeechText(bestMatch, detectedDosage, expectedDosage)

                Log.d(TAG, "Match found: ${bestMatch.name} - Processing time: ${processingTime}ms")

                ProcessedResult.Success(
                    medicine = bestMatch,
                    speech = speech,
                    processingTimeMs = processingTime
                )
            } else {
                // Veritabanında bulunamadı, ama OCR'dan bir şey çıkarıldı
                val speech = "Tespit edilen metin: ${medicineInfo.name}. " +
                        (medicineInfo.dosage?.let { "Dozaj: $it. " } ?: "") +
                        "Ancak veritabanında bulunamadı."

                Log.d(TAG, "No match - Processing time: ${processingTime}ms")

                ProcessedResult.NotFound(
                    extractedName = medicineInfo.name,
                    speech = speech,
                    processingTimeMs = processingTime
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing OCR text", e)
            ProcessedResult.Error(
                message = e.message ?: "Bilinmeyen hata",
                speech = "İşlem sırasında hata oluştu. Lütfen tekrar deneyin."
            )
        }
    }

    /**
     * İlaç bilgilerinden TTS için konuşma metni oluşturur
     */
    private fun buildSpeechText(
        medicine: com.kadir.bitirme.data.model.MedicineEntity,
        detectedDosage: String?,
        expectedDosage: String
    ): String {
        val sb = StringBuilder()

        // İlaç adı
        sb.append("${medicine.name}. ")

        // Dozaj kontrolü
        if (detectedDosage != null) {
            if (detectedDosage.equals(expectedDosage, ignoreCase = true)) {
                sb.append("Dozaj: $detectedDosage. Doğru dozaj tespit edildi. ")
            } else {
                sb.append("Tespit edilen dozaj: $detectedDosage. ")
                sb.append("Veritabanındaki dozaj: $expectedDosage. Lütfen kontrol edin. ")
            }
        } else {
            sb.append("Dozaj: $expectedDosage. ")
        }

        // İlaç formu
        sb.append("${medicine.form}. ")

        // Etken madde
        sb.append("Etken madde: ${medicine.genericName}. ")

        // Kullanım talimatı (kısa versiyon)
        val usageShort = medicine.usage.take(100)
        sb.append(usageShort)

        // Uyarı varsa ekle
        if (!medicine.warnings.isNullOrBlank()) {
            sb.append(" Uyarı: ${medicine.warnings}")
        }

        return sb.toString()
    }

    companion object {
        private const val TAG = "ProcessOcrTextUseCase"
    }
}

package com.kadir.bitirme.domain.usecase

import android.util.Log
import com.kadir.bitirme.data.model.ProcessedResult
import com.kadir.bitirme.data.repository.MedicineRepository
import com.kadir.bitirme.data.repository.ScanHistoryRepository
import com.kadir.bitirme.domain.processor.MedicineTextProcessor
import java.util.concurrent.TimeUnit

/**
 * OCR metnini işleme use case
 * OCR → Filtreleme → Veritabanı Arama → Etkileşim Kontrolü → TTS pipeline
 */
class ProcessOcrTextUseCase(
    private val textProcessor: MedicineTextProcessor,
    private val repository: MedicineRepository,
    private val scanHistoryRepository: ScanHistoryRepository
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
                
                // 6. Etkileşim Kontrolü (Son 24 saat içinde taranan ilaçlarla)
                val interactionWarning = checkDrugInteractions(bestMatch)

                // Dozaj bilgilerini karşılaştır
                val detectedDosage = medicineInfo.dosage
                val expectedDosage = bestMatch.dosage

                // TTS için konuşma metni oluştur
                val speech = buildSpeechText(bestMatch, detectedDosage, expectedDosage, interactionWarning)

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
     * Taranan ilaç ile son 24 saatteki başarılı taramalar arasında etkileşim kontrolü yapar
     */
    private fun checkDrugInteractions(currentMedicine: com.kadir.bitirme.data.model.MedicineEntity): String? {
        try {
            if (currentMedicine.interactingDrugs.isBlank()) return null

            val recentScans = scanHistoryRepository.getRecentScans(10) // Son 10 taramayı al
            if (recentScans.isEmpty()) return null
            
            val oneDayAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            val interactionsList = currentMedicine.interactingDrugs.split(",").map { it.trim().lowercase() }
            
            for (scan in recentScans) {
                if (!scan.isSuccess || scan.scanDate < oneDayAgo) continue
                
                // Kendi kendine etkileşim uyarısı verme
                if (scan.medicineName.equals(currentMedicine.name, ignoreCase = true)) continue
                
                // Taranan ilacın detayını bulmak için repository'den çek
                val pastMedicineMatches = repository.fuzzySearch(scan.medicineName)
                if (pastMedicineMatches.isNotEmpty()) {
                    val pastMedicine = pastMedicineMatches.first()
                    val pastGenericName = pastMedicine.genericName.lowercase()
                    
                    // Etken madde eşleşmesi kontrolü
                    if (interactionsList.any { it.contains(pastGenericName) || pastGenericName.contains(it) }) {
                        return "DİKKAT! Bu ilacın, yakın zamanda tarattığınız ${pastMedicine.name} ile etkileşim riski bulunmaktadır. Lütfen doktorunuza danışın."
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking interactions", e)
        }
        return null
    }

    /**
     * Dozaj metnini TTS için düzenler (örn: "25mg" → "yirmi beş miligram")
     */
    private fun formatDosageForSpeech(dosage: String): String {
        return dosage
            .replace(Regex("(\\d+)\\s*mg"), "$1 miligram")
            .replace(Regex("(\\d+)\\s*mcg"), "$1 mikrogram")
            .replace(Regex("(\\d+)\\s*g"), "$1 gram")
            .replace(Regex("(\\d+)\\s*ml"), "$1 mililitre")
            .replace(Regex("(\\d+)/\\s*(\\d+)"), "$1 bölü $2")
            // Sayıları kelime olarak okutmak için (opsiyonel)
            .replace("0.5", "yarım")
            .replace("1.5", "bir buçuk")
            .replace("2.5", "iki buçuk")
    }

    /**
     * İlaç bilgilerinden TTS için konuşma metni oluşturur
     */
    private fun buildSpeechText(
        medicine: com.kadir.bitirme.data.model.MedicineEntity,
        detectedDosage: String?,
        expectedDosage: String,
        interactionWarning: String?
    ): String {
        val sb = StringBuilder()

        // İlaç adı
        sb.append("${medicine.name}. ")
        
        // Etkileşim uyarısı varsa hemen en başta söyle (hayat kurtarıcı özellik)
        if (interactionWarning != null) {
            sb.append(interactionWarning).append(" ")
        }

        // Dozaj kontrolü
        if (detectedDosage != null) {
            val formattedDetected = formatDosageForSpeech(detectedDosage)
            val formattedExpected = formatDosageForSpeech(expectedDosage)
            
            if (detectedDosage.equals(expectedDosage, ignoreCase = true)) {
                sb.append("Dozaj: $formattedDetected. Doğru dozaj tespit edildi. ")
            } else {
                sb.append("Tespit edilen dozaj: $formattedDetected. ")
                sb.append("Veritabanındaki dozaj: $formattedExpected. Lütfen kontrol edin. ")
            }
        } else {
            val formattedExpected = formatDosageForSpeech(expectedDosage)
            sb.append("Dozaj: $formattedExpected. ")
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

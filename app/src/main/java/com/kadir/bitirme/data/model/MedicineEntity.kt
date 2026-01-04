package com.kadir.bitirme.data.model

/**
 * İlaç bilgilerini temsil eden veri sınıfı
 * SQLite veritabanında saklanacak
 */
data class MedicineEntity(
    val id: Int = 0,
    val name: String,              // İlaç ticari adı (örn: "Aspirin", "Parol")
    val genericName: String,       // Etken madde (örn: "Asetilsalisilik Asit")
    val dosage: String,            // Dozaj bilgisi (örn: "500mg", "10ml")
    val form: String,              // İlaç formu (örn: "Tablet", "Şurup", "Enjeksiyon")
    val usage: String,             // Kullanım talimatı
    val sideEffects: String?,      // Yan etkiler (opsiyonel)
    val warnings: String?          // Uyarılar (opsiyonel)
)

/**
 * OCR'dan çıkarılan ham ilaç bilgisi
 */
data class MedicineInfo(
    val name: String,
    val dosage: String?,
    val rawText: String
)

/**
 * İşlenmiş OCR sonucu
 */
sealed class ProcessedResult {
    data class Success(
        val medicine: MedicineEntity,
        val speech: String,
        val processingTimeMs: Long
    ) : ProcessedResult()
    
    data class NotFound(
        val extractedName: String,
        val speech: String,
        val processingTimeMs: Long
    ) : ProcessedResult()
    
    data class Error(
        val message: String,
        val speech: String
    ) : ProcessedResult()
}

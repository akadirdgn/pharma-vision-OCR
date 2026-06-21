package com.kadir.bitirme.data.model

/**
 * Tarama geçmişi kaydını temsil eden veri sınıfı
 */
data class ScanHistoryEntity(
    val id: Int = 0,
    val medicineName: String,                           // Tespit edilen ilaç adı
    val scanDate: Long = System.currentTimeMillis(),    // Tarama zamanı (epoch ms)
    val isSuccess: Boolean,                             // Veritabanında bulundu mu?
    val rawText: String = "",                           // Ham OCR metni
    val speechOutput: String = ""                       // TTS'e gönderilen metin
)

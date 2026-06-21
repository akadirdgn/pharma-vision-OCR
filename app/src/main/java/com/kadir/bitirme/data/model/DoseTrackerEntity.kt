package com.kadir.bitirme.data.model

/**
 * Günlük doz takibi için veri sınıfı
 */
data class DoseTrackerEntity(
    val id: Int = 0,
    val medicineName: String, // İlaç adı
    val date: Long,           // Tarih (sadece gün bilgisi, epoch)
    val isTaken: Boolean      // Alındı mı?
)

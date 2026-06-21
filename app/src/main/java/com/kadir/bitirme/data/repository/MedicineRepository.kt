package com.kadir.bitirme.data.repository

import android.content.Context
import android.database.Cursor
import android.util.LruCache
import com.kadir.bitirme.data.local.MedicineDatabaseHelper
import com.kadir.bitirme.data.local.MedicineDatabaseHelper.Companion.COLUMN_DOSAGE
import com.kadir.bitirme.data.local.MedicineDatabaseHelper.Companion.COLUMN_FORM
import com.kadir.bitirme.data.local.MedicineDatabaseHelper.Companion.COLUMN_GENERIC_NAME
import com.kadir.bitirme.data.local.MedicineDatabaseHelper.Companion.COLUMN_ID
import com.kadir.bitirme.data.local.MedicineDatabaseHelper.Companion.COLUMN_NAME
import com.kadir.bitirme.data.local.MedicineDatabaseHelper.Companion.COLUMN_SIDE_EFFECTS
import com.kadir.bitirme.data.local.MedicineDatabaseHelper.Companion.COLUMN_USAGE
import com.kadir.bitirme.data.local.MedicineDatabaseHelper.Companion.COLUMN_WARNINGS
import com.kadir.bitirme.data.local.MedicineDatabaseHelper.Companion.COLUMN_CATEGORY
import com.kadir.bitirme.data.local.MedicineDatabaseHelper.Companion.COLUMN_INTERACTING_DRUGS
import com.kadir.bitirme.data.local.MedicineDatabaseHelper.Companion.TABLE_MEDICINES
import com.kadir.bitirme.data.model.MedicineEntity

/**
 * İlaç veritabanı repository sınıfı
 * Veritabanı işlemlerini yönetir
 */
class MedicineRepository(context: Context) {

    private val dbHelper = MedicineDatabaseHelper(context)
    
    // LRU Cache for fuzzy search results (max 50 entries)
    private val searchCache = LruCache<String, List<MedicineEntity>>(50)

    /**
     * İlaç adına göre tam eşleşme arar
     */
    fun searchByName(query: String): MedicineEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE_MEDICINES,
            null,
            "$COLUMN_NAME = ? COLLATE NOCASE",
            arrayOf(query),
            null,
            null,
            null
        )

        return cursor.use {
            if (it.moveToFirst()) parseMedicine(it) else null
        }
    }

    /**
     * Etken madde adına göre arama
     */
    fun searchByGenericName(query: String): List<MedicineEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE_MEDICINES,
            null,
            "$COLUMN_GENERIC_NAME LIKE ? COLLATE NOCASE",
            arrayOf("%$query%"),
            null,
            null,
            null
        )

        return cursor.use { parseMedicineList(it) }
    }

    /**
     * Fuzzy search - benzer isimleri bulur
     * Levenshtein distance kullanır
     * LRU Cache kullanır
     */
    fun fuzzySearch(query: String): List<MedicineEntity> {
        if (query.isBlank()) return emptyList()
        
        // Check cache first
        val cacheKey = query.lowercase().trim()
        searchCache.get(cacheKey)?.let { return it }

        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE_MEDICINES,
            null,
            null,
            null,
            null,
            null,
            null
        )

        val allMedicines = cursor.use { parseMedicineList(it) }

        // Önce tam eşleşme ara
        val exactMatch = allMedicines.filter { 
            it.name.equals(query, ignoreCase = true) 
        }
        if (exactMatch.isNotEmpty()) {
            searchCache.put(cacheKey, exactMatch)
            return exactMatch
        }

        // Başlangıç ile eşleşme
        val startsWith = allMedicines.filter { 
            it.name.startsWith(query, ignoreCase = true) 
        }
        if (startsWith.isNotEmpty()) {
            val result = startsWith.take(3)
            searchCache.put(cacheKey, result)
            return result
        }

        // İçeren eşleşme
        val contains = allMedicines.filter { 
            it.name.contains(query, ignoreCase = true) 
        }
        if (contains.isNotEmpty()) {
            val result = contains.take(3)
            searchCache.put(cacheKey, result)
            return result
        }

        // Levenshtein distance ile benzerlik hesapla
        val similarities = allMedicines.map { medicine ->
            val distance = levenshteinDistance(
                query.lowercase(), 
                medicine.name.lowercase()
            )
            val similarity = 1.0 - (distance.toDouble() / maxOf(query.length, medicine.name.length))
            medicine to similarity
        }

        // Benzerlik skoru > 0.6 olanları döndür
        val result = similarities
            .filter { it.second > 0.6 }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
        
        // Cache the result
        searchCache.put(cacheKey, result)
        return result
    }

    /**
     * Tüm ilaçları getir
     */
    fun getAllMedicines(): List<MedicineEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE_MEDICINES,
            null,
            null,
            null,
            null,
            null,
            COLUMN_NAME
        )

        return cursor.use { parseMedicineList(it) }
    }

    /**
     * Yeni ilaç ekle (dinamik yapı için)
     */
    fun addMedicine(medicine: MedicineEntity): Long {
        val db = dbHelper.writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_NAME, medicine.name)
            put(COLUMN_GENERIC_NAME, medicine.genericName)
            put(COLUMN_DOSAGE, medicine.dosage)
            put(COLUMN_FORM, medicine.form)
            put(COLUMN_USAGE, medicine.usage)
            put(COLUMN_SIDE_EFFECTS, medicine.sideEffects)
            put(COLUMN_WARNINGS, medicine.warnings)
            put(COLUMN_CATEGORY, medicine.category)
            put(COLUMN_INTERACTING_DRUGS, medicine.interactingDrugs)
        }
        return db.insert(TABLE_MEDICINES, null, values)
    }

    /**
     * Levenshtein distance algoritması
     * İki string arasındaki benzerliği hesaplar
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        for (j in 0..s2.length) {
            dp[0][j] = j
        }

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    /**
     * Cursor'dan MedicineEntity parse eder
     */
    private fun parseMedicine(cursor: Cursor): MedicineEntity {
        return MedicineEntity(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
            genericName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENERIC_NAME)),
            dosage = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOSAGE)),
            form = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FORM)),
            usage = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USAGE)),
            sideEffects = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SIDE_EFFECTS)),
            warnings = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WARNINGS)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
            interactingDrugs = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTERACTING_DRUGS))
        )
    }

    /**
     * Cursor'dan MedicineEntity listesi parse eder
     */
    private fun parseMedicineList(cursor: Cursor): List<MedicineEntity> {
        val medicines = mutableListOf<MedicineEntity>()
        while (cursor.moveToNext()) {
            medicines.add(parseMedicine(cursor))
        }
        return medicines
    }

    /**
     * Kaynakları temizle
     */
    fun close() {
        dbHelper.close()
    }
}

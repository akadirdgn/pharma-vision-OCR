package com.kadir.bitirme.data.repository

import android.content.Context
import android.database.Cursor
import com.kadir.bitirme.data.local.MedicineDatabaseHelper
import com.kadir.bitirme.data.model.ScanHistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tarama geçmişi repository sınıfı
 */
class ScanHistoryRepository(context: Context) {

    private val dbHelper = MedicineDatabaseHelper(context)

    companion object {
        private const val TABLE = MedicineDatabaseHelper.TABLE_SCAN_HISTORY
        private const val COL_ID = MedicineDatabaseHelper.HISTORY_COL_ID
        private const val COL_MEDICINE_NAME = MedicineDatabaseHelper.HISTORY_COL_MEDICINE_NAME
        private const val COL_SCAN_DATE = MedicineDatabaseHelper.HISTORY_COL_SCAN_DATE
        private const val COL_IS_SUCCESS = MedicineDatabaseHelper.HISTORY_COL_IS_SUCCESS
        private const val COL_RAW_TEXT = MedicineDatabaseHelper.HISTORY_COL_RAW_TEXT
        private const val COL_SPEECH = MedicineDatabaseHelper.HISTORY_COL_SPEECH_OUTPUT
    }

    /**
     * Tarama kaydını ekle
     */
    fun insertScan(scan: ScanHistoryEntity): Long {
        val db = dbHelper.writableDatabase
        val values = android.content.ContentValues().apply {
            put(COL_MEDICINE_NAME, scan.medicineName)
            put(COL_SCAN_DATE, scan.scanDate)
            put(COL_IS_SUCCESS, if (scan.isSuccess) 1 else 0)
            put(COL_RAW_TEXT, scan.rawText)
            put(COL_SPEECH, scan.speechOutput)
        }
        return db.insert(TABLE, null, values)
    }

    /**
     * Tüm tarama geçmişini en yeni önce getir
     */
    fun getAllScans(): List<ScanHistoryEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE, null, null, null, null, null,
            "$COL_SCAN_DATE DESC"
        )
        return cursor.use { parseScanList(it) }
    }

    /**
     * Son N taramayı getir
     */
    fun getRecentScans(limit: Int = 20): List<ScanHistoryEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE, null, null, null, null, null,
            "$COL_SCAN_DATE DESC",
            limit.toString()
        )
        return cursor.use { parseScanList(it) }
    }

    /**
     * Toplam tarama sayısını getir
     */
    fun getTotalScanCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE", null)
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /**
     * Başarılı tarama sayısını getir
     */
    fun getSuccessfulScanCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE WHERE $COL_IS_SUCCESS = 1", null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /**
     * En sık taranan ilacı getir
     */
    fun getMostScannedMedicine(): String? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """SELECT $COL_MEDICINE_NAME, COUNT(*) as cnt 
               FROM $TABLE 
               WHERE $COL_IS_SUCCESS = 1 
               GROUP BY $COL_MEDICINE_NAME 
               ORDER BY cnt DESC 
               LIMIT 1""",
            null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    /**
     * Tüm geçmişi sil
     */
    fun clearHistory() {
        val db = dbHelper.writableDatabase
        db.delete(TABLE, null, null)
    }

    private fun parseScanList(cursor: Cursor): List<ScanHistoryEntity> {
        val list = mutableListOf<ScanHistoryEntity>()
        while (cursor.moveToNext()) {
            list.add(
                ScanHistoryEntity(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    medicineName = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEDICINE_NAME)),
                    scanDate = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SCAN_DATE)),
                    isSuccess = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_SUCCESS)) == 1,
                    rawText = cursor.getString(cursor.getColumnIndexOrThrow(COL_RAW_TEXT)) ?: "",
                    speechOutput = cursor.getString(cursor.getColumnIndexOrThrow(COL_SPEECH)) ?: ""
                )
            )
        }
        return list
    }

    fun close() {
        dbHelper.close()
    }

    /**
     * Tarih formatter (UI için)
     */
    fun formatDate(epochMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr"))
        return sdf.format(Date(epochMs))
    }
}

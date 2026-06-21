package com.kadir.bitirme.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.kadir.bitirme.data.local.MedicineDatabaseHelper
import com.kadir.bitirme.data.model.DoseTrackerEntity
import java.util.Calendar

class DoseTrackerRepository(context: Context) {

    private val dbHelper = MedicineDatabaseHelper(context)

    companion object {
        private const val TABLE = MedicineDatabaseHelper.TABLE_DOSE_TRACKER
        private const val COL_ID = MedicineDatabaseHelper.DOSE_COL_ID
        private const val COL_MEDICINE_NAME = MedicineDatabaseHelper.DOSE_COL_MEDICINE_NAME
        private const val COL_DATE = MedicineDatabaseHelper.DOSE_COL_DATE
        private const val COL_IS_TAKEN = MedicineDatabaseHelper.DOSE_COL_IS_TAKEN
    }

    /**
     * Bugünün başlangıç zamanını (epoch) getirir
     */
    private fun getTodayStartEpoch(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Belirli bir ilacı günlük doza ekler (veya günceller)
     */
    fun addOrUpdateDose(medicineName: String, isTaken: Boolean) {
        val today = getTodayStartEpoch()
        val db = dbHelper.writableDatabase

        // Önce bugün için bu ilaç var mı kontrol et
        val cursor = db.query(
            TABLE, arrayOf(COL_ID),
            "$COL_MEDICINE_NAME = ? AND $COL_DATE = ?",
            arrayOf(medicineName, today.toString()),
            null, null, null
        )

        val exists = cursor.moveToFirst()
        cursor.close()

        val values = ContentValues().apply {
            put(COL_MEDICINE_NAME, medicineName)
            put(COL_DATE, today)
            put(COL_IS_TAKEN, if (isTaken) 1 else 0)
        }

        if (exists) {
            db.update(
                TABLE, values,
                "$COL_MEDICINE_NAME = ? AND $COL_DATE = ?",
                arrayOf(medicineName, today.toString())
            )
        } else {
            db.insert(TABLE, null, values)
        }
    }

    /**
     * Bugünkü dozları getirir
     */
    fun getTodayDoses(): List<DoseTrackerEntity> {
        val today = getTodayStartEpoch()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE, null,
            "$COL_DATE = ?", arrayOf(today.toString()),
            null, null, "$COL_IS_TAKEN ASC, $COL_MEDICINE_NAME ASC"
        )
        return cursor.use { parseDoseList(it) }
    }
    
    /**
     * Belirtilen ilacın bugün alınıp alınmadığını döner
     */
    fun isDoseTakenToday(medicineName: String): Boolean {
        val today = getTodayStartEpoch()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE, arrayOf(COL_IS_TAKEN),
            "$COL_MEDICINE_NAME = ? AND $COL_DATE = ?",
            arrayOf(medicineName, today.toString()),
            null, null, null
        )
        
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) == 1 else false
        }
    }

    private fun parseDoseList(cursor: Cursor): List<DoseTrackerEntity> {
        val list = mutableListOf<DoseTrackerEntity>()
        while (cursor.moveToNext()) {
            list.add(
                DoseTrackerEntity(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    medicineName = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEDICINE_NAME)),
                    date = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)),
                    isTaken = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_TAKEN)) == 1
                )
            )
        }
        return list
    }

    fun close() {
        dbHelper.close()
    }
}

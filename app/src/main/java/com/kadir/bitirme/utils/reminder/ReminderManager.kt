package com.kadir.bitirme.utils.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Doz hatırlatıcısı zamanlayıcısı
 * AlarmManager kullanarak ilerleyen saatlerde bildirim planlar
 */
class ReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "ReminderManager"
        private const val REQUEST_CODE_BASE = 2000
    }

    /**
     * Belirtilen süre sonra hatırlatıcı planla
     * @param medicineName İlaç adı (bildirimde gösterilir)
     * @param delayMinutes Kaç dakika sonra hatırlatılsın
     */
    fun scheduleReminder(medicineName: String, delayMinutes: Int, isRecurring: Boolean = true) {
        val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MEDICINE_NAME, medicineName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + medicineName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (isRecurring) {
                // Günlük tekrarlayan alarm
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        // Exact alarm izni yoksa inexact kullan
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }
            Log.d(TAG, "Reminder set for $medicineName in $delayMinutes minutes. Recurring: $isRecurring")
        } catch (e: SecurityException) {
            // Güvenlik istisnası — inexact alarm'a düş
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            Log.w(TAG, "Exact alarm permission denied, using inexact", e)
        }
    }

    /**
     * Belirli bir ilaç için hatırlatıcıyı iptal et
     */
    fun cancelReminder(medicineName: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + medicineName.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            Log.d(TAG, "Reminder cancelled for $medicineName")
        }
    }
}

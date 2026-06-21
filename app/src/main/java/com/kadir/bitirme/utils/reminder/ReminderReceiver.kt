package com.kadir.bitirme.utils.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kadir.bitirme.R
import com.kadir.bitirme.ui.main.MainActivity

/**
 * Doz hatırlatıcısı BroadcastReceiver
 * AlarmManager tarafından tetiklenir, bildirim gösterir
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "dose_reminder_channel"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: "İlaç"
        createNotificationChannel(context)
        showNotification(context, medicineName)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Doz Hatırlatıcısı",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "İlaç doz hatırlatma bildirimleri"
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, medicineName: String) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💊 İlaç Zamanı: $medicineName")
            .setContentText("$medicineName almanızın zamanı geldi. Sağlıklı kalın!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$medicineName almanızın zamanı geldi.\nUygulamayı açarak ilaç bilgilerinizi tekrar dinleyebilirsiniz.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

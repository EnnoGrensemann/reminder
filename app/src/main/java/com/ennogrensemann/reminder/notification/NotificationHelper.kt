package com.ennogrensemann.reminder.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ennogrensemann.reminder.MainActivity
import com.ennogrensemann.reminder.R
import com.ennogrensemann.reminder.data.ReminderEntity

object NotificationHelper {

    const val CHANNEL_ID = "location_reminders_high_channel"
    private const val CHANNEL_NAME = "Standort-Erinnerungen"
    private const val CHANNEL_DESC = "Popup-Benachrichtigungen beim Erreichen eines Erinnerungs-Standorts"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showReminderNotification(context: Context, reminder: ReminderEntity) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("reminder_id", reminder.id)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_COMPLETED
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitle = buildString {
            append("📍 ")
            append(reminder.locationName)
            append(" (${reminder.radiusMeters.toInt()} m)")
            if (reminder.notes.isNotBlank()) {
                append("\n")
                append(reminder.notes)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.title)
            .setContentText("📍 ${reminder.locationName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Erledigt",
                completePendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
    }

    fun cancelNotification(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
    }
}

package com.ennogrensemann.reminder.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ennogrensemann.reminder.data.AppDatabase
import com.ennogrensemann.reminder.geofence.GeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_COMPLETED = "com.ennogrensemann.reminder.ACTION_MARK_COMPLETED"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_COMPLETED) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).reminderDao()
                val reminder = dao.getReminderById(reminderId)
                if (reminder != null) {
                    val updated = reminder.copy(
                        isCompleted = true,
                        hasTriggered = true,
                        completedAt = System.currentTimeMillis()
                    )
                    dao.updateReminder(updated)
                    GeofenceManager(context).removeGeofence(reminderId)
                }
                NotificationHelper.cancelNotification(context, reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

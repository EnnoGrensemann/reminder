package com.ennogrensemann.reminder.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ennogrensemann.reminder.data.AppDatabase
import com.ennogrensemann.reminder.notification.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error: $errorMessage")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER) {
            return
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).reminderDao()
                val geofenceManager = GeofenceManager(context)

                for (geofence in triggeringGeofences) {
                    val reminderId = geofence.requestId.toLongOrNull() ?: continue
                    val reminder = dao.getReminderById(reminderId) ?: continue

                    if (reminder.isCompleted) {
                        geofenceManager.removeGeofence(reminderId)
                        continue
                    }

                    // Show the high-priority popup notification
                    NotificationHelper.showReminderNotification(context, reminder)

                    // If not recurring, mark as triggered so it doesn't spam repeatedly
                    if (!reminder.isRecurring) {
                        dao.updateReminder(reminder.copy(hasTriggered = true))
                        geofenceManager.removeGeofence(reminderId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence enter transition", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}

package com.ennogrensemann.reminder.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.ennogrensemann.reminder.data.ReminderEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context.applicationContext)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context.applicationContext, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context.applicationContext, 0, intent, flags)
    }

    fun hasGeofencePermissions(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return fineGranted && backgroundGranted
    }

    @SuppressLint("MissingPermission")
    suspend fun registerGeofence(reminder: ReminderEntity): Boolean {
        if (!hasGeofencePermissions()) {
            Log.w(TAG, "Cannot register geofence: missing location permissions")
            return false
        }
        if (reminder.isCompleted || (!reminder.isRecurring && reminder.hasTriggered)) {
            removeGeofence(reminder.id)
            return false
        }

        val radius = reminder.radiusMeters.coerceIn(50f, 5000f)
        val geofence = Geofence.Builder()
            .setRequestId(reminder.id.toString())
            .setCircularRegion(reminder.latitude, reminder.longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        return try {
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
            Log.d(TAG, "Registered geofence for reminder ${reminder.id} (${reminder.locationName})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register geofence for reminder ${reminder.id}", e)
            false
        }
    }

    suspend fun removeGeofence(reminderId: Long) {
        try {
            geofencingClient.removeGeofences(listOf(reminderId.toString())).await()
            Log.d(TAG, "Removed geofence for reminder $reminderId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove geofence for reminder $reminderId", e)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun syncGeofences(reminders: List<ReminderEntity>) {
        if (!hasGeofencePermissions()) return
        for (reminder in reminders) {
            if (!reminder.isCompleted && (reminder.isRecurring || !reminder.hasTriggered)) {
                registerGeofence(reminder)
            } else {
                removeGeofence(reminder.id)
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceManager"
    }
}

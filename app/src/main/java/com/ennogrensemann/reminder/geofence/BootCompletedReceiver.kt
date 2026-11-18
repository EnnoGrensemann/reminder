package com.ennogrensemann.reminder.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ennogrensemann.reminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).reminderDao()
                val armable = dao.getArmableReminders()
                GeofenceManager(context).syncGeofences(armable)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

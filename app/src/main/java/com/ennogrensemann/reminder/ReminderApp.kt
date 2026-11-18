package com.ennogrensemann.reminder

import android.app.Application
import com.ennogrensemann.reminder.notification.NotificationHelper
import org.osmdroid.config.Configuration
import java.io.File

class ReminderApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure osmdroid user agent and local tile cache inside app cache dir
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir, "osmdroid").apply { mkdirs() }
        val tileCache = File(basePath, "tiles").apply { mkdirs() }
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = tileCache

        // Initialize high-priority notification channel for popup alerts
        NotificationHelper.createNotificationChannel(this)
    }
}

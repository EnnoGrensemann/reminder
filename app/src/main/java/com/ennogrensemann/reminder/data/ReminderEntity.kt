package com.ennogrensemann.reminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 150f,
    val favoriteId: Long? = null,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val hasTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

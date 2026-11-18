package com.ennogrensemann.reminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_locations")
data class FavoriteLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "Place",
    val latitude: Double,
    val longitude: Double,
    val defaultRadiusMeters: Float = 150f,
    val createdAt: Long = System.currentTimeMillis()
)

package com.ennogrensemann.reminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ennogrensemann.reminder.data.AppDatabase
import com.ennogrensemann.reminder.data.FavoriteLocationEntity
import com.ennogrensemann.reminder.data.ReminderEntity
import com.ennogrensemann.reminder.geofence.GeofenceManager
import com.ennogrensemann.reminder.notification.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).reminderDao()
    private val geofenceManager = GeofenceManager(application)

    val activeReminders: StateFlow<List<ReminderEntity>> = dao.observeActiveReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedReminders: StateFlow<List<ReminderEntity>> = dao.observeCompletedReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteLocationEntity>> = dao.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncAllGeofences()
    }

    fun syncAllGeofences() {
        viewModelScope.launch {
            val armable = dao.getArmableReminders()
            geofenceManager.syncGeofences(armable)
        }
    }

    fun saveReminder(
        existingId: Long? = null,
        title: String,
        notes: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        favoriteId: Long?,
        isRecurring: Boolean,
        alsoSaveAsFavorite: Boolean = false,
        favoriteName: String = "",
        favoriteIconName: String = "Place"
    ) {
        viewModelScope.launch {
            var resolvedFavoriteId = favoriteId
            var resolvedLocationName = locationName.ifBlank {
                "%.4f, %.4f".format(latitude, longitude)
            }

            if (alsoSaveAsFavorite) {
                val favTitle = favoriteName.ifBlank { resolvedLocationName }
                val newFavId = dao.insertFavorite(
                    FavoriteLocationEntity(
                        name = favTitle,
                        iconName = favoriteIconName,
                        latitude = latitude,
                        longitude = longitude,
                        defaultRadiusMeters = radiusMeters
                    )
                )
                resolvedFavoriteId = newFavId
                resolvedLocationName = favTitle
            }

            val entity = if (existingId != null && existingId > 0) {
                val old = dao.getReminderById(existingId)
                ReminderEntity(
                    id = existingId,
                    title = title.trim(),
                    notes = notes.trim(),
                    locationName = resolvedLocationName,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radiusMeters,
                    favoriteId = resolvedFavoriteId,
                    isCompleted = false,
                    isRecurring = isRecurring,
                    hasTriggered = false,
                    createdAt = old?.createdAt ?: System.currentTimeMillis(),
                    completedAt = null
                )
            } else {
                ReminderEntity(
                    title = title.trim(),
                    notes = notes.trim(),
                    locationName = resolvedLocationName,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radiusMeters,
                    favoriteId = resolvedFavoriteId,
                    isCompleted = false,
                    isRecurring = isRecurring,
                    hasTriggered = false
                )
            }

            val savedId = dao.insertReminder(entity)
            val savedEntity = entity.copy(id = savedId)
            geofenceManager.registerGeofence(savedEntity)
        }
    }

    fun toggleReminderCompletion(reminder: ReminderEntity) {
        viewModelScope.launch {
            val nowCompleted = !reminder.isCompleted
            val updated = reminder.copy(
                isCompleted = nowCompleted,
                hasTriggered = if (nowCompleted) true else false,
                completedAt = if (nowCompleted) System.currentTimeMillis() else null
            )
            dao.updateReminder(updated)
            if (nowCompleted) {
                geofenceManager.removeGeofence(reminder.id)
                NotificationHelper.cancelNotification(getApplication(), reminder.id)
            } else {
                geofenceManager.registerGeofence(updated)
            }
        }
    }

    fun rearmTriggeredReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(hasTriggered = false, isCompleted = false)
            dao.updateReminder(updated)
            geofenceManager.registerGeofence(updated)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            dao.deleteReminder(reminder)
            geofenceManager.removeGeofence(reminder.id)
            NotificationHelper.cancelNotification(getApplication(), reminder.id)
        }
    }

    fun clearCompletedReminders() {
        viewModelScope.launch {
            dao.deleteAllCompletedReminders()
        }
    }

    fun triggerTestPopup(reminder: ReminderEntity) {
        NotificationHelper.showReminderNotification(getApplication(), reminder)
    }

    fun saveFavorite(
        existingId: Long? = null,
        name: String,
        iconName: String,
        latitude: Double,
        longitude: Double,
        defaultRadiusMeters: Float
    ) {
        viewModelScope.launch {
            val entity = FavoriteLocationEntity(
                id = existingId ?: 0L,
                name = name.trim(),
                iconName = iconName,
                latitude = latitude,
                longitude = longitude,
                defaultRadiusMeters = defaultRadiusMeters
            )
            dao.insertFavorite(entity)
        }
    }

    fun deleteFavorite(favorite: FavoriteLocationEntity) {
        viewModelScope.launch {
            dao.deleteFavorite(favorite)
        }
    }
}

package com.ennogrensemann.reminder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun observeActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY completedAt DESC, createdAt DESC")
    fun observeCompletedReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND (isRecurring = 1 OR hasTriggered = 0)")
    suspend fun getArmableReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE isCompleted = 1")
    suspend fun deleteAllCompletedReminders()

    // Favorites
    @Query("SELECT * FROM favorite_locations ORDER BY createdAt ASC")
    fun observeFavorites(): Flow<List<FavoriteLocationEntity>>

    @Query("SELECT * FROM favorite_locations ORDER BY createdAt ASC")
    suspend fun getAllFavorites(): List<FavoriteLocationEntity>

    @Query("SELECT * FROM favorite_locations WHERE id = :id LIMIT 1")
    suspend fun getFavoriteById(id: Long): FavoriteLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteLocationEntity): Long

    @Update
    suspend fun updateFavorite(favorite: FavoriteLocationEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteLocationEntity)
}

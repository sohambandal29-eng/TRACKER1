package com.example.tracker.data.local.dao

import androidx.room.*
import com.example.tracker.data.local.entities.TimetableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable WHERE userId = :userId AND isDeleted = 0 ORDER BY day, orderIndex ASC")
    fun getAllTimetableItems(userId: String): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable WHERE userId = :userId AND isDeleted = 0")
    suspend fun getAllTimetableItemsSync(userId: String): List<TimetableEntity>

    @Query("SELECT * FROM timetable WHERE userId = :userId AND day = :day AND isDeleted = 0 ORDER BY orderIndex ASC")
    fun getTimetableByDay(userId: String, day: String): Flow<List<TimetableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableItem(item: TimetableEntity): Long

    @Update
    suspend fun updateTimetableItem(item: TimetableEntity)

    @Delete
    suspend fun deleteTimetableItem(item: TimetableEntity)

    @Query("DELETE FROM timetable WHERE userId = :userId")
    suspend fun clearAll(userId: String)

    @Query("SELECT * FROM timetable WHERE userId = :userId AND remoteId = :remoteId LIMIT 1")
    suspend fun findTimetableByRemoteId(userId: String, remoteId: String): TimetableEntity?

    @Query("SELECT * FROM timetable WHERE userId = :userId AND lastSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedTimetableItems(userId: String): List<TimetableEntity>

    @Query("SELECT * FROM timetable WHERE userId = :userId AND isDeleted = 1")
    suspend fun getDeletedTimetableItems(userId: String): List<TimetableEntity>

    @Delete
    suspend fun permanentlyDeleteTimetableItem(item: TimetableEntity)
}

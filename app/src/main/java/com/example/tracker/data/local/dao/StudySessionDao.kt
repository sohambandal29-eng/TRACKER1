package com.example.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.tracker.data.local.entities.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Insert
    suspend fun insertSession(session: StudySessionEntity): Long

    @Query("UPDATE study_sessions SET durationSeconds = :duration, endTime = :endTime, lastSynced = 0 WHERE id = :id")
    suspend fun updateSessionDuration(id: Long, duration: Long, endTime: Long)

    @Query("SELECT * FROM study_sessions WHERE userId = :userId AND startTime >= :startOfDay AND isDeleted = 0 ORDER BY startTime ASC")
    fun getSessionsForDay(userId: String, startOfDay: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE userId = :userId AND isDeleted = 0 ORDER BY startTime DESC")
    fun getAllSessions(userId: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE userId = :userId AND isDeleted = 0")
    suspend fun getAllSessionsSync(userId: String): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE userId = :userId AND remoteId = :remoteId LIMIT 1")
    suspend fun findSessionByRemoteId(userId: String, remoteId: String): StudySessionEntity?

    @Query("SELECT * FROM study_sessions WHERE userId = :userId AND lastSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedSessions(userId: String): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE userId = :userId AND isDeleted = 1")
    suspend fun getDeletedSessions(userId: String): List<StudySessionEntity>

    @Delete
    suspend fun permanentlyDeleteSession(session: StudySessionEntity)

    @Update
    suspend fun updateSession(session: StudySessionEntity)

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE userId = :userId AND startTime >= :start AND isDeleted = 0")
    fun getStudyTimeSum(userId: String, start: Long): Flow<Long?>

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE userId = :userId AND startTime >= :start AND startTime < :end AND isDeleted = 0")
    fun getStudyTimeSumBetween(userId: String, start: Long, end: Long): Flow<Long?>

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE userId = :userId AND isDeleted = 0")
    fun getTotalStudyTime(userId: String): Flow<Long?>

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE userId = :userId AND taskId = :taskId AND startTime >= :start AND isDeleted = 0")
    fun getTaskStudyTimeSum(userId: String, taskId: Long, start: Long): Flow<Long?>

    @Query("SELECT CAST((startTime + :offset - 21600000) / 86400000 AS INTEGER) as day FROM study_sessions WHERE userId = :userId AND isDeleted = 0 GROUP BY day HAVING SUM(durationSeconds) >= 7200 ORDER BY day DESC")
    fun getDistinctStudyDays(userId: String, offset: Long): Flow<List<Int>>

    @Query("SELECT CAST((startTime + :offset - 21600000) / 86400000 AS INTEGER) as day, SUM(durationSeconds) as totalTime FROM study_sessions WHERE userId = :userId AND startTime >= :start AND isDeleted = 0 GROUP BY day ORDER BY day ASC")
    fun getDailyStudyTime(userId: String, start: Long, offset: Long): Flow<List<DailyStudySessionTime>>
}

data class DailyStudySessionTime(
    val day: Int,
    val totalTime: Long
)

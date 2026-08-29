package com.example.tracker.data.local.dao

import androidx.room.*
import com.example.tracker.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC")
    fun getAllTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDeleted = 0")
    suspend fun getAllTasksSync(userId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND date >= :startOfDay AND isDeleted = 0")
    fun getTasksForToday(userId: String, startOfDay: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE userId = :userId AND id = :id")
    suspend fun getTaskById(userId: String, id: Long): TaskEntity?

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT SUM(timeSpentSeconds) FROM tasks WHERE userId = :userId")
    fun getTotalStudyTime(userId: String): Flow<Long?>

    @Query("SELECT SUM(timeSpentSeconds) FROM tasks WHERE userId = :userId AND date >= :start")
    fun getStudyTimeSum(userId: String, start: Long): Flow<Long?>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND title = :title AND date >= :startOfDay AND isDeleted = 0 LIMIT 1")
    suspend fun findTaskByTitleForToday(userId: String, title: String, startOfDay: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE userId = :userId AND date >= :start AND date < :end AND isDeleted = 0")
    suspend fun getTasksInTimeRange(userId: String, start: Long, end: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND remoteId = :remoteId LIMIT 1")
    suspend fun findTaskByRemoteId(userId: String, remoteId: String): TaskEntity?

    @Query("SELECT MAX(date) FROM tasks WHERE userId = :userId AND date < :todayStart")
    suspend fun getLastTaskDateBefore(userId: String, todayStart: Long): Long?

    @Query("SELECT SUM(timeSpentSeconds) FROM tasks WHERE userId = :userId AND date >= :start AND date < :end")
    fun getStudyTimeSumBetween(userId: String, start: Long, end: Long): Flow<Long?>

    @Query("SELECT CAST((date + :offset - 21600000) / 86400000 AS INTEGER) as day FROM tasks WHERE userId = :userId AND isDeleted = 0 GROUP BY day HAVING SUM(timeSpentSeconds) >= 7200 ORDER BY day DESC")
    fun getDistinctStudyDays(userId: String, offset: Long): Flow<List<Int>>

    @Query("SELECT category, SUM(timeSpentSeconds) as totalTime, SUM(targetMinutes * 60) as targetTime FROM tasks WHERE userId = :userId AND date >= :start GROUP BY category")
    fun getTimePerCategory(userId: String, start: Long): Flow<List<CategoryTime>>

    @Query("SELECT CAST((date + :offset - 21600000) / 86400000 AS INTEGER) as date, SUM(timeSpentSeconds) as totalTime FROM tasks WHERE userId = :userId AND date >= :start GROUP BY date ORDER BY date ASC")
    fun getDailyStudyTime(userId: String, start: Long, offset: Long): Flow<List<DailyTime>>

    @Query("SELECT (date + :offset - 21600000) / 86400000 as day, COUNT(*) as totalTasks, SUM(CASE WHEN isCompleted = 1 OR (targetMinutes > 0 AND timeSpentSeconds >= targetMinutes * 60) THEN 1 ELSE 0 END) as completedTasks FROM tasks WHERE userId = :userId AND isDeleted = 0 GROUP BY day")
    fun getDailyTaskStats(userId: String, offset: Long): Flow<List<DailyTaskStatus>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND lastSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedTasks(userId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDeleted = 1")
    suspend fun getDeletedTasks(userId: String): List<TaskEntity>

    @Delete
    suspend fun permanentlyDeleteTask(task: TaskEntity)
}

data class CategoryTime(
    val category: String,
    val totalTime: Long,
    val targetTime: Long
)

data class DailyTime(
    val date: Long,
    val totalTime: Long
)

data class DailyTaskTime(
    val day: Int,
    val totalTime: Long
)

data class DailyTaskStatus(
    val day: Long,
    val totalTasks: Int,
    val completedTasks: Int
)

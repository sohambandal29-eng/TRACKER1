package com.example.tracker.data.local.dao

import androidx.room.*
import com.example.tracker.data.local.entities.SubTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubTaskDao {
    @Query("SELECT * FROM subtasks WHERE userId = :userId AND taskId = :taskId AND isDeleted = 0")
    fun getSubTasksForTask(userId: String, taskId: Long): Flow<List<SubTaskEntity>>

    @Query("SELECT * FROM subtasks WHERE userId = :userId AND taskId = :taskId AND isDeleted = 0")
    suspend fun getSubTasksForTaskSync(userId: String, taskId: Long): List<SubTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity)

    @Update
    suspend fun updateSubTask(subTask: SubTaskEntity)

    @Delete
    suspend fun deleteSubTask(subTask: SubTaskEntity)

    @Query("SELECT * FROM subtasks WHERE userId = :userId AND remoteId = :remoteId LIMIT 1")
    suspend fun findSubTaskByRemoteId(userId: String, remoteId: String): SubTaskEntity?

    @Query("SELECT * FROM subtasks WHERE userId = :userId AND lastSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedSubTasks(userId: String): List<SubTaskEntity>

    @Query("SELECT * FROM subtasks WHERE userId = :userId AND isDeleted = 1")
    suspend fun getDeletedSubTasks(userId: String): List<SubTaskEntity>

    @Delete
    suspend fun permanentlyDeleteSubTask(subTask: SubTaskEntity)
}

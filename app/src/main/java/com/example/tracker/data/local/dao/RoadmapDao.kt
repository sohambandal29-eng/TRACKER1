package com.example.tracker.data.local.dao

import androidx.room.*
import com.example.tracker.data.local.entities.RoadmapHeaderEntity
import com.example.tracker.data.local.entities.RoadmapStageEntity
import com.example.tracker.data.local.entities.TimelineItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoadmapDao {
    @Query("SELECT * FROM roadmap_stages WHERE userId = :userId AND isDeleted = 0 ORDER BY `order` ASC")
    fun getAllStages(userId: String): Flow<List<RoadmapStageEntity>>

    @Query("SELECT * FROM roadmap_stages WHERE userId = :userId AND isDeleted = 0 ORDER BY `order` ASC")
    suspend fun getAllStagesSync(userId: String): List<RoadmapStageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStage(stage: RoadmapStageEntity)

    @Update
    suspend fun updateStage(stage: RoadmapStageEntity)

    @Update
    suspend fun updateStages(stages: List<RoadmapStageEntity>)

    @Delete
    suspend fun deleteStage(stage: RoadmapStageEntity)

    @Query("SELECT * FROM timeline_items WHERE userId = :userId AND isDeleted = 0 ORDER BY `order` ASC")
    fun getAllTimelineItems(userId: String): Flow<List<TimelineItemEntity>>

    @Query("SELECT * FROM timeline_items WHERE userId = :userId AND isDeleted = 0")
    suspend fun getAllTimelineItemsSync(userId: String): List<TimelineItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelineItem(item: TimelineItemEntity)

    @Update
    suspend fun updateTimelineItem(item: TimelineItemEntity)

    @Update
    suspend fun updateTimelineItems(items: List<TimelineItemEntity>)

    @Delete
    suspend fun deleteTimelineItem(item: TimelineItemEntity)

    @Query("SELECT * FROM roadmap_headers WHERE userId = :userId")
    fun getAllHeaders(userId: String): Flow<List<RoadmapHeaderEntity>>

    @Query("SELECT * FROM roadmap_headers WHERE userId = :userId")
    suspend fun getAllHeadersSync(userId: String): List<RoadmapHeaderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: RoadmapHeaderEntity)

    @Query("DELETE FROM roadmap_stages WHERE userId = :userId")
    suspend fun deleteAllStages(userId: String)

    @Query("DELETE FROM timeline_items WHERE userId = :userId")
    suspend fun deleteAllTimelineItems(userId: String)

    @Query("DELETE FROM roadmap_headers WHERE userId = :userId")
    suspend fun deleteAllHeaders(userId: String)

    @Query("SELECT * FROM roadmap_stages WHERE userId = :userId AND remoteId = :remoteId LIMIT 1")
    suspend fun findStageByRemoteId(userId: String, remoteId: String): RoadmapStageEntity?

    @Query("SELECT * FROM timeline_items WHERE userId = :userId AND remoteId = :remoteId LIMIT 1")
    suspend fun findTimelineItemByRemoteId(userId: String, remoteId: String): TimelineItemEntity?

    @Query("SELECT * FROM roadmap_stages WHERE userId = :userId AND lastSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedStages(userId: String): List<RoadmapStageEntity>

    @Query("SELECT * FROM roadmap_stages WHERE userId = :userId AND isDeleted = 1")
    suspend fun getDeletedStages(userId: String): List<RoadmapStageEntity>

    @Delete
    suspend fun permanentlyDeleteStage(stage: RoadmapStageEntity)

    @Query("SELECT * FROM timeline_items WHERE userId = :userId AND lastSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedTimelineItems(userId: String): List<TimelineItemEntity>

    @Query("SELECT * FROM timeline_items WHERE userId = :userId AND isDeleted = 1")
    suspend fun getDeletedTimelineItems(userId: String): List<TimelineItemEntity>

    @Delete
    suspend fun permanentlyDeleteTimelineItem(item: TimelineItemEntity)
}

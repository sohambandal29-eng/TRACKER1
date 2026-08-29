package com.example.tracker.data.repository

import com.example.tracker.data.local.dao.RoadmapDao
import com.example.tracker.data.local.entities.RoadmapHeaderEntity
import com.example.tracker.data.local.entities.RoadmapStageEntity
import com.example.tracker.data.local.entities.TimelineItemEntity
import com.example.tracker.utils.FirebaseAuthManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
class RoadmapRepository(private val roadmapDao: RoadmapDao) {
    private val currentUserId: String
        get() = FirebaseAuthManager.getCurrentUserId() ?: ""

    val allStages: Flow<List<RoadmapStageEntity>> = FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
        if (userId != null) roadmapDao.getAllStages(userId) else flowOf(emptyList())
    }
    
    val allTimelineItems: Flow<List<TimelineItemEntity>> = FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
        if (userId != null) roadmapDao.getAllTimelineItems(userId) else flowOf(emptyList())
    }
    
    val allHeaders: Flow<List<RoadmapHeaderEntity>> = FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
        if (userId != null) roadmapDao.getAllHeaders(userId) else flowOf(emptyList())
    }

    suspend fun insertStage(stage: RoadmapStageEntity) {
        roadmapDao.insertStage(stage.copy(userId = currentUserId, lastSynced = 0))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun updateStage(stage: RoadmapStageEntity) {
        roadmapDao.updateStage(stage.copy(lastSynced = 0))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun updateStages(stages: List<RoadmapStageEntity>) {
        roadmapDao.updateStages(stages.map { it.copy(lastSynced = 0) })
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun deleteStage(stage: RoadmapStageEntity) {
        roadmapDao.updateStage(stage.copy(isDeleted = true, lastSynced = 0))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun insertTimelineItem(item: TimelineItemEntity) {
        roadmapDao.insertTimelineItem(item.copy(userId = currentUserId, lastSynced = 0))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun updateTimelineItem(item: TimelineItemEntity) {
        roadmapDao.updateTimelineItem(item.copy(lastSynced = 0))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun updateTimelineItems(items: List<TimelineItemEntity>) {
        roadmapDao.updateTimelineItems(items.map { it.copy(lastSynced = 0) })
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun deleteTimelineItem(item: TimelineItemEntity) {
        roadmapDao.updateTimelineItem(item.copy(isDeleted = true, lastSynced = 0))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun insertHeader(header: RoadmapHeaderEntity) {
        roadmapDao.insertHeader(header.copy(userId = currentUserId))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun clearRoadmap() {
        val userId = currentUserId
        if (userId.isNotEmpty()) {
            try {
                val db = Firebase.firestore
                val userRef = db.collection("users").document(userId)
                
                // Clear remote stages
                val stages = userRef.collection("roadmap_stages").get().await()
                stages.documents.forEach { it.reference.delete().await() }
                
                // Clear remote timeline items
                val items = userRef.collection("timeline_items").get().await()
                items.documents.forEach { it.reference.delete().await() }

                // Clear remote headers
                val headers = userRef.collection("roadmap_headers").get().await()
                headers.documents.forEach { it.reference.delete().await() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            roadmapDao.deleteAllStages(userId)
            roadmapDao.deleteAllTimelineItems(userId)
            roadmapDao.deleteAllHeaders(userId)
        }
    }
}

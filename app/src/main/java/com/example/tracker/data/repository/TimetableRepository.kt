package com.example.tracker.data.repository

import com.example.tracker.data.local.dao.TimetableDao
import com.example.tracker.data.local.entities.TimetableEntity
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
class TimetableRepository(private val timetableDao: TimetableDao) {
    private val currentUserId: String
        get() = FirebaseAuthManager.getCurrentUserId() ?: ""

    fun getAllTimetableItems(): Flow<List<TimetableEntity>> = 
        FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) timetableDao.getAllTimetableItems(userId) else flowOf(emptyList())
        }

    fun getTimetableByDay(day: String): Flow<List<TimetableEntity>> = 
        FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) timetableDao.getTimetableByDay(userId, day) else flowOf(emptyList())
        }

    suspend fun insertItem(item: TimetableEntity): Int {
        val id = timetableDao.insertTimetableItem(item.copy(userId = currentUserId, lastSynced = 0))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
        return id.toInt()
    }

    suspend fun updateItem(item: TimetableEntity) {
        timetableDao.updateTimetableItem(item.copy(lastSynced = 0))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun deleteItem(item: TimetableEntity) {
        timetableDao.updateTimetableItem(item.copy(isDeleted = true, lastSynced = 0))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun clearAll() {
        val userId = currentUserId
        if (userId.isNotEmpty()) {
            val items = timetableDao.getAllTimetableItemsSync(userId)
            items.forEach { item ->
                timetableDao.updateTimetableItem(item.copy(isDeleted = true, lastSynced = 0))
            }
            SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
        }
    }
}

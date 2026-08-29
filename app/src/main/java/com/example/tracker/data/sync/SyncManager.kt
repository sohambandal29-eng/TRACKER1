package com.example.tracker.data.sync

import android.content.Context
import android.util.Log
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.data.local.entities.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncManager(private val context: Context) {
    private val db = Firebase.firestore
    private val database = AppDatabase.getDatabase(context)
    private val TAG = "SyncManager"

    companion object {
        private val syncMutex = Mutex()
    }

    suspend fun syncAll(forceRefresh: Boolean = false) = syncMutex.withLock {
        val userId = Firebase.auth.currentUser?.uid ?: return@withLock
        Log.d(TAG, "Starting full sync for user: $userId (forceRefresh: $forceRefresh)")
        
        try {
            uploadLocalChangesInternal(userId)
            downloadRemoteChanges(userId, forceRefresh)
            Log.d(TAG, "Full sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            throw e
        }
    }

    suspend fun uploadLocalChanges(userId: String) = syncMutex.withLock {
        uploadLocalChangesInternal(userId)
    }

    private suspend fun uploadLocalChangesInternal(userId: String) {
        val taskDao = database.taskDao()
        val subTaskDao = database.subTaskDao()
        val timetableDao = database.timetableDao()
        val roadmapDao = database.roadmapDao()
        val sessionDao = database.studySessionDao()
        val blockedAppDao = database.blockedAppDao()
        val ruleDao = database.consistencyRuleDao()

        // 1. Sync Tasks
        taskDao.getUnsyncedTasks(userId).forEach { task ->
            val docRef = if (task.remoteId != null) {
                db.collection("users").document(userId).collection("tasks").document(task.remoteId)
            } else {
                db.collection("users").document(userId).collection("tasks").document()
            }
            val taskToSync = task.copy(remoteId = docRef.id, lastSynced = System.currentTimeMillis())
            docRef.set(taskToSync).await()
            taskDao.updateTask(taskToSync)
        }

        // 2. Sync Subtasks
        subTaskDao.getUnsyncedSubTasks(userId).forEach { sub ->
            val parentTask = taskDao.getAllTasksSync(userId).find { it.id == sub.taskId }
            if (parentTask?.remoteId != null) {
                val docRef = if (sub.remoteId != null) {
                    db.collection("users").document(userId).collection("tasks").document(parentTask.remoteId).collection("subtasks").document(sub.remoteId)
                } else {
                    db.collection("users").document(userId).collection("tasks").document(parentTask.remoteId).collection("subtasks").document()
                }
                val subToSync = sub.copy(remoteId = docRef.id, lastSynced = System.currentTimeMillis())
                docRef.set(subToSync).await()
                subTaskDao.updateSubTask(subToSync)
            }
        }

        // 3. Sync Timetable
        timetableDao.getUnsyncedTimetableItems(userId).forEach { item ->
            val docRef = if (item.remoteId != null) {
                db.collection("users").document(userId).collection("timetable").document(item.remoteId)
            } else {
                db.collection("users").document(userId).collection("timetable").document()
            }
            val toSync = item.copy(remoteId = docRef.id, lastSynced = System.currentTimeMillis())
            docRef.set(toSync).await()
            timetableDao.updateTimetableItem(toSync)
        }

        // 4. Sync Roadmap Stages
        roadmapDao.getUnsyncedStages(userId).forEach { stage ->
            val docRef = if (stage.remoteId != null) {
                db.collection("users").document(userId).collection("roadmap_stages").document(stage.remoteId)
            } else {
                db.collection("users").document(userId).collection("roadmap_stages").document()
            }
            val toSync = stage.copy(remoteId = docRef.id, lastSynced = System.currentTimeMillis())
            docRef.set(toSync).await()
            roadmapDao.updateStage(toSync)
        }

        // 5. Sync Timeline Items
        roadmapDao.getUnsyncedTimelineItems(userId).forEach { item ->
            val docRef = if (item.remoteId != null) {
                db.collection("users").document(userId).collection("timeline_items").document(item.remoteId)
            } else {
                db.collection("users").document(userId).collection("timeline_items").document()
            }
            val toSync = item.copy(remoteId = docRef.id, lastSynced = System.currentTimeMillis())
            docRef.set(toSync).await()
            roadmapDao.updateTimelineItem(toSync)
        }

        // 6. Sync Study Sessions
        sessionDao.getUnsyncedSessions(userId).forEach { session ->
            val docRef = if (session.remoteId != null) {
                db.collection("users").document(userId).collection("study_sessions").document(session.remoteId)
            } else {
                db.collection("users").document(userId).collection("study_sessions").document()
            }
            val toSync = session.copy(remoteId = docRef.id, lastSynced = System.currentTimeMillis())
            docRef.set(toSync).await()
            sessionDao.updateSession(toSync)
        }

        // 7. Sync Blocked Apps (Only sync unsynced ones)
        blockedAppDao.getUnsyncedBlockedApps(userId).forEach { app ->
            val toSync = app.copy(isSynced = true)
            db.collection("users").document(userId).collection("blocked_apps").document(app.packageName).set(toSync).await()
            blockedAppDao.insert(toSync)
        }

        // 8. Sync Consistency Rules
        ruleDao.getAllRules(userId).first().forEach { rule ->
            db.collection("users").document(userId).collection("consistency_rules").document(rule.id.toString()).set(rule).await()
        }

        // --- HANDLE DELETIONS ---
        
        taskDao.getDeletedTasks(userId).forEach { task ->
            if (task.remoteId != null) {
                db.collection("users").document(userId).collection("tasks").document(task.remoteId).delete().await()
            }
            taskDao.permanentlyDeleteTask(task)
        }

        subTaskDao.getDeletedSubTasks(userId).forEach { sub ->
            if (sub.remoteId != null) {
                val parentTask = taskDao.getAllTasksSync(userId).find { it.id == sub.taskId }
                if (parentTask?.remoteId != null) {
                    db.collection("users").document(userId).collection("tasks").document(parentTask.remoteId).collection("subtasks").document(sub.remoteId).delete().await()
                }
            }
            subTaskDao.permanentlyDeleteSubTask(sub)
        }

        timetableDao.getDeletedTimetableItems(userId).forEach { item ->
            if (item.remoteId != null) {
                db.collection("users").document(userId).collection("timetable").document(item.remoteId).delete().await()
            }
            timetableDao.permanentlyDeleteTimetableItem(item)
        }

        roadmapDao.getDeletedStages(userId).forEach { stage ->
            if (stage.remoteId != null) {
                db.collection("users").document(userId).collection("roadmap_stages").document(stage.remoteId).delete().await()
            }
            roadmapDao.permanentlyDeleteStage(stage)
        }

        roadmapDao.getDeletedTimelineItems(userId).forEach { item ->
            if (item.remoteId != null) {
                db.collection("users").document(userId).collection("timeline_items").document(item.remoteId).delete().await()
            }
            roadmapDao.permanentlyDeleteTimelineItem(item)
        }

        sessionDao.getDeletedSessions(userId).forEach { session ->
            if (session.remoteId != null) {
                db.collection("users").document(userId).collection("study_sessions").document(session.remoteId).delete().await()
            }
            sessionDao.permanentlyDeleteSession(session)
        }
    }

    suspend fun downloadRemoteChanges(userId: String, forceRefresh: Boolean = false) {
        val source = if (forceRefresh) Source.SERVER else Source.DEFAULT
        Log.d(TAG, "Downloading remote changes for $userId using source $source")
        
        // Restore Tasks
        val tasksSnapshot = db.collection("users").document(userId).collection("tasks").get(source).await()
        Log.d(TAG, "Fetched ${tasksSnapshot.size()} tasks")
        
        val remoteTaskRemoteIds = tasksSnapshot.documents.map { it.id }.toSet()
        if (!tasksSnapshot.metadata.isFromCache) {
            val localTasks = database.taskDao().getAllTasksSync(userId)
            localTasks.forEach { local ->
                if (local.remoteId != null && local.remoteId !in remoteTaskRemoteIds) {
                    database.taskDao().permanentlyDeleteTask(local)
                }
            }
        }

        tasksSnapshot.documents.forEach { doc ->
            val remote = try {
                doc.toObject(TaskEntity::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse Task document ${doc.id}", e)
                null
            } ?: return@forEach
            
            val rid = remote.remoteId ?: doc.id
            val local = database.taskDao().findTaskByRemoteId(userId, rid)
            val savedId = if (local == null) {
                database.taskDao().insertTask(remote.copy(id = 0, userId = userId, remoteId = rid, lastSynced = System.currentTimeMillis()))
            } else {
                database.taskDao().updateTask(remote.copy(id = local.id, userId = userId, remoteId = rid, lastSynced = System.currentTimeMillis()))
                local.id
            }

            // Restore Subtasks
            val subSnapshot = doc.reference.collection("subtasks").get(source).await()
            Log.d(TAG, "Fetched ${subSnapshot.size()} subtasks for task $rid")
            subSnapshot.documents.forEach { subDoc ->
                val rSub = try {
                    subDoc.toObject(SubTaskEntity::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse SubTask document ${subDoc.id}", e)
                    null
                } ?: return@forEach
                
                val sRid = rSub.remoteId ?: subDoc.id
                val lSub = database.subTaskDao().findSubTaskByRemoteId(userId, sRid)
                if (lSub == null) {
                    database.subTaskDao().insertSubTask(rSub.copy(id = 0, userId = userId, taskId = savedId, remoteId = sRid, lastSynced = System.currentTimeMillis()))
                } else {
                    database.subTaskDao().updateSubTask(rSub.copy(id = lSub.id, userId = userId, taskId = savedId, remoteId = sRid, lastSynced = System.currentTimeMillis()))
                }
            }
        }

        // Restore Timetable
        val ttSnapshot = db.collection("users").document(userId).collection("timetable").get(source).await()
        Log.d(TAG, "Fetched ${ttSnapshot.size()} timetable items")
        
        val remoteTimetableRemoteIds = ttSnapshot.documents.map { it.id }.toSet()
        if (!ttSnapshot.metadata.isFromCache) {
            val localTimetable = database.timetableDao().getAllTimetableItemsSync(userId)
            localTimetable.forEach { local ->
                if (local.remoteId != null && local.remoteId !in remoteTimetableRemoteIds) {
                    database.timetableDao().permanentlyDeleteTimetableItem(local)
                }
            }
        }

        ttSnapshot.documents.forEach { doc ->
            val remote = try {
                doc.toObject(TimetableEntity::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse Timetable document ${doc.id}", e)
                null
            } ?: return@forEach
            
            val rid = remote.remoteId ?: doc.id
            val local = database.timetableDao().findTimetableByRemoteId(userId, rid)
            if (local == null) database.timetableDao().insertTimetableItem(remote.copy(id = 0, userId = userId, remoteId = rid, lastSynced = System.currentTimeMillis()))
            else database.timetableDao().updateTimetableItem(remote.copy(id = local.id, userId = userId, remoteId = rid, lastSynced = System.currentTimeMillis()))
        }

        // Restore Roadmap Stages
        val rsSnapshot = db.collection("users").document(userId).collection("roadmap_stages").get(source).await()
        Log.d(TAG, "Fetched ${rsSnapshot.size()} roadmap stages")
        rsSnapshot.documents.forEach { doc ->
            val remote = try {
                doc.toObject(RoadmapStageEntity::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse RoadmapStage document ${doc.id}", e)
                null
            } ?: return@forEach
            
            val rid = remote.remoteId ?: doc.id
            val local = database.roadmapDao().findStageByRemoteId(userId, rid)
            if (local == null) database.roadmapDao().insertStage(remote.copy(id = 0, userId = userId, remoteId = rid, lastSynced = System.currentTimeMillis()))
            else database.roadmapDao().updateStage(remote.copy(id = local.id, userId = userId, remoteId = rid, lastSynced = System.currentTimeMillis()))
        }

        // Restore Timeline Items
        val tiSnapshot = db.collection("users").document(userId).collection("timeline_items").get(source).await()
        Log.d(TAG, "Fetched ${tiSnapshot.size()} timeline items")
        tiSnapshot.documents.forEach { doc ->
            val remote = try {
                doc.toObject(TimelineItemEntity::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse TimelineItem document ${doc.id}", e)
                null
            } ?: return@forEach
            
            val rid = remote.remoteId ?: doc.id
            val local = database.roadmapDao().findTimelineItemByRemoteId(userId, rid)
            if (local == null) database.roadmapDao().insertTimelineItem(remote.copy(id = 0, userId = userId, remoteId = rid, lastSynced = System.currentTimeMillis()))
            else database.roadmapDao().updateTimelineItem(remote.copy(id = local.id, userId = userId, remoteId = rid, lastSynced = System.currentTimeMillis()))
        }

        // Restore Study Sessions
        val ssSnapshot = db.collection("users").document(userId).collection("study_sessions").get(source).await()
        Log.d(TAG, "Fetched ${ssSnapshot.size()} study sessions")
        
        val remoteSessionRemoteIds = ssSnapshot.documents.map { it.id }.toSet()
        if (!ssSnapshot.metadata.isFromCache) {
            val localSessions = database.studySessionDao().getAllSessionsSync(userId)
            localSessions.forEach { local ->
                if (local.remoteId != null && local.remoteId !in remoteSessionRemoteIds) {
                    database.studySessionDao().permanentlyDeleteSession(local)
                }
            }
        }

        ssSnapshot.documents.forEach { doc ->
            Log.d(TAG, "StudySession document ${doc.id} data: ${doc.data}")
            val remote = try {
                doc.toObject(StudySessionEntity::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse StudySession document ${doc.id}", e)
                null
            } ?: return@forEach

            val rid = remote.remoteId ?: doc.id
            val local = database.studySessionDao().findSessionByRemoteId(userId, rid)
            if (local == null) database.studySessionDao().insertSession(remote.copy(id = 0, userId = userId, remoteId = rid, lastSynced = System.currentTimeMillis()))
            else database.studySessionDao().updateSession(remote.copy(id = local.id, userId = userId, remoteId = rid, lastSynced = System.currentTimeMillis()))
        }

        // Restore Headers (Roadmap titles)
        val headSnapshot = db.collection("users").document(userId).collection("roadmap_headers").get(source).await()
        headSnapshot.toObjects(RoadmapHeaderEntity::class.java).forEach {
            database.roadmapDao().insertHeader(it.copy(userId = userId))
        }

        // Restore Blocked Apps (with Remote Deletion Handling)
        val blockedSnapshot = db.collection("users").document(userId).collection("blocked_apps").get(source).await()
        val remoteApps = blockedSnapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(BlockedAppEntity::class.java)?.copy(packageName = doc.id, userId = userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing BlockedAppEntity for doc id: ${doc.id}", e)
                null
            }
        }
        val remotePackageNames = remoteApps.map { it.packageName }.toSet()
        
        // Delete local apps not present in remote (Mirror Cloud Deletion)
        // ONLY perform if data is from server to avoid accidental wipes due to empty/stale cache
        if (!blockedSnapshot.metadata.isFromCache) {
            val localApps = database.blockedAppDao().getAllBlockedApps(userId).first()
            localApps.forEach { local ->
                if (local.packageName !in remotePackageNames && local.isSynced) {
                    Log.d(TAG, "Mirror deletion: Removing ${local.packageName} as it is no longer on server")
                    database.blockedAppDao().delete(local)
                }
            }
        }
        
        // Update/Insert remote apps and mark as synced
        remoteApps.forEach { remote ->
            database.blockedAppDao().insert(remote.copy(isSynced = true))
        }

        // --- NEW: Sync Unblock Request Statuses ---
        // If an app is no longer in the blocked list, mark its requests as 'completed'
        try {
            val requestsSnapshot = db.collection("unblock_requests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "pending")
                .get(source).await()
            
            for (doc in requestsSnapshot.documents) {
                val pkgName = doc.getString("packageName")
                if (pkgName != null && pkgName !in remotePackageNames) {
                    Log.d(TAG, "Marking request for $pkgName as completed because it is no longer blocked")
                    doc.reference.update("status", "completed").await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync unblock request statuses", e)
        }

        // Restore Consistency Rules
        val rulesSnapshot = db.collection("users").document(userId).collection("consistency_rules").get(source).await()
        rulesSnapshot.toObjects(ConsistencyRuleEntity::class.java).forEach {
            database.consistencyRuleDao().insertRule(it.copy(userId = userId))
        }
    }
}

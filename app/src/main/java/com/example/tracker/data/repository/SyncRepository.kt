package com.example.tracker.data.repository

import android.content.Context
import androidx.work.*
import com.example.tracker.data.sync.SyncWorker
import java.util.concurrent.TimeUnit

import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class SyncRepository(private val context: Context) {
    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "FirestoreSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    suspend fun forceSync() {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            try {
                com.example.tracker.data.sync.SyncManager(context).uploadLocalChanges(userId)
            } catch (e: Exception) {
                android.util.Log.e("SyncRepository", "Force sync failed", e)
                // Fallback to background worker
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                WorkManager.getInstance(context).enqueue(syncRequest)
            }
        }
    }
}

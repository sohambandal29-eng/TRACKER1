package com.example.tracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tracker.utils.NotificationHelper

class TimetableNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val subject = inputData.getString("subject") ?: "Routine"
        val timing = inputData.getString("timing") ?: ""
        
        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.showNotification(
            "Upcoming Routine",
            "Starting in 5 minutes: $subject at $timing",
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        )

        return Result.success()
    }
}

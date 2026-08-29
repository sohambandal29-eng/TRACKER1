package com.example.tracker.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.utils.NotificationHelper
import com.example.tracker.data.local.UserPreferences
import com.example.tracker.utils.FirebaseAuthManager
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId

class StreakProtectionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return Result.success()
        Log.d("StreakProtectionWorker", "Starting streak protection check for user: $userId")
        
        val notificationHelper = NotificationHelper(applicationContext)
        val database = AppDatabase.getDatabase(applicationContext)
        val sessionDao = database.studySessionDao()
        val userPreferences = UserPreferences(applicationContext)
        val userName = userPreferences.userName.first() ?: "Scholar"

        val now = LocalDateTime.now(ZoneId.systemDefault())
        val trackerDayStart = if (now.hour < 6) now.minusDays(1).withHour(6).withMinute(0).withSecond(0).withNano(0) 
                             else now.withHour(6).withMinute(0).withSecond(0).withNano(0)
        val todayStart = trackerDayStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            val totalTimeSpent = sessionDao.getStudyTimeSum(userId, todayStart).first() ?: 0L
            val targetSeconds = 7200L // 2 hours

            if (totalTimeSpent < targetSeconds) {
                val remainingSeconds = targetSeconds - totalTimeSpent
                val remainingHours = remainingSeconds / 3600
                val remainingMinutes = (remainingSeconds % 3600) / 60
                
                val message = if (totalTimeSpent == 0L) {
                    "You haven't studied today! Your streak will break in a few hours. Study for 2 hours to protect it!"
                } else {
                    "Almost there, $userName! Just ${if (remainingHours > 0) "${remainingHours}h " else ""}${remainingMinutes}m more to save your streak."
                }

                notificationHelper.showNotification(
                    "Streak Protection Active!",
                    message,
                    106
                )
            }
        } catch (e: Exception) {
            Log.e("StreakProtectionWorker", "Error in StreakProtectionWorker", e)
            return Result.retry()
        }

        return Result.success()
    }
}

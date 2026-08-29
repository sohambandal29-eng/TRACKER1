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

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val type = inputData.getString("type") ?: return Result.failure()
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return Result.success()
        
        Log.d("ReminderWorker", "Starting work for type: $type for user: $userId")
        val notificationHelper = NotificationHelper(applicationContext)
        val database = AppDatabase.getDatabase(applicationContext)
        val taskDao = database.taskDao()
        val userPreferences = UserPreferences(applicationContext)
        val userName = userPreferences.userName.first() ?: "Scholar"

        val now = LocalDateTime.now(ZoneId.systemDefault())
        val trackerDayStart = if (now.hour < 6) now.minusDays(1).withHour(6).withMinute(0).withSecond(0).withNano(0) 
                             else now.withHour(6).withMinute(0).withSecond(0).withNano(0)
        val todayStart = trackerDayStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            when (type) {
                "MORNING" -> {
                    val prevDayStart = trackerDayStart.minusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val prevDayEnd = todayStart
                    val prevTasks = taskDao.getTasksInTimeRange(userId, prevDayStart, prevDayEnd)
                    val incompletePrev = prevTasks.filter { !it.isCompleted }

                    if (incompletePrev.isNotEmpty()) {
                        notificationHelper.showNotification(
                            "EMERGENCY: Incomplete Tasks!",
                            "You have ${incompletePrev.size} incomplete tasks from yesterday. Don't let them pile up!",
                            911
                        )
                    } else {
                        notificationHelper.showNotification(
                            "Good Morning, $userName!",
                            "Time to plan your study goals for today!",
                            101
                        )
                    }
                }
                "MID_DAY" -> {
                    val tasks = taskDao.getTasksForToday(userId, todayStart).first()
                    val pending = tasks.count { !it.isCompleted }
                    if (pending > 0) {
                        notificationHelper.showNotification(
                            "Keep Going, $userName!",
                            "You have $pending tasks left for today. Keep it up!",
                            102
                        )
                    }
                }
                "NIGHT" -> {
                    val tasks = taskDao.getTasksForToday(userId, todayStart).first()
                    val completed = tasks.count { it.isCompleted }
                    val total = tasks.size
                    if (total > 0) {
                        notificationHelper.showNotification(
                            "Day Recap, $userName",
                            "Great job! You completed $completed out of $total tasks today.",
                            103
                        )
                    } else {
                        notificationHelper.showNotification(
                            "Day Recap, $userName",
                            "End of the day! Don't forget to track your progress tomorrow.",
                            103
                        )
                    }
                }
                "STREAK_CHECK" -> {
                    val totalTimeSpent = taskDao.getStudyTimeSum(userId, todayStart).first() ?: 0L
                    if (totalTimeSpent < 7200) {
                        val remainingHours = (7200 - totalTimeSpent) / 3600
                        val remainingMinutes = ((7200 - totalTimeSpent) % 3600) / 60
                        
                        val message = if (totalTimeSpent == 0L) {
                            "Your daily streak is at risk! You need 2 hours of study time today."
                        } else {
                            "Almost there! You need ${if (remainingHours > 0) "${remainingHours}h " else ""}${remainingMinutes}m more to maintain your streak."
                        }

                        notificationHelper.showNotification(
                            "Streak Alert, $userName!",
                            message,
                            104
                        )
                    }
                }
                "INTERVAL_3H" -> {
                    val tasks = taskDao.getTasksForToday(userId, todayStart).first()
                    val pending = tasks.count { !it.isCompleted }
                    if (pending > 0) {
                        notificationHelper.showNotification(
                            "Time Check, $userName",
                            "Just a friendly reminder to keep moving! You have $pending tasks waiting for you.",
                            105
                        )
                    } else {
                        notificationHelper.showNotification(
                            "Keep it up, $userName",
                            "You're doing great! Take a short break or plan your next session.",
                            105
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ReminderWorker", "Error in doWork for $type", e)
            return Result.retry()
        }

        return Result.success()
    }
}

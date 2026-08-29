package com.example.tracker.data.repository

import com.example.tracker.data.local.dao.CategoryTime
import com.example.tracker.data.local.dao.DailyTaskStatus
import com.example.tracker.data.local.dao.DailyTime
import com.example.tracker.data.local.dao.ConsistencyRuleDao
import com.example.tracker.data.local.dao.DailyStudySessionTime
import com.example.tracker.data.local.dao.TaskDao
import com.example.tracker.data.local.entities.ConsistencyRuleEntity
import com.example.tracker.data.local.entities.TaskEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import com.example.tracker.data.repository.SyncRepository
import com.example.tracker.utils.FirebaseAuthManager
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepository(
    private val taskDao: TaskDao,
    private val sessionDao: com.example.tracker.data.local.dao.StudySessionDao,
    private val ruleDao: ConsistencyRuleDao
) {
    private val currentUserId: String
        get() = FirebaseAuthManager.getCurrentUserId() ?: ""

    val allTasks: Flow<List<TaskEntity>> = FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
        if (userId != null) taskDao.getAllTasks(userId) else flowOf(emptyList())
    }

    val allRules: Flow<List<ConsistencyRuleEntity>> = FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
        if (userId != null) ruleDao.getAllRules(userId) else flowOf(emptyList())
    }

    suspend fun initializeDefaultRules() {
        val userId = currentUserId
        if (userId.isNotEmpty() && ruleDao.getRuleCount(userId) == 0) {
            ruleDao.insertRule(ConsistencyRuleEntity(userId = userId, text = "STUDY MINIMUM 3 HOURS", isCompulsory = true))
            ruleDao.insertRule(ConsistencyRuleEntity(userId = userId, text = "30 MINUTES PHYSICAL ACTIVITY", isCompulsory = false))
            ruleDao.insertRule(ConsistencyRuleEntity(userId = userId, text = "NO WASTING TIME ON REELS / SHORTS", isCompulsory = false))
            ruleDao.insertRule(ConsistencyRuleEntity(userId = userId, text = "30 MIN REVISION BEFORE SLEEP", isCompulsory = false))
        }
    }

    suspend fun insertRule(rule: ConsistencyRuleEntity) = ruleDao.insertRule(rule.copy(userId = currentUserId))
    suspend fun updateRule(rule: ConsistencyRuleEntity) = ruleDao.updateRule(rule)
    suspend fun deleteRule(rule: ConsistencyRuleEntity) = ruleDao.deleteRule(rule)

    fun getTrackerDayStart(): Long {
        val now = java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
        val trackerDay = if (now.hour < 6) now.minusDays(1) else now
        return trackerDay.withHour(6).withMinute(0).withSecond(0).withNano(0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getTasksForToday(): Flow<List<TaskEntity>> {
        return FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) taskDao.getTasksForToday(userId, getTrackerDayStart()) else flowOf(emptyList())
        }
    }

    suspend fun insertTask(task: TaskEntity) {
        val userId = currentUserId
        taskDao.insertTask(task.copy(userId = userId, date = System.currentTimeMillis()))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task.copy(lastSynced = 0))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.updateTask(task.copy(isDeleted = true, lastSynced = 0))
        SyncRepository(com.example.tracker.TrackerApplication.instance).forceSync()
    }

    suspend fun initializeTasksForToday() {}

    fun getWeeklyStudyTime(): Flow<Long?> {
        return FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) {
                val now = java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
                val startOfWeek = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .withHour(6).withMinute(0).withSecond(0).withNano(0)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                
                sessionDao.getStudyTimeSum(userId, startOfWeek).map { it ?: 0L }
            } else flowOf(0L)
        }
    }

    fun getLastWeeklyStudyTime(): Flow<Long?> {
        return FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) {
                val now = java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
                val startOfThisWeek = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .withHour(6).withMinute(0).withSecond(0).withNano(0)
                val startOfLastWeek = startOfThisWeek.minusWeeks(1)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val endOfLastWeek = startOfThisWeek
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                
                sessionDao.getStudyTimeSumBetween(userId, startOfLastWeek, endOfLastWeek).map { it ?: 0L }
            } else flowOf(0L)
        }
    }

    fun getTodayStudyTime(): Flow<Long?> {
        return FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) {
                val start = getTrackerDayStart()
                sessionDao.getStudyTimeSum(userId, start).map { it ?: 0L }
            } else flowOf(0L)
        }
    }

    fun getTotalStudyTime(): Flow<Long> = FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
        if (userId != null) {
            sessionDao.getTotalStudyTime(userId).map { it ?: 0L }
        } else flowOf(0L)
    }

    fun getStreakCount(): Flow<Int> {
        val offset = ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()).totalSeconds * 1000L
        return FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) {
                sessionDao.getDailyStudyTime(userId, 0, offset).map { sessions ->
                    val combinedDays = mutableMapOf<Int, Long>()
                    sessions.forEach { combinedDays[it.day] = it.totalTime }
                    
                    val activeDays = combinedDays.filter { it.value >= 7200 } // 2 hours goal
                        .keys.sortedDescending()

                    if (activeDays.isEmpty()) return@map 0
                    
                    val now = System.currentTimeMillis()
                    val sixHours = 6 * 3600 * 1000L
                    val today = ((now + offset - sixHours) / 86400000L).toInt()
                    
                    var currentDay = today
                    var streak = 0
                    
                    for (day in activeDays) {
                        if (day == currentDay) {
                            streak++
                            currentDay--
                        } else if (day < currentDay) {
                            if (streak > 0) break // Streak broken
                            // If today is not finished yet, check if yesterday was active
                            if (day == today - 1) {
                                streak++
                                currentDay = today - 2
                            } else break
                        }
                    }
                    streak
                }
            } else flowOf(0)
        }
    }

    fun getTimePerCategory(start: Long): Flow<List<CategoryTime>> = FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
        if (userId != null) taskDao.getTimePerCategory(userId, start) else flowOf(emptyList())
    }

    fun getMonthlyStudyTime(): Flow<Long?> {
        return FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) {
                val now = java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
                val startOfMonth = now.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth())
                    .withHour(0).withMinute(0).withSecond(0).withNano(0)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                
                sessionDao.getStudyTimeSum(userId, startOfMonth).map { it ?: 0L }
            } else flowOf(0L)
        }
    }

    fun getMostStudiedDayOfWeek(): Flow<DayOfWeek?> {
        val offset = ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()).totalSeconds * 1000L
        return FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) {
                val now = java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
                val startOfWeek = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .withHour(6).withMinute(0).withSecond(0).withNano(0)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                sessionDao.getDailyStudyTime(userId, startOfWeek, offset).map { list ->
                    list.maxByOrNull { it.totalTime }?.let {
                        LocalDate.ofEpochDay(it.day.toLong()).dayOfWeek
                    }
                }
            } else flowOf(null)
        }
    }

    fun getWeeklyDailyTime(): Flow<List<DailyTime>> {
        val offset = ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()).totalSeconds * 1000L
        return FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) {
                val now = java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
                val startOfWeek = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .withHour(6).withMinute(0).withSecond(0).withNano(0)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                
                sessionDao.getDailyStudyTime(userId, startOfWeek, offset).map { sessionDaily ->
                    sessionDaily.map { 
                        DailyTime(it.day.toLong() * 86400000L - offset + 21600000L, it.totalTime)
                    }.sortedBy { it.date }
                }
            } else flowOf(emptyList())
        }
    }

    fun getYearlyDailyTime(): Flow<List<DailyTime>> {
        val offset = ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()).totalSeconds * 1000L
        return FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) {
                val now = java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
                val startOfYear = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                sessionDao.getDailyStudyTime(userId, startOfYear, offset).map { sessionDaily ->
                    sessionDaily.map { 
                        DailyTime(it.day.toLong() * 86400000L - offset + 21600000L, it.totalTime)
                    }.sortedBy { it.date }
                }
            } else flowOf(emptyList())
        }
    }

    fun getDailyTaskStats(): Flow<List<DailyTaskStatus>> {
        val offset = ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()).totalSeconds * 1000L
        return FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId != null) taskDao.getDailyTaskStats(userId, offset) else flowOf(emptyList())
        }
    }
}

package com.example.tracker.ui.screens.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.data.local.dao.CategoryTime
import com.example.tracker.data.local.dao.DailyTaskStatus
import com.example.tracker.data.local.dao.DailyTime
import com.example.tracker.data.local.entities.ConsistencyRuleEntity
import com.example.tracker.data.local.entities.TaskEntity
import com.example.tracker.data.repository.TaskRepository
import com.example.tracker.ui.screens.timer.TimerManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    val allTasks: StateFlow<List<TaskEntity>>
    val todayTasks: StateFlow<List<TaskEntity>>
    val todayStudyTime: StateFlow<Long>
    val weeklyStudyTime: StateFlow<Long>
    val streakCount: StateFlow<Int>
    val categoryDistribution: StateFlow<List<CategoryTime>>
    val weeklyDailyTime: StateFlow<List<DailyTime>>
    val yearlyDailyTime: StateFlow<List<DailyTime>>
    val totalStudyTime: StateFlow<Long>
    val lastWeeklyStudyTime: StateFlow<Long>
    val allRules: StateFlow<List<ConsistencyRuleEntity>>
    val dailyTaskStats: StateFlow<List<DailyTaskStatus>>
    val monthlyStudyTime: StateFlow<Long>
    val mostStudiedDay: StateFlow<java.time.DayOfWeek?>

    init {
        val db = AppDatabase.getDatabase(application)
        val taskDao = db.taskDao()
        val sessionDao = db.studySessionDao()
        val ruleDao = db.consistencyRuleDao()
        repository = TaskRepository(taskDao, sessionDao, ruleDao)
        viewModelScope.launch {
            repository.initializeDefaultRules()
        }
        allTasks = repository.allTasks.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        todayTasks = repository.getTasksForToday().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        todayStudyTime = combine(
            repository.getTodayStudyTime().map { it ?: 0L },
            TimerManager.currentSessionSeconds
        ) { dbTime, liveSeconds -> dbTime + liveSeconds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

        weeklyStudyTime = combine(
            repository.getWeeklyStudyTime().map { it ?: 0L },
            TimerManager.currentSessionSeconds
        ) { dbTime, liveSeconds -> dbTime + liveSeconds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

        lastWeeklyStudyTime = repository.getLastWeeklyStudyTime()
            .map { it ?: 0L }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

        streakCount = repository.getStreakCount().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )
        categoryDistribution = repository.getTimePerCategory(repository.getTrackerDayStart()).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        monthlyStudyTime = repository.getMonthlyStudyTime()
            .map { it ?: 0L }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

        mostStudiedDay = repository.getMostStudiedDayOfWeek()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        weeklyDailyTime = repository.getWeeklyDailyTime().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        yearlyDailyTime = repository.getYearlyDailyTime().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        totalStudyTime = combine(
            repository.getTotalStudyTime(),
            TimerManager.currentSessionSeconds
        ) { dbTime, liveSeconds -> dbTime + liveSeconds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
        allRules = repository.allRules.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        dailyTaskStats = repository.getDailyTaskStats().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        viewModelScope.launch {
            repository.initializeTasksForToday()
        }
    }

    fun addTask(title: String, category: String, priority: Int, targetMinutes: Long = 0) {
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    title = title,
                    category = category,
                    priority = priority,
                    targetMinutes = targetMinutes
                )
            )
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun addRule(text: String) {
        viewModelScope.launch {
            repository.insertRule(ConsistencyRuleEntity(text = text))
        }
    }

    fun updateRule(rule: ConsistencyRuleEntity) {
        viewModelScope.launch {
            repository.updateRule(rule)
        }
    }

    fun deleteRule(rule: ConsistencyRuleEntity) {
        if (!rule.isCompulsory) {
            viewModelScope.launch {
                repository.deleteRule(rule)
            }
        }
    }
}

package com.example.tracker.ui.screens.timer

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.data.local.entities.TaskEntity
import com.example.tracker.data.repository.TaskRepository
import com.example.tracker.ui.theme.PrimaryAccent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val studySessionDao = AppDatabase.getDatabase(application).studySessionDao()
    
    val todayTasks: StateFlow<List<TaskEntity>>
    val selectedTask = TimerManager.selectedTask
    val todayStudyTime: StateFlow<Long>
    val weeklyStudyTime: StateFlow<Long>
    val selectedTaskStudyTime: StateFlow<Long>
    val selectedSoundscape = TimerManager.selectedSoundscape
    val customSoundUri = TimerManager.customSoundUri
    val timeLeft = TimerManager.timeLeft
    val isRunning = TimerManager.isRunning
    val timerMode = TimerManager.timerMode
    val totalTimeSeconds = timerMode.map { mode ->
        TimerManager.getDurationForMode(mode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25 * 60L)
    val timerFinished = TimerManager.timerFinished
    val accentColor = TimerManager.accentColor
    
    private val userPreferences = com.example.tracker.data.local.UserPreferences(application)
    val strictMode = userPreferences.strictMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    init {
        val db = AppDatabase.getDatabase(application)
        val taskDao = db.taskDao()
        val sessionDao = db.studySessionDao()
        val ruleDao = db.consistencyRuleDao()
        repository = TaskRepository(taskDao, sessionDao, ruleDao)
        
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

        selectedTaskStudyTime = combine(
            selectedTask,
            TimerManager.currentSessionSeconds
        ) { task, unsavedSeconds ->
            (task?.timeSpentSeconds ?: 0L) + unsavedSeconds
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    }

    fun selectTask(task: TaskEntity?) {
        TimerManager.selectTask(task)
    }

    fun setSoundscape(soundscape: Soundscape) {
        TimerManager.setSoundscape(soundscape, getApplication())
    }

    fun setCustomSoundUri(uri: String?) {
        TimerManager.setCustomSoundUri(uri, getApplication())
    }

    fun startTimer() {
        if (timerMode.value == TimerMode.POMODORO && selectedTask.value == null) {
            android.widget.Toast.makeText(getApplication(), "Please select a task first!", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        TimerManager.startTimer(getApplication())
    }

    fun pauseTimer() {
        if (strictMode.value && timerMode.value == TimerMode.POMODORO && isRunning.value) {
            android.widget.Toast.makeText(getApplication(), "Strict Mode: Focus is locked!", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        TimerManager.pauseTimer(getApplication())
    }

    fun resetTimer() {
        if (strictMode.value && timerMode.value == TimerMode.POMODORO && isRunning.value) {
            android.widget.Toast.makeText(getApplication(), "Strict Mode: Cannot reset during focus!", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        TimerManager.resetTimer(getApplication())
    }

    fun setTimerMode(mode: TimerMode) {
        if (strictMode.value && timerMode.value == TimerMode.POMODORO && isRunning.value) {
            android.widget.Toast.makeText(getApplication(), "Strict Mode: Finish your session first!", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        TimerManager.setTimerMode(mode, getApplication())
    }

    fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }
}

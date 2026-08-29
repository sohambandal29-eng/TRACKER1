package com.example.tracker.ui.screens.timer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.ui.graphics.Color
import com.example.tracker.TrackerApplication
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.data.local.UserPreferences
import com.example.tracker.data.local.entities.TaskEntity
import com.example.tracker.data.local.entities.StudySessionEntity
import com.example.tracker.data.repository.TaskRepository
import com.example.tracker.data.repository.SyncRepository
import com.example.tracker.service.TimerService
import com.example.tracker.ui.theme.PrimaryAccent
import com.example.tracker.utils.FirebaseAuthManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class TimerMode {
    POMODORO, SHORT_BREAK, LONG_BREAK
}

enum class Soundscape(val title: String) {
    NONE("None"),
    CUSTOM("Custom")
}

object TimerManager {
    private val context: Context get() = TrackerApplication.instance
    private val db = AppDatabase.getDatabase(context)
    private val taskDao = db.taskDao()
    private val studySessionDao = db.studySessionDao()
    private val consistencyRuleDao = db.consistencyRuleDao()
    private val repository = TaskRepository(taskDao, studySessionDao, consistencyRuleDao)

    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _timeLeft = MutableStateFlow(25 * 60L)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _currentSessionSeconds = MutableStateFlow(0L)
    val currentSessionSeconds: StateFlow<Long> = _currentSessionSeconds.asStateFlow()

    private val _currentTaskUnsavedSeconds = MutableStateFlow(0L)
    val currentTaskUnsavedSeconds: StateFlow<Long> = _currentTaskUnsavedSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.POMODORO)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _selectedTask = MutableStateFlow<TaskEntity?>(null)
    val selectedTask: StateFlow<TaskEntity?> = _selectedTask.asStateFlow()

    private val _selectedSoundscape = MutableStateFlow(Soundscape.NONE)
    val selectedSoundscape: StateFlow<Soundscape> = _selectedSoundscape.asStateFlow()

    private val _customSoundUri = MutableStateFlow<String?>(null)
    val customSoundUri: StateFlow<String?> = _customSoundUri.asStateFlow()

    val accentColor: StateFlow<Color> = _selectedTask.map { task ->
        when (task?.category?.lowercase()) {
            "python" -> Color(0xFF4CAF50) // Green
            "mpsc" -> Color(0xFF2196F3) // Blue
            "dsa" -> Color(0xFFFF9800) // Orange
            "aptitude" -> Color(0xFFE91E63) // Pink
            else -> PrimaryAccent
        }
    }.stateIn(managerScope, SharingStarted.Eagerly, PrimaryAccent)

    private val _timerFinished = MutableSharedFlow<Unit>()
    val timerFinished = _timerFinished.asSharedFlow()

    private var timerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    
    private val userPrefs by lazy { UserPreferences(context) }
    
    private val _isStrictMode = MutableStateFlow(false)
    val isStrictMode: StateFlow<Boolean> = _isStrictMode.asStateFlow()

    private var lastSavedTime: Long = 25 * 60L
    private var sessionStartTimerValue: Long = 0
    private var sessionStartTime: Long = 0
    private var targetEndTime: Long = 0
    private val prefs by lazy { context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE) }

    init {
        loadState()
        managerScope.launch {
            userPrefs.strictMode.collect {
                _isStrictMode.value = it
            }
        }
    }

    fun getDurationForMode(mode: TimerMode): Long {
        return when (mode) {
            TimerMode.POMODORO -> prefs.getLong("pref_pomodoro_min", 25L) * 60L
            TimerMode.SHORT_BREAK -> prefs.getLong("pref_short_break_min", 5L) * 60L
            TimerMode.LONG_BREAK -> prefs.getLong("pref_long_break_min", 15L) * 60L
        }
    }

    fun setCustomDuration(mode: TimerMode, minutes: Long) {
        val key = when (mode) {
            TimerMode.POMODORO -> "pref_pomodoro_min"
            TimerMode.SHORT_BREAK -> "pref_short_break_min"
            TimerMode.LONG_BREAK -> "pref_long_break_min"
        }
        prefs.edit().putLong(key, minutes).apply()
        if (!_isRunning.value && _timerMode.value == mode) {
            _timeLeft.value = minutes * 60L
            lastSavedTime = _timeLeft.value
            sessionStartTimerValue = _timeLeft.value
        }
    }

    fun selectTask(task: TaskEntity?) {
        if (_selectedTask.value?.id == task?.id) return

        if (_isRunning.value && _timerMode.value == TimerMode.POMODORO) {
            val taskDuration = lastSavedTime - _timeLeft.value
            if (taskDuration > 0) saveProgress(taskDuration)
            
            val sessionDuration = sessionStartTimerValue - _timeLeft.value
            if (sessionDuration > 0) saveStudySession(sessionDuration)
        }
        
        _selectedTask.value = task
        
        // Reset timer to initial state when switching tasks
        timerJob?.cancel()
        _isRunning.value = false
        stopSound()
        _timeLeft.value = getDurationForMode(_timerMode.value)
        _currentSessionSeconds.value = 0
        _currentTaskUnsavedSeconds.value = 0
        lastSavedTime = _timeLeft.value
        sessionStartTimerValue = _timeLeft.value
        targetEndTime = 0
        
        saveState()
        stopService(context)
    }

    fun setSoundscape(soundscape: Soundscape, context: Context) {
        val previousSoundscape = _selectedSoundscape.value
        Log.d("TimerManager", "setSoundscape: $previousSoundscape -> $soundscape")
        _selectedSoundscape.value = soundscape
        
        if (previousSoundscape != soundscape || mediaPlayer == null) {
            stopSound()
            if (_isRunning.value) {
                playSound(context)
            }
        }
        saveState()
    }

    fun setCustomSoundUri(uri: String?, context: Context) {
        _customSoundUri.value = uri
        prefs.edit().putString("custom_sound_uri", uri).apply()
        // Always play when selected/updated to provide feedback
        if (_selectedSoundscape.value == Soundscape.CUSTOM) {
            stopSound()
            playSound(context)
        }
    }

    private fun playSound(context: Context) {
        Log.d("TimerManager", "playSound: ${_selectedSoundscape.value}")
        if (_selectedSoundscape.value != Soundscape.CUSTOM) return
        
        stopSound() 

        try {
            val uriString = _customSoundUri.value
            if (uriString != null) {
                val uri = Uri.parse(uriString)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, uri)
                    isLooping = true
                    setOnPreparedListener { it.start() }
                    setOnErrorListener { mp, what, extra ->
                        android.util.Log.e("TimerManager", "MediaPlayer error: $what, $extra")
                        false
                    }
                    prepareAsync()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerManager", "Error in playSound: ${e.message}", e)
        }
    }

    private fun stopSound() {
        Log.d("TimerManager", "stopSound")
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("TimerManager", "Error in stopSound", e)
        } finally {
            mediaPlayer = null
        }
    }

    fun startTimer(context: Context) {
        if (_isRunning.value && timerJob?.isActive == true) return
        
        // Prevent task-less starts in Pomodoro mode
        if (_timerMode.value == TimerMode.POMODORO && _selectedTask.value == null) {
            Log.w("TimerManager", "Cannot start Pomodoro without a selected task")
            return
        }
        
        if (!_isRunning.value) {
            _isRunning.value = true
            targetEndTime = System.currentTimeMillis() + _timeLeft.value * 1000
        }
        
        lastSavedTime = _timeLeft.value
        sessionStartTimerValue = _timeLeft.value
        sessionStartTime = System.currentTimeMillis()

        saveState()
        playSound(context)
        startService(context)

        timerJob?.cancel()
        timerJob = managerScope.launch {
            while (isActive && _isRunning.value && _timeLeft.value > 0) {
                val remaining = (targetEndTime - System.currentTimeMillis()) / 1000
                _timeLeft.value = maxOf(0, remaining)
                
                if (_timerMode.value == TimerMode.POMODORO) {
                    _currentSessionSeconds.value = (sessionStartTimerValue - _timeLeft.value).toInt().toLong()
                    _currentTaskUnsavedSeconds.value = (lastSavedTime - _timeLeft.value).toInt().toLong()
                } else {
                    _currentSessionSeconds.value = 0
                    _currentTaskUnsavedSeconds.value = 0
                }

                // Save task progress every 10 seconds locally (skip heavy sync)
                if (lastSavedTime - _timeLeft.value >= 10) {
                    val secondsToSave = lastSavedTime - _timeLeft.value
                    saveProgress(secondsToSave)
                    lastSavedTime = _timeLeft.value
                    saveState()
                }
                delay(1000)
            }
            if (_timeLeft.value <= 0 && _isRunning.value) {
                _isRunning.value = false
                stopSound()
                
                // Save remainder for task
                val taskDuration = lastSavedTime - _timeLeft.value
                if (taskDuration > 0) saveProgress(taskDuration)
                
                // Save continuous session segment
                val sessionDuration = sessionStartTimerValue - _timeLeft.value
                if (sessionDuration > 0) {
                    saveStudySession(sessionDuration)
                }
                
                saveState()
                _timerFinished.emit(Unit)
                resetTimer(context)
            }
        }
    }

    fun pauseTimer(context: Context) {
        if (!_isRunning.value) return
        if (isStrictMode.value && _timerMode.value == TimerMode.POMODORO) {
            Log.d("TimerManager", "Pause blocked by Strict Mode")
            return
        }
        timerJob?.cancel()
        _isRunning.value = false
        stopSound()
        
        // Save remainder for task
        val taskDuration = lastSavedTime - _timeLeft.value
        if (taskDuration > 0) saveProgress(taskDuration)
        
        // Save the continuous session segment
        val sessionDuration = sessionStartTimerValue - _timeLeft.value
        if (sessionDuration > 0) saveStudySession(sessionDuration)
        
        _currentSessionSeconds.value = 0
        _currentTaskUnsavedSeconds.value = 0
        targetEndTime = 0
        saveState()
        // Notify service that we are paused
        startService(context)
    }

    fun resetTimer(context: Context) {
        if (_isRunning.value) {
            if (isStrictMode.value && _timerMode.value == TimerMode.POMODORO) {
                Log.d("TimerManager", "Reset blocked by Strict Mode")
                return
            }
            val taskDuration = lastSavedTime - _timeLeft.value
            if (taskDuration > 0) saveProgress(taskDuration)
            
            val sessionDuration = sessionStartTimerValue - _timeLeft.value
            if (sessionDuration > 0) saveStudySession(sessionDuration)
        }
        timerJob?.cancel()
        _isRunning.value = false
        stopSound()
        _timeLeft.value = getDurationForMode(_timerMode.value)
        _currentSessionSeconds.value = 0
        _currentTaskUnsavedSeconds.value = 0
        lastSavedTime = _timeLeft.value
        sessionStartTimerValue = _timeLeft.value
        targetEndTime = 0
        saveState()
        
        // Stop the foreground service since timer is reset/idle
        stopService(context)
    }

    fun clearAll(context: Context) {
        timerJob?.cancel()
        _isRunning.value = false
        stopSound()
        
        _timeLeft.value = 25 * 60L
        _currentSessionSeconds.value = 0L
        _currentTaskUnsavedSeconds.value = 0L
        _timerMode.value = TimerMode.POMODORO
        _selectedTask.value = null
        _selectedSoundscape.value = Soundscape.NONE
        _customSoundUri.value = null
        _isStrictMode.value = false
        
        lastSavedTime = 25 * 60L
        sessionStartTimerValue = 25 * 60L
        sessionStartTime = 0
        targetEndTime = 0
        
        prefs.edit().clear().apply()
        stopService(context)
    }

    fun setTimerMode(mode: TimerMode, context: Context) {
        if (_isRunning.value && isStrictMode.value && _timerMode.value == TimerMode.POMODORO) {
            Log.d("TimerManager", "Mode change blocked by Strict Mode")
            return
        }
        _timerMode.value = mode
        resetTimer(context)
    }

    private fun saveProgress(seconds: Long) {
        val currentTask = _selectedTask.value ?: return
        if (seconds <= 0 || _timerMode.value != TimerMode.POMODORO) return
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return

        managerScope.launch(Dispatchers.IO) {
            // Fetch the latest version from DB to avoid overwriting manual changes (like isCompleted)
            val latestTask = taskDao.getTaskById(userId, currentTask.id) ?: currentTask
            
            val newTimeSpent = latestTask.timeSpentSeconds + seconds
            // Automatically mark completed if targetMinutes (or default 1h) is reached
            val targetSeconds = if (latestTask.targetMinutes > 0) latestTask.targetMinutes * 60 else 3600
            val isAutoCompleted = newTimeSpent >= targetSeconds
            
            val updatedTask = latestTask.copy(
                timeSpentSeconds = newTimeSpent,
                isCompleted = latestTask.isCompleted || isAutoCompleted,
                lastSynced = 0
            )
            
            // Update local DB
            taskDao.updateTask(updatedTask)
            
            withContext(Dispatchers.Main) {
                _currentTaskUnsavedSeconds.value = 0
                _selectedTask.value = updatedTask
            }
        }
    }

    private fun saveStudySession(seconds: Long) {
        if (seconds <= 0 || _timerMode.value != TimerMode.POMODORO) return
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (seconds * 1000)

        managerScope.launch(Dispatchers.IO) {
            studySessionDao.insertSession(
                StudySessionEntity(
                    userId = userId,
                    taskId = _selectedTask.value?.id,
                    startTime = startTime,
                    endTime = endTime,
                    durationSeconds = seconds,
                    lastSynced = 0
                )
            )
            SyncRepository(context).forceSync()
        }
    }

    private fun startService(context: Context) {
        val serviceIntent = Intent(context, TimerService::class.java)
        context.startForegroundService(serviceIntent)
    }

    private fun stopService(context: Context) {
        val serviceIntent = Intent(context, TimerService::class.java)
        context.stopService(serviceIntent)
    }

    fun saveState() {
        prefs.edit().apply {
            putLong("time_left", _timeLeft.value)
            putBoolean("is_running", _isRunning.value)
            putInt("mode", _timerMode.value.ordinal)
            putLong("target_end_time", targetEndTime)
            putInt("selected_soundscape", _selectedSoundscape.value.ordinal)
            putLong("selected_task_id", _selectedTask.value?.id ?: -1L)
            apply()
        }
    }

    fun loadState() {
        _customSoundUri.value = prefs.getString("custom_sound_uri", null)
        val savedSoundscapeOrdinal = prefs.getInt("selected_soundscape", Soundscape.NONE.ordinal)
        _selectedSoundscape.value = if (savedSoundscapeOrdinal in Soundscape.entries.indices) {
            Soundscape.entries[savedSoundscapeOrdinal]
        } else {
            Soundscape.NONE
        }

        val savedTaskId = prefs.getLong("selected_task_id", -1L)
        if (savedTaskId != -1L) {
            managerScope.launch(Dispatchers.IO) {
                val userId = FirebaseAuthManager.getCurrentUserId()
                if (userId != null) {
                    val task = taskDao.getTaskById(userId, savedTaskId)
                    withContext(Dispatchers.Main) {
                        _selectedTask.value = task
                    }
                }
            }
        }

        val isRunning = false // Force isRunning to false on app load to prevent auto-start
        val modeOrdinal = prefs.getInt("mode", TimerMode.POMODORO.ordinal)
        val mode = if (modeOrdinal < TimerMode.entries.size) TimerMode.entries[modeOrdinal] else TimerMode.POMODORO
        _timerMode.value = mode
        targetEndTime = prefs.getLong("target_end_time", 0)
        
        if (isRunning && targetEndTime > System.currentTimeMillis()) {
            val remaining = (targetEndTime - System.currentTimeMillis()) / 1000
            _timeLeft.value = remaining
            lastSavedTime = remaining
            sessionStartTimerValue = remaining
            _isRunning.value = true
        } else {
            _timeLeft.value = prefs.getLong("time_left", getDurationForMode(mode))
            lastSavedTime = _timeLeft.value
            sessionStartTimerValue = _timeLeft.value
            _isRunning.value = false
        }
    }
}

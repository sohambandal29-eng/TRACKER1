package com.example.tracker.ui.screens.timetable

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.data.local.entities.TimetableEntity
import com.example.tracker.data.repository.TimetableRepository
import com.example.tracker.utils.FirebaseAuthManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import androidx.work.*
import com.example.tracker.worker.TimetableNotificationWorker
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TimetableRepository
    private val workManager = WorkManager.getInstance(application)
    val timetableItems: StateFlow<Map<String, List<TimetableEntity>>>

    init {
        val timetableDao = AppDatabase.getDatabase(application).timetableDao()
        repository = TimetableRepository(timetableDao)

        timetableItems = FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList<TimetableEntity>())
            else timetableDao.getAllTimetableItems(userId)
        }.map { items ->
            items.groupBy { it.day }
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
        )
    }

    fun updateItem(item: TimetableEntity) {
        viewModelScope.launch {
            repository.updateItem(item)
            scheduleNotification(item)
        }
    }

    fun deleteItem(item: TimetableEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
            cancelNotification(item.id)
        }
    }

    fun addItem(day: String, timing: String, endTime: String, subject: String) {
        viewModelScope.launch {
            val orderIndex = (timetableItems.value[day]?.size ?: 0)
            val id = repository.insertItem(TimetableEntity(day = day, timing = timing, endTime = endTime, subject = subject, orderIndex = orderIndex))
            scheduleNotification(TimetableEntity(id = id, day = day, timing = timing, endTime = endTime, subject = subject))
        }
    }

    fun addItemsToAllDays(timing: String, endTime: String, subject: String, excludedDays: List<String> = emptyList()) {
        viewModelScope.launch {
            val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            days.filter { it !in excludedDays }.forEach { day ->
                val orderIndex = (timetableItems.value[day]?.size ?: 0)
                val id = repository.insertItem(TimetableEntity(day = day, timing = timing, endTime = endTime, subject = subject, orderIndex = orderIndex))
                scheduleNotification(TimetableEntity(id = id, day = day, timing = timing, endTime = endTime, subject = subject))
            }
        }
    }
    
    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            workManager.cancelAllWorkByTag("timetable_notification")
        }
    }

    private fun scheduleNotification(item: TimetableEntity) {
        val delay = calculateDelay(item.day, item.timing) ?: return
        
        val data = workDataOf(
            "subject" to item.subject,
            "timing" to item.timing
        )

        val workRequest = OneTimeWorkRequestBuilder<TimetableNotificationWorker>()
            .setInitialDelay(delay)
            .addTag("timetable_notification")
            .addTag("timetable_item_${item.id}")
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(
            "timetable_item_${item.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun cancelNotification(itemId: Int) {
        workManager.cancelUniqueWork("timetable_item_$itemId")
    }

    private fun calculateDelay(day: String, timing: String): Duration? {
        return try {
            val cleanedTiming = timing.uppercase()
                .replace(".", "")
                .trim()
                .let { 
                    if (it.contains("AM") || it.contains("PM")) {
                        val timePart = it.substring(0, it.length - 2).trim()
                        val suffix = it.substring(it.length - 2)
                        val formattedTime = if (!timePart.contains(":")) "$timePart:00" else timePart
                        "$formattedTime $suffix"
                    } else {
                        if (!it.contains(":")) "$it:00" else it
                    }
                }
            
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
            val scheduleTime = LocalTime.parse(cleanedTiming, formatter)
            val targetDay = java.time.DayOfWeek.valueOf(day.uppercase())
            
            var targetDateTime = LocalDateTime.now()
                .with(java.time.temporal.TemporalAdjusters.nextOrSame(targetDay))
                .with(scheduleTime)
                .minusMinutes(5)

            // Debug log or print if possible, but let's ensure it's not in the past
            if (targetDateTime.isBefore(LocalDateTime.now())) {
                targetDateTime = targetDateTime.plusWeeks(1)
            }
            
            // Log the calculated time for debugging
            println("Scheduling notification for $targetDateTime")

            Duration.between(LocalDateTime.now(), targetDateTime)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

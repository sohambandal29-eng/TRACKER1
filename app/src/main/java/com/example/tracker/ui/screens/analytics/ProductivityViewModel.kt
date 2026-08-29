package com.example.tracker.ui.screens.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.utils.FirebaseAuthManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ProductivityViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val sessionDao = db.studySessionDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    val hourlyProductivity: Flow<Map<Int, Int>> = FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(emptyMap())
        } else {
            sessionDao.getAllSessions(userId).map { sessions ->
                val now = LocalDateTime.now(ZoneId.systemDefault())
                val trackerDayStart = if (now.hour < 6) {
                    now.minusDays(1).withHour(6).withMinute(0).withSecond(0).withNano(0)
                } else {
                    now.withHour(6).withMinute(0).withSecond(0).withNano(0)
                }
                val trackerDayStartMillis = trackerDayStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val trackerDayEndMillis = trackerDayStartMillis + 86400000

                sessions.filter {
                    it.startTime in trackerDayStartMillis until trackerDayEndMillis
                }
                .groupBy {
                    Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).hour
                }
                .mapValues { entry ->
                    entry.value.sumOf { s -> s.durationSeconds.toInt() } / 60
                }
            }
        }
    }

    val peakHour: Flow<Int?> = hourlyProductivity.map { map ->
        map.maxByOrNull { it.value }?.key
    }
}

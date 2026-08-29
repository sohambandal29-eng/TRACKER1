package com.example.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tracker.data.local.dao.BlockedAppDao
import com.example.tracker.data.local.dao.ConsistencyRuleDao
import com.example.tracker.data.local.dao.RoadmapDao
import com.example.tracker.data.local.dao.StudySessionDao
import com.example.tracker.data.local.dao.SubTaskDao
import com.example.tracker.data.local.dao.TaskDao
import com.example.tracker.data.local.dao.TimetableDao
import com.example.tracker.data.local.entities.*

@Database(
    entities = [
        TaskEntity::class,
        SubTaskEntity::class,
        RoadmapStageEntity::class,
        TimelineItemEntity::class,
        RoadmapHeaderEntity::class,
        TimetableEntity::class,
        StudySessionEntity::class,
        ConsistencyRuleEntity::class,
        BlockedAppEntity::class
    ],
    version = 25,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subTaskDao(): SubTaskDao
    abstract fun roadmapDao(): RoadmapDao
    abstract fun timetableDao(): TimetableDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun consistencyRuleDao(): ConsistencyRuleDao
    abstract fun blockedAppDao(): BlockedAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

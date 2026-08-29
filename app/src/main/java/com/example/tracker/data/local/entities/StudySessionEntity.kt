package com.example.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val remoteId: String? = null,
    val taskId: Long? = null,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val durationSeconds: Long = 0,
    val lastSynced: Long = 0,
    @get:PropertyName("deleted")
    @set:PropertyName("deleted")
    @PropertyName("deleted")
    var isDeleted: Boolean = false
)

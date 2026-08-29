package com.example.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "timetable")
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val remoteId: String? = null,
    val day: String = "",
    val timing: String = "", // Start Time
    val endTime: String = "", // End Time
    val subject: String = "",
    val orderIndex: Int = 0,
    val lastSynced: Long = 0,
    @get:PropertyName("deleted")
    @PropertyName("deleted")
    val isDeleted: Boolean = false
)

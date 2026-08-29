package com.example.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val remoteId: String? = null,
    val title: String = "",
    val category: String = "", // MPSC, Python, DSA, Aptitude, etc.
    val priority: Int = 1, // 1: Low, 2: Medium, 3: High
    
    @get:PropertyName("completed")
    @set:PropertyName("completed")
    @PropertyName("completed")
    var isCompleted: Boolean = false,
    
    val timeSpentSeconds: Long = 0,
    val targetMinutes: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val lastSynced: Long = 0,
    
    @get:PropertyName("deleted")
    @set:PropertyName("deleted")
    @PropertyName("deleted")
    var isDeleted: Boolean = false
)

package com.example.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey
    val packageName: String = "",
    val userId: String = "",
    val appName: String = "",
    val timeLimitMinutes: Int = 0,
    @get:PropertyName("synced")
    @PropertyName("synced")
    val isSynced: Boolean = false
)

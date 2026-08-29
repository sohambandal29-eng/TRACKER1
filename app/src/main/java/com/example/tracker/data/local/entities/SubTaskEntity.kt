package com.example.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val remoteId: String? = null,
    val taskId: Long,
    val title: String = "",
    @get:PropertyName("completed")
    @set:PropertyName("completed")
    @PropertyName("completed")
    var isCompleted: Boolean = false,
    val lastSynced: Long = 0,
    @get:PropertyName("deleted")
    @set:PropertyName("deleted")
    @PropertyName("deleted")
    var isDeleted: Boolean = false
)

package com.example.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(
    tableName = "roadmap_stages",
    indices = [Index(value = ["order"]), Index(value = ["isDeleted"])]
)
data class RoadmapStageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val remoteId: String? = null,
    val title: String = "",
    val duration: String = "",
    val topics: String = "",
    val resources: String = "",
    val order: Int = 0,
    val lastSynced: Long = 0,
    @get:PropertyName("deleted")
    @PropertyName("deleted")
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "timeline_items",
    indices = [Index(value = ["order"]), Index(value = ["isDeleted"])]
)
data class TimelineItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val remoteId: String? = null,
    val period: String = "",
    val description: String = "",
    val order: Int = 0,
    val lastSynced: Long = 0,
    @get:PropertyName("deleted")
    @PropertyName("deleted")
    val isDeleted: Boolean = false
)

@Entity(tableName = "roadmap_headers")
data class RoadmapHeaderEntity(
    @PrimaryKey
    val id: String = "", // "journey" or "timeline"
    val userId: String = "",
    val title: String = ""
)

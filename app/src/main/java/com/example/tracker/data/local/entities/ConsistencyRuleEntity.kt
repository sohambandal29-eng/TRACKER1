package com.example.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "consistency_rules")
data class ConsistencyRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val remoteId: String? = null,
    val text: String = "",
    @get:PropertyName("compulsory")
    @PropertyName("compulsory")
    val isCompulsory: Boolean = false,
    @get:PropertyName("enabled")
    @PropertyName("enabled")
    val isEnabled: Boolean = true,
    val lastSynced: Long = 0,
    @get:PropertyName("deleted")
    @PropertyName("deleted")
    val isDeleted: Boolean = false
)

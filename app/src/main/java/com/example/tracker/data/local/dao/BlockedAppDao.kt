package com.example.tracker.data.local.dao

import androidx.room.*
import com.example.tracker.data.local.entities.BlockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps WHERE userId = :userId")
    fun getAllBlockedApps(userId: String): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedBlockedApps(userId: String): List<BlockedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: BlockedAppEntity)

    @Delete
    suspend fun delete(app: BlockedAppEntity)

    @Query("SELECT * FROM blocked_apps WHERE userId = :userId AND packageName = :packageName LIMIT 1")
    suspend fun getBlockedAppByPackage(userId: String, packageName: String): BlockedAppEntity?
}

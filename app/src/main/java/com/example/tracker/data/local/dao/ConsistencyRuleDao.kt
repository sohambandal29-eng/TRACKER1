package com.example.tracker.data.local.dao

import androidx.room.*
import com.example.tracker.data.local.entities.ConsistencyRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsistencyRuleDao {
    @Query("SELECT * FROM consistency_rules WHERE userId = :userId ORDER BY isCompulsory DESC, id ASC")
    fun getAllRules(userId: String): Flow<List<ConsistencyRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ConsistencyRuleEntity)

    @Update
    suspend fun updateRule(rule: ConsistencyRuleEntity)

    @Delete
    suspend fun deleteRule(rule: ConsistencyRuleEntity)

    @Query("SELECT * FROM consistency_rules WHERE userId = :userId AND remoteId = :remoteId LIMIT 1")
    suspend fun findRuleByRemoteId(userId: String, remoteId: String): ConsistencyRuleEntity?

    @Query("SELECT * FROM consistency_rules WHERE userId = :userId AND lastSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedRules(userId: String): List<ConsistencyRuleEntity>

    @Query("SELECT * FROM consistency_rules WHERE userId = :userId AND isDeleted = 1")
    suspend fun getDeletedRules(userId: String): List<ConsistencyRuleEntity>

    @Delete
    suspend fun permanentlyDeleteRule(rule: ConsistencyRuleEntity)

    @Query("SELECT COUNT(*) FROM consistency_rules WHERE userId = :userId AND isDeleted = 0")
    suspend fun getRuleCount(userId: String): Int
}

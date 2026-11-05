package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlocklistDao {
    @Query("SELECT * FROM blocklist_domains ORDER BY addedAt DESC")
    fun getAllDomains(): Flow<List<BlocklistDomainEntity>>

    @Query("SELECT domain FROM blocklist_domains")
    suspend fun getAllDomainStrings(): List<String>

    @Query("SELECT COUNT(*) FROM blocklist_domains")
    fun getDomainCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocklist_domains")
    suspend fun getDomainCountDirect(): Int

    @Query("SELECT * FROM blocklist_domains WHERE isCustom = 1 ORDER BY addedAt DESC")
    fun getCustomDomains(): Flow<List<BlocklistDomainEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDomain(domain: BlocklistDomainEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(domains: List<BlocklistDomainEntity>)

    @Query("DELETE FROM blocklist_domains WHERE domain = :domain")
    suspend fun deleteDomain(domain: String)

    @Query("DELETE FROM blocklist_domains WHERE isCustom = 0")
    suspend fun clearBundledDomains()

    @Query("SELECT EXISTS(SELECT 1 FROM blocklist_domains WHERE domain = :domain)")
    suspend fun containsDomain(domain: String): Boolean
}

@Dao
interface BlockedEventDao {
    @Query("SELECT * FROM blocked_events ORDER BY timestamp DESC LIMIT 50")
    fun getRecentEvents(): Flow<List<BlockedEventEntity>>

    @Query("SELECT COUNT(*) FROM blocked_events")
    fun getTotalBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_events")
    suspend fun getTotalBlockedCountDirect(): Int

    @Insert
    suspend fun insertEvent(event: BlockedEventEntity)

    @Query("DELETE FROM blocked_events")
    suspend fun clearAllEvents()
}

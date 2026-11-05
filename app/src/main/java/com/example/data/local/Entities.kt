package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocklist_domains",
    indices = [Index(value = ["domain"], unique = true)]
)
data class BlocklistDomainEntity(
    @PrimaryKey
    val domain: String,
    val isCustom: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val category: String = "Adult Content",
    val source: String = "Bundled"
)

@Entity(
    tableName = "blocked_events",
    indices = [Index(value = ["timestamp"])]
)
data class BlockedEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val timestamp: Long = System.currentTimeMillis(),
    val queryType: String = "A",
    val clientInfo: String = "10.254.1.1",
    val mode: String = "PERSONAL"
)

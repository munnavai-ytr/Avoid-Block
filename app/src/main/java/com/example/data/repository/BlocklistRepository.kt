package com.example.data.repository

import com.example.data.local.BlockedEventDao
import com.example.data.local.BlockedEventEntity
import com.example.data.local.BlocklistDao
import com.example.data.local.BlocklistDomainEntity
import com.example.data.local.BundledAdultDomains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class BlocklistRepository(
    private val blocklistDao: BlocklistDao,
    private val blockedEventDao: BlockedEventDao
) {
    // In-memory concurrent set for real-time, low-latency DNS query filtering in VPN thread
    private val inMemoryBlockedDomains = ConcurrentHashMap.newKeySet<String>()

    val allDomains: Flow<List<BlocklistDomainEntity>> = blocklistDao.getAllDomains()
    val customDomains: Flow<List<BlocklistDomainEntity>> = blocklistDao.getCustomDomains()
    val domainCount: Flow<Int> = blocklistDao.getDomainCount()
    val recentEvents: Flow<List<BlockedEventEntity>> = blockedEventDao.getRecentEvents()
    val totalBlockedCount: Flow<Int> = blockedEventDao.getTotalBlockedCount()

    suspend fun initializeDefaultBlocklistIfNeeded() = withContext(Dispatchers.IO) {
        val count = blocklistDao.getDomainCountDirect()
        if (count == 0) {
            val entities = BundledAdultDomains.INITIAL_ADULT_DOMAINS.map { domain ->
                BlocklistDomainEntity(
                    domain = domain.lowercase().trim(),
                    isCustom = false,
                    category = "Adult / Pornography",
                    source = "Bundled Verified"
                )
            }
            blocklistDao.insertAll(entities)
        }
        refreshInMemoryCache()
    }

    suspend fun refreshInMemoryCache() = withContext(Dispatchers.IO) {
        val domains = blocklistDao.getAllDomainStrings()
        inMemoryBlockedDomains.clear()
        inMemoryBlockedDomains.addAll(domains.map { it.lowercase().trim() })
    }

    /**
     * High-speed O(k) domain matching where k is number of domain labels.
     * Checks exact domain, and walks up subdomains:
     * e.g., "video.cdn.pornhub.com" -> checks:
     *   "video.cdn.pornhub.com"
     *   "cdn.pornhub.com"
     *   "pornhub.com"
     */
    fun isDomainBlocked(rawDomain: String): Boolean {
        val domain = BundledAdultDomains.normalizeDomain(rawDomain)
        if (domain.isBlank()) return false

        // Exact match
        if (inMemoryBlockedDomains.contains(domain)) return true

        // Walk up domain hierarchy
        var current = domain
        while (current.contains('.')) {
            val nextDot = current.indexOf('.')
            if (nextDot == -1 || nextDot == current.length - 1) break
            current = current.substring(nextDot + 1)
            if (inMemoryBlockedDomains.contains(current)) {
                return true
            }
        }

        return false
    }

    suspend fun addCustomDomain(rawDomain: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = BundledAdultDomains.normalizeDomain(rawDomain)
        if (normalized.isBlank() || !normalized.contains('.')) {
            return@withContext false
        }
        val entity = BlocklistDomainEntity(
            domain = normalized,
            isCustom = true,
            category = "Custom Blocked",
            source = "User Custom"
        )
        blocklistDao.insertDomain(entity)
        inMemoryBlockedDomains.add(normalized)
        true
    }

    suspend fun removeCustomDomain(rawDomain: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = BundledAdultDomains.normalizeDomain(rawDomain)
        blocklistDao.deleteDomain(normalized)
        inMemoryBlockedDomains.remove(normalized)
        true
    }

    suspend fun recordBlockedEvent(
        domain: String,
        queryType: String = "A",
        clientInfo: String = "10.254.1.1",
        mode: String = "PERSONAL"
    ) = withContext(Dispatchers.IO) {
        val event = BlockedEventEntity(
            domain = domain,
            timestamp = System.currentTimeMillis(),
            queryType = queryType,
            clientInfo = clientInfo,
            mode = mode
        )
        blockedEventDao.insertEvent(event)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        blockedEventDao.clearAllEvents()
    }

    suspend fun fetchRemoteBlocklistUpdate(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Curated, privacy-first StevenBlack hosts / adult filter mirror
            val sourceUrl = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn-only/hosts"
            val url = URL(sourceUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.requestMethod = "GET"

            if (connection.responseCode != 200) {
                return@withContext Result.failure(Exception("HTTP Error ${connection.responseCode} while fetching blocklist"))
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val newDomains = mutableListOf<BlocklistDomainEntity>()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim() ?: continue
                if (trimmed.startsWith("#") || trimmed.isBlank()) continue

                // Format: "0.0.0.0 domain.com" or "127.0.0.1 domain.com" or just "domain.com"
                val parts = trimmed.split("\\s+".toRegex())
                val targetDomain = if (parts.size >= 2) parts[1] else parts[0]
                val normalized = BundledAdultDomains.normalizeDomain(targetDomain)

                if (normalized.isNotBlank() &&
                    normalized.contains('.') &&
                    normalized != "localhost" &&
                    normalized != "broadcasthost" &&
                    !normalized.endsWith(".local")
                ) {
                    newDomains.add(
                        BlocklistDomainEntity(
                            domain = normalized,
                            isCustom = false,
                            category = "Adult / Pornography",
                            source = "StevenBlack Remote"
                        )
                    )
                }

                // Batch safeguard: cap at 15,000 top domains to maintain fast low-memory execution on mobile
                if (newDomains.size >= 15_000) break
            }
            reader.close()
            connection.disconnect()

            if (newDomains.isEmpty()) {
                return@withContext Result.failure(Exception("Downloaded list was empty or invalid. Previous list retained."))
            }

            // Only clear and replace bundled domains if the download succeeded and validated
            blocklistDao.clearBundledDomains()
            // Re-insert standard bundled fallback domains as well
            val bundledFallback = BundledAdultDomains.INITIAL_ADULT_DOMAINS.map {
                BlocklistDomainEntity(domain = it, isCustom = false, category = "Adult Content", source = "Bundled")
            }
            blocklistDao.insertAll(bundledFallback)
            blocklistDao.insertAll(newDomains)
            refreshInMemoryCache()

            Result.success(newDomains.size + bundledFallback.size)
        } catch (e: Exception) {
            // Keep the last known working list if an update fails
            Result.failure(e)
        }
    }
}

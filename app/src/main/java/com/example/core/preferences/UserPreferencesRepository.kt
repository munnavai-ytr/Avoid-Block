package com.example.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.OperatingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "purelock_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val PROTECTION_ACTIVE = booleanPreferencesKey("is_protection_active")
        val OPERATING_MODE = stringPreferencesKey("operating_mode")
        val UPSTREAM_DNS_IP = stringPreferencesKey("upstream_dns_ip")
        val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val RECOVERY_QUESTION = stringPreferencesKey("recovery_question")
        val RECOVERY_ANSWER_HASH = stringPreferencesKey("recovery_answer_hash")
        val FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        val LOCKOUT_UNTIL_TIMESTAMP = longPreferencesKey("lockout_until_timestamp")
        val LAST_BLOCKLIST_UPDATE = longPreferencesKey("last_blocklist_update")
        val TOTAL_QUERIES_COUNTER = longPreferencesKey("total_queries_counter")
        val APP_ICON_HIDDEN = booleanPreferencesKey("app_icon_hidden")
    }

    val isProtectionActive: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.PROTECTION_ACTIVE] ?: false }

    val operatingMode: Flow<OperatingMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val modeStr = preferences[PreferencesKeys.OPERATING_MODE]
            if (modeStr != null) {
                try {
                    OperatingMode.valueOf(modeStr)
                } catch (e: Exception) {
                    OperatingMode.PERSONAL
                }
            } else {
                OperatingMode.PERSONAL
            }
        }

    val upstreamDnsIp: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.UPSTREAM_DNS_IP] ?: "1.1.1.3" }

    val autoStartOnBoot: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.AUTO_START_ON_BOOT] ?: true }

    val isPinConfigured: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            !preferences[PreferencesKeys.PIN_HASH].isNullOrEmpty() &&
            !preferences[PreferencesKeys.PIN_SALT].isNullOrEmpty()
        }

    val recoveryQuestion: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.RECOVERY_QUESTION] }

    val lastBlocklistUpdate: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.LAST_BLOCKLIST_UPDATE] ?: 0L }

    val totalQueriesCounter: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.TOTAL_QUERIES_COUNTER] ?: 0L }

    suspend fun setProtectionActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROTECTION_ACTIVE] = active
        }
    }

    suspend fun setOperatingMode(mode: OperatingMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPERATING_MODE] = mode.name
        }
    }

    suspend fun setUpstreamDnsIp(ip: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.UPSTREAM_DNS_IP] = ip
        }
    }

    suspend fun setAutoStartOnBoot(autoStart: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_START_ON_BOOT] = autoStart
        }
    }

    suspend fun savePin(pinHash: String, salt: String, question: String, answerHash: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PIN_HASH] = pinHash
            preferences[PreferencesKeys.PIN_SALT] = salt
            preferences[PreferencesKeys.RECOVERY_QUESTION] = question
            preferences[PreferencesKeys.RECOVERY_ANSWER_HASH] = answerHash
            preferences[PreferencesKeys.FAILED_PIN_ATTEMPTS] = 0
            preferences[PreferencesKeys.LOCKOUT_UNTIL_TIMESTAMP] = 0L
        }
    }

    suspend fun getPinSecurityData(): Pair<String?, String?> {
        var hash: String? = null
        var salt: String? = null
        context.dataStore.edit { preferences ->
            hash = preferences[PreferencesKeys.PIN_HASH]
            salt = preferences[PreferencesKeys.PIN_SALT]
        }
        return Pair(hash, salt)
    }

    suspend fun getRecoveryAnswerHash(): String? {
        var answerHash: String? = null
        context.dataStore.edit { preferences ->
            answerHash = preferences[PreferencesKeys.RECOVERY_ANSWER_HASH]
        }
        return answerHash
    }

    suspend fun getThrottlingData(): Pair<Int, Long> {
        var attempts = 0
        var lockout = 0L
        context.dataStore.edit { preferences ->
            attempts = preferences[PreferencesKeys.FAILED_PIN_ATTEMPTS] ?: 0
            lockout = preferences[PreferencesKeys.LOCKOUT_UNTIL_TIMESTAMP] ?: 0L
        }
        return Pair(attempts, lockout)
    }

    suspend fun updateThrottling(failedAttempts: Int, lockoutUntil: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAILED_PIN_ATTEMPTS] = failedAttempts
            preferences[PreferencesKeys.LOCKOUT_UNTIL_TIMESTAMP] = lockoutUntil
        }
    }

    suspend fun resetThrottling() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAILED_PIN_ATTEMPTS] = 0
            preferences[PreferencesKeys.LOCKOUT_UNTIL_TIMESTAMP] = 0L
        }
    }

    suspend fun setLastBlocklistUpdate(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BLOCKLIST_UPDATE] = timestamp
        }
    }

    val isAppIconHidden: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.APP_ICON_HIDDEN] ?: false }

    suspend fun setAppIconHidden(hidden: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_ICON_HIDDEN] = hidden
        }
    }

    suspend fun incrementTotalQueries() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.TOTAL_QUERIES_COUNTER] ?: 0L
            preferences[PreferencesKeys.TOTAL_QUERIES_COUNTER] = current + 1L
        }
    }
}

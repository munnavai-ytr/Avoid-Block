package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.PureLockDatabase
import com.example.core.preferences.UserPreferencesRepository
import com.example.core.security.PinManager
import com.example.data.repository.BlocklistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PureLockApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { PureLockDatabase.getInstance(this) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val pinManager by lazy { PinManager(this, userPreferencesRepository) }
    val blocklistRepository by lazy {
        BlocklistRepository(database.blocklistDao(), database.blockedEventDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()

        // Prepopulate bundled adult domains on first launch
        applicationScope.launch {
            blocklistRepository.initializeDefaultBlocklistIfNeeded()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                VPN_NOTIFICATION_CHANNEL_ID,
                "PURELOCK Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time protection status and blocked DNS queries"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val VPN_NOTIFICATION_CHANNEL_ID = "purelock_vpn_status_channel"
        lateinit var instance: PureLockApp
            private set
    }
}

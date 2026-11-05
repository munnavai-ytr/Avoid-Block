package com.example.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.example.PureLockApp
import com.example.vpn.PureLockVpnService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Boot or package update received ($action). Checking auto-restart status...")

            val app = context.applicationContext as PureLockApp
            app.applicationScope.launch {
                val shouldAutoStart = app.userPreferencesRepository.autoStartOnBoot.first()
                val wasActive = app.userPreferencesRepository.isProtectionActive.first()

                // Check if VPN permission is already prepared/granted
                val vpnPrepared = VpnService.prepare(context) == null

                if (shouldAutoStart && wasActive && vpnPrepared) {
                    Log.i(TAG, "Auto-restoring PURELOCK VPN protection after boot")
                    PureLockVpnService.start(context)
                } else {
                    Log.i(TAG, "Auto-start skipped: shouldAutoStart=$shouldAutoStart, wasActive=$wasActive, vpnPrepared=$vpnPrepared")
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}

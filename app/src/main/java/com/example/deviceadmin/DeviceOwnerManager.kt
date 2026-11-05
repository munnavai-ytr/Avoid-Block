package com.example.deviceadmin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log

class DeviceOwnerManager(private val context: Context) {

    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    val adminComponent = ComponentName(context, PureLockDeviceAdminReceiver::class.java)

    fun isDeviceAdmin(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    fun isDeviceOwner(): Boolean {
        return devicePolicyManager.isDeviceOwnerApp(context.packageName)
    }

    fun isUninstallBlocked(): Boolean {
        return if (isDeviceOwner()) {
            devicePolicyManager.isUninstallBlocked(adminComponent, context.packageName)
        } else {
            false
        }
    }

    /**
     * Official Android DevicePolicyManager API to prevent uninstallation of PureLock.
     * Only works when provisioned as Device Owner (Android Enterprise).
     */
    fun setUninstallBlocked(blocked: Boolean): Boolean {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Cannot setUninstallBlocked: App is not Device Owner")
            return false
        }
        return try {
            devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, blocked)
            Log.i(TAG, "Uninstall blocked set to $blocked")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException setting uninstall blocked: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "DeviceOwnerManager"
        const val ADB_PROVISION_COMMAND =
            "adb shell dpm set-device-owner com.purelock.app/com.example.deviceadmin.PureLockDeviceAdminReceiver"
    }
}

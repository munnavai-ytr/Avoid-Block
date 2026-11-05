package com.example.core.security

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object LauncherIconManager {

    private const val TAG = "LauncherIconManager"
    const val ALIAS_CLASS_NAME = "com.example.LauncherActivityAlias"
    const val RECOVERY_DEEP_LINK = "purelock://open"
    const val ADB_LAUNCH_COMMAND = "adb shell am start -n com.purelock.app/com.example.MainActivity"

    fun getAliasComponentName(context: Context): ComponentName {
        return ComponentName(context.packageName, ALIAS_CLASS_NAME)
    }

    /**
     * Checks the actual component enabled setting directly from Android's PackageManager.
     */
    fun isIconHidden(context: Context): Boolean {
        return try {
            val component = getAliasComponentName(context)
            val state = context.packageManager.getComponentEnabledSetting(component)
            state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking launcher component status: ${e.message}")
            false
        }
    }

    /**
     * Toggles launcher icon visibility using Android PackageManager component enabled setting.
     * CRITICAL: Uses PackageManager.DONT_KILL_APP so Android does not terminate the app
     * or disrupt the running VPN filtering service.
     */
    fun setIconHidden(context: Context, hide: Boolean): Result<Boolean> {
        return try {
            val component = getAliasComponentName(context)
            val newState = if (hide) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }

            context.packageManager.setComponentEnabledSetting(
                component,
                newState,
                PackageManager.DONT_KILL_APP
            )

            Log.i(TAG, "Launcher icon component state changed to: $newState (hidden=$hide)")
            Result.success(hide)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException changing component setting: ${e.message}", e)
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException for component $ALIAS_CLASS_NAME: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error changing component setting: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Diagnostic info regarding Android launcher compatibility and behavior.
     */
    fun getCompatibilityNotes(): String {
        val osVersion = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        return buildString {
            append("Android OS $osVersion (API $sdkInt)\n\n")
            append("• Launcher Visibility: Uses official PackageManager component-enabled API with DONT_KILL_APP.\n")
            append("• VPN Filtering: Continues active and uninterrupted; hiding the launcher entry does not stop background services.\n")
            append("• Launcher Refresh: Stock Android, Pixel, and AOSP update the app drawer immediately. OEM launchers (Realme UI, ColorOS, MIUI, One UI) may take a few seconds or a screen lock/unlock to refresh cached icons.\n")
            append("• System Visibility: In compliance with Android security, the app remains visible in System Settings > Apps, VPN Settings, and Device Management.\n")
            append("• Recovery Methods: Ongoing VPN notification, browser URL (purelock://open), Settings > Apps > Open, or ADB.")
        }
    }
}

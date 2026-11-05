package com.example.deviceadmin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class PureLockDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "PURELOCK Device Admin enabled")
        Toast.makeText(context, "PURELOCK Family Protection administrator enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "PURELOCK Device Admin disabled")
    }

    companion object {
        private const val TAG = "PureLockDeviceAdmin"
    }
}

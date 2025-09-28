package com.mapshoppinglist.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "BOOT_COMPLETED received, scheduling geofence sync")
            GeofenceSyncWorker.enqueueNow(context)
        } else {
            Log.d(TAG, "Ignored broadcast action=${intent?.action}")
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}

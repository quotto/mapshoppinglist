package com.mapshoppinglist.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mapshoppinglist.nearby.NearbyActivityTransitionWorker
import com.mapshoppinglist.nearby.NearbySuggestionTriggerWorker

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "BOOT_COMPLETED received, scheduling geofence full rebuild")
            GeofenceSyncWorker.enqueueNow(context, forceRebuild = true)
            NearbyActivityTransitionWorker.enqueueNow(context)
            NearbySuggestionTriggerWorker.enqueueNow(
                context = context,
                reason = NearbySuggestionTriggerWorker.REASON_BOOT_RESTORE
            )
        } else {
            Log.d(TAG, "Ignored broadcast action=${intent?.action}")
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}

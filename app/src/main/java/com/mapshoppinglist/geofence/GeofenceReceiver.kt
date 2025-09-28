package com.mapshoppinglist.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != GeofencePendingIntentProvider.ACTION_GEOFENCE_EVENT) {
            Log.d(TAG, "Ignored broadcast action=${intent?.action}")
            return
        }

        val event = GeofencingEvent.fromIntent(intent)
        if (event == null) {
            Log.w(TAG, "GeofencingEvent was null")
            return
        }
        if (event.hasError()) {
            Log.e(TAG, "Geofence error code=${event.errorCode}")
            GeofenceSyncWorker.enqueueNow(context)
            return
        }

        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val placeIds = event.triggeringGeofences
                ?.mapNotNull { parsePlaceId(it.requestId) }
                ?.distinct()
                .orEmpty()
            Log.d(TAG, "ENTER transition for placeIds=$placeIds")
            GeofenceSyncWorker.enqueueNow(context)
            GeofenceNotificationWorker.enqueue(context, placeIds)
        } else {
            Log.d(TAG, "Unsupported transition=${event.geofenceTransition}")
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"

        private fun parsePlaceId(requestId: String?): Long? {
            if (requestId.isNullOrBlank()) return null
            return requestId.removePrefix("place_").toLongOrNull()
        }
    }
}

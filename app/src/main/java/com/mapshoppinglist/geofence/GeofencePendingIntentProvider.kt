package com.mapshoppinglist.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Geofenceイベント受信に使用するPendingIntentを提供する。
 */
class GeofencePendingIntentProvider(private val context: Context) {

    fun get(): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    companion object {
        private const val REQUEST_CODE = 1001
        const val ACTION_GEOFENCE_EVENT = "com.mapshoppinglist.ACTION_GEOFENCE_EVENT"
    }
}

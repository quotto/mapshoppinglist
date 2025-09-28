package com.mapshoppinglist.geofence

import android.content.Context

/**
 * ジオフェンス同期をWorkManagerに委ねるためのユーティリティ。
 */
class GeofenceSyncScheduler(private val context: Context) {

    fun scheduleImmediateSync() {
        GeofenceSyncWorker.enqueueNow(context)
    }
}

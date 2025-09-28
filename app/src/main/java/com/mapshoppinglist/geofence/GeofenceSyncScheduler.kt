package com.mapshoppinglist.geofence

import android.content.Context

/**
 * ジオフェンス同期をWorkManagerに委ねるためのユーティリティ。
 */
open class GeofenceSyncScheduler(private val context: Context) {

    open fun scheduleImmediateSync() {
        GeofenceSyncWorker.enqueueNow(context)
    }
}

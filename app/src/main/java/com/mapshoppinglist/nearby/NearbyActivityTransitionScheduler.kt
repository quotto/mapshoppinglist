package com.mapshoppinglist.nearby

import android.content.Context

open class NearbyActivityTransitionScheduler(private val context: Context) {

    open fun scheduleRegistration() {
        NearbyActivityTransitionWorker.enqueueNow(context)
    }
}

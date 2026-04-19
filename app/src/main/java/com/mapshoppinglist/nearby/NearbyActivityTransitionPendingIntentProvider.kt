package com.mapshoppinglist.nearby

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class NearbyActivityTransitionPendingIntentProvider(private val context: Context) {

    fun get(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, NearbyActivityTransitionReceiver::class.java).apply {
            action = ACTION_ACTIVITY_TRANSITION
        },
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            PendingIntent.FLAG_CANCEL_CURRENT
        } else {
            PendingIntent.FLAG_MUTABLE
        }
//        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )

    companion object {
        const val ACTION_ACTIVITY_TRANSITION = "com.mapshoppinglist.action.ACTIVITY_TRANSITION"

        private const val REQUEST_CODE = 4_201
    }
}

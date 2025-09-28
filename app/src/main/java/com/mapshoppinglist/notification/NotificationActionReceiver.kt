package com.mapshoppinglist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.mapshoppinglist.MapShoppingListApplication
import com.mapshoppinglist.geofence.GeofenceNotificationWorker
import com.mapshoppinglist.geofence.GeofenceSyncWorker

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val placeId = intent.getLongExtra(NotificationActions.EXTRA_PLACE_ID, -1L)
        if (placeId <= 0L) {
            Log.w(TAG, "Invalid placeId in notification action")
            return
        }
        when (intent.action) {
            NotificationActions.ACTION_MARK_PURCHASED -> handleMarkPurchased(context, placeId)
            NotificationActions.ACTION_SNOOZE -> handleSnooze(context, placeId)
            else -> Log.d(TAG, "Unknown action=${intent.action}")
        }
    }

    private fun handleMarkPurchased(context: Context, placeId: Long) {
        Log.d(TAG, "Received mark purchased for placeId=$placeId")
        // 後続タスクで実際の購入処理を実装予定。現時点では再同期のみ実施。
        GeofenceSyncWorker.enqueueNow(context)
        GeofenceNotificationWorker.enqueue(context, listOf(placeId))
        cancelNotification(context, placeId)
    }

    private fun handleSnooze(context: Context, placeId: Long) {
        Log.d(TAG, "Received snooze for placeId=$placeId")
        // Snoozeロジックはタスク7で実装予定。暫定的に通知のみキャンセル。
        cancelNotification(context, placeId)
    }

    private fun cancelNotification(context: Context, placeId: Long) {
        NotificationManagerCompat.from(context).cancel(placeId.hashCode())
    }

    companion object {
        private const val TAG = "NotificationAction"
    }
}


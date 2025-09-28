package com.mapshoppinglist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.mapshoppinglist.MapShoppingListApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val placeId = intent.getLongExtra(NotificationActions.EXTRA_PLACE_ID, -1L)
        if (placeId <= 0L) {
            Log.w(TAG, "Invalid placeId in notification action")
            return
        }
        val pendingResult = goAsync()
        val app = context.applicationContext as MapShoppingListApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NotificationActions.ACTION_MARK_PURCHASED -> handleMarkPurchased(app, placeId)
                    NotificationActions.ACTION_SNOOZE -> handleSnooze(app, placeId)
                    else -> Log.d(TAG, "Unknown action=${intent.action}")
                }
            } finally {
                cancelNotification(context, placeId)
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleMarkPurchased(app: MapShoppingListApplication, placeId: Long) {
        Log.d(TAG, "Received mark purchased for placeId=$placeId")
        app.markPlaceItemsPurchasedUseCase(placeId)
        app.recordPlaceNotificationUseCase(placeId)
    }

    private suspend fun handleSnooze(app: MapShoppingListApplication, placeId: Long) {
        Log.d(TAG, "Received snooze for placeId=$placeId")
        app.snoozePlaceNotificationsUseCase(placeId)
    }

    private fun cancelNotification(context: Context, placeId: Long) {
        NotificationManagerCompat.from(context).cancel(placeId.hashCode())
    }

    companion object {
        private const val TAG = "NotificationAction"
    }
}

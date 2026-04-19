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
        val pendingResult = goAsync()
        val app = context.applicationContext as MapShoppingListApplication
        val action = intent.action
        val placeId = intent.getLongExtra(NotificationActions.EXTRA_PLACE_ID, -1L)
        val itemId = intent.getLongExtra(NotificationActions.EXTRA_ITEM_ID, -1L)
        val itemIds = intent.getLongArrayExtra(NotificationActions.EXTRA_ITEM_IDS)?.toList().orEmpty()
        val notificationId = intent.getIntExtra(
            NotificationActions.EXTRA_NOTIFICATION_ID,
            when {
                placeId > 0L -> placeId.hashCode()
                itemId > 0L -> itemId.hashCode()
                else -> 0
            }
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    NotificationActions.ACTION_MARK_PURCHASED -> {
                        if (placeId <= 0L) {
                            Log.w(TAG, "Invalid placeId in notification action")
                            return@launch
                        }
                        handleMarkPurchased(app, placeId)
                    }
                    NotificationActions.ACTION_DELETE -> {
                        if (placeId <= 0L) {
                            Log.w(TAG, "Invalid placeId in notification action")
                            return@launch
                        }
                        handleDelete(app, placeId, itemIds)
                    }
                    NotificationActions.ACTION_MARK_ITEM_PURCHASED -> {
                        if (itemId <= 0L) {
                            Log.w(TAG, "Invalid itemId in notification action")
                            return@launch
                        }
                        handleMarkItemPurchased(app, itemId)
                    }
                    NotificationActions.ACTION_DELETE_ITEM -> {
                        if (itemId <= 0L) {
                            Log.w(TAG, "Invalid itemId in notification action")
                            return@launch
                        }
                        handleDeleteItem(app, itemId)
                    }
                    else -> Log.d(TAG, "Unknown action=${intent.action}")
                }
            } finally {
                cancelNotification(context, notificationId)
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleMarkPurchased(app: MapShoppingListApplication, placeId: Long) {
        Log.d(TAG, "Received mark purchased for placeId=$placeId")
        app.markPlaceItemsPurchasedUseCase(placeId)
        app.recordPlaceNotificationUseCase(placeId)
    }

    private suspend fun handleDelete(app: MapShoppingListApplication, placeId: Long, itemIds: List<Long>) {
        Log.d(TAG, "Received delete for placeId=$placeId items=$itemIds")
        val targets = itemIds.ifEmpty { app.shoppingListRepository.getItemsForPlace(placeId).map { it.id } }
        targets.forEach { id ->
            app.deleteShoppingItemUseCase(id)
        }
        app.recordPlaceNotificationUseCase(placeId)
        app.geofenceSyncScheduler.scheduleImmediateSync()
    }

    private suspend fun handleMarkItemPurchased(app: MapShoppingListApplication, itemId: Long) {
        Log.d(TAG, "Received mark purchased for itemId=$itemId")
        app.updatePurchasedStateUseCase(itemId, true)
    }

    private suspend fun handleDeleteItem(app: MapShoppingListApplication, itemId: Long) {
        Log.d(TAG, "Received delete for itemId=$itemId")
        app.deleteShoppingItemUseCase(itemId)
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    companion object {
        private const val TAG = "NotificationAction"
    }
}

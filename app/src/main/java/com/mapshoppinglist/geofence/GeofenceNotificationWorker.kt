package com.mapshoppinglist.geofence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mapshoppinglist.MapShoppingListApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeofenceNotificationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as MapShoppingListApplication
        val placeIds = inputData.getLongArray(KEY_PLACE_IDS)?.toList().orEmpty()
        if (placeIds.isEmpty()) {
            return@withContext Result.success()
        }
        val canShowNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(app).areNotificationsEnabled()
        }
        if (!canShowNotifications) {
            // POST_NOTIFICATIONS 権限が無い場合は通知処理を中断する
            return@withContext Result.success()
        }
        val now = System.currentTimeMillis()
        placeIds.forEach { placeId ->
            val place = app.placesRepository.findById(placeId) ?: return@forEach
            if (!app.shouldSendNotificationUseCase(placeId, now)) {
                return@forEach
            }
            val items = app.shoppingListRepository.getItemsForPlace(placeId)
            if (items.isEmpty()) return@forEach
            val message = app.buildNotificationMessageUseCase.invoke(place.name, items)
            app.notificationSender.showPlaceReminder(placeId, items.map { it.id }, message)
            app.recordPlaceNotificationUseCase(placeId, now)
        }
        return@withContext Result.success()
    }

    companion object {
        private const val WORK_NAME_PREFIX = "geofence_notification_"
        private const val KEY_PLACE_IDS = "place_ids"

        fun enqueue(context: Context, placeIds: List<Long>) {
            if (placeIds.isEmpty()) return
            val data = Data.Builder()
                .putLongArray(KEY_PLACE_IDS, placeIds.toLongArray())
                .build()
            val request = OneTimeWorkRequestBuilder<GeofenceNotificationWorker>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_PREFIX + placeIds.hashCode(),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

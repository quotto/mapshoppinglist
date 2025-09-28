package com.mapshoppinglist.geofence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mapshoppinglist.MapShoppingListApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeofenceNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as MapShoppingListApplication
        val placeIds = inputData.getLongArray(KEY_PLACE_IDS)?.toList().orEmpty()
        if (placeIds.isEmpty()) {
            return@withContext Result.success()
        }
        placeIds.forEach { placeId ->
            val place = app.placesRepository.findById(placeId) ?: return@forEach
            val items = app.shoppingListRepository.getItemsForPlace(placeId)
            if (items.isEmpty()) return@forEach
            val message = app.buildNotificationMessageUseCase.invoke(place.name, items)
            app.notificationSender.showPlaceReminder(placeId, message)
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

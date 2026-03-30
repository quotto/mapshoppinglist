package com.mapshoppinglist.nearby

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mapshoppinglist.MapShoppingListApplication
import com.mapshoppinglist.domain.model.NearbyStoreCandidate
import com.mapshoppinglist.domain.model.ShoppingItem
import java.util.concurrent.TimeUnit

class NearbySuggestionTriggerWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val locationProvider: CurrentLocationProvider = DefaultCurrentLocationProvider(appContext)
    private val eventLogWriter = NearbyActivityEventLogWriter.fromContext(appContext)

    override suspend fun doWork(): Result {
        if (!ActivityRecognitionPermission.isGranted(applicationContext)) {
            eventLogWriter.appendDiagnostic(null, "suggestion_skipped_missing_activity_recognition")
            Log.d(TAG, "Skipping nearby suggestion evaluation: ACTIVITY_RECOGNITION not granted")
            return Result.success()
        }

        val hasBackgroundLocation = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasBackgroundLocation) {
            eventLogWriter.appendDiagnostic(null, "suggestion_skipped_missing_background_location")
            Log.d(TAG, "Skipping nearby suggestion evaluation: background location not granted")
            return Result.success()
        }

        val canShowNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        }
        if (!canShowNotifications) {
            eventLogWriter.appendDiagnostic(null, "suggestion_skipped_notifications_disabled")
            Log.d(TAG, "Skipping nearby suggestion evaluation: notifications disabled")
            return Result.success()
        }

        val reason = inputData.getString(KEY_REASON) ?: REASON_UNSPECIFIED
        val app = applicationContext as MapShoppingListApplication
        val currentLocation = locationProvider.getCurrentLocation()
        if (currentLocation == null) {
            eventLogWriter.appendDiagnostic(null, "suggestion_skipped_location_unavailable reason=$reason")
            Log.d(TAG, "Skipping nearby suggestion evaluation: current location unavailable")
            return Result.success()
        }

        val items = app.getUnlinkedShoppingItemsUseCase()
        if (items.isEmpty()) {
            eventLogWriter.appendDiagnostic(null, "suggestion_skipped_no_unlinked_items reason=$reason")
            Log.d(TAG, "No unlinked items available for nearby suggestion: reason=$reason")
            return Result.success()
        }
        val now = System.currentTimeMillis()
        val latE6 = (currentLocation.latitude * 1_000_000).toInt()
        val lngE6 = (currentLocation.longitude * 1_000_000).toInt()
        val bestOpportunity = findBestOpportunity(
            app = app,
            items = items,
            location = currentLocation,
            now = now,
            latE6 = latE6,
            lngE6 = lngE6
        )
        if (bestOpportunity == null) {
            eventLogWriter.appendDiagnostic(null, "suggestion_no_opportunity reason=$reason")
            Log.d(TAG, "No nearby suggestion opportunity found: reason=$reason")
            return Result.success()
        }

        app.notificationSender.showNearbySuggestion(
            itemId = bestOpportunity.item.id,
            itemTitle = bestOpportunity.item.title,
            placeName = bestOpportunity.candidate.name,
            distanceMeters = bestOpportunity.candidate.distanceMeters
        )
        app.recordNearbySuggestionUseCase(
            itemId = bestOpportunity.item.id,
            candidatePlaceId = bestOpportunity.candidate.placeId,
            candidatePlaceName = bestOpportunity.candidate.name,
            now = now,
            latE6 = latE6,
            lngE6 = lngE6
        )
        eventLogWriter.appendDiagnostic(
            null,
            "suggestion_notified reason=$reason itemId=${bestOpportunity.item.id} placeId=${bestOpportunity.candidate.placeId} distance=${bestOpportunity.candidate.distanceMeters}"
        )
        Log.d(
            TAG,
            "Notified nearby suggestion: item=${bestOpportunity.item.id} place=${bestOpportunity.candidate.placeId} distance=${bestOpportunity.candidate.distanceMeters}"
        )
        return Result.success()
    }

    private suspend fun findBestOpportunity(
        app: MapShoppingListApplication,
        items: List<ShoppingItem>,
        location: Location,
        now: Long,
        latE6: Int,
        lngE6: Int
    ): NearbySuggestionOpportunity? {
        val opportunities = mutableListOf<NearbySuggestionOpportunity>()
        items.take(MAX_ITEM_EVALUATION_COUNT).forEach { item ->
            val searchQuery = item.title.trim()
            eventLogWriter.appendSuggestionSearchQuery(
                reason = inputData.getString(KEY_REASON) ?: REASON_UNSPECIFIED,
                itemTitle = item.title,
                searchQuery = searchQuery
            )
            val candidates = app.nearbyStoreSuggestionRepository.search(
                itemTitle = searchQuery,
                latitude = location.latitude,
                longitude = location.longitude,
                limit = MAX_PLACE_CANDIDATE_COUNT
            )
            val topCandidate = candidates.firstOrNull { candidate ->
                candidate.distanceMeters <= MAX_NOTIFICATION_DISTANCE_METERS &&
                    app.shouldSendNearbySuggestionUseCase(
                        itemId = item.id,
                        candidatePlaceId = candidate.placeId,
                        now = now,
                        currentLatE6 = latE6,
                        currentLngE6 = lngE6
                    )
            }
            eventLogWriter.appendSuggestionSearchResult(
                reason = inputData.getString(KEY_REASON) ?: REASON_UNSPECIFIED,
                itemTitle = item.title,
                candidates = candidates,
                selectedCandidate = topCandidate
            )
            if (topCandidate != null) {
                opportunities += NearbySuggestionOpportunity(item = item, candidate = topCandidate)
            }
        }
        return opportunities.minByOrNull { it.candidate.distanceMeters }
    }

    companion object {
        private const val TAG = "NearbySuggestTrigger"
        private const val WORK_NAME = "nearby_suggestion_trigger"
        private const val KEY_REASON = "reason"
        private const val MAX_NOTIFICATION_DISTANCE_METERS = 300
        private const val MAX_ITEM_EVALUATION_COUNT = 3
        private const val MAX_PLACE_CANDIDATE_COUNT = 5

        const val REASON_ACTIVITY_STILL = "activity_still"
        const val REASON_APP_START = "app_start"
        const val REASON_BOOT_RESTORE = "boot_restore"
        const val REASON_UNSPECIFIED = "unspecified"

        fun enqueueNow(context: Context, reason: String = REASON_UNSPECIFIED) {
            Log.d(TAG, "Enqueuing nearby suggestion trigger: reason=$reason")
            val request = OneTimeWorkRequestBuilder<NearbySuggestionTriggerWorker>()
                .setInputData(Data.Builder().putString(KEY_REASON, reason).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

private data class NearbySuggestionOpportunity(
    val item: ShoppingItem,
    val candidate: NearbyStoreCandidate
)

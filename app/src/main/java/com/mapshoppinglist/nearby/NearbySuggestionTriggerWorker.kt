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
import com.mapshoppinglist.notification.NotificationSender.NearbySuggestionNotificationEntry
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
        val opportunities = findNotificationOpportunities(
            app = app,
            items = items,
            location = currentLocation,
            now = now,
            latE6 = latE6,
            lngE6 = lngE6
        )
        Log.d(TAG, "Found nearby suggestion opportunities: reason=$reason count=${opportunities.size}")
        if (opportunities.isEmpty()) {
            eventLogWriter.appendDiagnostic(null, "suggestion_no_opportunity reason=$reason")
            return Result.success()
        }

        Log.d(TAG, "Found nearby suggestion opportunities: reason=$reason count=${opportunities.size}")
        app.notificationSender.showNearbySuggestion(
            entries = opportunities.map { opportunity ->
                NearbySuggestionNotificationEntry(
                    itemId = opportunity.item.id,
                    itemTitle = opportunity.item.title,
                    placeName = opportunity.candidate.name,
                    distanceMeters = opportunity.candidate.distanceMeters
                )
            }
        )
        opportunities.forEach { opportunity ->
            app.recordNearbySuggestionUseCase(
                itemId = opportunity.item.id,
                candidatePlaceId = opportunity.candidate.placeId,
                candidatePlaceName = opportunity.candidate.name,
                now = now,
                latE6 = latE6,
                lngE6 = lngE6
            )
        }
        eventLogWriter.appendDiagnostic(
            null,
            "suggestion_notified reason=$reason count=${opportunities.size} itemIds=${opportunities.joinToString(",") { it.item.id.toString() }}"
        )
        Log.d(
            TAG,
            "Notified nearby suggestion count=${opportunities.size} items=${opportunities.joinToString(",") { it.item.id.toString() }}"
        )
        return Result.success()
    }

    private suspend fun findNotificationOpportunities(
        app: MapShoppingListApplication,
        items: List<ShoppingItem>,
        location: Location,
        now: Long,
        latE6: Int,
        lngE6: Int
    ): List<NearbySuggestionOpportunity> {
        val opportunities = mutableListOf<NearbySuggestionOpportunity>()
        items.take(MAX_ITEM_EVALUATION_COUNT).forEach { item ->
            val canEvaluateItem = app.shouldSendNearbySuggestionUseCase.canEvaluateItem(
                itemId = item.id,
                now = now,
                currentLatE6 = latE6,
                currentLngE6 = lngE6
            )
            if (!canEvaluateItem) {
                val latestState = app.nearbySuggestionStateRepository.getLatestByItemId(item.id)
                val distanceMeters = latestState?.let { state ->
                    calculateDistanceMeters(
                        latE6 = latE6,
                        lngE6 = lngE6,
                        otherLatE6 = state.lastNotifiedLatE6,
                        otherLngE6 = state.lastNotifiedLngE6
                    )
                }
                eventLogWriter.appendDiagnostic(
                    null,
                    "suggestion_skipped_item_gate itemId=${item.id} itemTitle=${item.title} lastNotifiedAt=${latestState?.lastNotifiedAt ?: "none"} distanceMeters=${distanceMeters ?: "unknown"}"
                )
                return@forEach
            }
            val fallbackQuery = item.title.trim()
            val categoryQueries = app.nearbyStoreCategoryRepository.classify(item.title)
                .map { it.placeType.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(MAX_CATEGORY_QUERY_COUNT)
            val searchQueries = categoryQueries.ifEmpty { listOf(fallbackQuery) }
            searchQueries.forEach { searchQuery ->
                eventLogWriter.appendSuggestionSearchQuery(
                    reason = inputData.getString(KEY_REASON) ?: REASON_UNSPECIFIED,
                    itemTitle = item.title,
                    searchQuery = searchQuery
                )
            }
            val candidates = app.nearbyStoreSuggestionRepository.search(
                itemTitle = item.title,
                latitude = location.latitude,
                longitude = location.longitude,
                limit = MAX_PLACE_CANDIDATE_COUNT,
                searchQueries = searchQueries
            )
            val topCandidate = candidates.firstOrNull()
            if (topCandidate != null && topCandidate.distanceMeters > MAX_NOTIFICATION_DISTANCE_METERS) {
                eventLogWriter.appendDiagnostic(
                    null,
                    "suggestion_candidate_rejected itemId=${item.id} itemTitle=${item.title} placeId=${topCandidate.placeId} placeName=${topCandidate.name} candidateDistanceMeters=${topCandidate.distanceMeters} distanceGatePassed=false"
                )
            }
            eventLogWriter.appendSuggestionSearchResult(
                reason = inputData.getString(KEY_REASON) ?: REASON_UNSPECIFIED,
                itemTitle = item.title,
                candidates = candidates,
                selectedCandidate = topCandidate?.takeIf { it.distanceMeters <= MAX_NOTIFICATION_DISTANCE_METERS }
            )
            if (topCandidate != null && topCandidate.distanceMeters <= MAX_NOTIFICATION_DISTANCE_METERS) {
                opportunities += NearbySuggestionOpportunity(item = item, candidate = topCandidate)
            }
        }
        return opportunities
            .sortedBy { it.candidate.distanceMeters }
            .take(MAX_NOTIFICATION_ITEM_COUNT)
    }

    companion object {
        private const val TAG = "NearbySuggestTrigger"
        private const val WORK_NAME = "nearby_suggestion_trigger"
        private const val KEY_REASON = "reason"
        private const val MAX_NOTIFICATION_DISTANCE_METERS = 300
        private const val MAX_ITEM_EVALUATION_COUNT = 5
        private const val MAX_PLACE_CANDIDATE_COUNT = 5
        private const val MAX_CATEGORY_QUERY_COUNT = 3
        private const val MAX_NOTIFICATION_ITEM_COUNT = 5

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

    private fun calculateDistanceMeters(
        latE6: Int,
        lngE6: Int,
        otherLatE6: Int?,
        otherLngE6: Int?
    ): Int? {
        if (otherLatE6 == null || otherLngE6 == null) return null
        val latMeters = kotlin.math.abs(latE6 - otherLatE6) * 0.11132
        val lngMeters = kotlin.math.abs(lngE6 - otherLngE6) * 0.09100
        return kotlin.math.sqrt((latMeters * latMeters + lngMeters * lngMeters).toDouble()).toInt()
    }
}

private data class NearbySuggestionOpportunity(
    val item: ShoppingItem,
    val candidate: NearbyStoreCandidate
)

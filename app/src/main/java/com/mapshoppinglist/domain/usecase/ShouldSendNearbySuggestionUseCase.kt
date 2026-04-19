package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.NearbySuggestionStateRepository
import kotlin.math.abs

class ShouldSendNearbySuggestionUseCase(
    private val repository: NearbySuggestionStateRepository,
    private val revisitCooldownMillis: Long = DEFAULT_NEARBY_REVISIT_COOLDOWN_MILLIS,
    private val hardCooldownMillis: Long = DEFAULT_NEARBY_HARD_COOLDOWN_MILLIS,
    private val minDistanceMeters: Int = DEFAULT_MIN_DISTANCE_METERS
) {

    suspend fun canEvaluateItem(
        itemId: Long,
        now: Long = System.currentTimeMillis(),
        currentLatE6: Int? = null,
        currentLngE6: Int? = null
    ): Boolean {
        val state = repository.getLatestByItemId(itemId) ?: return true
        return canSendFromState(
            state = state,
            now = now,
            currentLatE6 = currentLatE6,
            currentLngE6 = currentLngE6
        )
    }

    suspend operator fun invoke(
        itemId: Long,
        candidatePlaceId: String,
        now: Long = System.currentTimeMillis(),
        currentLatE6: Int? = null,
        currentLngE6: Int? = null
    ): Boolean {
        return canEvaluateItem(
            itemId = itemId,
            now = now,
            currentLatE6 = currentLatE6,
            currentLngE6 = currentLngE6
        )
    }

    private fun canSendFromState(
        state: com.mapshoppinglist.domain.model.NearbySuggestionState,
        now: Long,
        currentLatE6: Int?,
        currentLngE6: Int?
    ): Boolean {
        val lastNotifiedAt = state.lastNotifiedAt
        if (lastNotifiedAt == null) return true

        val elapsedMillis = now - lastNotifiedAt
        if (elapsedMillis >= hardCooldownMillis) return true
        if (elapsedMillis < revisitCooldownMillis) return false

        val lat = currentLatE6
        val lng = currentLngE6
        val lastLat = state.lastNotifiedLatE6
        val lastLng = state.lastNotifiedLngE6
        if (lat == null || lng == null || lastLat == null || lastLng == null) return false

        val meters = approximateDistanceMeters(lat, lng, lastLat, lastLng)
        return meters >= minDistanceMeters
    }

    private fun approximateDistanceMeters(
        latE6: Int,
        lngE6: Int,
        otherLatE6: Int,
        otherLngE6: Int
    ): Int {
        val latMeters = abs(latE6 - otherLatE6) * METERS_PER_E6_LAT
        val lngMeters = abs(lngE6 - otherLngE6) * METERS_PER_E6_LNG
        return kotlin.math.sqrt((latMeters * latMeters + lngMeters * lngMeters).toDouble()).toInt()
    }
}

internal const val DEFAULT_NEARBY_REVISIT_COOLDOWN_MILLIS = 60 * 60 * 1000L
internal const val DEFAULT_NEARBY_HARD_COOLDOWN_MILLIS = 24 * 60 * 60 * 1000L
internal const val DEFAULT_MIN_DISTANCE_METERS = 300
private const val METERS_PER_E6_LAT = 0.11132
private const val METERS_PER_E6_LNG = 0.09100

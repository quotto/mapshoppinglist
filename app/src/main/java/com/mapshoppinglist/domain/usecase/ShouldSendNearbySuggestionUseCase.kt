package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.NearbySuggestionStateRepository
import kotlin.math.abs

class ShouldSendNearbySuggestionUseCase(
    private val repository: NearbySuggestionStateRepository,
    private val cooldownMillis: Long = DEFAULT_NEARBY_COOLDOWN_MILLIS,
    private val minDistanceMeters: Int = DEFAULT_MIN_DISTANCE_METERS
) {

    suspend operator fun invoke(
        itemId: Long,
        candidatePlaceId: String,
        now: Long = System.currentTimeMillis(),
        currentLatE6: Int? = null,
        currentLngE6: Int? = null
    ): Boolean {
        val state = repository.get(itemId, candidatePlaceId) ?: return true
        val lastNotifiedAt = state.lastNotifiedAt
        if (lastNotifiedAt != null && lastNotifiedAt + cooldownMillis > now) return false

        val lat = currentLatE6
        val lng = currentLngE6
        val lastLat = state.lastNotifiedLatE6
        val lastLng = state.lastNotifiedLngE6
        if (lat != null && lng != null && lastLat != null && lastLng != null) {
            val meters = approximateDistanceMeters(lat, lng, lastLat, lastLng)
            if (meters < minDistanceMeters) return false
        }
        return true
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

internal const val DEFAULT_NEARBY_COOLDOWN_MILLIS = 24 * 60 * 60 * 1000L
internal const val DEFAULT_MIN_DISTANCE_METERS = 150
private const val METERS_PER_E6_LAT = 0.11132
private const val METERS_PER_E6_LNG = 0.09100

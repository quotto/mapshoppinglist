package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.NotificationState
import com.mapshoppinglist.domain.repository.NotificationStateRepository

class SnoozePlaceNotificationsUseCase(
    private val notificationStateRepository: NotificationStateRepository
) {

    suspend operator fun invoke(placeId: Long, durationMillis: Long = DEFAULT_SNOOZE_DURATION_MS) {
        val now = System.currentTimeMillis()
        val current = notificationStateRepository.get(placeId)
        val updated = NotificationState(
            placeId = placeId,
            lastNotifiedAt = current?.lastNotifiedAt,
            snoozeUntil = now + durationMillis
        )
        notificationStateRepository.upsert(updated)
    }

    companion object {
        const val DEFAULT_SNOOZE_DURATION_MS = 2 * 60 * 60 * 1000L // 2時間
    }
}

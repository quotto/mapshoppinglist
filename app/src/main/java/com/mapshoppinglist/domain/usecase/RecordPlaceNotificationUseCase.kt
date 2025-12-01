package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.NotificationState
import com.mapshoppinglist.domain.repository.NotificationStateRepository

class RecordPlaceNotificationUseCase(private val notificationStateRepository: NotificationStateRepository) {

    suspend operator fun invoke(placeId: Long, now: Long = System.currentTimeMillis()) {
        val current = notificationStateRepository.get(placeId)
        val updated = NotificationState(
            placeId = placeId,
            lastNotifiedAt = now,
            snoozeUntil = null
        )
        notificationStateRepository.upsert(updated)
    }
}

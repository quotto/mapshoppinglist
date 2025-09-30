package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.NotificationStateRepository

class ShouldSendNotificationUseCase(
    private val notificationStateRepository: NotificationStateRepository,
    private val coolDownMillis: Long = DEFAULT_COOLDOWN_MILLIS
) {

    suspend operator fun invoke(placeId: Long, now: Long = System.currentTimeMillis()): Boolean {
        val state = notificationStateRepository.get(placeId) ?: return true
        val snoozeUntil = state.snoozeUntil
        if (snoozeUntil != null && snoozeUntil > now) return false
        val lastNotified = state.lastNotifiedAt
        if (lastNotified != null && lastNotified + coolDownMillis > now) return false
        return true
    }
}

private const val DEFAULT_COOLDOWN_MILLIS = 5 * 60 * 1000L

package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.NotificationState
import com.mapshoppinglist.domain.repository.NotificationStateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class InMemoryNotificationStateRepository : NotificationStateRepository {
    private val map = mutableMapOf<Long, NotificationState>()

    override suspend fun get(placeId: Long): NotificationState? = map[placeId]

    override suspend fun upsert(state: NotificationState) {
        map[state.placeId] = state
    }

    override suspend fun clear(placeId: Long) {
        map.remove(placeId)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationStateUseCasesTest {

    private lateinit var repository: InMemoryNotificationStateRepository

    @Before
    fun setUp() {
        repository = InMemoryNotificationStateRepository()
    }

    @Test
    fun shouldSendReturnsFalseWhenSnoozed() = runTest {
        repository.upsert(NotificationState(1L, lastNotifiedAt = null, snoozeUntil = System.currentTimeMillis() + 60_000))
        val useCase = ShouldSendNotificationUseCase(repository)
        assertFalse(useCase(1L, System.currentTimeMillis()))
    }

    @Test
    fun shouldSendReturnsFalseWithinCooldown() = runTest {
        val now = System.currentTimeMillis()
        repository.upsert(NotificationState(1L, lastNotifiedAt = now - 1_000, snoozeUntil = null))
        val useCase = ShouldSendNotificationUseCase(repository, coolDownMillis = 10_000)
        assertFalse(useCase(1L, now))
    }

    @Test
    fun recordNotificationClearsSnooze() = runTest {
        repository.upsert(NotificationState(1L, lastNotifiedAt = null, snoozeUntil = System.currentTimeMillis() + 10_000))
        val record = RecordPlaceNotificationUseCase(repository)
        val shouldSend = ShouldSendNotificationUseCase(repository, coolDownMillis = 0)
        record(1L)
        assertTrue(shouldSend(1L, System.currentTimeMillis()))
    }

    @Test
    fun snoozeUpdatesState() = runTest {
        val snooze = SnoozePlaceNotificationsUseCase(repository)
        snooze(1L, durationMillis = 1_000)
        val state = repository.get(1L)
        assertTrue(state?.snoozeUntil ?: 0L > System.currentTimeMillis())
    }
}


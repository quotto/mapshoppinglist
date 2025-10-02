package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.NotificationState
import com.mapshoppinglist.domain.repository.NotificationStateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

/**
 * 通知クールダウンと履歴更新のユースケースを検証するテスト。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationStateUseCasesTest {

    private lateinit var repository: InMemoryNotificationStateRepository

    @Before
    fun setUp() {
        repository = InMemoryNotificationStateRepository()
    }

    @Test
    fun shouldSendReturnsTrueWhenNoHistory() = runTest {
        val useCase = ShouldSendNotificationUseCase(repository, coolDownMillis = 10_000)
        assertTrue(useCase(1L, System.currentTimeMillis()))
    }

    @Test
    fun shouldSendReturnsFalseWithinCooldown() = runTest {
        val now = System.currentTimeMillis()
        repository.upsert(NotificationState(1L, lastNotifiedAt = now - 1_000, snoozeUntil = null))
        val useCase = ShouldSendNotificationUseCase(repository, coolDownMillis = 10_000)
        assertFalse(useCase(1L, now))
    }

    @Test
    fun shouldSendReturnsTrueAfterCooldown() = runTest {
        val now = System.currentTimeMillis()
        repository.upsert(NotificationState(1L, lastNotifiedAt = now - 20_000, snoozeUntil = null))
        val useCase = ShouldSendNotificationUseCase(repository, coolDownMillis = 5_000)
        assertTrue(useCase(1L, now))
    }

    @Test
    fun recordNotificationUpdatesState() = runTest {
        val record = RecordPlaceNotificationUseCase(repository)
        val timestamp = 12345L
        record(1L, timestamp)
        val state = repository.get(1L)
        assertEquals(timestamp, state?.lastNotifiedAt)
        assertNull(state?.snoozeUntil)
    }

    @Test
    fun recordNotificationEnforcesCooldown() = runTest {
        val record = RecordPlaceNotificationUseCase(repository)
        val shouldSend = ShouldSendNotificationUseCase(repository)
        record(1L, now = 0L)
        assertFalse(shouldSend(1L, now = DEFAULT_COOLDOWN_MILLIS - 1L))
        assertTrue(shouldSend(1L, now = DEFAULT_COOLDOWN_MILLIS + 1L))
    }
}

private const val DEFAULT_COOLDOWN_MILLIS = 5 * 60 * 1000L

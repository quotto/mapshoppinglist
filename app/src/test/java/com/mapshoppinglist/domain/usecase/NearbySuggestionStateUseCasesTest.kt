package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.NearbySuggestionState
import com.mapshoppinglist.domain.repository.NearbySuggestionStateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class InMemoryNearbySuggestionStateRepository : NearbySuggestionStateRepository {
    private val map = mutableMapOf<Pair<Long, String>, NearbySuggestionState>()

    override suspend fun get(itemId: Long, candidatePlaceId: String): NearbySuggestionState? {
        return map[itemId to candidatePlaceId]
    }

    override suspend fun upsert(state: NearbySuggestionState) {
        map[state.itemId to state.candidatePlaceId] = state
    }

    override suspend fun clear(itemId: Long, candidatePlaceId: String) {
        map.remove(itemId to candidatePlaceId)
    }

    override suspend fun clearByItemId(itemId: Long) {
        map.keys.removeAll { it.first == itemId }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NearbySuggestionStateUseCasesTest {

    private lateinit var repository: InMemoryNearbySuggestionStateRepository

    @Before
    fun setUp() {
        repository = InMemoryNearbySuggestionStateRepository()
    }

    @Test
    fun shouldSendReturnsTrueWhenNoHistory() = runTest {
        val useCase = ShouldSendNearbySuggestionUseCase(repository, cooldownMillis = 10_000, minDistanceMeters = 100)
        assertTrue(useCase(itemId = 1L, candidatePlaceId = "place-a", now = 1_000L))
    }

    @Test
    fun shouldSendReturnsFalseWithinCooldown() = runTest {
        repository.upsert(
            NearbySuggestionState(
                itemId = 1L,
                candidatePlaceId = "place-a",
                candidatePlaceName = "A",
                lastNotifiedAt = 1_000L,
                lastNotifiedLatE6 = null,
                lastNotifiedLngE6 = null
            )
        )
        val useCase = ShouldSendNearbySuggestionUseCase(repository, cooldownMillis = 10_000, minDistanceMeters = 100)
        assertFalse(useCase(itemId = 1L, candidatePlaceId = "place-a", now = 5_000L))
    }

    @Test
    fun shouldSendReturnsFalseWhenCurrentLocationIsAlmostSame() = runTest {
        repository.upsert(
            NearbySuggestionState(
                itemId = 1L,
                candidatePlaceId = "place-a",
                candidatePlaceName = "A",
                lastNotifiedAt = 0L,
                lastNotifiedLatE6 = 35_000_000,
                lastNotifiedLngE6 = 139_000_000
            )
        )
        val useCase = ShouldSendNearbySuggestionUseCase(repository, cooldownMillis = 1L, minDistanceMeters = 150)
        assertFalse(
            useCase(
                itemId = 1L,
                candidatePlaceId = "place-a",
                now = DEFAULT_NEARBY_COOLDOWN_MILLIS + 1L,
                currentLatE6 = 35_000_500,
                currentLngE6 = 139_000_500
            )
        )
    }

    @Test
    fun recordNearbySuggestionStoresState() = runTest {
        val useCase = RecordNearbySuggestionUseCase(repository)
        useCase(
            itemId = 2L,
            candidatePlaceId = "place-b",
            candidatePlaceName = "B",
            now = 123L,
            latE6 = 35_100_000,
            lngE6 = 139_100_000
        )
        val state = repository.get(2L, "place-b")
        assertEquals(123L, state?.lastNotifiedAt)
        assertEquals("B", state?.candidatePlaceName)
        assertEquals(35_100_000, state?.lastNotifiedLatE6)
        assertEquals(139_100_000, state?.lastNotifiedLngE6)
    }
}

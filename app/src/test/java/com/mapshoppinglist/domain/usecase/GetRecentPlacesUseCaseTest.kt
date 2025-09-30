package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.repository.PlacesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class RecentPlacesRepositoryStub : PlacesRepository {
    var recentPlaces: List<Place> = emptyList()

    override suspend fun getTotalCount(): Int = recentPlaces.size

    override suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean = false

    override suspend fun loadActivePlaces(): List<Place> = emptyList()

    override suspend fun findById(placeId: Long): Place? = recentPlaces.find { it.id == placeId }

    override suspend fun addPlace(name: String, latE6: Int, lngE6: Int, note: String?): Long = 0L

    override suspend fun deletePlace(placeId: Long) {}

    override suspend fun linkItemToPlace(placeId: Long, itemId: Long) {}

    override suspend fun unlinkItemFromPlace(placeId: Long, itemId: Long) {}

    override suspend fun loadRecentPlaces(limit: Int): List<Place> = recentPlaces.take(limit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class GetRecentPlacesUseCaseTest {

    private lateinit var repository: RecentPlacesRepositoryStub
    private lateinit var useCase: GetRecentPlacesUseCase

    @Before
    fun setUp() {
        repository = RecentPlacesRepositoryStub()
        useCase = GetRecentPlacesUseCase(repository)
    }

    @Test
    fun `returns places up to requested limit`() = runTest {
        repository.recentPlaces = listOf(
            Place(1L, "スーパーA", 0, 0, true),
            Place(2L, "コンビニB", 0, 0, false)
        )

        val result = useCase(limit = 1)

        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws when limit is not positive`() = runTest {
        useCase(limit = 0)
    }
}

package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.exception.DuplicatePlaceException
import com.mapshoppinglist.domain.exception.PlaceLimitExceededException
import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.repository.PlacesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

private class CountingPlacesRepository(var count: Int = 0, var duplicates: Set<Pair<Int, Int>> = emptySet()) : PlacesRepository {
    override suspend fun getTotalCount(): Int = count

    override suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean = duplicates.contains(latE6 to lngE6)

    override suspend fun loadActivePlaces(): List<Place> = emptyList()

    override suspend fun findById(placeId: Long): Place? = null

    override suspend fun loadAll(): List<Place> = emptyList()

    override suspend fun addPlace(name: String, latE6: Int, lngE6: Int, note: String?): Long = throw NotImplementedError()

    override suspend fun deletePlace(placeId: Long): Unit = throw NotImplementedError()

    override suspend fun updateName(placeId: Long, newName: String): Unit = throw NotImplementedError()

    override suspend fun linkItemToPlace(placeId: Long, itemId: Long): Unit = throw NotImplementedError()

    override suspend fun unlinkItemFromPlace(placeId: Long, itemId: Long): Unit = throw NotImplementedError()

    override suspend fun loadRecentPlaces(limit: Int): List<Place> = emptyList()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ValidatePlaceRegistrationUseCaseTest {

    @Test(expected = PlaceLimitExceededException::class)
    fun throwsWhenLimitExceeded() = runTest {
        val repository = CountingPlacesRepository(count = 100)
        val useCase = ValidatePlaceRegistrationUseCase(repository)
        useCase(1, 1)
    }

    @Test(expected = DuplicatePlaceException::class)
    fun throwsWhenDuplicateLocation() = runTest {
        val repository = CountingPlacesRepository(
            count = 10,
            duplicates = setOf(35_000_000 to 139_000_000)
        )
        val useCase = ValidatePlaceRegistrationUseCase(repository)
        useCase(35_000_000, 139_000_000)
    }

    @Test
    fun passesWhenConstraintsSatisfied() = runTest {
        val repository = CountingPlacesRepository(count = 5)
        val useCase = ValidatePlaceRegistrationUseCase(repository)
        useCase(35_000_001, 139_000_001)
    }
}

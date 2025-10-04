package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.GeofenceRegistration
import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.repository.GeofenceRegistryRepository
import com.mapshoppinglist.domain.repository.PlacesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class GeofenceTestPlacesRepository : PlacesRepository {
    var places: List<Place> = emptyList()

    override suspend fun getTotalCount(): Int = places.size

    override suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean =
        places.any { it.latitudeE6 == latE6 && it.longitudeE6 == lngE6 }

    override suspend fun loadActivePlaces(): List<Place> = places.filter { it.isActive }

    override suspend fun findById(placeId: Long): Place? = places.find { it.id == placeId }

    override suspend fun loadAll(): List<Place> = places

    override suspend fun addPlace(name: String, latE6: Int, lngE6: Int, note: String?): Long {
        throw NotImplementedError()
    }

    override suspend fun deletePlace(placeId: Long) {
        throw NotImplementedError()
    }

    override suspend fun updateName(placeId: Long, newName: String) {
        throw NotImplementedError()
    }

    override suspend fun linkItemToPlace(placeId: Long, itemId: Long) {
        throw NotImplementedError()
    }

    override suspend fun unlinkItemFromPlace(placeId: Long, itemId: Long) {
        throw NotImplementedError()
    }

    override suspend fun loadRecentPlaces(limit: Int): List<Place> = places.take(limit)
}

private class FakeGeofenceRegistryRepository : GeofenceRegistryRepository {
    var registrations: List<GeofenceRegistration> = emptyList()

    override suspend fun loadAll(): List<GeofenceRegistration> = registrations

    override suspend fun replaceAll(registrations: List<GeofenceRegistration>) {
        this.registrations = registrations
    }

    override suspend fun deleteByPlaceIds(placeIds: List<Long>) {
        registrations = registrations.filterNot { it.placeId in placeIds }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BuildGeofenceSyncPlanUseCaseTest {

    private lateinit var placesRepository: GeofenceTestPlacesRepository
    private lateinit var registryRepository: FakeGeofenceRegistryRepository
    private lateinit var useCase: BuildGeofenceSyncPlanUseCase

    @Before
    fun setUp() {
        placesRepository = GeofenceTestPlacesRepository()
        registryRepository = FakeGeofenceRegistryRepository()
        useCase = BuildGeofenceSyncPlanUseCase(placesRepository, registryRepository)
    }

    @Test
    fun planAddsActivePlaceWhenNotRegistered() = runTest {
        placesRepository.places = listOf(
            Place(1L, "スーパー", 35_000_000, 139_000_000, isActive = true)
        )
        val plan = useCase()
        assertEquals(1, plan.toAdd.size)
        assertTrue(plan.toRemoveRequestIds.isEmpty())
        assertEquals("place_1", plan.toAdd.first().requestId)
    }

    @Test
    fun planRemovesInactiveRegistrations() = runTest {
        placesRepository.places = emptyList()
        registryRepository.registrations = listOf(
            GeofenceRegistration(placeId = 42L, requestId = "place_42")
        )
        val plan = useCase()
        assertEquals(listOf("place_42"), plan.toRemoveRequestIds)
        assertTrue(plan.toAdd.isEmpty())
        assertTrue(plan.targetRegistrations.isEmpty())
    }
}

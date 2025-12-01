package com.mapshoppinglist.ui.placemanage

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapshoppinglist.R
import com.mapshoppinglist.domain.model.GeofenceRegistration
import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.repository.GeofenceRegistryRepository
import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.domain.usecase.DeletePlaceUseCase
import com.mapshoppinglist.domain.usecase.LoadAllPlacesUseCase
import com.mapshoppinglist.domain.usecase.LoadRegisteredGeofencesUseCase
import com.mapshoppinglist.domain.usecase.UpdatePlaceNameUseCase
import com.mapshoppinglist.geofence.GeofenceSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PlaceManagementViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: FakePlacesRepository
    private lateinit var scheduler: RecordingScheduler
    private lateinit var geofenceRepository: FakeGeofenceRegistryRepository
    private lateinit var viewModel: PlaceManagementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakePlacesRepository().apply {
            places += Place(
                id = 1L,
                name = "スーパーA",
                latitudeE6 = 35_123_456,
                longitudeE6 = 139_654_321,
                isActive = true
            )
        }
        val context: Context = ApplicationProvider.getApplicationContext()
        scheduler = RecordingScheduler(context)
        geofenceRepository = FakeGeofenceRegistryRepository().apply {
            registrations = mutableListOf(
                GeofenceRegistration(placeId = 1L, requestId = "place_1")
            )
        }
        viewModel = PlaceManagementViewModel(
            loadAllPlacesUseCase = LoadAllPlacesUseCase(repository),
            loadRegisteredGeofencesUseCase = LoadRegisteredGeofencesUseCase(geofenceRepository),
            updatePlaceNameUseCase = UpdatePlaceNameUseCase(repository, scheduler),
            deletePlaceUseCase = DeletePlaceUseCase(repository, scheduler)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initLoadsPlaces() = runTest(dispatcher) {
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.places.size)
        assertEquals("スーパーA", viewModel.uiState.value.places.first().name)
        assertTrue(viewModel.uiState.value.places.first().isSubscribed)
    }

    @Test
    fun editPlaceUpdatesRepositoryAndEmitsSnackbar() = runTest(dispatcher) {
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }

        viewModel.onEditRequested(1L)
        viewModel.onEditNameChange(" 新しい名前")
        viewModel.onEditConfirm()

        advanceUntilIdle()
        val event = eventDeferred.await() as PlaceManagementEvent.ShowSnackbar
        assertEquals(R.string.place_management_snackbar_updated, event.messageResId)
        assertEquals("新しい名前", repository.places.first().name)
    }

    @Test
    fun deletePlaceRemovesRepositoryAndEmitsSnackbar() = runTest(dispatcher) {
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }

        viewModel.onDeleteRequested(1L)
        viewModel.onDeleteConfirm()

        advanceUntilIdle()
        val event = eventDeferred.await() as PlaceManagementEvent.ShowSnackbar
        assertEquals(R.string.place_management_snackbar_deleted, event.messageResId)
        assertTrue(repository.places.isEmpty())
        assertEquals(1, scheduler.count)
    }

    @Test
    fun refreshReflectsSubscriptionState() = runTest(dispatcher) {
        advanceUntilIdle()
        geofenceRepository.registrations.clear()
        viewModel.refresh()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.places.first().isSubscribed)
    }
}

private class FakeGeofenceRegistryRepository : GeofenceRegistryRepository {
    var registrations: MutableList<GeofenceRegistration> = mutableListOf()

    override suspend fun loadAll(): List<GeofenceRegistration> = registrations.toList()

    override suspend fun replaceAll(registrations: List<GeofenceRegistration>) {
        this.registrations = registrations.toMutableList()
    }

    override suspend fun deleteByPlaceIds(placeIds: List<Long>) {
        registrations.removeAll { it.placeId in placeIds }
    }
}

private class FakePlacesRepository : PlacesRepository {
    val places = mutableListOf<Place>()

    override suspend fun getTotalCount(): Int = places.size

    override suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean = places.any { it.latitudeE6 == latE6 && it.longitudeE6 == lngE6 }

    override suspend fun loadActivePlaces(): List<Place> = places.filter { it.isActive }

    override suspend fun findById(placeId: Long): Place? = places.find { it.id == placeId }

    override suspend fun loadAll(): List<Place> = places.toList()

    override suspend fun addPlace(name: String, latE6: Int, lngE6: Int, note: String?): Long {
        val nextId = (places.maxOfOrNull { it.id } ?: 0L) + 1L
        val newPlace = Place(
            id = nextId,
            name = name,
            latitudeE6 = latE6,
            longitudeE6 = lngE6,
            isActive = false
        )
        places += newPlace
        return nextId
    }

    override suspend fun deletePlace(placeId: Long) {
        places.removeAll { it.id == placeId }
    }

    override suspend fun updateName(placeId: Long, newName: String) {
        val index = places.indexOfFirst { it.id == placeId }
        if (index >= 0) {
            val current = places[index]
            places[index] = current.copy(name = newName.trim())
        }
    }

    override suspend fun linkItemToPlace(placeId: Long, itemId: Long) {
        // 未使用のため処理なし
    }

    override suspend fun unlinkItemFromPlace(placeId: Long, itemId: Long) {
        // 未使用のため処理なし
    }

    override suspend fun loadRecentPlaces(limit: Int): List<Place> = places.take(limit)
}

private class RecordingScheduler(context: Context) : GeofenceSyncScheduler(context) {
    var count = 0
    override fun scheduleImmediateSync() {
        count += 1
    }
}

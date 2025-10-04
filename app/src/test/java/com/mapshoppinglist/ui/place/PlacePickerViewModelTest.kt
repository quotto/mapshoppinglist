package com.mapshoppinglist.ui.place

import android.app.Application
import android.content.ContextWrapper
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPhotoResponse
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.libraries.places.api.net.IsOpenRequest
import com.google.android.libraries.places.api.net.IsOpenResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.libraries.places.api.net.SearchByTextResponse
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.libraries.places.api.net.SearchNearbyResponse
import com.mapshoppinglist.domain.model.Place as DomainPlace
import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.domain.usecase.CreatePlaceUseCase
import com.mapshoppinglist.domain.usecase.ValidatePlaceRegistrationUseCase
import com.mapshoppinglist.geofence.GeofenceSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * POIタップ時の状態遷移を検証するテスト。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlacePickerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var placesRepository: FakePlacesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        application = TestApplication()
        placesRepository = FakePlacesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onPoiClick updates selected place and camera`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val latLng = LatLng(35.0, 139.0)
        val poi = PointOfInterest(latLng, "poi-id", "テストPOI")

        viewModel.onPoiClick(poi)

        val state = viewModel.uiState.value
        assertEquals("テストPOI", state.selectedPlace?.name)
        assertEquals(latLng, state.selectedPlace?.latLng)
        assertEquals(latLng, state.cameraLocation)
        assertEquals("テストPOI", state.query)
    }

    private fun createViewModel(): PlacePickerViewModel {
        val validate = ValidatePlaceRegistrationUseCase(placesRepository, maxPlaces = 100)
        val create = CreatePlaceUseCase(validate, placesRepository, TestGeofenceSyncScheduler(application))
        return PlacePickerViewModel(
            application = application,
            createPlaceUseCase = create,
            placesClient = FakePlacesClient()
        )
    }
}

private class FakePlacesRepository : PlacesRepository {
    override suspend fun getTotalCount(): Int = 0

    override suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean = false

    override suspend fun loadActivePlaces(): List<DomainPlace> = emptyList()

    override suspend fun findById(placeId: Long): DomainPlace? = null

    override suspend fun loadAll(): List<DomainPlace> = emptyList()

    override suspend fun addPlace(name: String, latE6: Int, lngE6: Int, note: String?): Long = 1L

    override suspend fun deletePlace(placeId: Long) {}

    override suspend fun updateName(placeId: Long, newName: String) {}

    override suspend fun linkItemToPlace(placeId: Long, itemId: Long) {}

    override suspend fun unlinkItemFromPlace(placeId: Long, itemId: Long) {}

    override suspend fun loadRecentPlaces(limit: Int): List<DomainPlace> = emptyList()
}

private class TestGeofenceSyncScheduler(context: Application) : GeofenceSyncScheduler(ContextWrapper(context)) {
    override fun scheduleImmediateSync() {}
}

private class TestApplication : Application()

private class FakePlacesClient : PlacesClient {
    override fun fetchPhoto(request: FetchPhotoRequest): Task<FetchPhotoResponse> = unsupported()

    override fun fetchPlace(request: FetchPlaceRequest): Task<FetchPlaceResponse> = unsupported()

    override fun fetchResolvedPhotoUri(request: FetchResolvedPhotoUriRequest): Task<FetchResolvedPhotoUriResponse> = unsupported()

    override fun findAutocompletePredictions(request: FindAutocompletePredictionsRequest): Task<FindAutocompletePredictionsResponse> = unsupported()

    override fun findCurrentPlace(request: FindCurrentPlaceRequest): Task<FindCurrentPlaceResponse> = unsupported()

    override fun isOpen(request: IsOpenRequest): Task<IsOpenResponse> = unsupported()

    override fun searchByText(request: SearchByTextRequest): Task<SearchByTextResponse> = unsupported()

    override fun searchNearby(request: SearchNearbyRequest): Task<SearchNearbyResponse> = unsupported()

    override fun zzb(request: FetchPlaceRequest, i: Int): Task<FetchPlaceResponse> = unsupported()

    override fun zzd(request: FindAutocompletePredictionsRequest, i: Int): Task<FindAutocompletePredictionsResponse> = unsupported()

    private fun <T> unsupported(): Task<T> {
        return Tasks.forException(UnsupportedOperationException("Not implemented in tests"))
    }
}

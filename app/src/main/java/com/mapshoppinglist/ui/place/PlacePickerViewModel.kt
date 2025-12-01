package com.mapshoppinglist.ui.place

import android.app.Application
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.mapshoppinglist.domain.usecase.CreatePlaceUseCase
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PlacePickerViewModel(
    application: Application,
    private val createPlaceUseCase: CreatePlaceUseCase,
    private val placesClient: PlacesClient = Places.createClient(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlacePickerUiState())
    val uiState: StateFlow<PlacePickerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PlacePickerEvent>()
    val events: SharedFlow<PlacePickerEvent> = _events.asSharedFlow()

    private var searchJob: Job? = null
    private val geocoder = Geocoder(application, Locale.getDefault())

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.length < 2) {
            searchJob?.cancel()
            _uiState.update { it.copy(predictions = emptyList(), isLoading = false, errorMessage = null) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            var origin = DEFAULT_LOCATION
            _uiState.update { state ->
                origin = state.searchOrigin
                state.copy(isLoading = true, errorMessage = null)
            }
            try {
                val requestBuilder = SearchByTextRequest.builder(
                    query,
                    PLACE_FIELDS_FOR_SEARCH
                )
                    .setMaxResultCount(SEARCH_RESULT_LIMIT)
                    .setRankPreference(SearchByTextRequest.RankPreference.DISTANCE)

                requestBuilder.setLocationBias(CircularBounds.newInstance(origin, SEARCH_RADIUS_METERS))

                val response = placesClient.searchByText(requestBuilder.build()).await()
                val predictions = response.places.mapNotNull { place ->
                    val id = place.id ?: return@mapNotNull null
                    val primary = place.name ?: place.address ?: return@mapNotNull null
                    PlacePredictionUiModel(
                        placeId = id,
                        primaryText = primary,
                        secondaryText = place.address
                    )
                }
                _uiState.update {
                    it.copy(
                        predictions = predictions,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    fun onPredictionSelected(prediction: PlacePredictionUiModel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val request = FetchPlaceRequest.builder(
                    prediction.placeId,
                    listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                ).build()
                val response = placesClient.fetchPlace(request).await()
                val place = response.place
                val latLng = place.latLng ?: throw IllegalStateException("Place has no latLng")
                _uiState.update {
                    it.copy(
                        query = place.name ?: prediction.primaryText,
                        predictions = emptyList(),
                        selectedPlace = SelectedPlaceUiModel(
                            placeId = place.id ?: prediction.placeId,
                            name = place.name ?: prediction.primaryText,
                            address = place.address,
                            latLng = latLng
                        ),
                        cameraLocation = latLng,
                        searchOrigin = latLng,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    fun confirmSelection() {
        val selected = _uiState.value.selectedPlace ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }
            try {
                val placeId = createPlaceUseCase(
                    CreatePlaceUseCase.Params(
                        name = selected.name,
                        latitude = selected.latLng.latitude,
                        longitude = selected.latLng.longitude,
                        note = selected.address
                    )
                )
                _events.emit(PlacePickerEvent.PlaceRegistered(placeId))
                _uiState.value = PlacePickerUiState(
                    cameraLocation = selected.latLng,
                    searchOrigin = selected.latLng
                )
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isCreating = false, errorMessage = error.message)
                }
            }
        }
    }

    fun onClearSelection() {
        _uiState.update { it.copy(selectedPlace = null) }
    }

    fun onMapLongClick(latLng: LatLng) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val inferred = reverseGeocode(latLng)
            _uiState.update {
                it.copy(
                    selectedPlace = SelectedPlaceUiModel(
                        placeId = "manual_${latLng.latitude}_${latLng.longitude}",
                        name = inferred ?: "選択した地点",
                        address = inferred,
                        latLng = latLng
                    ),
                    query = inferred ?: "",
                    predictions = emptyList(),
                    isLoading = false,
                    cameraLocation = latLng,
                    searchOrigin = latLng
                )
            }
        }
    }

    fun onPoiClick(poi: PointOfInterest) {
        val latLng = poi.latLng
        val name = poi.name
        _uiState.update {
            it.copy(
                query = name,
                predictions = emptyList(),
                selectedPlace = SelectedPlaceUiModel(
                    placeId = poi.placeId,
                    name = name,
                    address = null,
                    latLng = latLng
                ),
                cameraLocation = latLng,
                searchOrigin = latLng,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    /**
     * Maps SDK 側でカメラ位置を決定した際に UI 状態へ反映するヘルパー。
     * 現在地ボタンなどアプリ主導の移動では、検索の基準点も同じ地点に揃えたいので
     * cameraLocation/searchOrigin の双方を同時に更新する。
     */
    fun updateCameraLocation(latLng: LatLng) {
        _uiState.update { it.copy(cameraLocation = latLng, searchOrigin = latLng) }
    }

    /**
     * ユーザーのドラッグ/ピンチなど UI 操作でカメラが移動した際に呼び出し、
     * 検索 origin のみを最新カメラ位置へ更新する。
     * cameraLocation はアプリが最後に指示した座標を維持するため変更しない。
     */
    fun onCameraMoved(latLng: LatLng) {
        val current = _uiState.value.searchOrigin
        if (current == latLng) return
        _uiState.update { it.copy(searchOrigin = latLng) }
    }

    private suspend fun reverseGeocode(latLng: LatLng): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            results?.firstOrNull()?.getAddressLine(0)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Geocoder failed", e)
            null
        }
    }
}

sealed interface PlacePickerEvent {
    data class PlaceRegistered(val placeId: Long) : PlacePickerEvent
}

data class PlacePickerUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val predictions: List<PlacePredictionUiModel> = emptyList(),
    val selectedPlace: SelectedPlaceUiModel? = null,
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
    val cameraLocation: LatLng = DEFAULT_LOCATION,
    val searchOrigin: LatLng = DEFAULT_LOCATION
)

data class PlacePredictionUiModel(val placeId: String, val primaryText: String, val secondaryText: String?)

data class SelectedPlaceUiModel(val placeId: String, val name: String, val address: String?, val latLng: LatLng)

val DEFAULT_LOCATION: LatLng = LatLng(35.681236, 139.767125)

private const val SEARCH_RESULT_LIMIT = 8

/**
 * SearchText API に与えるバイアスの半径 (m)。
 * 3km 程度に抑えることで現在地周辺の候補に集中させつつ、ユーザー移動時の再検索回数も抑制する。
 */
private const val SEARCH_RADIUS_METERS = 3_000.0
private val PLACE_FIELDS_FOR_SEARCH = listOf(
    Place.Field.ID,
    Place.Field.NAME,
    Place.Field.ADDRESS,
    Place.Field.LAT_LNG
)

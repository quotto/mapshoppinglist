package com.mapshoppinglist.ui.place

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.mapshoppinglist.domain.usecase.CreatePlaceUseCase
import java.util.Locale
import android.location.Geocoder
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .setTypeFilter(TypeFilter.ESTABLISHMENT)
                    .build()
                val response = placesClient.findAutocompletePredictions(request).await()
                val predictions = response.autocompletePredictions.map { prediction ->
                    PlacePredictionUiModel(
                        placeId = prediction.placeId,
                        primaryText = prediction.getPrimaryText(null).toString(),
                        secondaryText = prediction.getSecondaryText(null)?.toString()
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
                _uiState.value = PlacePickerUiState(cameraLocation = selected.latLng)
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
                    cameraLocation = latLng
                )
            }
        }
    }

    fun updateCameraLocation(latLng: LatLng) {
        _uiState.update { it.copy(cameraLocation = latLng) }
    }

    private suspend fun reverseGeocode(latLng: LatLng): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            results?.firstOrNull()?.getAddressLine(0)
        } catch (e: Exception) {
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
    val cameraLocation: LatLng = DEFAULT_LOCATION
)

data class PlacePredictionUiModel(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String?
)

data class SelectedPlaceUiModel(
    val placeId: String,
    val name: String,
    val address: String?,
    val latLng: LatLng
)

val DEFAULT_LOCATION: LatLng = LatLng(35.681236, 139.767125)

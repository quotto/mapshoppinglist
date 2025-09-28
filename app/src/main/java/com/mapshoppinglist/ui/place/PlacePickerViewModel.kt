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
                _uiState.value = PlacePickerUiState()
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
    val errorMessage: String? = null
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

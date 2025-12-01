package com.mapshoppinglist.ui.placemanage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapshoppinglist.R
import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.usecase.DeletePlaceUseCase
import com.mapshoppinglist.domain.usecase.LoadAllPlacesUseCase
import com.mapshoppinglist.domain.usecase.LoadRegisteredGeofencesUseCase
import com.mapshoppinglist.domain.usecase.UpdatePlaceNameUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaceManagementViewModel(
    private val loadAllPlacesUseCase: LoadAllPlacesUseCase,
    private val loadRegisteredGeofencesUseCase: LoadRegisteredGeofencesUseCase,
    private val updatePlaceNameUseCase: UpdatePlaceNameUseCase,
    private val deletePlaceUseCase: DeletePlaceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceManagementUiState(isLoading = true))
    val uiState: StateFlow<PlaceManagementUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PlaceManagementEvent>()
    val events: SharedFlow<PlaceManagementEvent> = _events.asSharedFlow()

    init {
        loadPlaces()
    }

    fun refresh() {
        loadPlaces()
    }

    fun onEditRequested(placeId: Long) {
        val target = findPlace(placeId) ?: return
        _uiState.update {
            it.copy(
                dialogState = PlaceDialogState.Edit(
                    place = target,
                    nameInput = target.name,
                    isSaving = false,
                    errorMessage = null
                )
            )
        }
    }

    fun onEditNameChange(newValue: String) {
        val dialog = _uiState.value.dialogState as? PlaceDialogState.Edit ?: return
        _uiState.update {
            it.copy(dialogState = dialog.copy(nameInput = newValue, errorMessage = null))
        }
    }

    fun onEditConfirm() {
        val dialog = _uiState.value.dialogState as? PlaceDialogState.Edit ?: return
        val normalized = dialog.nameInput.trim()
        if (normalized.isEmpty()) {
            _uiState.update {
                it.copy(dialogState = dialog.copy(errorMessage = R.string.place_management_validation_required))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(dialogState = dialog.copy(isSaving = true, errorMessage = null))
            }
            try {
                updatePlaceNameUseCase(dialog.place.id, normalized)
                _uiState.update { state ->
                    state.copy(dialogState = null)
                }
                loadPlaces()
                _events.emit(PlaceManagementEvent.ShowSnackbar(R.string.place_management_snackbar_updated))
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(dialogState = dialog.copy(isSaving = false))
                }
            }
        }
    }

    fun onDeleteRequested(placeId: Long) {
        val target = findPlace(placeId) ?: return
        _uiState.update {
            it.copy(
                dialogState = PlaceDialogState.DeleteConfirm(
                    place = target,
                    isProcessing = false
                )
            )
        }
    }

    fun onDeleteConfirm() {
        val dialog = _uiState.value.dialogState as? PlaceDialogState.DeleteConfirm ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(dialogState = dialog.copy(isProcessing = true))
            }
            try {
                deletePlaceUseCase(dialog.place.id)
                _uiState.update { state ->
                    state.copy(dialogState = null)
                }
                loadPlaces()
                _events.emit(PlaceManagementEvent.ShowSnackbar(R.string.place_management_snackbar_deleted))
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(dialogState = dialog.copy(isProcessing = false))
                }
            }
        }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(dialogState = null) }
    }

    private fun loadPlaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val places = loadAllPlacesUseCase()
                val registered = loadRegisteredGeofencesUseCase().toSet()
                val uiPlaces = places.map { it.toUiModel(isSubscribed = it.id in registered) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        places = uiPlaces,
                        errorMessage = null
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message)
                }
            }
        }
    }

    private fun findPlace(placeId: Long): ManagedPlaceUiModel? = _uiState.value.places.find { it.id == placeId }

    private fun Place.toUiModel(isSubscribed: Boolean): ManagedPlaceUiModel = ManagedPlaceUiModel(
        id = id,
        name = name,
        latitude = latitudeE6 / 1_000_000.0,
        longitude = longitudeE6 / 1_000_000.0,
        isActive = isActive,
        isSubscribed = isSubscribed
    )
}

sealed interface PlaceDialogState {
    data class Edit(val place: ManagedPlaceUiModel, val nameInput: String, val isSaving: Boolean, val errorMessage: Int?) : PlaceDialogState

    data class DeleteConfirm(val place: ManagedPlaceUiModel, val isProcessing: Boolean) : PlaceDialogState
}

data class PlaceManagementUiState(
    val isLoading: Boolean = false,
    val places: List<ManagedPlaceUiModel> = emptyList(),
    val dialogState: PlaceDialogState? = null,
    val errorMessage: String? = null
)

data class ManagedPlaceUiModel(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isActive: Boolean,
    val isSubscribed: Boolean
)

sealed interface PlaceManagementEvent {
    data class ShowSnackbar(val messageResId: Int) : PlaceManagementEvent
}

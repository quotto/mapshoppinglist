package com.mapshoppinglist.ui.itemdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapshoppinglist.domain.model.ItemDetail
import com.mapshoppinglist.domain.model.PlaceSummary
import com.mapshoppinglist.domain.usecase.LinkItemToPlaceUseCase
import com.mapshoppinglist.domain.usecase.ObserveItemDetailUseCase
import com.mapshoppinglist.domain.usecase.UnlinkItemFromPlaceUseCase
import com.mapshoppinglist.domain.usecase.UpdateItemUseCase
import com.mapshoppinglist.domain.usecase.UpdatePurchasedStateUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemDetailViewModel(
    private val itemId: Long,
    private val observeItemDetailUseCase: ObserveItemDetailUseCase,
    private val updatePurchasedStateUseCase: UpdatePurchasedStateUseCase,
    private val linkItemToPlaceUseCase: LinkItemToPlaceUseCase,
    private val unlinkItemFromPlaceUseCase: UnlinkItemFromPlaceUseCase,
    private val updateItemUseCase: UpdateItemUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ItemDetailEvent>()
    val events: SharedFlow<ItemDetailEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeItemDetailUseCase(itemId).collect { detail ->
                if (detail == null) {
                    _uiState.value = ItemDetailUiState(isLoading = false, isNotFound = true)
                } else {
                    _uiState.update { current ->
                        val base = detail.toUiState()
                        val keepInput = current.hasPendingInput || current.isSaving
                        base.copy(
                            titleInput = if (keepInput) current.titleInput else base.title,
                            noteInput = if (keepInput) current.noteInput else base.note.orEmpty(),
                            showTitleValidationError = current.showTitleValidationError,
                            errorMessage = current.errorMessage,
                            isSaving = current.isSaving,
                            hasPendingInput = current.hasPendingInput
                        )
                    }
                }
            }
        }
    }

    fun onTogglePurchased(newState: Boolean) {
        viewModelScope.launch {
            updatePurchasedStateUseCase(itemId, newState)
        }
    }

    fun onRemovePlace(placeId: Long) {
        viewModelScope.launch {
            unlinkItemFromPlaceUseCase(placeId = placeId, itemId = itemId)
        }
    }

    fun onPlaceLinked(placeId: Long) {
        viewModelScope.launch {
            linkItemToPlaceUseCase(itemId = itemId, placeId = placeId)
            _events.emit(ItemDetailEvent.PlaceLinked(placeId))
        }
    }

    fun onEditTitleChange(value: String) {
        _uiState.update {
            it.copy(
                titleInput = value,
                showTitleValidationError = false,
                errorMessage = null,
                hasPendingInput = true
            )
        }
    }

    fun onEditNoteChange(value: String) {
        _uiState.update {
            it.copy(
                noteInput = value,
                hasPendingInput = true
            )
        }
    }

    suspend fun saveIfNeeded(): ItemDetailSaveResult {
        val current = _uiState.value
        if (current.isLoading || current.isNotFound) return ItemDetailSaveResult.Success
        if (current.isSaving) return ItemDetailSaveResult.Success
        val inputTitle = current.titleInput.trim()
        val inputNote = current.noteInput.trim().ifBlank { null }
        val currentNoteNormalized = current.note?.takeIf { it.isNotBlank() }

        val finalTitle: String
        val finalNote = inputNote
        val finalNoteNormalized = finalNote
        val needsUpdate: Boolean

        if (inputTitle.isBlank()) {
            finalTitle = current.title
            needsUpdate = finalNoteNormalized != currentNoteNormalized
            if (!needsUpdate) {
                _uiState.update {
                    it.copy(
                        titleInput = current.title,
                        noteInput = current.note.orEmpty(),
                        showTitleValidationError = false,
                        errorMessage = null,
                        hasPendingInput = false
                    )
                }
                return ItemDetailSaveResult.Success
            }
        } else {
            finalTitle = inputTitle
            needsUpdate = finalTitle != current.title || finalNoteNormalized != currentNoteNormalized
            if (!needsUpdate && !current.hasPendingInput) {
                _uiState.update {
                    it.copy(
                        titleInput = current.title,
                        noteInput = current.note.orEmpty(),
                        hasPendingInput = false,
                        showTitleValidationError = false,
                        errorMessage = null
                    )
                }
                return ItemDetailSaveResult.Success
            }
        }

        if (!needsUpdate && !current.hasPendingInput) {
            return ItemDetailSaveResult.Success
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        return try {
            updateItemUseCase(itemId, finalTitle, finalNote)
            _uiState.update {
                it.copy(
                    title = finalTitle,
                    note = finalNote,
                    titleInput = finalTitle,
                    noteInput = finalNote.orEmpty(),
                    showTitleValidationError = false,
                    errorMessage = null,
                    isSaving = false,
                    hasPendingInput = false
                )
            }
            _events.emit(ItemDetailEvent.ItemUpdated)
            ItemDetailSaveResult.Success
        } catch (error: Exception) {
            _uiState.update {
                it.copy(
                    errorMessage = error.message,
                    isSaving = false
                )
            }
            ItemDetailSaveResult.Error
        }
    }

    private fun ItemDetail.toUiState(): ItemDetailUiState {
        return ItemDetailUiState(
            isLoading = false,
            title = title,
            note = note,
            isPurchased = isPurchased,
            linkedPlaces = places.map { it.toUiModel() },
            isNotFound = false,
            titleInput = title,
            noteInput = note.orEmpty()
        )
    }

    private fun PlaceSummary.toUiModel(): LinkedPlaceUiModel {
        return LinkedPlaceUiModel(
            placeId = id,
            name = name,
            address = address
        )
    }
}

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val note: String? = null,
    val isPurchased: Boolean = false,
    val linkedPlaces: List<LinkedPlaceUiModel> = emptyList(),
    val isNotFound: Boolean = false,
    val titleInput: String = "",
    val noteInput: String = "",
    val showTitleValidationError: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val hasPendingInput: Boolean = false
)

data class LinkedPlaceUiModel(
    val placeId: Long,
    val name: String,
    val address: String?
)

sealed interface ItemDetailEvent {
    data class PlaceLinked(val placeId: Long) : ItemDetailEvent
    data object ItemUpdated : ItemDetailEvent
}

sealed interface ItemDetailSaveResult {
    data object Success : ItemDetailSaveResult
    data object ValidationError : ItemDetailSaveResult
    data object Error : ItemDetailSaveResult
}

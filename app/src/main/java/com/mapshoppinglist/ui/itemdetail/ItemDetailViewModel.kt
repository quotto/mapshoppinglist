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
                        base.copy(
                            isEditDialogVisible = current.isEditDialogVisible,
                            editTitle = current.editTitle.takeIf { current.isEditDialogVisible } ?: base.title,
                            editNote = current.editNote.takeIf { current.isEditDialogVisible } ?: (base.note ?: ""),
                            showTitleValidationError = current.showTitleValidationError,
                            editErrorMessage = current.editErrorMessage,
                            isUpdating = current.isUpdating
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

    fun onEditClick() {
        val current = _uiState.value
        if (current.isLoading || current.isNotFound) return
        _uiState.update {
            it.copy(
                isEditDialogVisible = true,
                editTitle = current.title,
                editNote = current.note.orEmpty(),
                showTitleValidationError = false,
                editErrorMessage = null
            )
        }
    }

    fun onEditDialogDismiss() {
        _uiState.update {
            it.copy(
                isEditDialogVisible = false,
                editTitle = "",
                editNote = "",
                showTitleValidationError = false,
                editErrorMessage = null,
                isUpdating = false
            )
        }
    }

    fun onEditTitleChange(value: String) {
        _uiState.update {
            it.copy(
                editTitle = value,
                showTitleValidationError = false,
                editErrorMessage = null
            )
        }
    }

    fun onEditNoteChange(value: String) {
        _uiState.update {
            it.copy(editNote = value)
        }
    }

    fun onEditConfirm() {
        val current = _uiState.value
        val title = current.editTitle.trim()
        if (title.isBlank()) {
            _uiState.update {
                it.copy(showTitleValidationError = true, editErrorMessage = null)
            }
            return
        }
        val note = current.editNote.trim().ifBlank { null }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, editErrorMessage = null) }
            try {
                updateItemUseCase(itemId, title, note)
                _uiState.update {
                    it.copy(
                        title = title,
                        note = note,
                        isEditDialogVisible = false,
                        editTitle = "",
                        editNote = "",
                        showTitleValidationError = false,
                        editErrorMessage = null,
                        isUpdating = false
                    )
                }
                _events.emit(ItemDetailEvent.ItemUpdated)
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        editErrorMessage = error.message
                    )
                }
            }
        }
    }

    private fun ItemDetail.toUiState(): ItemDetailUiState {
        return ItemDetailUiState(
            isLoading = false,
            title = title,
            note = note,
            isPurchased = isPurchased,
            linkedPlaces = places.map { it.toUiModel() },
            isNotFound = false
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
    val isEditDialogVisible: Boolean = false,
    val editTitle: String = "",
    val editNote: String = "",
    val showTitleValidationError: Boolean = false,
    val editErrorMessage: String? = null,
    val isUpdating: Boolean = false
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

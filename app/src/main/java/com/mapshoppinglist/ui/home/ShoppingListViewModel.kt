package com.mapshoppinglist.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapshoppinglist.domain.exception.DuplicateItemException
import com.mapshoppinglist.domain.model.ShoppingItem
import com.mapshoppinglist.domain.usecase.AddShoppingItemUseCase
import com.mapshoppinglist.domain.usecase.DeleteShoppingItemUseCase
import com.mapshoppinglist.domain.usecase.ObserveShoppingItemsUseCase
import com.mapshoppinglist.domain.usecase.UpdatePurchasedStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 買い物リスト画面の状態と操作を管理するViewModel。
 */
class ShoppingListViewModel(
    private val observeShoppingItems: ObserveShoppingItemsUseCase,
    private val addShoppingItem: AddShoppingItemUseCase,
    private val deleteShoppingItem: DeleteShoppingItemUseCase,
    private val updatePurchasedState: UpdatePurchasedStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init {
        // データベースの変更を監視してUI状態へ反映する
        viewModelScope.launch {
            observeShoppingItems().collect { items ->
                _uiState.update { current ->
                    current.copy(
                        notPurchased = items.filterNot { it.isPurchased }.map { it.toUiModel() },
                        purchased = items.filter { it.isPurchased }.map { it.toUiModel() }
                    )
                }
            }
        }
    }

    fun onAddFabClick() {
        _uiState.update {
            it.copy(
                isAddDialogVisible = true,
                showTitleValidationError = false,
                addDialogErrorMessage = null
            )
        }
    }

    fun onAddDialogDismiss() {
        _uiState.update { it.resetInput() }
    }

    fun onTitleInputChange(value: String) {
        _uiState.update {
            it.copy(
                inputTitle = value,
                showTitleValidationError = false,
                addDialogErrorMessage = null
            )
        }
    }

    fun onNoteInputChange(value: String) {
        _uiState.update { it.copy(inputNote = value) }
    }

    fun onAddConfirm() {
        val title = uiState.value.inputTitle.trim()
        val note = uiState.value.inputNote.trim().ifBlank { null }
        if (title.isBlank()) {
            _uiState.update {
                it.copy(
                    showTitleValidationError = true,
                    addDialogErrorMessage = null
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                addShoppingItem(title, note)
                _uiState.update { it.resetInput() }
            } catch (error: Exception) {
                when (error) {
                    is DuplicateItemException -> {
                        _uiState.update {
                            it.copy(addDialogErrorMessage = error.message)
                        }
                    }
                    else -> throw error
                }
            }
        }
    }

    fun onTogglePurchased(itemId: Long, newState: Boolean) {
        viewModelScope.launch {
            updatePurchasedState(itemId, newState)
        }
    }

    fun onDeleteItem(itemId: Long) {
        viewModelScope.launch {
            deleteShoppingItem(itemId)
        }
    }

    private fun ShoppingItem.toUiModel(): ShoppingItemUiModel {
        return ShoppingItemUiModel(
            id = id,
            title = title,
            note = note,
            linkedPlaceCount = linkedPlaceCount,
            isPurchased = isPurchased
        )
    }

    private fun ShoppingListUiState.resetInput(): ShoppingListUiState {
        return copy(
            isAddDialogVisible = false,
            inputTitle = "",
            inputNote = "",
            showTitleValidationError = false,
            addDialogErrorMessage = null
        )
    }
}

/**
 * 画面全体の状態を表すデータクラス。
 */
data class ShoppingListUiState(
    val notPurchased: List<ShoppingItemUiModel> = emptyList(),
    val purchased: List<ShoppingItemUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isAddDialogVisible: Boolean = false,
    val inputTitle: String = "",
    val inputNote: String = "",
    val showTitleValidationError: Boolean = false,
    val addDialogErrorMessage: String? = null
)

/**
 * 一覧に表示するアイテムのUIモデル。
 */
data class ShoppingItemUiModel(
    val id: Long,
    val title: String,
    val note: String?,
    val linkedPlaceCount: Int,
    val isPurchased: Boolean
)

package com.mapshoppinglist.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapshoppinglist.domain.exception.DuplicateItemException
import com.mapshoppinglist.domain.model.PlaceGroup
import com.mapshoppinglist.domain.model.ShoppingItem
import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.domain.repository.ShoppingListRepository
import com.mapshoppinglist.domain.usecase.AddShoppingItemUseCase
import com.mapshoppinglist.domain.usecase.DeleteShoppingItemUseCase
import com.mapshoppinglist.domain.usecase.GetRecentPlacesUseCase
import com.mapshoppinglist.domain.usecase.LinkItemToPlaceUseCase
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
    private val updatePurchasedState: UpdatePurchasedStateUseCase,
    private val linkItemToPlaceUseCase: LinkItemToPlaceUseCase,
    private val placesRepository: PlacesRepository,
    private val getRecentPlacesUseCase: GetRecentPlacesUseCase,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init {
        // 購入状況タブのデータを監視
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

        // 購入場所タブのデータを監視
        viewModelScope.launch {
            shoppingListRepository.observePlaceGroups().collect { placeGroups ->
                _uiState.update { current ->
                    current.copy(placeGroups = placeGroups.map { it.toUiModel() })
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
        loadRecentPlaces()
    }

    fun onAddDialogDismiss() {
        _uiState.update { it.resetInput() }
        loadRecentPlaces()
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
                val itemId = addShoppingItem(title, note)
                val pendingPlaces = uiState.value.pendingPlaces
                pendingPlaces.forEach { place ->
                    linkItemToPlaceUseCase(itemId = itemId, placeId = place.placeId)
                }
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

    fun onPlaceRegistered(placeId: Long) {
        viewModelScope.launch {
            val place = placesRepository.findById(placeId) ?: return@launch
            _uiState.update { state ->
                if (state.pendingPlaces.any { it.placeId == placeId }) {
                    state
                } else {
                    state.copy(
                        pendingPlaces = state.pendingPlaces + PendingPlaceUiModel(placeId, place.name)
                    )
                }
            }
            loadRecentPlaces()
        }
    }

    fun onRemovePendingPlace(placeId: Long) {
        _uiState.update { state ->
            state.copy(pendingPlaces = state.pendingPlaces.filterNot { it.placeId == placeId })
        }
        loadRecentPlaces()
    }

    fun onRecentPlaceSelected(placeId: Long) {
        onPlaceRegistered(placeId)
    }

    fun onTabSelected(tab: ListTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun PlaceGroup.toUiModel(): PlaceGroupUiModel = PlaceGroupUiModel(
        placeId = place?.id,
        placeName = place?.name ?: "未設定",
        items = items.map { it.toUiModel() }
    )

    private fun loadRecentPlaces(limit: Int = RECENT_LIMIT) {
        viewModelScope.launch {
            val places = getRecentPlacesUseCase(limit)
            _uiState.update { state ->
                val pendingIds = state.pendingPlaces.map { it.placeId }.toSet()
                state.copy(
                    recentPlaces = places
                        .filter { it.id !in pendingIds }
                        .map { place -> RecentPlaceUiModel(place.id, place.name) }
                )
            }
        }
    }

    private fun ShoppingItem.toUiModel(): ShoppingItemUiModel = ShoppingItemUiModel(
        id = id,
        title = title,
        note = note,
        linkedPlaceCount = linkedPlaceCount,
        isPurchased = isPurchased
    )

    private fun ShoppingListUiState.resetInput(): ShoppingListUiState = copy(
        isAddDialogVisible = false,
        inputTitle = "",
        inputNote = "",
        showTitleValidationError = false,
        addDialogErrorMessage = null,
        pendingPlaces = emptyList()
    )
}

/**
 * 画面全体の状態を表すデータクラス。
 */
data class ShoppingListUiState(
    val selectedTab: ListTab = ListTab.PurchaseStatus,
    val notPurchased: List<ShoppingItemUiModel> = emptyList(),
    val purchased: List<ShoppingItemUiModel> = emptyList(),
    val placeGroups: List<PlaceGroupUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isAddDialogVisible: Boolean = false,
    val inputTitle: String = "",
    val inputNote: String = "",
    val showTitleValidationError: Boolean = false,
    val addDialogErrorMessage: String? = null,
    val pendingPlaces: List<PendingPlaceUiModel> = emptyList(),
    val recentPlaces: List<RecentPlaceUiModel> = emptyList()
)

/**
 * タブの種類
 */
enum class ListTab {
    PurchaseStatus, // 購入状況
    PlaceGroup // 購入場所
}

/**
 * 一覧に表示するアイテムのUIモデル。
 */
data class ShoppingItemUiModel(val id: Long, val title: String, val note: String?, val linkedPlaceCount: Int, val isPurchased: Boolean)

/**
 * 地点グループのUIモデル
 */
data class PlaceGroupUiModel(val placeId: Long?, val placeName: String, val items: List<ShoppingItemUiModel>)

data class PendingPlaceUiModel(val placeId: Long, val name: String)

data class RecentPlaceUiModel(val placeId: Long, val name: String)

private const val RECENT_LIMIT = 5

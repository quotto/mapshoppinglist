package com.mapshoppinglist.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ダミーデータを提供するだけのViewModelスタブ。
 * 実データ接続前のCompose UI検証に利用する。
 */
class ShoppingListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    /**
     * 購入済み/未購入の両方にダミーアイテムを用意する。
     */
    private fun createInitialState(): ShoppingListUiState {
        val demoItems = listOf(
            ShoppingItemUiModel(
                id = 1L,
                title = "牛乳",
                note = "2本買う",
                linkedPlaceCount = 2,
                isPurchased = false
            ),
            ShoppingItemUiModel(
                id = 2L,
                title = "卵",
                note = "Lサイズ",
                linkedPlaceCount = 1,
                isPurchased = false
            ),
            ShoppingItemUiModel(
                id = 3L,
                title = "掃除用具",
                note = "スポンジ",
                linkedPlaceCount = 3,
                isPurchased = true
            )
        )
        return ShoppingListUiState(
            notPurchased = demoItems.filterNot { it.isPurchased },
            purchased = demoItems.filter { it.isPurchased }
        )
    }
}

/**
 * 画面全体の状態を表すデータクラス。
 */
data class ShoppingListUiState(
    val notPurchased: List<ShoppingItemUiModel>,
    val purchased: List<ShoppingItemUiModel>,
    val isLoading: Boolean = false
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

package com.mapshoppinglist.ui.itemdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mapshoppinglist.MapShoppingListApplication

class ItemDetailViewModelFactory(
    private val application: MapShoppingListApplication,
    private val itemId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemDetailViewModel::class.java)) {
            val viewModel = ItemDetailViewModel(
                itemId = itemId,
                observeItemDetailUseCase = application.observeItemDetailUseCase,
                updatePurchasedStateUseCase = application.updatePurchasedStateUseCase,
                linkItemToPlaceUseCase = application.linkItemToPlaceUseCase,
                unlinkItemFromPlaceUseCase = application.unlinkItemFromPlaceUseCase,
                updateItemUseCase = application.updateItemUseCase
            )
            @Suppress("UNCHECKED_CAST")
            return viewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

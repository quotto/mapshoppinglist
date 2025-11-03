package com.mapshoppinglist.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mapshoppinglist.MapShoppingListApplication

/**
 * ShoppingListViewModelの生成を担当するFactory。
 */
class ShoppingListViewModelFactory(
    private val application: MapShoppingListApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
            val viewModel = ShoppingListViewModel(
                observeShoppingItems = application.observeShoppingItemsUseCase,
                addShoppingItem = application.addShoppingItemUseCase,
                deleteShoppingItem = application.deleteShoppingItemUseCase,
                updatePurchasedState = application.updatePurchasedStateUseCase,
                linkItemToPlaceUseCase = application.linkItemToPlaceUseCase,
                placesRepository = application.placesRepository,
                getRecentPlacesUseCase = application.getRecentPlacesUseCase,
                shoppingListRepository = application.shoppingListRepository
            )
            @Suppress("UNCHECKED_CAST")
            return viewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

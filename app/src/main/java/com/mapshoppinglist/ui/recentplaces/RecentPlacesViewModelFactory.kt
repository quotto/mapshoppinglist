package com.mapshoppinglist.ui.recentplaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mapshoppinglist.MapShoppingListApplication

class RecentPlacesViewModelFactory(private val application: MapShoppingListApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecentPlacesViewModel::class.java)) {
            val viewModel = RecentPlacesViewModel(
                getRecentPlacesUseCase = application.getRecentPlacesUseCase
            )
            @Suppress("UNCHECKED_CAST")
            return viewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

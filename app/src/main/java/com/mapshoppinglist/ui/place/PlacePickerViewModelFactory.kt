package com.mapshoppinglist.ui.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mapshoppinglist.MapShoppingListApplication

class PlacePickerViewModelFactory(
    private val application: MapShoppingListApplication
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlacePickerViewModel::class.java)) {
            val viewModel = PlacePickerViewModel(
                application = application,
                createPlaceUseCase = application.createPlaceUseCase
            )
            @Suppress("UNCHECKED_CAST")
            return viewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

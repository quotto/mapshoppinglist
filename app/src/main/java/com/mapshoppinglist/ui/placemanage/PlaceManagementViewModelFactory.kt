package com.mapshoppinglist.ui.placemanage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mapshoppinglist.MapShoppingListApplication

class PlaceManagementViewModelFactory(
    private val application: MapShoppingListApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaceManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaceManagementViewModel(
                loadAllPlacesUseCase = application.loadAllPlacesUseCase,
                loadRegisteredGeofencesUseCase = application.loadRegisteredGeofencesUseCase,
                updatePlaceNameUseCase = application.updatePlaceNameUseCase,
                deletePlaceUseCase = application.deletePlaceUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}

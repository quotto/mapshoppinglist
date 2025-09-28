package com.mapshoppinglist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapshoppinglist.ui.home.ShoppingListRoute
import com.mapshoppinglist.ui.place.PlacePickerRoute

object Destinations {
    const val SHOPPING_LIST = "shopping_list"
    const val PLACE_PICKER = "place_picker"
}

@Composable
fun MapShoppingListApp(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destinations.SHOPPING_LIST) {
        composable(Destinations.SHOPPING_LIST) { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            val newPlaceIdLiveData = savedStateHandle.getLiveData<Long?>(KEY_NEW_PLACE_ID)
            val newPlaceId by newPlaceIdLiveData.observeAsState()
            ShoppingListRoute(
                onAddPlaceRequest = { navController.navigate(Destinations.PLACE_PICKER) },
                newPlaceId = newPlaceId,
                onNewPlaceConsumed = { savedStateHandle.set(KEY_NEW_PLACE_ID, null) }
            )
        }
        composable(Destinations.PLACE_PICKER) {
            PlacePickerRoute(
                onPlaceRegistered = { placeId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(KEY_NEW_PLACE_ID, placeId)
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() }
            )
        }
    }
}

private const val KEY_NEW_PLACE_ID = "new_place_id"

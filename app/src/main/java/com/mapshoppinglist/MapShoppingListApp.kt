package com.mapshoppinglist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mapshoppinglist.ui.home.ShoppingListRoute
import com.mapshoppinglist.ui.itemdetail.ItemDetailRoute
import com.mapshoppinglist.ui.place.PlacePickerRoute
import com.mapshoppinglist.ui.placemanage.PlaceManagementRoute
import com.mapshoppinglist.ui.recentplaces.RecentPlacesRoute
import com.mapshoppinglist.ui.settings.OssLicensesRoute
import com.mapshoppinglist.ui.settings.PrivacyPolicyRoute

object Destinations {
    const val SHOPPING_LIST = "shopping_list"
    const val ITEM_DETAIL = "item_detail"
    const val PLACE_PICKER = "place_picker"
    const val RECENT_PLACES = "recent_places"
    const val PLACE_MANAGEMENT = "place_management"
    const val PRIVACY_POLICY = "privacy_policy"
    const val OSS_LICENSES = "oss_licenses"

    fun itemDetailRoute(itemId: Long): String = "$ITEM_DETAIL/$itemId"
    fun placePickerRoute(requestKey: String): String = "$PLACE_PICKER/$requestKey"
    fun recentPlacesRoute(requestKey: String): String = "$RECENT_PLACES/$requestKey"
}

@Composable
fun MapShoppingListApp(
    startItemId: Long? = null,
    navController: NavHostController = rememberNavController(),
    onDetailConsumed: () -> Unit = {}
) {
    NavHost(navController = navController, startDestination = Destinations.SHOPPING_LIST) {
        composable(Destinations.SHOPPING_LIST) { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            val newPlaceIdLiveData = savedStateHandle.getLiveData<Long?>(REQUEST_KEY_SHOPPING_LIST_PLACE)
            val newPlaceId by newPlaceIdLiveData.observeAsState()
            ShoppingListRoute(
                onAddPlaceViaSearch = { navController.navigate(Destinations.placePickerRoute(REQUEST_KEY_SHOPPING_LIST_PLACE)) },
                onAddPlaceViaRecent = { navController.navigate(Destinations.recentPlacesRoute(REQUEST_KEY_SHOPPING_LIST_PLACE)) },
                onItemClick = { itemId -> navController.navigate(Destinations.itemDetailRoute(itemId)) },
                newPlaceId = newPlaceId,
                onNewPlaceConsumed = { savedStateHandle.set<Long?>(REQUEST_KEY_SHOPPING_LIST_PLACE, null) },
                onManagePlaces = { navController.navigate(Destinations.PLACE_MANAGEMENT) },
                onShowPrivacyPolicy = { navController.navigate(Destinations.PRIVACY_POLICY) },
                onShowOssLicenses = { navController.navigate(Destinations.OSS_LICENSES) }
            )
        }
        composable(
            route = "${Destinations.ITEM_DETAIL}/{$ARG_ITEM_ID}",
            arguments = listOf(navArgument(ARG_ITEM_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong(ARG_ITEM_ID) ?: return@composable
            val savedStateHandle = backStackEntry.savedStateHandle
            val linkPlaceLiveData = savedStateHandle.getLiveData<Long?>(REQUEST_KEY_ITEM_DETAIL_PLACE)
            val linkedPlaceId by linkPlaceLiveData.observeAsState()
            ItemDetailRoute(
                itemId = itemId,
                linkedPlaceId = linkedPlaceId,
                onLinkedPlaceConsumed = { savedStateHandle.set<Long?>(REQUEST_KEY_ITEM_DETAIL_PLACE, null) },
                onAddPlaceViaSearch = {
                    navController.navigate(Destinations.placePickerRoute(REQUEST_KEY_ITEM_DETAIL_PLACE))
                },
                onAddPlaceViaRecent = {
                    navController.navigate(Destinations.recentPlacesRoute(REQUEST_KEY_ITEM_DETAIL_PLACE))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "${Destinations.PLACE_PICKER}/{$ARG_REQUEST_KEY}",
            arguments = listOf(navArgument(ARG_REQUEST_KEY) { type = NavType.StringType })
        ) { backStackEntry ->
            val requestKey = backStackEntry.arguments?.getString(ARG_REQUEST_KEY) ?: REQUEST_KEY_SHOPPING_LIST_PLACE
            PlacePickerRoute(
                onPlaceRegistered = { placeId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(requestKey, placeId)
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() }
            )
        }
        composable(
            route = "${Destinations.RECENT_PLACES}/{$ARG_REQUEST_KEY}",
            arguments = listOf(navArgument(ARG_REQUEST_KEY) { type = NavType.StringType })
        ) { backStackEntry ->
            val requestKey = backStackEntry.arguments?.getString(ARG_REQUEST_KEY) ?: REQUEST_KEY_SHOPPING_LIST_PLACE
            RecentPlacesRoute(
                onPlaceSelected = { placeId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(requestKey, placeId)
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() }
            )
        }
        composable(Destinations.PLACE_MANAGEMENT) {
            PlaceManagementRoute(onBack = { navController.popBackStack() })
        }
        composable(Destinations.PRIVACY_POLICY) {
            PrivacyPolicyRoute(onBack = { navController.popBackStack() })
        }
        composable(Destinations.OSS_LICENSES) {
            OssLicensesRoute(onBack = { navController.popBackStack() })
        }
    }

    LaunchedEffect(startItemId) {
        val targetId = startItemId
        if (targetId != null) {
            navController.navigate(Destinations.itemDetailRoute(targetId)) {
                launchSingleTop = true
            }
            onDetailConsumed()
        }
    }
}

private const val ARG_ITEM_ID = "itemId"
private const val ARG_REQUEST_KEY = "requestKey"
private const val REQUEST_KEY_SHOPPING_LIST_PLACE = "request_shopping_list_place"
private const val REQUEST_KEY_ITEM_DETAIL_PLACE = "request_item_detail_place"

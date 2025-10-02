package com.mapshoppinglist.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mapshoppinglist.R
import com.mapshoppinglist.ui.home.PermissionPromptUiModel
import com.mapshoppinglist.ui.home.ShoppingListScreen
import com.mapshoppinglist.ui.home.ShoppingListUiState
import com.mapshoppinglist.ui.place.PlacePickerScreen
import com.mapshoppinglist.ui.place.PlacePickerUiState
import com.mapshoppinglist.ui.theme.MapShoppingListTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PermissionsUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shoppingList_showsPermissionPromptAndInvokesAction() {
        val activity = composeRule.activity
        var actionCount = 0
        val prompt = PermissionPromptUiModel(
            key = "background",
            title = activity.getString(R.string.permission_background_title),
            message = activity.getString(R.string.permission_background_message),
            actionLabel = activity.getString(R.string.permission_background_button),
            onClick = { actionCount += 1 }
        )

        composeRule.setContent {
            MapShoppingListTheme {
                ShoppingListScreen(
                    uiState = ShoppingListUiState(),
                    permissionPrompts = listOf(prompt),
                    onAddItemClick = {},
                    onTogglePurchased = { _, _ -> },
                    onDeleteItem = {},
                    onAddDialogDismiss = {},
                    onAddDialogConfirm = {},
                    onTitleInputChange = {},
                    onNoteInputChange = {},
                    onAddPlaceViaSearch = {},
                    onAddPlaceViaRecent = {},
                    onRemovePendingPlace = {},
                    onItemClick = {}
                )
            }
        }

        composeRule.onNodeWithText(prompt.title).assertIsDisplayed()
        composeRule.onNodeWithText(prompt.message).assertIsDisplayed()
        composeRule.onNodeWithText(prompt.actionLabel).performClick()
        assertEquals(1, actionCount)
    }

    @Test
    fun placePicker_showsPermissionPlaceholderWhenDenied() {
        val activity = composeRule.activity
        val expectedMessage = activity.getString(R.string.permission_location_denied_message)
        val expectedButton = activity.getString(R.string.permission_location_request_button)
        var requested = false

        composeRule.setContent {
            MapShoppingListTheme {
                PlacePickerScreen(
                    uiState = PlacePickerUiState(),
                    onQueryChange = {},
                    onPredictionSelected = {},
                    onConfirm = {},
                    onClearSelection = {},
                    onClose = {},
                    snackbarHostState = androidx.compose.material3.SnackbarHostState(),
                    hasLocationPermission = false,
                    onMapLongClick = {},
                    onPoiClick = {},
                    onRequestLocationPermission = { requested = true }
                )
            }
        }

        composeRule.onNodeWithText(expectedMessage).assertIsDisplayed()
        composeRule.onNodeWithText(expectedButton).performClick()
        assertEquals(true, requested)
    }
}

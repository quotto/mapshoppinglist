@file:OptIn(ExperimentalTestApi::class)

package com.mapshoppinglist.ui.home

import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.compose.ui.test.hasTestTag
import com.mapshoppinglist.MainActivity
import com.mapshoppinglist.R
import com.mapshoppinglist.testtag.ItemDetailTestTags
import com.mapshoppinglist.testtag.ShoppingListTestTags
import com.mapshoppinglist.ui.theme.MapShoppingListTheme
import com.mapshoppinglist.util.TestDataHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        TestDataHelper.clearDatabase()
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() {
        TestDataHelper.clearDatabase()
    }

    @Test
    fun displayEmptyListWhenLaunchApps() {
        composeRule.onNodeWithTag(ShoppingListTestTags.EMPTY_STATE).assertIsDisplayed()
    }

    @Test
    fun canAddItemWithoutPlace() {
        val title = "りんご"

        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_FAB).performClick()

        composeRule.onNode(
            hasSetTextAction() and hasTestTag(ShoppingListTestTags.ADD_ITEM_TITLE_INPUT), //hasText(composeRule.getString(R.string.shopping_list_add_dialog_title_hint)),
            useUnmergedTree = true
        ).performTextInput(title)

        composeRule.onNodeWithText(composeRule.getString(R.string.shopping_list_add_dialog_confirm)).performClick()

        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.getString(R.string.shopping_list_linked_places_count, 0)
        ).assertIsDisplayed()
    }

    @Test
    fun showErrorWhenTitleIsEmpty() {
        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_FAB).performClick()

        composeRule.onNodeWithText(composeRule.getString(R.string.shopping_list_add_dialog_confirm)).performClick()

        composeRule.onNodeWithText(
            composeRule.getString(R.string.shopping_list_validation_title_required)
        ).assertIsDisplayed()
    }

    @Test
    fun canChangeNoPurchasedToPurchased() {
        val itemId = TestDataHelper.insertItem("牛乳")

        // 全てのアイテムが未購入の場合は未購入カードは表示されない
        composeRule.waitUntilTagDisplayed(ShoppingListTestTags.ITEM_NOT_PURCHASED_PREFIX + itemId)
        composeRule.onNodeWithTag(ShoppingListTestTags.EMPTY_STATE).assertDoesNotExist()

        composeRule.onNodeWithTag(ShoppingListTestTags.ITEM_CHECKBOX_PREFIX + itemId).performClick()

        composeRule.waitUntilNodeExists(ShoppingListTestTags.ITEM_PURCHASED_PREFIX + itemId)

        composeRule.onNodeWithTag(ShoppingListTestTags.ITEM_NOT_PURCHASED_PREFIX + itemId).assertDoesNotExist()
        composeRule.onNodeWithTag(ShoppingListTestTags.ITEM_PURCHASED_PREFIX + itemId).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.getString(R.string.shopping_list_purchased_header)).assertIsDisplayed()

        // 全てのアイテムが購入済みの場合は未購入カードが表示される
        composeRule.onNodeWithTag(ShoppingListTestTags.EMPTY_STATE).assertIsDisplayed()
    }

    @Test
    fun canChangePurchasedToNoPurchased() {
        val itemId = TestDataHelper.insertItem("牛乳")
        TestDataHelper.insertItem("パン")
        val checkboxTag = ShoppingListTestTags.ITEM_CHECKBOX_PREFIX + itemId

        // 未購入リストが空になるとリスト構造が再構成されチェックボックスが一時的に消える場合があるため、
        // 補助データを投入して未購入セクションを維持しつつ、描画が安定するまで待機する。
        composeRule.waitUntilTagDisplayed(checkboxTag)
        composeRule.onNodeWithTag(checkboxTag).performClick()

        composeRule.waitUntilNodeExists(ShoppingListTestTags.ITEM_PURCHASED_PREFIX + itemId)

        composeRule.waitUntilTagDisplayed(checkboxTag)
        composeRule.onNodeWithTag(checkboxTag).performClick()

        composeRule.waitUntilNodeExists(ShoppingListTestTags.ITEM_NOT_PURCHASED_PREFIX + itemId)

        composeRule.onNodeWithTag(ShoppingListTestTags.ITEM_PURCHASED_PREFIX + itemId).assertDoesNotExist()
        composeRule.onNodeWithTag(ShoppingListTestTags.ITEM_NOT_PURCHASED_PREFIX + itemId).assertIsDisplayed()
    }

    @Test
    fun canDeleteItem() {
        val itemId = TestDataHelper.insertItem("牛乳")
        val deleteTag = ShoppingListTestTags.ITEM_DELETE_PREFIX + itemId

        composeRule.waitUntilTagDisplayed(deleteTag, useUnmergedTree = true)
        composeRule.onNodeWithTag(deleteTag, useUnmergedTree = true).performClick()

        composeRule.waitUntilNodeExists(ShoppingListTestTags.EMPTY_STATE)

        composeRule.onNodeWithTag(ShoppingListTestTags.ITEM_NOT_PURCHASED_PREFIX + itemId).assertDoesNotExist()
        composeRule.onNodeWithTag(ShoppingListTestTags.ITEM_PURCHASED_PREFIX + itemId).assertDoesNotExist()
    }


    @Test
    fun canRegisterRecentlyPlace() {
        val placeName = "テストスーパー"
        TestDataHelper.createPlace(placeName, latitude = 35.0, longitude = 139.0)

        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_FAB).performClick()

        composeRule.onNode(
            hasSetTextAction() and hasTestTag(ShoppingListTestTags.ADD_ITEM_TITLE_INPUT),  //hasText(composeRule.getString(R.string.shopping_list_add_dialog_title_hint)),
            useUnmergedTree = true
        ).performTextInput("お米")

        composeRule.onNodeWithText(composeRule.getString(R.string.item_detail_add_place_recent)).performClick()

        composeRule.onNodeWithText(composeRule.getString(R.string.recent_places_title)).assertIsDisplayed()
        composeRule.onNodeWithText(placeName).performClick()

        composeRule.waitUntilTextDisplayed(placeName)

        composeRule.onNodeWithText(composeRule.getString(R.string.shopping_list_add_dialog_confirm)).performClick()

        composeRule.waitUntilTextDisplayed("お米")

        val linkedCountText = composeRule.getString(R.string.shopping_list_linked_places_count, 1)
        // アイテムへの地点紐付けは非同期で反映されるため、表示が安定するまで待機する
        composeRule.waitUntilTextDisplayed(linkedCountText)
        composeRule.onNodeWithText(linkedCountText).assertIsDisplayed()
    }

    @Test
    fun showMessageWhenRecentlyPlaceIsNotExist() {
        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_FAB).performClick()

        composeRule.onNodeWithText(
            composeRule.getString(R.string.recent_places_empty)
        ).assertIsDisplayed()
    }

    @Test
    fun showsPermissionPromptAndInvokesActionWhenNotPermitted() {
        val activity = composeRule.activity
        var actionCount = 0
        val prompt = PermissionPromptUiModel(
            key = "background",
            title = activity.getString(R.string.permission_background_title),
            message = activity.getString(R.string.permission_background_message),
            actionLabel = activity.getString(R.string.permission_background_button),
            onClick = { actionCount += 1 }
        )

        composeRule.runOnUiThread {
            activity.setContent {
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
                        onItemClick = {},
                        onManagePlaces = {},
                        onShowPrivacyPolicy = {},
                        onShowOssLicenses = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(prompt.title).assertIsDisplayed()
        composeRule.onNodeWithText(prompt.message).assertIsDisplayed()
        composeRule.onNodeWithText(prompt.actionLabel).performClick()
        assertEquals(1, actionCount)
    }

    private fun MainActivityRule.getString(resId: Int): String {
        return activity.getString(resId)
    }

    private fun MainActivityRule.getString(resId: Int, vararg args: Any): String {
        return activity.getString(resId, *args)
    }

    private fun ComposeTestRule.waitUntilNodeExists(tag: String, timeoutMillis: Long = 5_000) {
        waitUntilWithClock(timeoutMillis) {
            runCatching {
                onNodeWithTag(tag, useUnmergedTree = false).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilTextDisplayed(
        text: String,
        useUnmergedTree: Boolean = false,
        timeoutMillis: Long = 5_000
    ) {
        waitUntilWithClock(timeoutMillis) {
            runCatching {
                onNodeWithText(text, useUnmergedTree).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilPlaceLinked(placeId: Long) {
        waitUntilWithClock {
            runCatching {
                onNodeWithTag("${ItemDetailTestTags.LINKED_PLACE_PREFIX}$placeId").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilTagDisplayed(
        tag: String,
        useUnmergedTree: Boolean = false
    ) {
        waitUntilWithClock {
            runCatching {
                onNodeWithTag(tag, useUnmergedTree).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilWithClock(
        timeoutMillis: Long = 5_000,
        condition: () -> Boolean
    ) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (!condition()) {
            if (SystemClock.elapsedRealtime() > deadline) {
                throw AssertionError("Condition not met within ${timeoutMillis}ms")
            }
            waitForIdle()
            Thread.sleep(16)
        }
    }
}

private typealias MainActivityRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

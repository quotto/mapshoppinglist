@file:OptIn(ExperimentalTestApi::class)

package com.mapshoppinglist.ui.home

import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    fun addFabIsInteractive() {
        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_FAB)
            .assertHasClickAction()
            .assertIsDisplayed()
    }

    @Test
    fun canAddItemWithoutPlace() {
        val title = "りんご"

        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_FAB).performClick()

        composeRule.onNode(
            hasSetTextAction() and hasTestTag(
                ShoppingListTestTags.ADD_ITEM_TITLE_INPUT
            ),
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
            hasSetTextAction() and hasTestTag(
                ShoppingListTestTags.ADD_ITEM_TITLE_INPUT
            ),
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
                        onShowOssLicenses = {},
                        onShowNearbyDiagnosticLog = {},
                        onTabSelected = {}
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

    @Test
    fun tabsAreDisplayedAndSwitchable() {
        // タブが表示されることを確認
        composeRule.onNodeWithTag(ShoppingListTestTags.TAB_PURCHASE_STATUS).assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.TAB_PLACE_GROUP).assertIsDisplayed()

        // 初期表示は購入状況タブ
        composeRule.onNodeWithText(composeRule.getString(R.string.tab_purchase_status)).assertIsDisplayed()

        // 買う場所タブに切り替え
        composeRule.onNodeWithTag(ShoppingListTestTags.TAB_PLACE_GROUP).performClick()
        composeRule.waitForIdle()

        // 購入状況タブに戻す
        composeRule.onNodeWithTag(ShoppingListTestTags.TAB_PURCHASE_STATUS).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun placeGroupTabShowsItemsGroupedByPlace() {
        // テストデータ作成: 2つの地点と3つのアイテム
        val place1Id = TestDataHelper.createPlace("スーパーA", 35.0, 139.0)
        val place2Id = TestDataHelper.createPlace("スーパーB", 35.1, 139.1)

        val item1Id = TestDataHelper.insertItem("牛乳")
        val item2Id = TestDataHelper.insertItem("パン")
        val item3Id = TestDataHelper.insertItem("卵")

        // 地点とアイテムを紐付け
        TestDataHelper.linkItemToPlace(item1Id, place1Id)
        TestDataHelper.linkItemToPlace(item2Id, place1Id)
        TestDataHelper.linkItemToPlace(item3Id, place2Id)

        composeRule.waitForIdle()

        // 買う場所タブに切り替え
        composeRule.onNodeWithTag(ShoppingListTestTags.TAB_PLACE_GROUP).performClick()
        composeRule.waitForIdle()

        // 地点グループヘッダーが表示されることを確認
        composeRule.waitUntilTagDisplayed(ShoppingListTestTags.PLACE_GROUP_HEADER_PREFIX + place1Id)
        composeRule.onNodeWithTag(ShoppingListTestTags.PLACE_GROUP_HEADER_PREFIX + place1Id).assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.PLACE_GROUP_HEADER_PREFIX + place2Id).assertIsDisplayed()

        // 地点名が表示されることを確認
        composeRule.onNodeWithText("📍 スーパーA").assertIsDisplayed()
        composeRule.onNodeWithText("📍 スーパーB").assertIsDisplayed()

        // アイテム数が表示されることを確認
        composeRule.onNodeWithText("(2)").assertIsDisplayed() // スーパーAに2件
        composeRule.onNodeWithText("(1)").assertIsDisplayed() // スーパーBに1件

        // 各地点のアイテムが表示されることを確認
        composeRule.onNodeWithText("牛乳").assertIsDisplayed()
        composeRule.onNodeWithText("パン").assertIsDisplayed()
        composeRule.onNodeWithText("卵").assertIsDisplayed()
    }

    @Test
    fun placeGroupTabShowsUnsetGroupForItemsWithoutPlace() {
        // 地点未設定のアイテムを作成
        TestDataHelper.insertItem("ノート")
        TestDataHelper.insertItem("ペン")

        composeRule.waitForIdle()

        // 買う場所タブに切り替え
        composeRule.onNodeWithTag(ShoppingListTestTags.TAB_PLACE_GROUP).performClick()
        composeRule.waitForIdle()

        // 「未設定」グループヘッダーが表示されることを確認
        composeRule.waitUntilTagDisplayed(ShoppingListTestTags.PLACE_GROUP_HEADER_PREFIX + "unset")
        composeRule.onNodeWithTag(ShoppingListTestTags.PLACE_GROUP_HEADER_PREFIX + "unset").assertIsDisplayed()

        // 地点名が「未設定」であることを確認
        composeRule.onNodeWithText("📍 未設定").assertIsDisplayed()

        // アイテム数が表示されることを確認
        composeRule.onNodeWithText("(2)").assertIsDisplayed()

        // アイテムが表示されることを確認
        composeRule.onNodeWithText("ノート").assertIsDisplayed()
        composeRule.onNodeWithText("ペン").assertIsDisplayed()
    }

    @Test
    fun placeGroupTabShowsMixedGroupsWithPlaceAndUnset() {
        // 地点ありのアイテム
        val placeId = TestDataHelper.createPlace("コンビニ", 35.0, 139.0)
        val item1Id = TestDataHelper.insertItem("おにぎり")
        TestDataHelper.linkItemToPlace(item1Id, placeId)

        // 地点なしのアイテム
        TestDataHelper.insertItem("ノート")

        composeRule.waitForIdle()

        // 買う場所タブに切り替え
        composeRule.onNodeWithTag(ShoppingListTestTags.TAB_PLACE_GROUP).performClick()
        composeRule.waitForIdle()

        // 両方のグループヘッダーが表示されることを確認
        composeRule.waitUntilTagDisplayed(ShoppingListTestTags.PLACE_GROUP_HEADER_PREFIX + placeId)
        composeRule.onNodeWithTag(ShoppingListTestTags.PLACE_GROUP_HEADER_PREFIX + placeId).assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.PLACE_GROUP_HEADER_PREFIX + "unset").assertIsDisplayed()

        // 地点名が表示されることを確認
        composeRule.onNodeWithText("📍 コンビニ").assertIsDisplayed()
        composeRule.onNodeWithText("📍 未設定").assertIsDisplayed()

        // 各グループのアイテムが表示されることを確認
        composeRule.onNodeWithText("おにぎり").assertIsDisplayed()
        composeRule.onNodeWithText("ノート").assertIsDisplayed()
    }

    @Test
    fun placeGroupTabShowsEmptyStateWhenNoItems() {
        // アイテムなしの状態で買う場所タブに切り替え
        composeRule.onNodeWithTag(ShoppingListTestTags.TAB_PLACE_GROUP).performClick()
        composeRule.waitForIdle()

        // 空状態が表示されることを確認
        composeRule.onNodeWithTag(ShoppingListTestTags.EMPTY_STATE).assertIsDisplayed()
    }

    @Test
    fun canTogglePurchasedInPlaceGroupTab() {
        // テストデータ作成
        val placeId = TestDataHelper.createPlace("スーパー", 35.0, 139.0)
        val itemId = TestDataHelper.insertItem("りんご")
        TestDataHelper.linkItemToPlace(itemId, placeId)

        composeRule.waitForIdle()

        // 買う場所タブに切り替え
        composeRule.onNodeWithTag(ShoppingListTestTags.TAB_PLACE_GROUP).performClick()
        composeRule.waitForIdle()

        // アイテムが表示されるまで待機
        composeRule.waitUntilTextDisplayed("りんご")

        // チェックボックスで購入状態を変更
        val checkboxTag = ShoppingListTestTags.ITEM_CHECKBOX_PREFIX + itemId
        composeRule.waitUntilTagDisplayed(checkboxTag)
        composeRule.onNodeWithTag(checkboxTag).performClick()

        composeRule.waitForIdle()

        // 購入状況タブに切り替えて確認
        composeRule.onNodeWithTag(ShoppingListTestTags.TAB_PURCHASE_STATUS).performClick()
        composeRule.waitForIdle()

        // 購入済みセクションに移動していることを確認
        composeRule.waitUntilNodeExists(ShoppingListTestTags.ITEM_PURCHASED_PREFIX + itemId)
        composeRule.onNodeWithTag(ShoppingListTestTags.ITEM_PURCHASED_PREFIX + itemId).assertIsDisplayed()
    }

    private fun MainActivityRule.getString(resId: Int): String = activity.getString(resId)

    private fun MainActivityRule.getString(resId: Int, vararg args: Any): String = activity.getString(resId, *args)

    private fun ComposeTestRule.waitUntilNodeExists(tag: String, timeoutMillis: Long = 5_000) {
        waitUntilWithClock(timeoutMillis) {
            runCatching {
                onNodeWithTag(tag, useUnmergedTree = false).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilTextDisplayed(text: String, useUnmergedTree: Boolean = false, timeoutMillis: Long = 5_000) {
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

    private fun ComposeTestRule.waitUntilTagDisplayed(tag: String, useUnmergedTree: Boolean = false) {
        waitUntilWithClock {
            runCatching {
                onNodeWithTag(tag, useUnmergedTree).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilWithClock(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
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

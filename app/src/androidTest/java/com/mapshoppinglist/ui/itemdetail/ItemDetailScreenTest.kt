package com.mapshoppinglist.ui.itemdetail

import android.os.SystemClock
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mapshoppinglist.MainActivity
import com.mapshoppinglist.R
import com.mapshoppinglist.testtag.ItemDetailTestTags
import com.mapshoppinglist.testtag.RecentPlacesTestTags
import com.mapshoppinglist.util.TestDataHelper
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ItemDetailScreenTest {

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
    fun タイトルとメモを修正して保存できる() {
        TestDataHelper.insertItem(title = "牛乳", note = "元のメモ")

        openItemDetail("牛乳")

        composeRule.onNodeWithTag(ItemDetailTestTags.TITLE_INPUT, useUnmergedTree = true)
            .performTextReplacement("牛乳（特売）")
        composeRule.onNodeWithTag(ItemDetailTestTags.NOTE_INPUT, useUnmergedTree = true)
            .performTextReplacement("明日の朝までに購入")

        clickBack()
        composeRule.waitForIdle()

        openItemDetail("牛乳（特売）")
        composeRule.onNodeWithTag(ItemDetailTestTags.TITLE_INPUT, useUnmergedTree = true)
            .assertTextEquals("牛乳（特売）")
        composeRule.onNodeWithTag(ItemDetailTestTags.NOTE_INPUT, useUnmergedTree = true)
            .assertTextEquals("明日の朝までに購入")
        clickBack()
    }

    @Test
    fun メモ欄をブランクにできる() {
        TestDataHelper.insertItem(title = "パン", note = "バター付き")

        openItemDetail("パン")
        composeRule.onNodeWithTag(ItemDetailTestTags.NOTE_INPUT, useUnmergedTree = true)
            .performTextReplacement("")
        clickBack()
        composeRule.waitForIdle()

        openItemDetail("パン")
        composeRule.onNodeWithTag(ItemDetailTestTags.NOTE_INPUT, useUnmergedTree = true)
            .assertTextEquals("")
        clickBack()
    }

    @Test
    fun タイトルをブランクにした場合は修正されない() {
        TestDataHelper.insertItem(title = "ヨーグルト", note = "無糖")

        openItemDetail("ヨーグルト")
        composeRule.onNodeWithTag(ItemDetailTestTags.TITLE_INPUT, useUnmergedTree = true)
            .performTextReplacement("")
        composeRule.onNodeWithTag(ItemDetailTestTags.NOTE_INPUT, useUnmergedTree = true)
            .performTextReplacement("加糖に変更")
        clickBack()
        composeRule.waitForIdle()

        openItemDetail("ヨーグルト")
        composeRule.onNodeWithTag(ItemDetailTestTags.TITLE_INPUT, useUnmergedTree = true)
            .assertTextEquals("ヨーグルト")
        composeRule.onNodeWithTag(ItemDetailTestTags.NOTE_INPUT, useUnmergedTree = true)
            .assertTextEquals("加糖に変更")
        clickBack()
    }

    @Test
    fun 紐付けたお店を削除できる() {
        val itemTitle = "卵"
        val placeId = TestDataHelper.createPlace("近所のスーパー", 35.1, 139.1)
        val itemId = TestDataHelper.insertItem(title = itemTitle)
        TestDataHelper.linkItemToPlace(itemId, placeId)

        openItemDetail(itemTitle)
        composeRule.onNodeWithTag("${ItemDetailTestTags.LINKED_PLACE_REMOVE_PREFIX}$placeId").performClick()
        composeRule.waitUntilPlaceUnlinked(placeId)
        clickBack()
        composeRule.waitForIdle()

        openItemDetail(itemTitle)
        composeRule.waitUntilPlaceUnlinked(placeId)
        clickBack()
    }

    @Test
    fun 新たにお店を紐付けできる() {
        val itemTitle = "野菜ジュース"
        val placeId = TestDataHelper.createPlace("テスト商店街", 35.2, 139.2)
        TestDataHelper.insertItem(title = itemTitle)

        openItemDetail(itemTitle)
        composeRule.onNodeWithTag(ItemDetailTestTags.ADD_PLACE_BUTTON).performClick()
        composeRule.onNodeWithTag(ItemDetailTestTags.ADD_PLACE_DIALOG_RECENT).performClick()
        composeRule.waitUntilRecentPlaceDisplayed(placeId)
        composeRule.onNodeWithTag("${RecentPlacesTestTags.PLACE_ROW_PREFIX}$placeId").performClick()
        openItemDetail(itemTitle)
        composeRule.waitUntilPlaceLinked(placeId)
        clickBack()

        // 加えて、買い物リスト画面でも紐付き件数が 1 件と表示されていることを確認
        composeRule.waitUntilTextDisplayed(itemTitle)
        composeRule.onNodeWithText(
            composeRule.getString(R.string.shopping_list_linked_places_count, 1)
        ).assertIsDisplayed()
    }

    private fun openItemDetail(title: String) {
        composeRule.waitUntilWithClock {
            runCatching {
                composeRule.onNodeWithText(title).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText(title).performClick()
        composeRule.waitUntilTagDisplayed(ItemDetailTestTags.TITLE_INPUT)
    }

    private fun clickBack() {
        // 戻る操作はUIスレッドで行う必要があるため、明示的にUIスレッドで呼び出す
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    private fun ComposeTestRule.waitUntilPlaceLinked(placeId: Long) {
        waitUntilWithClock {
            runCatching {
                onNodeWithTag("${ItemDetailTestTags.LINKED_PLACE_PREFIX}$placeId").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilPlaceUnlinked(placeId: Long) {
        waitUntilWithClock {
            runCatching {
                onNodeWithTag("${ItemDetailTestTags.LINKED_PLACE_PREFIX}$placeId").assertIsDisplayed()
                false
            }.getOrDefault(true)
        }
    }

    private fun ComposeTestRule.waitUntilRecentPlaceDisplayed(placeId: Long) {
        waitUntilWithClock {
            runCatching {
                onNodeWithTag("${RecentPlacesTestTags.PLACE_ROW_PREFIX}$placeId").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilTagDisplayed(tag: String) {
        waitUntilWithClock {
            runCatching {
                onNodeWithTag(tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilTextDisplayed(text: String, timeoutMillis: Long = 5_000) {
        waitUntilWithClock(timeoutMillis) {
            runCatching {
                onNodeWithText(text).assertIsDisplayed()
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

    private fun MainActivityRule.getString(resId: Int): String = activity.getString(resId)

    private fun MainActivityRule.getString(resId: Int, vararg args: Any): String = activity.getString(resId, *args)
}

private typealias MainActivityRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

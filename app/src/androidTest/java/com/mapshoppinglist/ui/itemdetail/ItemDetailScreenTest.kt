package com.mapshoppinglist.ui.itemdetail

import android.os.SystemClock
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.mapshoppinglist.MainActivity
import com.mapshoppinglist.ui.recentplaces.RecentPlacesTestTags
import com.mapshoppinglist.util.TestDataHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        val itemId = TestDataHelper.insertItem(title = "牛乳", note = "元のメモ")

        openItemDetail(itemId, "牛乳")

        composeRule.onNodeWithTag(ItemDetailTestTags.TITLE_INPUT)
            .performTextReplacement("牛乳（特売）")
        composeRule.onNodeWithTag(ItemDetailTestTags.NOTE_INPUT)
            .performTextReplacement("明日の朝までに購入")

        clickBack()
        composeRule.waitForIdle()
        val item = TestDataHelper.getItemWithPlaces(itemId)
        requireNotNull(item)
        assertEquals("牛乳（特売）", item.item.title)
        assertEquals("明日の朝までに購入", item.item.note)
    }

    @Test
    fun メモ欄をブランクにできる() {
        val itemId = TestDataHelper.insertItem(title = "パン", note = "バター付き")

        openItemDetail(itemId, "パン")
        composeRule.onNodeWithTag(ItemDetailTestTags.NOTE_INPUT)
            .performTextReplacement("")
        clickBack()
        composeRule.waitForIdle()
        val item = TestDataHelper.getItemWithPlaces(itemId)
        requireNotNull(item)
        assertEquals(null, item.item.note)
    }

    @Test
    fun タイトルをブランクにした場合は修正されない() {
        val itemId = TestDataHelper.insertItem(title = "ヨーグルト", note = "無糖")

        openItemDetail(itemId, "ヨーグルト")
        composeRule.onNodeWithTag(ItemDetailTestTags.TITLE_INPUT)
            .performTextReplacement("")
        composeRule.onNodeWithTag(ItemDetailTestTags.NOTE_INPUT)
            .performTextReplacement("加糖に変更")
        clickBack()
        composeRule.waitForIdle()
        val item = TestDataHelper.getItemWithPlaces(itemId)
        requireNotNull(item)
        assertEquals("ヨーグルト", item.item.title)
        assertEquals("加糖に変更", item.item.note)
    }

    @Test
    fun 紐付けたお店を削除できる() {
        val itemId = TestDataHelper.insertItem(title = "卵")
        val placeId = TestDataHelper.createPlace("近所のスーパー", 35.1, 139.1)
        TestDataHelper.linkItemToPlace(itemId, placeId)

        openItemDetail(itemId, "卵")
        composeRule.onNodeWithTag("${ItemDetailTestTags.LINKED_PLACE_REMOVE_PREFIX}$placeId")
            .performClick()

        composeRule.waitUntilPlaceUnlinked(placeId)
        clickBack()
        composeRule.waitForIdle()
        val linked = TestDataHelper.getLinkedPlaceIds(itemId)
        assertTrue(linked.isEmpty())
    }

    @Test
    fun 新たにお店を紐付けできる() {
        val itemId = TestDataHelper.insertItem(title = "野菜ジュース")
        val placeId = TestDataHelper.createPlace("テスト商店街", 35.2, 139.2)

        openItemDetail(itemId, "野菜ジュース")
        composeRule.onNodeWithTag(ItemDetailTestTags.ADD_PLACE_BUTTON).performClick()
        composeRule.onNodeWithTag(ItemDetailTestTags.ADD_PLACE_DIALOG_RECENT).performClick()

        composeRule.waitUntilRecentPlaceDisplayed(placeId)
        composeRule.onNodeWithTag("${RecentPlacesTestTags.PLACE_ROW_PREFIX}$placeId").performClick()

        composeRule.waitUntilPlaceLinked(placeId)
        clickBack()
        composeRule.waitForIdle()
        val linked = TestDataHelper.getLinkedPlaceIds(itemId)
        assertTrue(linked.contains(placeId))
    }

    private fun openItemDetail(itemId: Long, title: String) {
        composeRule.waitUntilTextDisplayed(title)
        composeRule.onNodeWithText(title).performClick()
        composeRule.waitUntilTagDisplayed(ItemDetailTestTags.TITLE_INPUT)
    }

    private fun clickBack() {
        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
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

    private fun ComposeTestRule.waitUntilTextDisplayed(
        text: String,
        timeoutMillis: Long = 5_000
    ) {
        waitUntilWithClock(timeoutMillis) {
            runCatching {
                onNodeWithText(text).assertIsDisplayed()
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

    private fun MainActivityRule.getString(resId: Int): String {
        return activity.getString(resId)
    }
}

private typealias MainActivityRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

package com.mapshoppinglist.ui.placemanage

import android.os.SystemClock
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mapshoppinglist.MainActivity
import com.mapshoppinglist.R
import com.mapshoppinglist.testtag.ItemDetailTestTags
import com.mapshoppinglist.testtag.PlaceManagementTestTags
import com.mapshoppinglist.util.TestDataHelper
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class PlaceManagementScreenTest {

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
    fun 登録済みのお店を削除できる() {
        val placeId = TestDataHelper.createPlace("削除対象店舗", 35.0, 139.0)

        openPlaceManagement()
        composeRule.waitUntilPlaceRowDisplayed(placeId)

        composeRule.onNodeWithTag("${PlaceManagementTestTags.PLACE_DELETE_PREFIX}$placeId").performClick()
        composeRule.onNodeWithTag(PlaceManagementTestTags.DELETE_DIALOG_CONFIRM).performClick()

        composeRule.waitUntilPlaceMissing(placeId)
    }

    @Test
    fun 紐付けられたお店を削除するとアイテムの紐付けも解除される() {
        val itemTitle = "削除テスト品"
        val itemId = TestDataHelper.insertItem(itemTitle)
        val placeId = TestDataHelper.createPlace("関連店舗", 35.1, 139.1)
        TestDataHelper.linkItemToPlace(itemId, placeId)

        openPlaceManagement()
        composeRule.waitUntilPlaceRowDisplayed(placeId)

        composeRule.onNodeWithTag("${PlaceManagementTestTags.PLACE_DELETE_PREFIX}$placeId").performClick()
        composeRule.onNodeWithTag(PlaceManagementTestTags.DELETE_DIALOG_CONFIRM).performClick()

        composeRule.waitUntilPlaceMissing(placeId)

        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        openItemDetail(itemTitle)
        composeRule.waitUntilPlaceUnlinked(placeId)
        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun openPlaceManagement() {
        composeRule.onNodeWithContentDescription(composeRule.getString(R.string.common_more_options)).performClick()
        composeRule.onNodeWithText(composeRule.getString(R.string.menu_manage_places)).performClick()
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

    private fun ComposeTestRule.waitUntilPlaceRowDisplayed(placeId: Long) {
        this.waitUntilWithClock {
            runCatching {
                onNodeWithTag("${PlaceManagementTestTags.PLACE_ROW_PREFIX}$placeId").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilPlaceMissing(placeId: Long) {
        this.waitUntilWithClock {
            runCatching {
                onNodeWithTag("${PlaceManagementTestTags.PLACE_ROW_PREFIX}$placeId").assertIsDisplayed()
                false
            }.getOrDefault(true)
        }
    }

    private fun ComposeTestRule.waitUntilPlaceUnlinked(placeId: Long) {
        this.waitUntilWithClock {
            runCatching {
                onNodeWithTag("${ItemDetailTestTags.LINKED_PLACE_PREFIX}$placeId").assertIsDisplayed()
                false
            }.getOrDefault(true)
        }
    }

    private fun ComposeTestRule.waitUntilTagDisplayed(tag: String) {
        this.waitUntilWithClock {
            runCatching {
                onNodeWithTag(tag).assertIsDisplayed()
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
}

private typealias MainActivityRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

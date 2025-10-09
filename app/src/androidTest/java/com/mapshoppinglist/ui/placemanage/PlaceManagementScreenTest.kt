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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.mapshoppinglist.MainActivity
import com.mapshoppinglist.R
import com.mapshoppinglist.ui.placemanage.PlaceManagementTestTags
import com.mapshoppinglist.util.TestDataHelper
import org.junit.After
import org.junit.Assert.assertTrue
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
        val place = TestDataHelper.getPlace(placeId)
        assertTrue(place == null)
    }

    @Test
    fun 紐付けられたお店を削除するとアイテムの紐付けも解除される() {
        val itemId = TestDataHelper.insertItem("削除テスト品")
        val placeId = TestDataHelper.createPlace("関連店舗", 35.1, 139.1)
        TestDataHelper.linkItemToPlace(itemId, placeId)

        openPlaceManagement()
        composeRule.waitUntilPlaceRowDisplayed(placeId)

        composeRule.onNodeWithTag("${PlaceManagementTestTags.PLACE_DELETE_PREFIX}$placeId").performClick()
        composeRule.onNodeWithTag(PlaceManagementTestTags.DELETE_DIALOG_CONFIRM).performClick()

        composeRule.waitUntilPlaceMissing(placeId)
        val links = TestDataHelper.getLinkedPlaceIds(itemId)
        assertTrue(links.isEmpty())
    }

    private fun openPlaceManagement() {
        composeRule.onNodeWithContentDescription(composeRule.getString(R.string.common_more_options)).performClick()
        composeRule.onNodeWithText(composeRule.getString(R.string.menu_manage_places)).performClick()
    }

    private fun ComposeTestRule.waitUntilPlaceRowDisplayed(placeId: Long) {
        waitUntilWithClock {
            runCatching {
                onNodeWithTag("${PlaceManagementTestTags.PLACE_ROW_PREFIX}$placeId").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilPlaceMissing(placeId: Long) {
        waitUntilWithClock {
            runCatching {
                onNodeWithTag("${PlaceManagementTestTags.PLACE_ROW_PREFIX}$placeId").assertIsDisplayed()
                false
            }.getOrDefault(true)
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

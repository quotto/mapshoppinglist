package com.mapshoppinglist.ui.settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mapshoppinglist.nearby.NearbyActivityEventLogWriter
import com.mapshoppinglist.testtag.NearbyDiagnosticLogTestTags
import com.mapshoppinglist.ui.theme.MapShoppingListTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NearbyDiagnosticLogRouteTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsGeneratedDeviceLog() {
        val writer = createWriter("showsGeneratedDeviceLog").apply {
            appendDiagnostic("TEST_ACTION", "first-entry")
            appendDiagnostic("TEST_ACTION", "second-entry")
        }

        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                MapShoppingListTheme {
                    NearbyDiagnosticLogRoute(
                        onBack = {},
                        logWriter = writer
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("first-entry", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("second-entry", substring = true).assertIsDisplayed()
    }

    @Test
    fun scrollToBottomButtonMovesToLatestLog() {
        val writer = createWriter("scrollToBottomButtonMovesToLatestLog").apply {
            repeat(120) { index ->
                appendDiagnostic("TEST_ACTION", "entry-$index")
            }
        }
        val scrollState = ScrollState(initial = 0)

        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                MapShoppingListTheme {
                    NearbyDiagnosticLogRoute(
                        onBack = {},
                        logWriter = writer,
                        providedScrollState = scrollState
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertTrue("Expected log content to be scrollable", scrollState.maxValue > 0)
            assertEquals("Expected initial scroll position to be top", 0, scrollState.value)
        }
        composeRule.onNodeWithTag(NearbyDiagnosticLogTestTags.SCROLL_TO_BOTTOM_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(
                "Expected scroll position to move to bottom",
                scrollState.maxValue,
                scrollState.value
            )
        }
    }

    private fun createWriter(name: String): NearbyActivityEventLogWriter {
        val filesDir = File(composeRule.activity.cacheDir, "nearby-diagnostic-log-tests/$name").apply {
            deleteRecursively()
            mkdirs()
        }
        return NearbyActivityEventLogWriter(
            filesDir = filesDir,
            nowMillisProvider = { 0L },
            enabled = true
        )
    }
}

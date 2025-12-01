@file:OptIn(ExperimentalTestApi::class)

package com.mapshoppinglist.ui.place

import android.Manifest
import android.location.Location
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mapshoppinglist.R
import com.mapshoppinglist.testtag.PlacePickerTestTags
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlacePickerTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    fun 現在地を初期表示のカメラ位置として利用する() {
        val expected = LatLng(35.0, 139.0)
        val fakeLocation = Location("test").apply {
            latitude = expected.latitude
            longitude = expected.longitude
        }

        composeRule.setContent {
            PlacePickerRoute(
                onPlaceRegistered = {},
                onClose = {},
                locationProvider = FakeLocationProvider(fakeLocation)
            )
        }

        composeRule.waitUntilMapRendered()
        // 地図コンポーネントの初期化やカメラアニメーションが端末によって遅延することがあるため、十分待機してから検証する
        composeRule.waitUntilCameraTarget(expected)

        composeRule.onNodeWithTag(PlacePickerTestTags.MAP)
            .assert(hasCameraTargetCloseTo(expected))
    }

    @Test
    fun 地図中心座標をコールバックへ伝搬する() {
        val expected = LatLng(34.5, 135.5)
        val received = mutableListOf<LatLng>()
        var cameraState: CameraPositionState? = null

        composeRule.setContent {
            val state = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, 10f)
            }
            LaunchedEffect(state) { cameraState = state }
            PlacePickerScreen(
                uiState = PlacePickerUiState(cameraLocation = expected),
                onQueryChange = {},
                onPredictionSelected = {},
                onConfirm = {},
                onClearSelection = {},
                onClose = {},
                snackbarHostState = SnackbarHostState(),
                hasLocationPermission = false,
                onMapLongClick = {},
                onPoiClick = {},
                onCameraMoved = { received.add(it) },
                onRequestLocationPermission = {},
                cameraPositionState = state
            )
        }

        composeRule.waitUntilWithClock { cameraState != null }

        composeRule.runOnIdle {
            cameraState?.position = CameraPosition.fromLatLngZoom(expected, 12f)
        }

        composeRule.waitUntilWithClock { received.any { isCloseTo(it, expected) } }

        val latest = received.last()
        assertTrue(isCloseTo(latest, expected))
    }

    @Test
    fun 位置情報権限が無い場合はプレースホルダーが表示される() {
        var requested = false
        val buttonLabel = composeRule.activity.getString(R.string.permission_location_request_button)

        composeRule.setContent {
            PlacePickerScreen(
                uiState = PlacePickerUiState(),
                onQueryChange = {},
                onPredictionSelected = {},
                onConfirm = {},
                onClearSelection = {},
                onClose = {},
                snackbarHostState = SnackbarHostState(),
                hasLocationPermission = false,
                onMapLongClick = {},
                onPoiClick = {},
                onCameraMoved = {},
                onRequestLocationPermission = { requested = true }
            )
        }

        composeRule.onNodeWithTag(PlacePickerTestTags.LOCATION_PERMISSION_PLACEHOLDER).assertIsDisplayed()
        composeRule.onNodeWithText(buttonLabel).performClick()
        assertTrue(requested)
    }

    private fun hasCameraTargetCloseTo(expected: LatLng, tolerance: Double = 5e-4): SemanticsMatcher {
        return SemanticsMatcher("camera target ≈ $expected") { node ->
            val actual = node.config.getOrNull(PlacePickerCameraTargetKey) ?: return@SemanticsMatcher false
            isCloseTo(actual, expected, tolerance)
        }
    }

    private fun isCloseTo(actual: LatLng, expected: LatLng, tolerance: Double = 5e-4): Boolean =
        abs(actual.latitude - expected.latitude) < tolerance &&
            abs(actual.longitude - expected.longitude) < tolerance

    private fun ComposeTestRule.waitUntilMapRendered(timeoutMillis: Long = 5_000) {
        waitUntilWithClock(timeoutMillis) {
            runCatching {
                onNodeWithTag(PlacePickerTestTags.MAP, useUnmergedTree = false).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun ComposeTestRule.waitUntilCameraTarget(expected: LatLng, tolerance: Double = 5e-4, timeoutMillis: Long = 10_000) {
        waitUntilWithClock(timeoutMillis) {
            runCatching {
                onNodeWithTag(PlacePickerTestTags.MAP).assert(hasCameraTargetCloseTo(expected, tolerance))
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

    private class FakeLocationProvider(private val location: Location?) : CurrentLocationProvider {
        override suspend fun getLastLocation(): Location? = location
    }
}

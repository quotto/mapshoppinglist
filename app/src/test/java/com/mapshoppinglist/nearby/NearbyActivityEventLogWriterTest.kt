package com.mapshoppinglist.nearby

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.DetectedActivity
import com.mapshoppinglist.domain.model.NearbyStoreCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NearbyActivityEventLogWriterTest {

    @Test
    fun `appendTransitionEvents writes readable transition lines`() {
        val filesDir = createTempDir(prefix = "nearby-activity-log")
        val writer = NearbyActivityEventLogWriter(filesDir, { 1_743_208_523_456L }, enabled = true)

        writer.appendTransitionEvents(
            action = "com.mapshoppinglist.ACTION_ACTIVITY_TRANSITION",
            events = listOf(
                ActivityTransitionEvent(
                    DetectedActivity.WALKING,
                    ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                    100L
                ),
                ActivityTransitionEvent(
                    DetectedActivity.STILL,
                    ActivityTransition.ACTIVITY_TRANSITION_EXIT,
                    200L
                )
            )
        )

        val lines = writer.logFile().readLines()

        assertEquals(2, lines.size)
        assertTrue(lines[0].matches(Regex("""^\[\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}] .+""")))
        assertTrue(lines[0].contains("kind=transition"))
        assertTrue(lines[0].contains("activity=WALKING"))
        assertTrue(lines[0].contains("transition=ENTER"))
        assertTrue(lines[0].contains("elapsedNanos=100"))
        assertTrue(lines[1].contains("activity=STILL"))
        assertTrue(lines[1].contains("transition=EXIT"))
    }

    @Test
    fun `appendDiagnostic appends diagnostic line to the same file`() {
        val filesDir = createTempDir(prefix = "nearby-activity-log")
        val writer = NearbyActivityEventLogWriter(filesDir, { 1_743_208_523_456L }, enabled = true)

        writer.appendDiagnostic(
            action = "com.mapshoppinglist.ACTION_ACTIVITY_TRANSITION",
            message = "no_still_enter_transition"
        )

        val content = writer.logFile().readText()

        assertTrue(content.contains("kind=diagnostic"))
        assertTrue(content.contains("message=no_still_enter_transition"))
    }

    @Test
    fun `appendSuggestionSearchResult writes candidates and selected candidate`() {
        val filesDir = createTempDir(prefix = "nearby-activity-log")
        val writer = NearbyActivityEventLogWriter(filesDir, { 1_743_208_523_456L }, enabled = true)

        writer.appendSuggestionSearchResult(
            reason = "activity_still",
            itemTitle = "牛乳 1L",
            candidates = listOf(
                NearbyStoreCandidate(
                    placeId = "store-1",
                    name = "近所 スーパー",
                    address = "東京都",
                    latitude = 35.0,
                    longitude = 139.0,
                    distanceMeters = 120,
                    primaryType = "supermarket"
                ),
                NearbyStoreCandidate(
                    placeId = "store-2",
                    name = "駅前ドラッグ",
                    address = "東京都",
                    latitude = 35.1,
                    longitude = 139.1,
                    distanceMeters = 240,
                    primaryType = "drugstore"
                )
            ),
            selectedCandidate = NearbyStoreCandidate(
                placeId = "store-1",
                name = "近所 スーパー",
                address = "東京都",
                latitude = 35.0,
                longitude = 139.0,
                distanceMeters = 120,
                primaryType = "supermarket"
            )
        )

        val content = writer.logFile().readText()

        assertTrue(content.contains("kind=suggestion_search"))
        assertTrue(content.contains("reason=activity_still"))
        assertTrue(content.contains("item=牛乳_1L"))
        assertTrue(content.contains("selected=store-1:近所_スーパー:120m:supermarket"))
        assertTrue(content.contains("candidates=[store-1:近所_スーパー:120m:supermarket, store-2:駅前ドラッグ:240m:drugstore]"))
    }

    @Test
    fun `appendSuggestionSearchQuery writes query before search`() {
        val filesDir = createTempDir(prefix = "nearby-activity-log")
        val writer = NearbyActivityEventLogWriter(filesDir, { 1_743_208_523_456L }, enabled = true)

        writer.appendSuggestionSearchQuery(
            reason = "activity_still",
            itemTitle = "牛乳 1L",
            searchQuery = "牛乳 1L"
        )

        val content = writer.logFile().readText()

        assertTrue(content.contains("kind=suggestion_query"))
        assertTrue(content.contains("reason=activity_still"))
        assertTrue(content.contains("item=牛乳_1L"))
        assertTrue(content.contains("query=牛乳_1L"))
    }

    @Test
    fun `writer does not create file when disabled`() {
        val filesDir = createTempDir(prefix = "nearby-activity-log")
        val writer = NearbyActivityEventLogWriter(filesDir, { 1_743_208_523_456L }, enabled = false)

        writer.appendDiagnostic(
            action = "com.mapshoppinglist.ACTION_ACTIVITY_TRANSITION",
            message = "ignored"
        )

        assertFalse(writer.logFile().exists())
    }

    private fun createTempDir(prefix: String): File {
        return kotlin.io.path.createTempDirectory(prefix).toFile().apply {
            deleteOnExit()
        }
    }
}

package com.mapshoppinglist.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapshoppinglist.R
import com.mapshoppinglist.domain.usecase.NotificationMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.TIRAMISU],
    application = android.app.Application::class
)
class NotificationSenderTest {

    @Test
    fun showPlaceReminder_setsAppIconAsSmallIcon() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sender = NotificationSender(context)
        val message = NotificationMessage(
            title = "テスト",
            lines = listOf("・牛乳"),
            summary = null
        )

        @Suppress("MissingPermission")
        sender.showPlaceReminder(
            placeId = 1L,
            itemIds = listOf(1L),
            message = message
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowManager = Shadows.shadowOf(notificationManager)
        val notification = shadowManager.allNotifications.single()

        assertEquals(R.drawable.ic_launcher_foreground, notification.smallIcon.resId)
    }

    @Test
    fun showNearbySuggestion_displaysItemNotificationWithActions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sender = NotificationSender(context)

        @Suppress("MissingPermission")
        sender.showNearbySuggestion(
            entry = NotificationSender.NearbySuggestionNotificationEntry(
                itemId = 1L,
                itemTitle = "牛乳",
                placeName = "近所スーパー",
                distanceMeters = 120,
                placeLatitude = 35.0,
                placeLongitude = 139.0
            )
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = Shadows.shadowOf(notificationManager).allNotifications.single()

        assertEquals(R.drawable.ic_launcher_foreground, notification.smallIcon.resId)
        assertTrue(notification.extras.getString("android.title").orEmpty().contains("牛乳"))
        assertTrue(notification.extras.getCharSequence("android.text").toString().contains("近所スーパー"))
        assertEquals(3, notification.actions.size)
        val actionTitles = notification.actions.map { it.title.toString() }
        assertTrue(actionTitles.contains("購入済みにする"))
        assertTrue(actionTitles.contains("削除"))
        assertTrue(actionTitles.contains("地図"))
    }
}

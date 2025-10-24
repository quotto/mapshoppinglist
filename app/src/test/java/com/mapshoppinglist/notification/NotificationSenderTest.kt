package com.mapshoppinglist.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapshoppinglist.R
import com.mapshoppinglist.domain.usecase.NotificationMessage
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
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
}

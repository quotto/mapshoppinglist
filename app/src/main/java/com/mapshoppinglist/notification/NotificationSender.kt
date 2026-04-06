package com.mapshoppinglist.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mapshoppinglist.MainActivity
import com.mapshoppinglist.R
import com.mapshoppinglist.domain.usecase.NotificationMessage

class NotificationSender(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showPlaceReminder(placeId: Long, itemIds: List<Long>, message: NotificationMessage) {
        ensureChannel()
        val contentIntent = PendingIntent.getActivity(
            context,
            placeId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NotificationActions.EXTRA_PLACE_ID, placeId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val markPurchasedIntent = PendingIntent.getBroadcast(
            context,
            placeId.hashCode(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActions.ACTION_MARK_PURCHASED
                putExtra(NotificationActions.EXTRA_PLACE_ID, placeId)
                putExtra(NotificationActions.EXTRA_ITEM_IDS, itemIds.toLongArray())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val deleteIntent = PendingIntent.getBroadcast(
            context,
            placeId.hashCode() + 1,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActions.ACTION_DELETE
                putExtra(NotificationActions.EXTRA_PLACE_ID, placeId)
                putExtra(NotificationActions.EXTRA_ITEM_IDS, itemIds.toLongArray())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val detailIntent = itemIds.firstOrNull()?.let { targetItemId ->
            PendingIntent.getActivity(
                context,
                placeId.hashCode() + 2,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(NotificationActions.EXTRA_ITEM_ID, targetItemId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        val style = NotificationCompat.InboxStyle().also { inbox ->
            message.lines.forEach { inbox.addLine(it) }
            message.summary?.let { inbox.setSummaryText(it) }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(message.title)
            .setContentText(message.summary ?: context.getString(R.string.notification_default_summary))
            .setStyle(style)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, context.getString(R.string.notification_action_mark_purchased), markPurchasedIntent)
            .addAction(0, context.getString(R.string.notification_action_delete), deleteIntent)
        detailIntent?.let {
            builder.addAction(0, context.getString(R.string.notification_action_detail), it)
        }

        notificationManager.notify(placeId.hashCode(), builder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNearbySuggestion(entry: NearbySuggestionNotificationEntry) {
        ensureChannel()
        val notificationId = entry.itemId.hashCode()
        val detailIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NotificationActions.EXTRA_ITEM_ID, entry.itemId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val markPurchasedIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActions.ACTION_MARK_ITEM_PURCHASED
                putExtra(NotificationActions.EXTRA_ITEM_ID, entry.itemId)
                putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val deleteIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActions.ACTION_DELETE_ITEM
                putExtra(NotificationActions.EXTRA_ITEM_ID, entry.itemId)
                putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val mapIntent = createMapIntent(entry) ?: detailIntent

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(
                context.getString(
                    R.string.notification_nearby_suggestion_title,
                    entry.itemTitle
                )
            )
            .setContentText(
                context.getString(
                    R.string.notification_nearby_suggestion_summary,
                    entry.placeName,
                    entry.distanceMeters
                )
            )
            .setContentIntent(detailIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, context.getString(R.string.notification_action_mark_purchased), markPurchasedIntent)
            .addAction(0, context.getString(R.string.notification_action_delete), deleteIntent)
            .addAction(0, context.getString(R.string.notification_action_detail), detailIntent)
            .addAction(0, context.getString(R.string.notification_action_map), mapIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = context.getString(R.string.notification_channel_desc)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "shopping_reminders"
        private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    }

    data class NearbySuggestionNotificationEntry(
        val itemId: Long,
        val itemTitle: String,
        val placeName: String,
        val distanceMeters: Int,
        val placeLatitude: Double,
        val placeLongitude: Double
    )

    private fun createMapIntent(entry: NearbySuggestionNotificationEntry): PendingIntent? {
        val uri = Uri.parse(
            "geo:${entry.placeLatitude},${entry.placeLongitude}?q=${entry.placeLatitude},${entry.placeLongitude}(${Uri.encode(entry.placeName)})"
        )
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            `package` = GOOGLE_MAPS_PACKAGE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val resolvedIntent = if (mapIntent.resolveActivity(context.packageManager) != null) {
            mapIntent
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NotificationActions.EXTRA_ITEM_ID, entry.itemId)
            }
        }
        return PendingIntent.getActivity(
            context,
            entry.itemId.hashCode() + 3,
            resolvedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}

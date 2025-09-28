package com.mapshoppinglist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 通知のクールダウン・スヌーズ状態を保持するテーブル。
 */
@Entity(
    tableName = "notify_state",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["place_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NotifyStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "place_id")
    val placeId: Long,
    @ColumnInfo(name = "last_notified_at")
    val lastNotifiedAt: Long? = null,
    @ColumnInfo(name = "snooze_until")
    val snoozeUntil: Long? = null
)

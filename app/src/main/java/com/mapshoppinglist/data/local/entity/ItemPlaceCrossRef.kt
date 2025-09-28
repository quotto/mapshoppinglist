package com.mapshoppinglist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * アイテムとお店の多対多関係を管理する中間テーブル。
 */
@Entity(
    tableName = "item_place",
    primaryKeys = ["item_id", "place_id"],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["place_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["place_id", "item_id"])
    ]
)
data class ItemPlaceCrossRef(
    @ColumnInfo(name = "item_id")
    val itemId: Long,
    @ColumnInfo(name = "place_id")
    val placeId: Long
)

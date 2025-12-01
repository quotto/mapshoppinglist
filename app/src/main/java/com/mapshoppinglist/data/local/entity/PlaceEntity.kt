package com.mapshoppinglist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * お店の位置情報を保持するエンティティ。
 */
@Entity(
    tableName = "places",
    indices = [
        Index(value = ["lat_e6", "lng_e6"], unique = true),
        Index(value = ["is_active", "last_used_at"])
    ]
)
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "lat_e6")
    val latitudeE6: Int,
    @ColumnInfo(name = "lng_e6")
    val longitudeE6: Int,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false
)

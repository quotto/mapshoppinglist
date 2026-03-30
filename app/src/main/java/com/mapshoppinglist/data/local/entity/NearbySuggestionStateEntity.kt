package com.mapshoppinglist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "nearby_suggestion_state",
    primaryKeys = ["item_id", "candidate_place_id"],
    indices = [Index(value = ["item_id"])]
)
data class NearbySuggestionStateEntity(
    @ColumnInfo(name = "item_id")
    val itemId: Long,
    @ColumnInfo(name = "candidate_place_id")
    val candidatePlaceId: String,
    @ColumnInfo(name = "candidate_place_name")
    val candidatePlaceName: String? = null,
    @ColumnInfo(name = "last_notified_at")
    val lastNotifiedAt: Long? = null,
    @ColumnInfo(name = "last_notified_lat_e6")
    val lastNotifiedLatE6: Int? = null,
    @ColumnInfo(name = "last_notified_lng_e6")
    val lastNotifiedLngE6: Int? = null
)

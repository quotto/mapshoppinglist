package com.mapshoppinglist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 現在登録済みのジオフェンスを追跡するテーブル。
 */
@Entity(tableName = "geofence_registry")
data class GeofenceRegistrationEntity(
    @PrimaryKey
    @ColumnInfo(name = "place_id")
    val placeId: Long,
    @ColumnInfo(name = "geofence_request_id")
    val geofenceRequestId: String
)

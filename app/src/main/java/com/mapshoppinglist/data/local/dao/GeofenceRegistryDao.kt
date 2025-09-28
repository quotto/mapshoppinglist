package com.mapshoppinglist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mapshoppinglist.data.local.entity.GeofenceRegistrationEntity

/**
 * ジオフェンス登録状態を管理するDAO。
 */
@Dao
interface GeofenceRegistryDao {
    @Query("SELECT * FROM geofence_registry")
    suspend fun loadAll(): List<GeofenceRegistrationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<GeofenceRegistrationEntity>)

    @Query("DELETE FROM geofence_registry WHERE place_id IN (:placeIds)")
    suspend fun deleteByPlaceIds(placeIds: List<Long>)

    @Query("DELETE FROM geofence_registry")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(entities: List<GeofenceRegistrationEntity>) {
        clear()
        if (entities.isNotEmpty()) {
            upsertAll(entities)
        }
    }
}

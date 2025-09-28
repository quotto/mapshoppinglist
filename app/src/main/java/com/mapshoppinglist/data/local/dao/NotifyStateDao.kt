package com.mapshoppinglist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mapshoppinglist.data.local.entity.NotifyStateEntity

/**
 * 通知状態を扱うDAO。
 */
@Dao
interface NotifyStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NotifyStateEntity)

    @Update
    suspend fun update(entity: NotifyStateEntity)

    @Query("SELECT * FROM notify_state WHERE place_id = :placeId")
    suspend fun findByPlaceId(placeId: Long): NotifyStateEntity?

    @Query("DELETE FROM notify_state WHERE place_id = :placeId")
    suspend fun deleteByPlaceId(placeId: Long)
}

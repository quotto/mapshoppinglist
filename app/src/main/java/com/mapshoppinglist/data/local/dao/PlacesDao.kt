package com.mapshoppinglist.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mapshoppinglist.data.local.entity.PlaceEntity

/**
 * お店テーブルを扱うDAO。
 */
@Dao
interface PlacesDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PlaceEntity): Long

    @Update
    suspend fun update(entity: PlaceEntity)

    @Delete
    suspend fun delete(entity: PlaceEntity)

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun findById(id: Long): PlaceEntity?

    @Query(
        "SELECT * FROM places " +
            "ORDER BY is_active DESC, " +
            "CASE WHEN last_used_at IS NULL THEN 1 ELSE 0 END, " +
            "last_used_at DESC"
    )
    suspend fun loadAll(): List<PlaceEntity>

    @Query("SELECT COUNT(*) FROM places")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM places WHERE lat_e6 = :latE6 AND lng_e6 = :lngE6")
    suspend fun countByLocation(latE6: Int, lngE6: Int): Int

    @Query("SELECT * FROM places WHERE is_active = 1")
    suspend fun loadActivePlaces(): List<PlaceEntity>
}

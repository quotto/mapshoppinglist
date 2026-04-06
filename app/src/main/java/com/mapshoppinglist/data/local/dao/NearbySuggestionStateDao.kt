package com.mapshoppinglist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mapshoppinglist.data.local.entity.NearbySuggestionStateEntity

@Dao
interface NearbySuggestionStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NearbySuggestionStateEntity)

    @Query(
        "SELECT * FROM nearby_suggestion_state " +
            "WHERE item_id = :itemId AND candidate_place_id = :candidatePlaceId"
    )
    suspend fun find(itemId: Long, candidatePlaceId: String): NearbySuggestionStateEntity?

    @Query(
        "SELECT * FROM nearby_suggestion_state " +
            "WHERE item_id = :itemId " +
            "ORDER BY COALESCE(last_notified_at, 0) DESC LIMIT 1"
    )
    suspend fun findLatestByItemId(itemId: Long): NearbySuggestionStateEntity?

    @Query(
        "DELETE FROM nearby_suggestion_state " +
            "WHERE item_id = :itemId AND candidate_place_id = :candidatePlaceId"
    )
    suspend fun delete(itemId: Long, candidatePlaceId: String)

    @Query("DELETE FROM nearby_suggestion_state WHERE item_id = :itemId")
    suspend fun deleteByItemId(itemId: Long)
}

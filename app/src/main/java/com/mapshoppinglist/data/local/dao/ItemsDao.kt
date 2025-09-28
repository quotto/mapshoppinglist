package com.mapshoppinglist.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mapshoppinglist.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * アイテムテーブルを扱うDAO。
 */
@Dao
interface ItemsDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ItemEntity>)

    @Update
    suspend fun update(entity: ItemEntity)

    @Delete
    suspend fun delete(entity: ItemEntity)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun findById(id: Long): ItemEntity?

    @Query(
        "SELECT i.*, COUNT(ip.place_id) AS linked_place_count " +
            "FROM items i LEFT JOIN item_place ip ON i.id = ip.item_id " +
            "GROUP BY i.id ORDER BY i.updated_at DESC"
    )
    fun observeAllWithPlaceCount(): Flow<List<ItemWithPlaceCount>>

    @Query("SELECT * FROM items WHERE is_purchased = 0 ORDER BY updated_at DESC")
    suspend fun loadNotPurchased(): List<ItemEntity>

    @Query("UPDATE items SET is_purchased = :isPurchased, updated_at = :updatedAt WHERE id = :itemId")
    suspend fun updatePurchaseState(itemId: Long, isPurchased: Boolean, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM items WHERE title = :title")
    suspend fun countByTitle(title: String): Int

    @Query(
        "SELECT i.* FROM items i " +
            "JOIN item_place ip ON i.id = ip.item_id " +
            "WHERE ip.place_id = :placeId AND i.is_purchased = 0"
    )
    suspend fun loadNotPurchasedByPlace(placeId: Long): List<ItemEntity>

    @Query(
        "UPDATE items SET is_purchased = 1, updated_at = :updatedAt " +
            "WHERE id IN (SELECT item_id FROM item_place WHERE place_id = :placeId) " +
            "AND is_purchased = 0"
    )
    suspend fun markPurchasedByPlace(placeId: Long, updatedAt: Long): Int
}

data class ItemWithPlaceCount(
    @Embedded val item: ItemEntity,
    @ColumnInfo(name = "linked_place_count") val linkedPlaceCount: Int
)

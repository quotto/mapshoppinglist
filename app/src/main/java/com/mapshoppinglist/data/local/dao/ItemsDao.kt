package com.mapshoppinglist.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.mapshoppinglist.data.local.entity.ItemEntity
import com.mapshoppinglist.data.local.entity.ItemPlaceCrossRef
import com.mapshoppinglist.data.local.entity.PlaceEntity
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

    @Transaction
    @Query("SELECT * FROM items WHERE id = :itemId")
    fun observeItemWithPlaces(itemId: Long): Flow<ItemWithPlaces?>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemWithPlaces(itemId: Long): ItemWithPlaces?

    /**
     * 地点に紐づいていないアイテムを取得する（購入場所タブ用）
     */
    @Query(
        "SELECT * FROM items WHERE id NOT IN (" +
            "SELECT DISTINCT item_id FROM item_place" +
            ") ORDER BY updated_at DESC"
    )
    fun observeItemsWithoutPlace(): Flow<List<ItemEntity>>
}

data class ItemWithPlaceCount(
    @Embedded val item: ItemEntity,
    @ColumnInfo(name = "linked_place_count") val linkedPlaceCount: Int
)

data class ItemWithPlaces(
    @Embedded val item: ItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemPlaceCrossRef::class,
            parentColumn = "item_id",
            entityColumn = "place_id"
        )
    )
    val places: List<PlaceEntity>
)

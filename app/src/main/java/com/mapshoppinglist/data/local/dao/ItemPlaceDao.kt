package com.mapshoppinglist.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.mapshoppinglist.data.local.entity.ItemEntity
import com.mapshoppinglist.data.local.entity.ItemPlaceCrossRef
import com.mapshoppinglist.data.local.entity.PlaceEntity

/**
 * アイテムとお店の紐づきを扱うDAO。
 */
@Dao
interface ItemPlaceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(crossRef: ItemPlaceCrossRef)

    @Query("DELETE FROM item_place WHERE item_id = :itemId AND place_id = :placeId")
    suspend fun deleteLink(itemId: Long, placeId: Long)

    @Query("SELECT * FROM item_place WHERE item_id = :itemId")
    suspend fun findLinksByItem(itemId: Long): List<ItemPlaceCrossRef>

    @Transaction
    @Query("SELECT * FROM places WHERE id = :placeId")
    suspend fun loadPlaceWithItems(placeId: Long): PlaceWithItems?
}

/**
 * お店と未購入アイテムの関連情報。
 */
data class PlaceWithItems(
    @Embedded
    val place: PlaceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemPlaceCrossRef::class,
            parentColumn = "place_id",
            entityColumn = "item_id"
        ),
        entity = ItemEntity::class
    )
    val items: List<ItemEntity>
)

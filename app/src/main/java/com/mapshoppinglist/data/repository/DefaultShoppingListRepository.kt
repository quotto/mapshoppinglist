package com.mapshoppinglist.data.repository

import com.mapshoppinglist.data.local.dao.ItemsDao
import com.mapshoppinglist.data.local.dao.ItemWithPlaceCount
import com.mapshoppinglist.data.local.dao.ItemWithPlaces
import com.mapshoppinglist.data.local.entity.ItemEntity
import com.mapshoppinglist.domain.exception.DuplicateItemException
import com.mapshoppinglist.domain.model.ItemDetail
import com.mapshoppinglist.domain.model.PlaceSummary
import com.mapshoppinglist.domain.model.ShoppingItem
import com.mapshoppinglist.domain.repository.ShoppingListRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room DAO を利用する買い物リストリポジトリの実装。
 */
class DefaultShoppingListRepository(
    private val itemsDao: ItemsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ShoppingListRepository {

    override fun observeAllItems(): Flow<List<ShoppingItem>> {
        return itemsDao.observeAllWithPlaceCount().map { records ->
            records.map { record ->
                record.item.toDomain(linkedPlaceCount = record.linkedPlaceCount)
            }
        }
    }

    override suspend fun addItem(title: String, note: String?): Long {
        require(title.isNotBlank()) { "title must not be blank" }
        val now = System.currentTimeMillis()
        val itemId = withContext(ioDispatcher) {
            val normalizedTitle = title.trim()
            if (itemsDao.countByTitle(normalizedTitle) > 0) {
                throw DuplicateItemException()
            }
            val entity = ItemEntity(
                title = normalizedTitle,
                note = note?.takeIf { it.isNotBlank() }?.trim(),
                isPurchased = false,
                createdAt = now,
                updatedAt = now
            )
            itemsDao.insert(entity)
        }
        return itemId
    }

    override suspend fun updatePurchasedState(itemId: Long, isPurchased: Boolean) {
        val now = System.currentTimeMillis()
        withContext(ioDispatcher) {
            itemsDao.updatePurchaseState(
                itemId = itemId,
                isPurchased = isPurchased,
                updatedAt = now
            )
        }
    }

    override suspend fun deleteItem(itemId: Long) {
        withContext(ioDispatcher) {
            val target = itemsDao.findById(itemId) ?: return@withContext
            itemsDao.delete(target)
        }
    }

    override suspend fun getItemsForPlace(placeId: Long): List<ShoppingItem> = withContext(ioDispatcher) {
        itemsDao.loadNotPurchasedByPlace(placeId).map { entity ->
            entity.toDomain(linkedPlaceCount = 0)
        }
    }

    override suspend fun markPlaceItemsPurchased(placeId: Long) {
        val now = System.currentTimeMillis()
        withContext(ioDispatcher) {
            itemsDao.markPurchasedByPlace(placeId, now)
        }
    }

    override fun observeItemDetail(itemId: Long): Flow<ItemDetail?> {
        return itemsDao.observeItemWithPlaces(itemId).map { record ->
            record?.toDomain()
        }
    }

    override suspend fun updateItem(itemId: Long, title: String, note: String?) {
        withContext(ioDispatcher) {
            val entity = itemsDao.findById(itemId) ?: return@withContext
            val updated = entity.copy(
                title = title.trim(),
                note = note?.trim(),
                updatedAt = System.currentTimeMillis()
            )
            itemsDao.update(updated)
        }
    }

    private fun ItemEntity.toDomain(linkedPlaceCount: Int): ShoppingItem {
        return ShoppingItem(
            id = id,
            title = title,
            note = note,
            isPurchased = isPurchased,
            createdAt = createdAt,
            updatedAt = updatedAt,
            linkedPlaceCount = linkedPlaceCount
        )
    }

    private fun ItemWithPlaces.toDomain(): ItemDetail {
        return ItemDetail(
            id = item.id,
            title = item.title,
            note = item.note,
            isPurchased = item.isPurchased,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
            places = places.map { place ->
                PlaceSummary(
                    id = place.id,
                    name = place.name,
                    address = place.note,
                    latitude = place.latitudeE6 / 1_000_000.0,
                    longitude = place.longitudeE6 / 1_000_000.0
                )
            }
        )
    }
}

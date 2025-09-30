package com.mapshoppinglist.data.repository

import android.util.Log
import com.mapshoppinglist.data.local.dao.ItemPlaceDao
import com.mapshoppinglist.data.local.dao.ItemsDao
import com.mapshoppinglist.data.local.dao.PlacesDao
import com.mapshoppinglist.data.local.entity.ItemPlaceCrossRef
import com.mapshoppinglist.data.local.entity.PlaceEntity
import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.repository.PlacesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultPlacesRepository(
    private val placesDao: PlacesDao,
    private val itemPlaceDao: ItemPlaceDao,
    private val itemsDao: ItemsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PlacesRepository {

    override suspend fun getTotalCount(): Int = withContext(ioDispatcher) {
        placesDao.count()
    }

    override suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean = withContext(ioDispatcher) {
        placesDao.countByLocation(latE6, lngE6) > 0
    }

    override suspend fun loadActivePlaces(): List<Place> = withContext(ioDispatcher) {
        placesDao.loadActivePlaces().map(::entityToDomain)
    }

    override suspend fun findById(placeId: Long): Place? = withContext(ioDispatcher) {
        placesDao.findById(placeId)?.let(::entityToDomain)
    }

    override suspend fun loadRecentPlaces(limit: Int): List<Place> = withContext(ioDispatcher) {
        placesDao.loadRecentPlaces(limit).map(::entityToDomain)
    }

    override suspend fun addPlace(name: String, latE6: Int, lngE6: Int, note: String?): Long = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val entity = PlaceEntity(
            name = name.trim(),
            latitudeE6 = latE6,
            longitudeE6 = lngE6,
            note = note?.trim(),
            lastUsedAt = now,
            isActive = false
        )
        placesDao.insert(entity)
    }

    override suspend fun deletePlace(placeId: Long) {
        withContext(ioDispatcher) {
            placesDao.findById(placeId)?.let { placesDao.delete(it) }
        }
    }

    override suspend fun linkItemToPlace(placeId: Long, itemId: Long) {
        Log.d("DefaultPlacesRepo", "Linking item $itemId to place $placeId")
        withContext(ioDispatcher) {
            itemPlaceDao.insertLink(ItemPlaceCrossRef(itemId = itemId, placeId = placeId))
            refreshActiveState(placeId)
        }
    }

    override suspend fun unlinkItemFromPlace(placeId: Long, itemId: Long) {
        withContext(ioDispatcher) {
            itemPlaceDao.deleteLink(itemId = itemId, placeId = placeId)
            refreshActiveState(placeId)
        }
    }

    private suspend fun refreshActiveState(placeId: Long) {
        val place = placesDao.findById(placeId) ?: return
        val hasUnpurchased = itemsDao.loadNotPurchasedByPlace(placeId).isNotEmpty()
        val updated = place.copy(
            isActive = hasUnpurchased,
            lastUsedAt = System.currentTimeMillis()
        )
        placesDao.update(updated)
    }

    private fun entityToDomain(entity: PlaceEntity): Place {
        return Place(
            id = entity.id,
            name = entity.name,
            latitudeE6 = entity.latitudeE6,
            longitudeE6 = entity.longitudeE6,
            isActive = entity.isActive
        )
    }
}

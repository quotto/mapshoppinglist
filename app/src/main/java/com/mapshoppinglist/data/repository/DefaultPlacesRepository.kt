package com.mapshoppinglist.data.repository

import com.mapshoppinglist.data.local.dao.PlacesDao
import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.domain.model.Place
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Room DAO を利用するお店リポジトリ。
 */
class DefaultPlacesRepository(
    private val placesDao: PlacesDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PlacesRepository {

    override suspend fun getTotalCount(): Int = withContext(ioDispatcher) {
        placesDao.count()
    }

    override suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean = withContext(ioDispatcher) {
        placesDao.countByLocation(latE6, lngE6) > 0
    }

    override suspend fun loadActivePlaces(): List<Place> = withContext(ioDispatcher) {
        placesDao.loadActivePlaces().map { entity ->
            Place(
                id = entity.id,
                name = entity.name,
                latitudeE6 = entity.latitudeE6,
                longitudeE6 = entity.longitudeE6,
                isActive = entity.isActive
            )
        }
    }
}

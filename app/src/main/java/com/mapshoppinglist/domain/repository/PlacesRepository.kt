package com.mapshoppinglist.domain.repository

/**
 * お店の永続化操作を抽象化するリポジトリ。
 */
import com.mapshoppinglist.domain.model.Place

interface PlacesRepository {
    suspend fun getTotalCount(): Int
    suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean
    suspend fun loadActivePlaces(): List<Place>
    suspend fun findById(placeId: Long): Place?
    suspend fun loadAll(): List<Place>
    suspend fun addPlace(name: String, latE6: Int, lngE6: Int, note: String? = null): Long
    suspend fun deletePlace(placeId: Long)
    suspend fun updateName(placeId: Long, newName: String)
    suspend fun linkItemToPlace(placeId: Long, itemId: Long)
    suspend fun unlinkItemFromPlace(placeId: Long, itemId: Long)
    suspend fun loadRecentPlaces(limit: Int): List<Place>
}

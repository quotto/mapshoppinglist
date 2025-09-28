package com.mapshoppinglist.domain.repository

/**
 * お店の永続化操作を抽象化するリポジトリ。
 */
import com.mapshoppinglist.domain.model.Place

interface PlacesRepository {
    suspend fun getTotalCount(): Int
    suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean
    suspend fun loadActivePlaces(): List<Place>
}

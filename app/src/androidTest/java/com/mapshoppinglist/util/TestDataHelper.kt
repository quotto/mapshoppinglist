package com.mapshoppinglist.util

import androidx.test.core.app.ApplicationProvider
import com.mapshoppinglist.MapShoppingListApplication
import com.mapshoppinglist.domain.usecase.CreatePlaceUseCase
import com.mapshoppinglist.data.local.dao.ItemWithPlaces
import com.mapshoppinglist.data.local.entity.PlaceEntity
import com.mapshoppinglist.data.local.entity.ItemEntity
import kotlinx.coroutines.runBlocking

/**
 * UIテスト用のデータ投入やクリーンアップをまとめるヘルパー。
 */
object TestDataHelper {
    private fun app(): MapShoppingListApplication {
        return ApplicationProvider.getApplicationContext()
    }

    fun clearDatabase() {
        app().database.clearAllTables()
    }

    fun insertItem(title: String, note: String? = null): Long {
        return runBlocking {
            app().addShoppingItemUseCase(title, note)
        }
    }

    fun createPlace(
        name: String,
        latitude: Double,
        longitude: Double,
        note: String? = null
    ): Long {
        return runBlocking {
            app().createPlaceUseCase(
                CreatePlaceUseCase.Params(
                    name = name,
                    latitude = latitude,
                    longitude = longitude,
                    note = note
                )
            )
        }
    }

    fun linkItemToPlace(itemId: Long, placeId: Long) {
        runBlocking {
            app().linkItemToPlaceUseCase(itemId, placeId)
        }
    }

    fun getItemWithPlaces(itemId: Long): ItemWithPlaces? {
        return runBlocking {
            app().database.itemsDao().getItemWithPlaces(itemId)
        }
    }

    fun getPlace(placeId: Long): PlaceEntity? {
        return runBlocking {
            app().database.placesDao().findById(placeId)
        }
    }

    fun getLinkedPlaceIds(itemId: Long): List<Long> {
        return runBlocking {
            app().database.itemPlaceDao().findLinksByItem(itemId).map { it.placeId }
        }
    }

    fun findItemByTitle(title: String): ItemEntity? {
        return runBlocking {
            app().database.itemsDao().loadNotPurchased().firstOrNull { it.title == title }
        }
    }
}

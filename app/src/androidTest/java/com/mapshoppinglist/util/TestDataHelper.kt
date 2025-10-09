package com.mapshoppinglist.util

import androidx.test.core.app.ApplicationProvider
import com.mapshoppinglist.MapShoppingListApplication
import com.mapshoppinglist.domain.usecase.CreatePlaceUseCase
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

}

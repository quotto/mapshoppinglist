package com.mapshoppinglist.data.repository

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mapshoppinglist.data.local.AppDatabase
import com.mapshoppinglist.domain.repository.PlacesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import org.junit.Assert.fail
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPlacesRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: PlacesRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultPlacesRepository(
            placesDao = database.placesDao(),
            itemPlaceDao = database.itemPlaceDao(),
            itemsDao = database.itemsDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun updateNameTrimsAndUpdatesTimestamp() = runTest {
        val placeId = repository.addPlace(name = "テスト店", latE6 = 100, lngE6 = 200, note = null)
        val beforeEntity = database.placesDao().findById(placeId)!!
        // 時刻差が検出できるように少し待機
        advanceTimeBy(TimeUnit.MILLISECONDS.toMillis(10))

        repository.updateName(placeId, "  新しい名称  ")

        val afterEntity = database.placesDao().findById(placeId)!!
        assertEquals("新しい名称", afterEntity.name)
        assertTrue((afterEntity.lastUsedAt ?: 0L) >= (beforeEntity.lastUsedAt ?: 0L))
    }

    @Test
    fun updateNameRejectsBlank() = runTest {
        val placeId = repository.addPlace(name = "テスト店", latE6 = 100, lngE6 = 200, note = null)
        try {
            repository.updateName(placeId, "   ")
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // 期待どおり
        }
    }
}

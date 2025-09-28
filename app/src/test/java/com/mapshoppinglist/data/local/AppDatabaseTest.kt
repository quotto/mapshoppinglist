package com.mapshoppinglist.data.local

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapshoppinglist.data.local.dao.ItemPlaceDao
import com.mapshoppinglist.data.local.dao.ItemsDao
import com.mapshoppinglist.data.local.dao.NotifyStateDao
import com.mapshoppinglist.data.local.dao.PlacesDao
import com.mapshoppinglist.data.local.entity.ItemEntity
import com.mapshoppinglist.data.local.entity.ItemPlaceCrossRef
import com.mapshoppinglist.data.local.entity.NotifyStateEntity
import com.mapshoppinglist.data.local.entity.PlaceEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@Ignore("Room統合テストはInstrumentedテストへ移行予定")
class AppDatabaseTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var itemsDao: ItemsDao
    private lateinit var placesDao: PlacesDao
    private lateinit var itemPlaceDao: ItemPlaceDao
    private lateinit var notifyStateDao: NotifyStateDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.buildInMemory(context)
        itemsDao = database.itemsDao()
        placesDao = database.placesDao()
        itemPlaceDao = database.itemPlaceDao()
        notifyStateDao = database.notifyStateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertItemAndFetchById() = runTest {
        val now = System.currentTimeMillis()
        val newItem = ItemEntity(
            title = "試験アイテム",
            note = "テスト用",
            isPurchased = false,
            createdAt = now,
            updatedAt = now
        )
        val itemId = itemsDao.insert(newItem)
        val stored = itemsDao.findById(itemId)

        assertNotNull("アイテムが保存されていること", stored)
        assertEquals("タイトルが一致すること", "試験アイテム", stored?.title)
        assertEquals("メモが一致すること", "テスト用", stored?.note)
    }

    @Test
    fun deleteItemRemovesCrossRef() = runTest {
        val now = System.currentTimeMillis()
        val itemId = itemsDao.insert(
            ItemEntity(
                title = "牛乳",
                note = null,
                isPurchased = false,
                createdAt = now,
                updatedAt = now
            )
        )
        val placeId = placesDao.insert(
            PlaceEntity(
                name = "スーパーA",
                latitudeE6 = 35678901,
                longitudeE6 = 139123456,
                note = null,
                lastUsedAt = now,
                isActive = true
            )
        )

        itemPlaceDao.insertLink(ItemPlaceCrossRef(itemId = itemId, placeId = placeId))
        var links = itemPlaceDao.findLinksByItem(itemId)
        assertEquals("紐づきが1件存在すること", 1, links.size)

        val storedItem = itemsDao.findById(itemId)!!.copy(id = itemId)
        itemsDao.delete(storedItem)

        links = itemPlaceDao.findLinksByItem(itemId)
        assertEquals("アイテム削除後に紐づきが削除されること", 0, links.size)
    }

    @Test
    fun loadPlaceWithItemsReturnsJoinedData() = runTest {
        val now = System.currentTimeMillis()
        val itemId = itemsDao.insert(
            ItemEntity(
                title = "パン",
                note = null,
                isPurchased = false,
                createdAt = now,
                updatedAt = now
            )
        )
        val placeId = placesDao.insert(
            PlaceEntity(
                name = "ベーカリー",
                latitudeE6 = 35678000,
                longitudeE6 = 139120000,
                note = "駅前",
                lastUsedAt = now,
                isActive = true
            )
        )
        itemPlaceDao.insertLink(ItemPlaceCrossRef(itemId = itemId, placeId = placeId))

        val result = itemPlaceDao.loadPlaceWithItems(placeId)
        assertNotNull("お店が取得できること", result)
        assertEquals("1件のアイテムが紐づくこと", 1, result?.items?.size)
        assertEquals("紐づくアイテムのタイトルが一致すること", "パン", result?.items?.first()?.title)
    }

    @Test
    fun upsertNotifyStateUpdatesValues() = runTest {
        val now = System.currentTimeMillis()
        val placeId = placesDao.insert(
            PlaceEntity(
                name = "コンビニB",
                latitudeE6 = 35678902,
                longitudeE6 = 139123400,
                note = null,
                lastUsedAt = null,
                isActive = false
            )
        )

        notifyStateDao.upsert(
            NotifyStateEntity(
                placeId = placeId,
                lastNotifiedAt = now,
                snoozeUntil = null
            )
        )

        var state = notifyStateDao.findByPlaceId(placeId)
        assertNotNull("通知状態が保存されていること", state)
        assertEquals("初回保存時の時刻が一致すること", now, state?.lastNotifiedAt)
        assertNull("初回はスヌーズされていないこと", state?.snoozeUntil)

        val later = now + 120_000L
        notifyStateDao.upsert(
            NotifyStateEntity(
                placeId = placeId,
                lastNotifiedAt = now,
                snoozeUntil = later
            )
        )

        state = notifyStateDao.findByPlaceId(placeId)
        assertEquals("スヌーズ時刻が更新されること", later, state?.snoozeUntil)
    }
}

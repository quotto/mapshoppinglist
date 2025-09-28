package com.mapshoppinglist.data.repository

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapshoppinglist.data.local.AppDatabase
import com.mapshoppinglist.geofence.GeofenceSyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import com.mapshoppinglist.domain.exception.DuplicateItemException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@Ignore("Room統合テストはInstrumentedテストへ移行予定")
class DefaultShoppingListRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: DefaultShoppingListRepository
    private lateinit var geofenceSyncScheduler: GeofenceSyncScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.buildInMemory(context)
        geofenceSyncScheduler = GeofenceSyncScheduler(context)
        repository = DefaultShoppingListRepository(
            itemsDao = database.itemsDao(),
            geofenceSyncScheduler = geofenceSyncScheduler
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addItemAndObserveReflectsNewEntry() = runBlocking {
        val initial = repository.observeAllItems().first()
        assertTrue(initial.isEmpty())

        repository.addItem(title = "牛乳", note = "2本")
        val updated = repository.observeAllItems()
            .first { items -> items.any { it.title == "牛乳" } }

        assertEquals(1, updated.size)
        assertEquals("牛乳", updated.first().title)
        assertEquals("2本", updated.first().note)
        assertTrue(!updated.first().isPurchased)
    }

    @Test
    fun togglePurchasedUpdatesState() = runBlocking {
        repository.addItem(title = "卵", note = null)
        val firstItem = repository.observeAllItems()
            .first { it.isNotEmpty() }
            .first()

        repository.updatePurchasedState(firstItem.id, true)
        val toggled = repository.observeAllItems()
            .first { items -> items.firstOrNull { it.id == firstItem.id }?.isPurchased == true }
            .first()

        assertTrue(toggled.isPurchased)
    }

    @Test
    fun deleteItemRemovesFromFlow() = runBlocking {
        repository.addItem(title = "パン", note = null)
        val initial = repository.observeAllItems().first { it.isNotEmpty() }
        val target = initial.first()

        repository.deleteItem(target.id)
        val afterDelete = repository.observeAllItems()
            .first { items -> items.none { it.id == target.id } }

        assertTrue(afterDelete.isEmpty())
    }

    @Test(expected = DuplicateItemException::class)
    fun addItemThrowsWhenDuplicateTitle() = runBlocking {
        repository.addItem(title = "牛乳", note = null)
        repository.addItem(title = "牛乳", note = null)
    }
}

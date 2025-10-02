package com.mapshoppinglist.ui.itemdetail

import android.content.Context
import android.content.ContextWrapper
import com.mapshoppinglist.domain.model.ItemDetail
import com.mapshoppinglist.domain.model.PlaceSummary
import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.domain.repository.ShoppingListRepository
import com.mapshoppinglist.domain.usecase.LinkItemToPlaceUseCase
import com.mapshoppinglist.domain.usecase.ObserveItemDetailUseCase
import com.mapshoppinglist.domain.usecase.UnlinkItemFromPlaceUseCase
import com.mapshoppinglist.domain.usecase.UpdateItemUseCase
import com.mapshoppinglist.domain.usecase.UpdatePurchasedStateUseCase
import com.mapshoppinglist.geofence.GeofenceSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeShoppingListRepository : ShoppingListRepository {
    val detailFlow = MutableStateFlow<ItemDetail?>(null)
    var updatedItemId: Long? = null
    var updatedState: Boolean? = null
    var updatedTitle: String? = null
    var updatedNote: String? = null

    override fun observeAllItems(): Flow<List<com.mapshoppinglist.domain.model.ShoppingItem>> = emptyFlow()

    override suspend fun addItem(title: String, note: String?): Long = 0L

    override suspend fun updatePurchasedState(itemId: Long, isPurchased: Boolean) {
        updatedItemId = itemId
        updatedState = isPurchased
    }

    override suspend fun deleteItem(itemId: Long) {}

    override suspend fun getItemsForPlace(placeId: Long): List<com.mapshoppinglist.domain.model.ShoppingItem> = emptyList()

    override suspend fun markPlaceItemsPurchased(placeId: Long) {}

    override fun observeItemDetail(itemId: Long): Flow<ItemDetail?> = detailFlow

    override suspend fun updateItem(itemId: Long, title: String, note: String?) {
        updatedItemId = itemId
        updatedTitle = title
        updatedNote = note
        val current = detailFlow.value
        if (current != null) {
            detailFlow.value = current.copy(title = title, note = note)
        }
    }
}

private class FakePlacesRepository : PlacesRepository {
    val linkCalls = mutableListOf<Pair<Long, Long>>()
    val unlinkCalls = mutableListOf<Pair<Long, Long>>()

    override suspend fun getTotalCount(): Int = 0

    override suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean = false

    override suspend fun loadActivePlaces(): List<com.mapshoppinglist.domain.model.Place> = emptyList()

    override suspend fun findById(placeId: Long): com.mapshoppinglist.domain.model.Place? = null

    override suspend fun addPlace(name: String, latE6: Int, lngE6: Int, note: String?): Long = 0L

    override suspend fun deletePlace(placeId: Long) {}

    override suspend fun linkItemToPlace(placeId: Long, itemId: Long) {
        linkCalls += placeId to itemId
    }

    override suspend fun unlinkItemFromPlace(placeId: Long, itemId: Long) {
        unlinkCalls += placeId to itemId
    }

    override suspend fun loadRecentPlaces(limit: Int): List<com.mapshoppinglist.domain.model.Place> = emptyList()
}

private class FakeGeofenceSyncScheduler(context: Context) : GeofenceSyncScheduler(context) {
    var called = false
    override fun scheduleImmediateSync() {
        called = true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ItemDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var shoppingListRepository: FakeShoppingListRepository
    private lateinit var placesRepository: FakePlacesRepository
    private lateinit var geofenceScheduler: FakeGeofenceSyncScheduler

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        shoppingListRepository = FakeShoppingListRepository()
        placesRepository = FakePlacesRepository()
        geofenceScheduler = FakeGeofenceSyncScheduler(StubContext())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(itemId: Long = 1L): ItemDetailViewModel {
        val observeUseCase = ObserveItemDetailUseCase(shoppingListRepository)
        val updateUseCase = UpdatePurchasedStateUseCase(shoppingListRepository)
        val linkUseCase = LinkItemToPlaceUseCase(placesRepository, geofenceScheduler)
        val unlinkUseCase = UnlinkItemFromPlaceUseCase(placesRepository, geofenceScheduler)
        val updateItemUseCase = UpdateItemUseCase(shoppingListRepository)
        return ItemDetailViewModel(
            itemId = itemId,
            observeItemDetailUseCase = observeUseCase,
            updatePurchasedStateUseCase = updateUseCase,
            linkItemToPlaceUseCase = linkUseCase,
            unlinkItemFromPlaceUseCase = unlinkUseCase,
            updateItemUseCase = updateItemUseCase
        )
    }

    @Test
    fun `uiState reflects observed detail`() = runTest(dispatcher) {
        val detail = ItemDetail(
            id = 1L,
            title = "牛乳",
            note = "2本",
            isPurchased = false,
            createdAt = 0L,
            updatedAt = 0L,
            places = listOf(PlaceSummary(10L, "スーパー", "住所", 0.0, 0.0))
        )
        shoppingListRepository.detailFlow.value = detail

        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("牛乳", state.title)
        assertEquals(1, state.linkedPlaces.size)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `toggle purchased updates repository`() = runTest(dispatcher) {
        shoppingListRepository.detailFlow.value = null
        val viewModel = createViewModel()

        advanceUntilIdle()

        viewModel.onTogglePurchased(true)
        advanceUntilIdle()

        assertEquals(1L, shoppingListRepository.updatedItemId)
        assertEquals(true, shoppingListRepository.updatedState)
    }

    @Test
    fun `link place triggers repository and event`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        advanceUntilIdle()

        val job = backgroundScope.launch {
            val event = viewModel.events.first()
            assertTrue(event is ItemDetailEvent.PlaceLinked)
        }

        viewModel.onPlaceLinked(42L)
        advanceUntilIdle()

        assertEquals(listOf(42L to 1L), placesRepository.linkCalls)
        assertTrue(geofenceScheduler.called)
        job.cancel()
    }

    @Test
    fun `remove place triggers repository unlink`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        advanceUntilIdle()

        viewModel.onRemovePlace(12L)
        advanceUntilIdle()

        assertEquals(listOf(12L to 1L), placesRepository.unlinkCalls)
    }

    @Test
    fun `edit item updates repository and emits event`() = runTest(dispatcher) {
        val detail = ItemDetail(
            id = 1L,
            title = "牛乳",
            note = "メモ",
            isPurchased = false,
            createdAt = 0L,
            updatedAt = 0L,
            places = emptyList()
        )
        shoppingListRepository.detailFlow.value = detail

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEditTitleChange("牛乳（特売）")
        viewModel.onEditNoteChange("メモ更新")

        val eventJob = backgroundScope.launch {
            val event = viewModel.events.first()
            assertTrue(event is ItemDetailEvent.ItemUpdated)
        }

        val result = viewModel.saveIfNeeded()
        advanceUntilIdle()

        assertEquals(1L, shoppingListRepository.updatedItemId)
        assertEquals("牛乳（特売）", shoppingListRepository.updatedTitle)
        assertEquals("メモ更新", shoppingListRepository.updatedNote)
        eventJob.cancel()
        assertTrue(result is ItemDetailSaveResult.Success)
    }

    @Test
    fun `blank title keeps existing title without blocking navigation`() = runTest(dispatcher) {
        val detail = ItemDetail(
            id = 1L,
            title = "牛乳",
            note = "",
            isPurchased = false,
            createdAt = 0L,
            updatedAt = 0L,
            places = emptyList()
        )
        shoppingListRepository.detailFlow.value = detail

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEditTitleChange("   ")

        val result = viewModel.saveIfNeeded()
        advanceUntilIdle()

        assertTrue(result is ItemDetailSaveResult.Success)
        assertEquals(null, shoppingListRepository.updatedTitle)
        assertEquals("牛乳", viewModel.uiState.value.titleInput)
        assertEquals(false, viewModel.uiState.value.showTitleValidationError)
    }

    @Test
    fun `no changes skips repository update`() = runTest(dispatcher) {
        val detail = ItemDetail(
            id = 1L,
            title = "牛乳",
            note = "メモ",
            isPurchased = false,
            createdAt = 0L,
            updatedAt = 0L,
            places = emptyList()
        )
        shoppingListRepository.detailFlow.value = detail

        val viewModel = createViewModel()
        advanceUntilIdle()

        val result = viewModel.saveIfNeeded()
        advanceUntilIdle()

        assertTrue(result is ItemDetailSaveResult.Success)
        assertEquals(null, shoppingListRepository.updatedTitle)
    }

    @Test
    fun `blank title still allows note update`() = runTest(dispatcher) {
        val detail = ItemDetail(
            id = 1L,
            title = "牛乳",
            note = "メモ",
            isPurchased = false,
            createdAt = 0L,
            updatedAt = 0L,
            places = emptyList()
        )
        shoppingListRepository.detailFlow.value = detail

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEditTitleChange("   ")
        viewModel.onEditNoteChange("メモ更新")

        val job = backgroundScope.launch {
            val event = viewModel.events.first()
            assertTrue(event is ItemDetailEvent.ItemUpdated)
        }

        val result = viewModel.saveIfNeeded()
        advanceUntilIdle()

        assertTrue(result is ItemDetailSaveResult.Success)
        assertEquals("牛乳", shoppingListRepository.updatedTitle)
        assertEquals("メモ更新", shoppingListRepository.updatedNote)
        assertEquals("牛乳", viewModel.uiState.value.titleInput)
        job.cancel()
    }
}

private class StubContext : ContextWrapper(null) {}

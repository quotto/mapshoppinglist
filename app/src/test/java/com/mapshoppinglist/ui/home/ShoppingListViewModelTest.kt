package com.mapshoppinglist.ui.home

import android.content.ContextWrapper
import com.mapshoppinglist.domain.model.ItemDetail
import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.model.ShoppingItem
import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.domain.repository.ShoppingListRepository
import com.mapshoppinglist.domain.usecase.AddShoppingItemUseCase
import com.mapshoppinglist.domain.usecase.DeleteShoppingItemUseCase
import com.mapshoppinglist.domain.usecase.GetRecentPlacesUseCase
import com.mapshoppinglist.domain.usecase.LinkItemToPlaceUseCase
import com.mapshoppinglist.domain.usecase.ObserveShoppingItemsUseCase
import com.mapshoppinglist.domain.usecase.UpdatePurchasedStateUseCase
import com.mapshoppinglist.geofence.GeofenceSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 最近のお店選択ロジックのユニットテスト。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var shoppingRepository: FakeShoppingListRepository
    private lateinit var placesRepository: FakePlacesRepository
    private lateinit var geofenceScheduler: TestGeofenceSyncScheduler

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        shoppingRepository = FakeShoppingListRepository()
        placesRepository = FakePlacesRepository()
        geofenceScheduler = TestGeofenceSyncScheduler()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onAddFabClick loads recent places`() = runTest(dispatcher) {
        placesRepository.recent = listOf(
            Place(id = 10L, name = "スーパーA", latitudeE6 = 0, longitudeE6 = 0, isActive = true),
            Place(id = 20L, name = "ドラッグストアB", latitudeE6 = 0, longitudeE6 = 0, isActive = true)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAddFabClick()
        advanceUntilIdle()

        val names = viewModel.uiState.value.recentPlaces.map { it.name }
        assertEquals(listOf("スーパーA", "ドラッグストアB"), names)
    }

    @Test
    fun `selecting recent place moves it to pending list`() = runTest(dispatcher) {
        val place = Place(id = 100L, name = "八百屋", latitudeE6 = 0, longitudeE6 = 0, isActive = true)
        placesRepository.recent = listOf(place)
        placesRepository.placeMap[place.id] = place

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAddFabClick()
        advanceUntilIdle()
        viewModel.onRecentPlaceSelected(place.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(place.id), state.pendingPlaces.map { it.placeId })
        assertEquals(emptyList<Long>(), state.recentPlaces.map { it.placeId })
    }

    @Test
    fun `removing pending place restores it to recent list`() = runTest(dispatcher) {
        val place = Place(id = 200L, name = "コンビニ", latitudeE6 = 0, longitudeE6 = 0, isActive = true)
        placesRepository.recent = listOf(place)
        placesRepository.placeMap[place.id] = place

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAddFabClick()
        advanceUntilIdle()
        viewModel.onRecentPlaceSelected(place.id)
        advanceUntilIdle()
        viewModel.onRemovePendingPlace(place.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(emptyList<Long>(), state.pendingPlaces.map { it.placeId })
        assertEquals(listOf(place.id), state.recentPlaces.map { it.placeId })
    }

    private fun createViewModel(): ShoppingListViewModel {
        val observe = ObserveShoppingItemsUseCase(shoppingRepository)
        val add = AddShoppingItemUseCase(shoppingRepository)
        val delete = DeleteShoppingItemUseCase(shoppingRepository, geofenceScheduler)
        val updatePurchased = UpdatePurchasedStateUseCase(shoppingRepository)
        val link = LinkItemToPlaceUseCase(placesRepository, geofenceScheduler)
        val recent = GetRecentPlacesUseCase(placesRepository)
        return ShoppingListViewModel(
            observeShoppingItems = observe,
            addShoppingItem = add,
            deleteShoppingItem = delete,
            updatePurchasedState = updatePurchased,
            linkItemToPlaceUseCase = link,
            placesRepository = placesRepository,
            getRecentPlacesUseCase = recent
        )
    }
}

private class FakeShoppingListRepository : ShoppingListRepository {
    private val itemsFlow = MutableStateFlow<List<ShoppingItem>>(emptyList())

    override fun observeAllItems(): Flow<List<ShoppingItem>> = itemsFlow

    override suspend fun addItem(title: String, note: String?): Long = 1L

    override suspend fun updatePurchasedState(itemId: Long, isPurchased: Boolean) {}

    override suspend fun deleteItem(itemId: Long) {}

    override suspend fun getItemsForPlace(placeId: Long): List<ShoppingItem> = emptyList()

    override suspend fun markPlaceItemsPurchased(placeId: Long) {}

    override fun observeItemDetail(itemId: Long): Flow<ItemDetail?> = emptyFlow()

    override suspend fun updateItem(itemId: Long, title: String, note: String?) {}
}

private class FakePlacesRepository : PlacesRepository {
    var recent: List<Place> = emptyList()
    val placeMap: MutableMap<Long, Place> = mutableMapOf()

    override suspend fun getTotalCount(): Int = placeMap.size

    override suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean = false

    override suspend fun loadActivePlaces(): List<Place> = placeMap.values.toList()

    override suspend fun findById(placeId: Long): Place? = placeMap[placeId]

    override suspend fun loadAll(): List<Place> = placeMap.values.toList()

    override suspend fun addPlace(name: String, latE6: Int, lngE6: Int, note: String?): Long = 0L

    override suspend fun deletePlace(placeId: Long) {}

    override suspend fun updateName(placeId: Long, newName: String) {}

    override suspend fun linkItemToPlace(placeId: Long, itemId: Long) {}

    override suspend fun unlinkItemFromPlace(placeId: Long, itemId: Long) {}

    override suspend fun loadRecentPlaces(limit: Int): List<Place> = recent.take(limit)
}

private class TestGeofenceSyncScheduler : GeofenceSyncScheduler(ContextWrapper(null)) {
    override fun scheduleImmediateSync() {}
}

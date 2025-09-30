package com.mapshoppinglist.ui.recentplaces

import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.domain.usecase.GetRecentPlacesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

private class FakePlacesRepository : PlacesRepository {
    var recentPlaces: List<Place> = emptyList()
    var shouldThrow: Boolean = false

    override suspend fun loadRecentPlaces(limit: Int): List<Place> {
        if (shouldThrow) throw IllegalStateException("error")
        return recentPlaces.take(limit)
    }

    override suspend fun getTotalCount(): Int = 0

    override suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean = false

    override suspend fun loadActivePlaces(): List<Place> = emptyList()

    override suspend fun findById(placeId: Long): Place? = null

    override suspend fun addPlace(name: String, latE6: Int, lngE6: Int, note: String?): Long = 0L

    override suspend fun deletePlace(placeId: Long) {}

    override suspend fun linkItemToPlace(placeId: Long, itemId: Long) {}

    override suspend fun unlinkItemFromPlace(placeId: Long, itemId: Long) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecentPlacesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakePlacesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakePlacesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RecentPlacesViewModel {
        val useCase = GetRecentPlacesUseCase(repository)
        return RecentPlacesViewModel(useCase)
    }

    @Test
    fun `loads recent places`() = runTest(dispatcher) {
        repository.recentPlaces = listOf(
            Place(1L, "スーパー", 0, 0, true),
            Place(2L, "ドラッグストア", 0, 0, false)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.places.size)
        assertEquals("スーパー", state.places.first().name)
    }

    @Test
    fun `sets error message when loading fails`() = runTest(dispatcher) {
        repository.shouldThrow = true

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
    }
}

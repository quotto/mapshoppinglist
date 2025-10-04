package com.mapshoppinglist.domain.usecase

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.geofence.GeofenceSyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class RecordingPlacesRepository : PlacesRepository {
    var totalCount: Int = 0
    var duplicateLocations: Set<Pair<Int, Int>> = emptySet()
    var addedPlaces = mutableListOf<AddedPlace>()
    val deletedPlaceIds = mutableListOf<Long>()
    val linkCalls = mutableListOf<Pair<Long, Long>>()
    val unlinkCalls = mutableListOf<Pair<Long, Long>>()
    val updateNameCalls = mutableListOf<Pair<Long, String>>()

    data class AddedPlace(val name: String, val latE6: Int, val lngE6: Int, val note: String?)

    override suspend fun getTotalCount(): Int = totalCount

    override suspend fun existsLocation(latE6: Int, lngE6: Int): Boolean =
        duplicateLocations.contains(latE6 to lngE6)

    override suspend fun loadActivePlaces(): List<Place> = emptyList()

    override suspend fun findById(placeId: Long): Place? = null

    override suspend fun loadAll(): List<Place> = emptyList()

    override suspend fun addPlace(name: String, latE6: Int, lngE6: Int, note: String?): Long {
        totalCount += 1
        addedPlaces += AddedPlace(name, latE6, lngE6, note)
        return totalCount.toLong()
    }

    override suspend fun deletePlace(placeId: Long) {
        deletedPlaceIds += placeId
        if (totalCount > 0) totalCount -= 1
    }

    override suspend fun linkItemToPlace(placeId: Long, itemId: Long) {
        linkCalls += placeId to itemId
    }

    override suspend fun unlinkItemFromPlace(placeId: Long, itemId: Long) {
        unlinkCalls += placeId to itemId
    }

    override suspend fun loadRecentPlaces(limit: Int): List<Place> = emptyList()

    override suspend fun updateName(placeId: Long, newName: String) {
        updateNameCalls += placeId to newName
    }
}

private class RecordingScheduler(context: Context) : GeofenceSyncScheduler(context) {
    var count = 0
    override fun scheduleImmediateSync() {
        count += 1
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class PlaceUseCasesTest {

    private lateinit var context: Context
    private lateinit var repository: RecordingPlacesRepository
    private lateinit var scheduler: RecordingScheduler
    private lateinit var validator: ValidatePlaceRegistrationUseCase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = RecordingPlacesRepository()
        scheduler = RecordingScheduler(context)
        validator = ValidatePlaceRegistrationUseCase(repository)
    }

    @Test
    fun createPlaceConvertsCoordinatesAndSchedulesSync() = runTest {
        val useCase = CreatePlaceUseCase(validator, repository, scheduler)
        val params = CreatePlaceUseCase.Params(
            name = "スーパー",
            latitude = 35.123456,
            longitude = 139.654321,
            note = "24H"
        )
        val id = useCase(params)
        assertEquals(1L, id)
        assertEquals(1, scheduler.count)
        val added = repository.addedPlaces.single()
        assertEquals("スーパー", added.name)
        assertEquals(35_123_456, added.latE6)
        assertEquals(139_654_321, added.lngE6)
        assertEquals("24H", added.note)
    }

    @Test
    fun deletePlaceTriggersSync() = runTest {
        val useCase = DeletePlaceUseCase(repository, scheduler)
        useCase(10L)
        assertEquals(listOf(10L), repository.deletedPlaceIds)
        assertEquals(1, scheduler.count)
    }

    @Test
    fun linkAndUnlinkPlaceTriggerSync() = runTest {
        val linkUseCase = LinkItemToPlaceUseCase(repository, scheduler)
        val unlinkUseCase = UnlinkItemFromPlaceUseCase(repository, scheduler)

        linkUseCase(itemId = 3L, placeId = 7L)
        unlinkUseCase(itemId = 3L, placeId = 7L)

        assertEquals(listOf(7L to 3L), repository.linkCalls)
        assertEquals(listOf(7L to 3L), repository.unlinkCalls)
        assertEquals(2, scheduler.count)
    }

    @Test
    fun updatePlaceNameSchedulesWhenRequested() = runTest {
        val useCase = UpdatePlaceNameUseCase(repository, scheduler)

        useCase(placeId = 5L, newName = " 新しいスーパー ", scheduleOnRename = true)
        assertEquals(listOf(5L to " 新しいスーパー "), repository.updateNameCalls)
        assertEquals(1, scheduler.count)

        useCase(placeId = 6L, newName = "別店舗", scheduleOnRename = false)
        assertEquals(listOf(5L to " 新しいスーパー ", 6L to "別店舗"), repository.updateNameCalls)
        assertEquals(1, scheduler.count)
    }
}

package com.mapshoppinglist.data.repository

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPhotoResponse
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.libraries.places.api.net.IsOpenRequest
import com.google.android.libraries.places.api.net.IsOpenResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.libraries.places.api.net.SearchByTextResponse
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.libraries.places.api.net.SearchNearbyResponse
import com.mapshoppinglist.monitoring.ExternalApiErrorReporter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GooglePlacesNearbyStoreSuggestionRepositoryTest {

    @Test
    fun `search applies location bias and filters unsupported types`() = runTest {
        val fakeClient = FakeNearbyPlacesClient()
        val supermarket = Place.builder()
            .setId("store-1")
            .setName("近所スーパー")
            .setAddress("東京都")
            .setLatLng(LatLng(35.0005, 139.0005))
            .setPrimaryType("supermarket")
            .build()
        val restaurant = Place.builder()
            .setId("store-2")
            .setName("近所レストラン")
            .setAddress("東京都")
            .setLatLng(LatLng(35.0004, 139.0004))
            .setPrimaryType("restaurant")
            .build()
        fakeClient.searchNearbyResponse = SearchNearbyResponse.newInstance(listOf(restaurant, supermarket))

        val repository = GooglePlacesNearbyStoreSuggestionRepository(fakeClient)
        val results = repository.search(
            itemTitle = "牛乳",
            latitude = 35.0,
            longitude = 139.0,
            limit = 5,
            typeQueries = listOf("supermarket")
        )

        assertEquals(1, results.size)
        assertEquals("store-1", results.first().placeId)
        val bias = fakeClient.lastSearchNearbyRequest?.locationRestriction as? CircularBounds
        assertEquals(LatLng(35.0, 139.0), bias?.center)
        assertTrue(results.first().distanceMeters >= 0)
    }

    @Test
    fun `search uses nearby type queries before text queries and deduplicates places`() = runTest {
        val fakeClient = FakeNearbyPlacesClient()
        fakeClient.searchResponsesByType["supermarket"] = SearchNearbyResponse.newInstance(
            listOf(
                Place.builder()
                    .setId("store-1")
                    .setName("近所スーパー")
                    .setAddress("東京都")
                    .setLatLng(LatLng(35.0005, 139.0005))
                    .setPrimaryType("supermarket")
                    .build()
            )
        )
        fakeClient.searchResponsesByType["drugstore"] = SearchNearbyResponse.newInstance(
            listOf(
                Place.builder()
                    .setId("store-1")
                    .setName("近所スーパー")
                    .setAddress("東京都")
                    .setLatLng(LatLng(35.0005, 139.0005))
                    .setPrimaryType("supermarket")
                    .build(),
                Place.builder()
                    .setId("store-2")
                    .setName("駅前ドラッグ")
                    .setAddress("東京都")
                    .setLatLng(LatLng(35.0008, 139.0008))
                    .setPrimaryType("drugstore")
                    .build()
            )
        )
        fakeClient.searchResponsesByQuery["牛乳"] = SearchByTextResponse.newInstance(
            listOf(
                Place.builder()
                    .setId("store-3")
                    .setName("牛乳専門店")
                    .setAddress("東京都")
                    .setLatLng(LatLng(35.0010, 139.0010))
                    .setPrimaryType("store")
                    .build()
            )
        )

        val repository = GooglePlacesNearbyStoreSuggestionRepository(fakeClient)
        val results = repository.search(
            itemTitle = "牛乳",
            latitude = 35.0,
            longitude = 139.0,
            limit = 5,
            typeQueries = listOf("supermarket", "drugstore"),
            textQueries = listOf("牛乳")
        )

        assertEquals(listOf("supermarket", "drugstore"), fakeClient.requestedTypes)
        assertEquals(listOf("牛乳"), fakeClient.requestedQueries)
        assertEquals(3, results.size)
        assertEquals(listOf("store-1", "store-2", "store-3"), results.map { it.placeId })
    }

    @Test
    fun `search records execution error when places api call fails`() = runTest {
        val fakeClient = FakeNearbyPlacesClient()
        val reporter = FakePlacesExternalApiErrorReporter()
        fakeClient.searchNearbyError = IllegalStateException("service unavailable")

        val repository = GooglePlacesNearbyStoreSuggestionRepository(
            placesClient = fakeClient,
            errorReporter = reporter
        )

        val results = repository.search(
            itemTitle = "牛乳",
            latitude = 35.0,
            longitude = 139.0,
            limit = 5,
            typeQueries = listOf("supermarket")
        )

        assertTrue(results.isEmpty())
        assertEquals(1, reporter.executionErrors.size)
        assertEquals("google_places", reporter.executionErrors.single().apiName)
        assertEquals("search_nearby", reporter.executionErrors.single().operation)
    }
}

private class FakeNearbyPlacesClient : PlacesClient {
    var lastSearchByTextRequest: SearchByTextRequest? = null
    var lastSearchNearbyRequest: SearchNearbyRequest? = null
    var searchByTextResponse: SearchByTextResponse = SearchByTextResponse.newInstance(emptyList())
    var searchNearbyResponse: SearchNearbyResponse = SearchNearbyResponse.newInstance(emptyList())
    var searchNearbyError: Exception? = null
    val searchResponsesByQuery = linkedMapOf<String, SearchByTextResponse>()
    val searchResponsesByType = linkedMapOf<String, SearchNearbyResponse>()
    val requestedQueries = mutableListOf<String>()
    val requestedTypes = mutableListOf<String>()

    override fun searchByText(request: SearchByTextRequest): Task<SearchByTextResponse> {
        lastSearchByTextRequest = request
        val query = request.textQuery ?: ""
        requestedQueries += query
        return Tasks.forResult(searchResponsesByQuery[query] ?: searchByTextResponse)
    }

    override fun searchNearby(request: SearchNearbyRequest): Task<SearchNearbyResponse> {
        lastSearchNearbyRequest = request
        val type = request.includedPrimaryTypes?.firstOrNull().orEmpty()
        requestedTypes += type
        searchNearbyError?.let { return Tasks.forException(it) }
        return Tasks.forResult(searchResponsesByType[type] ?: searchNearbyResponse)
    }

    override fun fetchPhoto(request: FetchPhotoRequest): Task<FetchPhotoResponse> = unsupported()

    override fun fetchPlace(request: FetchPlaceRequest): Task<FetchPlaceResponse> = unsupported()

    override fun fetchResolvedPhotoUri(request: FetchResolvedPhotoUriRequest): Task<FetchResolvedPhotoUriResponse> = unsupported()

    override fun findAutocompletePredictions(request: FindAutocompletePredictionsRequest): Task<FindAutocompletePredictionsResponse> =
        unsupported()

    override fun findCurrentPlace(request: FindCurrentPlaceRequest): Task<FindCurrentPlaceResponse> = unsupported()

    override fun isOpen(request: IsOpenRequest): Task<IsOpenResponse> = unsupported()

    override fun zzb(request: FetchPlaceRequest, i: Int): Task<FetchPlaceResponse> = unsupported()

    override fun zzd(request: FindAutocompletePredictionsRequest, i: Int): Task<FindAutocompletePredictionsResponse> = unsupported()

    private fun <T> unsupported(): Task<T> = Tasks.forException(UnsupportedOperationException("Not implemented in tests"))
}

private class FakePlacesExternalApiErrorReporter : ExternalApiErrorReporter {
    val executionErrors = mutableListOf<RecordedPlacesExecutionError>()

    override fun recordExecutionError(
        apiName: String,
        operation: String,
        throwable: Throwable,
        attributes: Map<String, String>
    ) {
        executionErrors += RecordedPlacesExecutionError(apiName, operation, throwable, attributes)
    }

    override fun recordResponseError(
        apiName: String,
        operation: String,
        statusCode: Int,
        responseBodyPreview: String?,
        attributes: Map<String, String>
    ) = Unit
}

private data class RecordedPlacesExecutionError(
    val apiName: String,
    val operation: String,
    val throwable: Throwable,
    val attributes: Map<String, String>
)

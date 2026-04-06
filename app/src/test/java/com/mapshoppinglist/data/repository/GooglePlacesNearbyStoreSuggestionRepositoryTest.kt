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
        fakeClient.searchByTextResponse = SearchByTextResponse.newInstance(listOf(restaurant, supermarket))

        val repository = GooglePlacesNearbyStoreSuggestionRepository(fakeClient)
        val results = repository.search(
            itemTitle = "牛乳",
            latitude = 35.0,
            longitude = 139.0,
            limit = 5
        )

        assertEquals(1, results.size)
        assertEquals("store-1", results.first().placeId)
        val bias = fakeClient.lastSearchByTextRequest?.locationBias as? CircularBounds
        assertEquals(LatLng(35.0, 139.0), bias?.center)
        assertTrue(results.first().distanceMeters >= 0)
    }

    @Test
    fun `search uses provided search queries and deduplicates places`() = runTest {
        val fakeClient = FakeNearbyPlacesClient()
        fakeClient.searchResponsesByQuery["supermarket"] = SearchByTextResponse.newInstance(
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
        fakeClient.searchResponsesByQuery["drugstore"] = SearchByTextResponse.newInstance(
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

        val repository = GooglePlacesNearbyStoreSuggestionRepository(fakeClient)
        val results = repository.search(
            itemTitle = "牛乳",
            latitude = 35.0,
            longitude = 139.0,
            limit = 5,
            searchQueries = listOf("supermarket", "drugstore")
        )

        assertEquals(listOf("supermarket", "drugstore"), fakeClient.requestedQueries)
        assertEquals(2, results.size)
        assertEquals(listOf("store-1", "store-2"), results.map { it.placeId })
    }
}

private class FakeNearbyPlacesClient : PlacesClient {
    var lastSearchByTextRequest: SearchByTextRequest? = null
    var searchByTextResponse: SearchByTextResponse = SearchByTextResponse.newInstance(emptyList())
    val searchResponsesByQuery = linkedMapOf<String, SearchByTextResponse>()
    val requestedQueries = mutableListOf<String>()

    override fun searchByText(request: SearchByTextRequest): Task<SearchByTextResponse> {
        lastSearchByTextRequest = request
        val query = request.textQuery ?: ""
        requestedQueries += query
        return Tasks.forResult(searchResponsesByQuery[query] ?: searchByTextResponse)
    }

    override fun fetchPhoto(request: FetchPhotoRequest): Task<FetchPhotoResponse> = unsupported()

    override fun fetchPlace(request: FetchPlaceRequest): Task<FetchPlaceResponse> = unsupported()

    override fun fetchResolvedPhotoUri(request: FetchResolvedPhotoUriRequest): Task<FetchResolvedPhotoUriResponse> = unsupported()

    override fun findAutocompletePredictions(request: FindAutocompletePredictionsRequest): Task<FindAutocompletePredictionsResponse> =
        unsupported()

    override fun findCurrentPlace(request: FindCurrentPlaceRequest): Task<FindCurrentPlaceResponse> = unsupported()

    override fun isOpen(request: IsOpenRequest): Task<IsOpenResponse> = unsupported()

    override fun searchNearby(request: SearchNearbyRequest): Task<SearchNearbyResponse> = unsupported()

    override fun zzb(request: FetchPlaceRequest, i: Int): Task<FetchPlaceResponse> = unsupported()

    override fun zzd(request: FindAutocompletePredictionsRequest, i: Int): Task<FindAutocompletePredictionsResponse> = unsupported()

    private fun <T> unsupported(): Task<T> = Tasks.forException(UnsupportedOperationException("Not implemented in tests"))
}

package com.mapshoppinglist.data.repository
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.mapshoppinglist.domain.model.NearbyStoreCandidate
import com.mapshoppinglist.domain.repository.NearbyStoreSuggestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class GooglePlacesNearbyStoreSuggestionRepository(
    private val placesClient: PlacesClient
) : NearbyStoreSuggestionRepository {

    override suspend fun search(
        itemTitle: String,
        latitude: Double,
        longitude: Double,
        limit: Int,
        typeQueries: List<String>,
        textQueries: List<String>
    ): List<NearbyStoreCandidate> = withContext(Dispatchers.IO) {
        val normalizedTypeQueries = typeQueries
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_TYPE_QUERY_COUNT)
        val normalizedTextQueries = textQueries
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(itemTitle.trim()) }
            .distinct()
            .take(MAX_TEXT_QUERY_COUNT)

        Log.i(
            TAG,
            "Starting Places search: itemTitle=$itemTitle typeQueries=${normalizedTypeQueries.joinToString("|")} textQueries=${normalizedTextQueries.joinToString("|")} lat=$latitude lng=$longitude"
        )

        val nearbyCandidates = normalizedTypeQueries.flatMap { placeType ->
            runCatching {
                searchNearbyByType(
                    placeType = placeType,
                    latitude = latitude,
                    longitude = longitude,
                    limit = limit
                )
            }.onFailure { error ->
                Log.w(TAG, "Nearby Search failed for type=$placeType", error)
            }.getOrDefault(emptyList())
        }
        val textCandidates = normalizedTextQueries.flatMap { query ->
            runCatching {
                searchByTextQuery(
                    query = query,
                    latitude = latitude,
                    longitude = longitude,
                    limit = limit
                )
            }.onFailure { error ->
                Log.w(TAG, "Text Search failed for query=$query", error)
            }.getOrDefault(emptyList())
        }

        (nearbyCandidates + textCandidates)
            .filter { candidate -> candidate.primaryType in ALLOWED_PRIMARY_TYPES || candidate.primaryType == null }
            .distinctBy { it.placeId }
            .sortedBy { it.distanceMeters }
            .take(limit.coerceIn(1, 10))
            .also { candidates ->
                Log.i(
                    TAG,
                    "Completed Places search: itemTitle=$itemTitle nearbyCandidateCount=${nearbyCandidates.size} textCandidateCount=${textCandidates.size} finalCandidateCount=${candidates.size}"
                )
            }
    }

    private suspend fun searchNearbyByType(
        placeType: String,
        latitude: Double,
        longitude: Double,
        limit: Int
    ): List<NearbyStoreCandidate> {
        val request = SearchNearbyRequest.builder(
            CircularBounds.newInstance(
                LatLng(latitude, longitude),
                SEARCH_RADIUS_METERS
            ),
            PLACE_FIELDS
        )
            .setIncludedPrimaryTypes(listOf(placeType))
            .setMaxResultCount(limit.coerceIn(1, 10))
            .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
            .build()

        val response = placesClient.searchNearby(request).await()
        return response.places.mapNotNull { place ->
            place.toCandidate(latitude, longitude)
        }
    }

    private suspend fun searchByTextQuery(
        query: String,
        latitude: Double,
        longitude: Double,
        limit: Int
    ): List<NearbyStoreCandidate> {
        val request = SearchByTextRequest.builder(
            query,
            PLACE_FIELDS
        )
            .setMaxResultCount(limit.coerceIn(1, 10))
            .setRankPreference(SearchByTextRequest.RankPreference.DISTANCE)
            .setLocationBias(
                CircularBounds.newInstance(
                    LatLng(latitude, longitude),
                    SEARCH_RADIUS_METERS
                )
            )
            .build()

        val response = placesClient.searchByText(request).await()
        return response.places.mapNotNull { place -> place.toCandidate(latitude, longitude) }
    }

    private fun Place.toCandidate(
        fromLatitude: Double,
        fromLongitude: Double
    ): NearbyStoreCandidate? {
        val placeId = id ?: return null
        val name = name ?: return null
        val latLng = latLng ?: return null
        val distanceMeters = calculateDistanceMeters(
            fromLatitude = fromLatitude,
            fromLongitude = fromLongitude,
            toLatitude = latLng.latitude,
            toLongitude = latLng.longitude
        )
        return NearbyStoreCandidate(
            placeId = placeId,
            name = name,
            address = address,
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            distanceMeters = distanceMeters,
            primaryType = primaryType
        )
    }

    private fun calculateDistanceMeters(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double
    ): Int {
        val earthRadiusMeters = 6_371_000.0
        val lat1 = Math.toRadians(fromLatitude)
        val lat2 = Math.toRadians(toLatitude)
        val deltaLat = Math.toRadians(toLatitude - fromLatitude)
        val deltaLng = Math.toRadians(toLongitude - fromLongitude)
        val a = kotlin.math.sin(deltaLat / 2) * kotlin.math.sin(deltaLat / 2) +
            kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
            kotlin.math.sin(deltaLng / 2) * kotlin.math.sin(deltaLng / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (earthRadiusMeters * c).toInt()
    }

    companion object {
        private const val TAG = "GooglePlacesStoreRepo"
        private const val SEARCH_RADIUS_METERS = 3_000.0
        private const val MAX_TYPE_QUERY_COUNT = 2
        private const val MAX_TEXT_QUERY_COUNT = 1

        private val PLACE_FIELDS = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
            Place.Field.PRIMARY_TYPE
        )

        private val ALLOWED_PRIMARY_TYPES = setOf(
            "supermarket",
            "grocery_store",
            "convenience_store",
            "drugstore",
            "store",
            "pet_store",
            "hardware_store",
            "home_goods_store",
            "department_store",
            "warehouse_store"
        )
    }
}

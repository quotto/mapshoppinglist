package com.mapshoppinglist.data.repository
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
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
        limit: Int
    ): List<NearbyStoreCandidate> = withContext(Dispatchers.IO) {
        val request = SearchByTextRequest.builder(
            itemTitle.trim(),
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
        response.places.mapNotNull { place ->
            val placeId = place.id ?: return@mapNotNull null
            val name = place.name ?: return@mapNotNull null
            val latLng = place.latLng ?: return@mapNotNull null
            val distanceMeters = calculateDistanceMeters(
                fromLatitude = latitude,
                fromLongitude = longitude,
                toLatitude = latLng.latitude,
                toLongitude = latLng.longitude
            )
            NearbyStoreCandidate(
                placeId = placeId,
                name = name,
                address = place.address,
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                distanceMeters = distanceMeters,
                primaryType = place.primaryType
            )
        }
            .filter { candidate -> candidate.primaryType in ALLOWED_PRIMARY_TYPES || candidate.primaryType == null }
            .sortedBy { it.distanceMeters }
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
        private const val SEARCH_RADIUS_METERS = 3_000.0

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
            "store"
        )
    }
}

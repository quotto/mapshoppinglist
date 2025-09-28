package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.GeofenceRegistration
import com.mapshoppinglist.domain.model.GeofenceSpec
import com.mapshoppinglist.domain.repository.GeofenceRegistryRepository
import com.mapshoppinglist.domain.repository.PlacesRepository

/**
 * アクティブなお店情報と現在の登録状況から、ジオフェンスの差分計画を算出するユースケース。
 */
class BuildGeofenceSyncPlanUseCase(
    private val placesRepository: PlacesRepository,
    private val geofenceRegistryRepository: GeofenceRegistryRepository,
    private val geofenceRadiusMeters: Float = 100f
) {
    suspend operator fun invoke(): GeofenceSyncPlan {
        val activePlaces = placesRepository.loadActivePlaces().filter { it.isActive }
        val currentRegistrations = geofenceRegistryRepository.loadAll()

        val activePlaceIds = activePlaces.map { it.id }.toSet()
        val registeredPlaceIds = currentRegistrations.map { it.placeId }.toSet()

        val placesToAdd = activePlaces.filter { it.id !in registeredPlaceIds }
        val registrationsToRemove = currentRegistrations.filter { it.placeId !in activePlaceIds }

        val specsToAdd = placesToAdd.map { place ->
            GeofenceSpec(
                placeId = place.id,
                requestId = buildRequestId(place.id),
                latitude = place.latitudeE6 / 1_000_000.0,
                longitude = place.longitudeE6 / 1_000_000.0,
                radiusMeters = geofenceRadiusMeters
            )
        }

        val requestIdsToRemove = registrationsToRemove.map { it.requestId }

        val targetRegistrations = activePlaces.map { place ->
            GeofenceRegistration(
                placeId = place.id,
                requestId = buildRequestId(place.id)
            )
        }

        return GeofenceSyncPlan(
            toAdd = specsToAdd,
            toRemoveRequestIds = requestIdsToRemove,
            targetRegistrations = targetRegistrations
        )
    }

    private fun buildRequestId(placeId: Long): String = "place_" + placeId
}

/**
 * ジオフェンス差分適用の計画。
 */
data class GeofenceSyncPlan(
    val toAdd: List<GeofenceSpec>,
    val toRemoveRequestIds: List<String>,
    val targetRegistrations: List<GeofenceRegistration>
)

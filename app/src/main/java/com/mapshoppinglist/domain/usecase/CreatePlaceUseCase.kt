package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.geofence.GeofenceSyncScheduler
import kotlin.math.roundToInt

class CreatePlaceUseCase(
    private val validatePlaceRegistrationUseCase: ValidatePlaceRegistrationUseCase,
    private val placesRepository: PlacesRepository,
    private val geofenceSyncScheduler: GeofenceSyncScheduler
) {

    suspend operator fun invoke(params: Params): Long {
        val latE6 = (params.latitude * SCALE).roundToInt()
        val lngE6 = (params.longitude * SCALE).roundToInt()
        validatePlaceRegistrationUseCase(latE6, lngE6)
        val placeId = placesRepository.addPlace(
            name = params.name,
            latE6 = latE6,
            lngE6 = lngE6,
            note = params.note
        )
        geofenceSyncScheduler.scheduleImmediateSync()
        return placeId
    }

    data class Params(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val note: String?
    )

    companion object {
        private const val SCALE = 1_000_000.0
    }
}

package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.geofence.GeofenceSyncScheduler

class DeletePlaceUseCase(
    private val placesRepository: PlacesRepository,
    private val geofenceSyncScheduler: GeofenceSyncScheduler
) {

    suspend operator fun invoke(placeId: Long) {
        placesRepository.deletePlace(placeId)
        geofenceSyncScheduler.scheduleImmediateSync()
    }
}

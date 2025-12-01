package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.geofence.GeofenceSyncScheduler

class UnlinkItemFromPlaceUseCase(private val placesRepository: PlacesRepository, private val geofenceSyncScheduler: GeofenceSyncScheduler) {

    suspend operator fun invoke(itemId: Long, placeId: Long) {
        placesRepository.unlinkItemFromPlace(placeId, itemId)
        geofenceSyncScheduler.scheduleImmediateSync()
    }
}

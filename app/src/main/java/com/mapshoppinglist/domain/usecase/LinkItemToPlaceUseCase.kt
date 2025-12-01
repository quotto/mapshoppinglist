package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.geofence.GeofenceSyncScheduler

class LinkItemToPlaceUseCase(private val placesRepository: PlacesRepository, private val geofenceSyncScheduler: GeofenceSyncScheduler) {

    suspend operator fun invoke(itemId: Long, placeId: Long) {
        placesRepository.linkItemToPlace(placeId, itemId)
        geofenceSyncScheduler.scheduleImmediateSync()
    }
}

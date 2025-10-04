package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.geofence.GeofenceSyncScheduler

/**
 * お店名称を更新するユースケース。
 */
class UpdatePlaceNameUseCase(
    private val placesRepository: PlacesRepository,
    private val geofenceSyncScheduler: GeofenceSyncScheduler
) {
    suspend operator fun invoke(placeId: Long, newName: String, scheduleOnRename: Boolean = false) {
        placesRepository.updateName(placeId, newName)
        if (scheduleOnRename) {
            geofenceSyncScheduler.scheduleImmediateSync()
        }
    }
}

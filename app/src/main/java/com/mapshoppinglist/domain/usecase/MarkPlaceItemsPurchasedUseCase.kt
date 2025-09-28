package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.ShoppingListRepository
import com.mapshoppinglist.geofence.GeofenceSyncScheduler

class MarkPlaceItemsPurchasedUseCase(
    private val shoppingListRepository: ShoppingListRepository,
    private val geofenceSyncScheduler: GeofenceSyncScheduler
) {

    suspend operator fun invoke(placeId: Long) {
        shoppingListRepository.markPlaceItemsPurchased(placeId)
        geofenceSyncScheduler.scheduleImmediateSync()
    }
}

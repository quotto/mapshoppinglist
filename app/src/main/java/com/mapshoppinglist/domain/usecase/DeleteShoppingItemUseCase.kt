package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.ShoppingListRepository
import com.mapshoppinglist.geofence.GeofenceSyncScheduler

/**
 * アイテム削除を担うユースケース。
 */
class DeleteShoppingItemUseCase(private val repository: ShoppingListRepository, private val geofenceSyncScheduler: GeofenceSyncScheduler) {
    suspend operator fun invoke(itemId: Long) {
        repository.deleteItem(itemId)
        geofenceSyncScheduler.scheduleImmediateSync()
    }
}

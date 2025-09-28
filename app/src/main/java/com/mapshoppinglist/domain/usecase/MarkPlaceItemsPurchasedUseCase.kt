package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.ShoppingListRepository

class MarkPlaceItemsPurchasedUseCase(
    private val shoppingListRepository: ShoppingListRepository
) {

    suspend operator fun invoke(placeId: Long) {
        shoppingListRepository.markPlaceItemsPurchased(placeId)
    }
}

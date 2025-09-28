package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.ShoppingListRepository

/**
 * 購入済みステータスを更新するユースケース。
 */
class UpdatePurchasedStateUseCase(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(itemId: Long, isPurchased: Boolean) {
        repository.updatePurchasedState(itemId = itemId, isPurchased = isPurchased)
    }
}

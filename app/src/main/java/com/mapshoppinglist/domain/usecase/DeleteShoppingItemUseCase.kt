package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.ShoppingListRepository

/**
 * アイテム削除を担うユースケース。
 */
class DeleteShoppingItemUseCase(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(itemId: Long) {
        repository.deleteItem(itemId)
    }
}

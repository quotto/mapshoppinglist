package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.ShoppingItem
import com.mapshoppinglist.domain.repository.ShoppingListRepository

class GetUnlinkedShoppingItemsUseCase(private val repository: ShoppingListRepository) {

    suspend operator fun invoke(limit: Int = DEFAULT_LIMIT): List<ShoppingItem> {
        require(limit > 0) { "limit must be positive" }
        return repository.getUnlinkedItems(limit)
    }

    companion object {
        private const val DEFAULT_LIMIT = 5
    }
}

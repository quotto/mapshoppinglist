package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.ShoppingItem
import com.mapshoppinglist.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow

/**
 * アイテム一覧を監視するユースケース。
 */
class ObserveShoppingItemsUseCase(
    private val repository: ShoppingListRepository
) {
    operator fun invoke(): Flow<List<ShoppingItem>> = repository.observeAllItems()
}

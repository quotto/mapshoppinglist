package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.ShoppingListRepository

/**
 * 新規アイテムを作成するユースケース。
 */
class AddShoppingItemUseCase(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(title: String, note: String?): Long {
        return repository.addItem(title = title, note = note)
    }
}

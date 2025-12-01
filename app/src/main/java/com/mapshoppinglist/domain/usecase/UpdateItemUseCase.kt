package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.ShoppingListRepository

class UpdateItemUseCase(private val repository: ShoppingListRepository) {

    suspend operator fun invoke(itemId: Long, title: String, note: String?) {
        repository.updateItem(itemId, title, note)
    }
}

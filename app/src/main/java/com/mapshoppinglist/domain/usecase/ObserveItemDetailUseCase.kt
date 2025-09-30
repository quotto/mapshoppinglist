package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.ItemDetail
import com.mapshoppinglist.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow

class ObserveItemDetailUseCase(
    private val repository: ShoppingListRepository
) {

    operator fun invoke(itemId: Long): Flow<ItemDetail?> = repository.observeItemDetail(itemId)
}

package com.mapshoppinglist.data.repository

import com.mapshoppinglist.data.local.dao.NearbySuggestionStateDao
import com.mapshoppinglist.data.local.entity.NearbySuggestionStateEntity
import com.mapshoppinglist.domain.model.NearbySuggestionState
import com.mapshoppinglist.domain.repository.NearbySuggestionStateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultNearbySuggestionStateRepository(
    private val dao: NearbySuggestionStateDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NearbySuggestionStateRepository {

    override suspend fun get(itemId: Long, candidatePlaceId: String): NearbySuggestionState? = withContext(ioDispatcher) {
        dao.find(itemId, candidatePlaceId)?.toDomain()
    }

    override suspend fun upsert(state: NearbySuggestionState) = withContext(ioDispatcher) {
        dao.upsert(
            NearbySuggestionStateEntity(
                itemId = state.itemId,
                candidatePlaceId = state.candidatePlaceId,
                candidatePlaceName = state.candidatePlaceName,
                lastNotifiedAt = state.lastNotifiedAt,
                lastNotifiedLatE6 = state.lastNotifiedLatE6,
                lastNotifiedLngE6 = state.lastNotifiedLngE6
            )
        )
    }

    override suspend fun clear(itemId: Long, candidatePlaceId: String) = withContext(ioDispatcher) {
        dao.delete(itemId, candidatePlaceId)
    }

    override suspend fun clearByItemId(itemId: Long) = withContext(ioDispatcher) {
        dao.deleteByItemId(itemId)
    }

    private fun NearbySuggestionStateEntity.toDomain(): NearbySuggestionState = NearbySuggestionState(
        itemId = itemId,
        candidatePlaceId = candidatePlaceId,
        candidatePlaceName = candidatePlaceName,
        lastNotifiedAt = lastNotifiedAt,
        lastNotifiedLatE6 = lastNotifiedLatE6,
        lastNotifiedLngE6 = lastNotifiedLngE6
    )
}

package com.mapshoppinglist.domain.repository

import com.mapshoppinglist.domain.model.NearbySuggestionState

interface NearbySuggestionStateRepository {
    suspend fun get(itemId: Long, candidatePlaceId: String): NearbySuggestionState?
    suspend fun upsert(state: NearbySuggestionState)
    suspend fun clear(itemId: Long, candidatePlaceId: String)
    suspend fun clearByItemId(itemId: Long)
}

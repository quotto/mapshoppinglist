package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.NearbySuggestionState
import com.mapshoppinglist.domain.repository.NearbySuggestionStateRepository

class RecordNearbySuggestionUseCase(
    private val repository: NearbySuggestionStateRepository
) {

    suspend operator fun invoke(
        itemId: Long,
        candidatePlaceId: String,
        candidatePlaceName: String?,
        now: Long = System.currentTimeMillis(),
        latE6: Int? = null,
        lngE6: Int? = null
    ) {
        repository.upsert(
            NearbySuggestionState(
                itemId = itemId,
                candidatePlaceId = candidatePlaceId,
                candidatePlaceName = candidatePlaceName,
                lastNotifiedAt = now,
                lastNotifiedLatE6 = latE6,
                lastNotifiedLngE6 = lngE6
            )
        )
    }
}

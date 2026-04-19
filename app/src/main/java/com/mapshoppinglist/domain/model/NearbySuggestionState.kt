package com.mapshoppinglist.domain.model

data class NearbySuggestionState(
    val itemId: Long,
    val candidatePlaceId: String,
    val candidatePlaceName: String?,
    val lastNotifiedAt: Long?,
    val lastNotifiedLatE6: Int?,
    val lastNotifiedLngE6: Int?
)

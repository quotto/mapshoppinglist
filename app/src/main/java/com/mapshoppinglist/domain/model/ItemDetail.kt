package com.mapshoppinglist.domain.model

data class ItemDetail(
    val id: Long,
    val title: String,
    val note: String?,
    val isPurchased: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val places: List<PlaceSummary>
)

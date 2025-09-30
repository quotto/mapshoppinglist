package com.mapshoppinglist.domain.model

data class PlaceSummary(
    val id: Long,
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double
)

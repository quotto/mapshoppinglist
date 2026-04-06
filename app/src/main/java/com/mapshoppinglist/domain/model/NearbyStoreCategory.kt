package com.mapshoppinglist.domain.model

data class NearbyStoreCategory(
    val placeType: String,
    val confidence: Double?,
    val reason: String?
)

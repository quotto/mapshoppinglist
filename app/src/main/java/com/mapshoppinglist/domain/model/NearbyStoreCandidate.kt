package com.mapshoppinglist.domain.model

data class NearbyStoreCandidate(
    val placeId: String,
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val primaryType: String?
)

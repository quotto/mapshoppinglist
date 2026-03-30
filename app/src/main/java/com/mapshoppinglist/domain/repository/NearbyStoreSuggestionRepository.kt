package com.mapshoppinglist.domain.repository

import com.mapshoppinglist.domain.model.NearbyStoreCandidate

interface NearbyStoreSuggestionRepository {
    suspend fun search(itemTitle: String, latitude: Double, longitude: Double, limit: Int = 5): List<NearbyStoreCandidate>
}

package com.mapshoppinglist.domain.repository

import com.mapshoppinglist.domain.model.NearbyStoreCategory

interface NearbyStoreCategoryRepository {
    suspend fun classify(itemTitle: String, maxCategories: Int = 3): List<NearbyStoreCategory>
}

package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.NearbyStoreCategory

class BuildNearbyStoreSearchQueriesUseCase(
    private val highConfidenceThreshold: Double = DEFAULT_HIGH_CONFIDENCE_THRESHOLD,
    private val minConfidenceThreshold: Double = DEFAULT_MIN_CONFIDENCE_THRESHOLD,
    private val maxCategoryQueries: Int = DEFAULT_MAX_CATEGORY_QUERY_COUNT
) {

    operator fun invoke(
        itemTitle: String,
        categories: List<NearbyStoreCategory>
    ): NearbyStoreSearchPlan {
        val normalizedItemTitle = itemTitle.trim()
        if (normalizedItemTitle.isBlank()) return NearbyStoreSearchPlan()

        val rankedCategories = categories
            .mapNotNull { category ->
                val placeType = category.placeType.trim().lowercase()
                if (placeType.isBlank()) null else category.copy(placeType = placeType)
            }
            .distinctBy { it.placeType }
            .sortedWith(
                compareByDescending<NearbyStoreCategory> { isSpecificType(it.placeType) }
                    .thenByDescending { it.confidence ?: Double.NEGATIVE_INFINITY }
            )

        if (rankedCategories.isEmpty()) {
            return NearbyStoreSearchPlan(textQueries = listOf(normalizedItemTitle))
        }

        val specificCategories = rankedCategories.filter { isSpecificType(it.placeType) }
        val effectiveCategories = if (specificCategories.isNotEmpty()) specificCategories else rankedCategories
        val topCategory = effectiveCategories.first()

        if ((topCategory.confidence ?: 1.0) >= highConfidenceThreshold) {
            return NearbyStoreSearchPlan(typeQueries = listOf(topCategory.placeType))
        }

        val confidentCategories = effectiveCategories
            .filter { it.confidence == null || it.confidence >= minConfidenceThreshold }
            .take(maxCategoryQueries)
            .map { it.placeType }

        if (confidentCategories.isNotEmpty()) {
            return NearbyStoreSearchPlan(typeQueries = confidentCategories)
        }

        if (!isSpecificType(topCategory.placeType)) {
            return NearbyStoreSearchPlan(textQueries = listOf(normalizedItemTitle))
        }

        return NearbyStoreSearchPlan(
            typeQueries = listOf(topCategory.placeType),
            textQueries = listOf(normalizedItemTitle)
        )
    }

    private fun isSpecificType(placeType: String): Boolean = placeType !in GENERIC_PLACE_TYPES
}

data class NearbyStoreSearchPlan(
    val typeQueries: List<String> = emptyList(),
    val textQueries: List<String> = emptyList()
)

internal const val DEFAULT_HIGH_CONFIDENCE_THRESHOLD = 0.75
internal const val DEFAULT_MIN_CONFIDENCE_THRESHOLD = 0.35
internal const val DEFAULT_MAX_CATEGORY_QUERY_COUNT = 2

private val GENERIC_PLACE_TYPES = setOf(
    "store"
)

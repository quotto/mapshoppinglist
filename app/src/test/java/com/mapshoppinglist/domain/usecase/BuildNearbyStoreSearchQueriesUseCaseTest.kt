package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.NearbyStoreCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildNearbyStoreSearchQueriesUseCaseTest {

    private val useCase = BuildNearbyStoreSearchQueriesUseCase()

    @Test
    fun `returns item title when categories are empty`() {
        val queries = useCase(
            itemTitle = "牛乳",
            categories = emptyList()
        )

        assertEquals(NearbyStoreSearchPlan(textQueries = listOf("牛乳")), queries)
    }

    @Test
    fun `uses single top category when confidence is high`() {
        val queries = useCase(
            itemTitle = "牛乳",
            categories = listOf(
                NearbyStoreCategory("supermarket", 0.92, "food"),
                NearbyStoreCategory("grocery_store", 0.51, "food")
            )
        )

        assertEquals(NearbyStoreSearchPlan(typeQueries = listOf("supermarket")), queries)
    }

    @Test
    fun `drops generic store when specific categories exist`() {
        val queries = useCase(
            itemTitle = "乾電池",
            categories = listOf(
                NearbyStoreCategory("store", 0.99, "generic"),
                NearbyStoreCategory("drugstore", 0.54, "daily"),
                NearbyStoreCategory("convenience_store", 0.42, "fallback")
            )
        )

        assertEquals(
            NearbyStoreSearchPlan(typeQueries = listOf("drugstore", "convenience_store")),
            queries
        )
    }

    @Test
    fun `falls back to item title when only generic low confidence categories are returned`() {
        val queries = useCase(
            itemTitle = "歯ブラシ",
            categories = listOf(
                NearbyStoreCategory("store", 0.21, "generic")
            )
        )

        assertEquals(NearbyStoreSearchPlan(textQueries = listOf("歯ブラシ")), queries)
    }

    @Test
    fun `uses top category and item title when only low confidence specific category exists`() {
        val queries = useCase(
            itemTitle = "猫砂",
            categories = listOf(
                NearbyStoreCategory("pet_store", 0.18, "pet")
            )
        )

        assertEquals(
            NearbyStoreSearchPlan(
                typeQueries = listOf("pet_store"),
                textQueries = listOf("猫砂")
            ),
            queries
        )
    }
}

package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.ShoppingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BuildNotificationMessageUseCaseTest {

    private val useCase = BuildNotificationMessageUseCase()

    @Test
    fun singleItemUsesNoteAsSummary() {
        val item = ShoppingItem(
            id = 1L,
            title = "牛乳",
            note = "2本",
            isPurchased = false,
            createdAt = 0L,
            updatedAt = 0L,
            linkedPlaceCount = 0
        )
        val result = useCase.invoke("スーパーA", listOf(item))
        assertEquals("スーパーA で買うもの", result.title)
        assertEquals(listOf("・牛乳"), result.lines)
        assertEquals("2本", result.summary)
    }

    @Test
    fun multipleItemsSummarizeRemainingCount() {
        val items = listOf(
            ShoppingItem(1, "牛乳", null, false, 0, 0, 0),
            ShoppingItem(2, "卵", null, false, 0, 1, 0),
            ShoppingItem(3, "食パン", null, false, 0, 2, 0)
        )
        val result = useCase.invoke("スーパーB", items)
        assertEquals(3, result.lines.size)
        assertEquals("ほか2件", result.summary)
    }

    @Test
    fun singleItemWithoutNoteHasNoSummary() {
        val item = ShoppingItem(1, "卵", null, false, 0, 0, 0)
        val result = useCase.invoke("スーパーC", listOf(item))
        assertNull(result.summary)
    }
}

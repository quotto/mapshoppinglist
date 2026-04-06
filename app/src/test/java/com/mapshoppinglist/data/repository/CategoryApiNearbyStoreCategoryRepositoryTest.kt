package com.mapshoppinglist.data.repository

import android.os.Build
import com.mapshoppinglist.domain.model.NearbyStoreCategory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.TIRAMISU],
    application = android.app.Application::class
)
class CategoryApiNearbyStoreCategoryRepositoryTest {

    @Test
    fun `classify sends api key and parses categories`() = runTest {
        val connection = FakeHttpURLConnection(
            responseCodeValue = 200,
            responseBody = """
                {
                  "categories": [
                    {"placeType":"supermarket","confidence":0.91,"reason":"food"},
                    {"placeType":"drugstore","confidence":0.45,"reason":"daily"}
                  ]
                }
            """.trimIndent()
        )
        val repository = CategoryApiNearbyStoreCategoryRepository(
            endpoint = "https://example.com/v1/item-category:classify",
            apiKey = "secret-key",
            openConnection = { connection }
        )

        val result = repository.classify(" 牛乳 ", maxCategories = 3)

        assertEquals(
            listOf(
                NearbyStoreCategory("supermarket", 0.91, "food"),
                NearbyStoreCategory("drugstore", 0.45, "daily")
            ),
            result
        )
        assertEquals("POST", connection.requestMethod)
        assertEquals("secret-key", connection.requestProperties["X-Api-Key"])
        assertTrue(connection.requestBodyUtf8.contains("\"itemName\":\"牛乳\""))
        assertTrue(connection.requestBodyUtf8.contains("\"maxCategories\":3"))
    }

    @Test
    fun `classify returns empty when endpoint or api key is missing`() = runTest {
        val repository = CategoryApiNearbyStoreCategoryRepository(
            endpoint = "",
            apiKey = ""
        )

        val result = repository.classify("牛乳")

        assertTrue(result.isEmpty())
    }
}

private class FakeHttpURLConnection(
    private val responseCodeValue: Int,
    responseBody: String
) : HttpURLConnection(URL("https://example.com")) {

    private val outputBuffer = ByteArrayOutputStream()
    private val inputBytes = responseBody.toByteArray()
    val requestProperties = linkedMapOf<String, String>()
    val requestBodyUtf8: String
        get() = outputBuffer.toString(Charsets.UTF_8.name())

    override fun setRequestProperty(key: String?, value: String?) {
        if (key != null && value != null) {
            requestProperties[key] = value
        }
    }

    override fun getOutputStream(): ByteArrayOutputStream = outputBuffer

    override fun getInputStream(): ByteArrayInputStream = ByteArrayInputStream(inputBytes)

    override fun getErrorStream(): ByteArrayInputStream = ByteArrayInputStream(inputBytes)

    override fun getResponseCode(): Int = responseCodeValue

    override fun disconnect() {}

    override fun usingProxy(): Boolean = false

    override fun connect() {}
}

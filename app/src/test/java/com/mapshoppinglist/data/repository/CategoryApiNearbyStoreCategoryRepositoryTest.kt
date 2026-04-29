package com.mapshoppinglist.data.repository

import android.os.Build
import com.mapshoppinglist.monitoring.ExternalApiErrorReporter
import com.mapshoppinglist.domain.model.NearbyStoreCategory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CancellationException
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

    @Test
    fun `classify records response error when api returns non-success`() = runTest {
        val connection = FakeHttpURLConnection(
            responseCodeValue = 500,
            responseBody = """{"message":"internal error"}"""
        )
        val reporter = FakeExternalApiErrorReporter()
        val repository = CategoryApiNearbyStoreCategoryRepository(
            endpoint = "https://example.com/v1/item-category:classify",
            apiKey = "secret-key",
            errorReporter = reporter,
            openConnection = { connection }
        )

        val result = repository.classify("牛乳")

        assertTrue(result.isEmpty())
        assertEquals(1, reporter.responseErrors.size)
        assertEquals("nearby_category_api", reporter.responseErrors.single().apiName)
        assertEquals("classify", reporter.responseErrors.single().operation)
        assertEquals(500, reporter.responseErrors.single().statusCode)
    }

    @Test(expected = CancellationException::class)
    fun `classify rethrows cancellation exception and disconnects connection`() = runTest {
        val connection = FakeHttpURLConnection(
            responseCodeValue = 200,
            responseBody = """{"categories": []}""",
            outputStreamError = CancellationException("cancelled")
        )
        val repository = CategoryApiNearbyStoreCategoryRepository(
            endpoint = "https://example.com/v1/item-category:classify",
            apiKey = "secret-key",
            openConnection = { connection }
        )

        try {
            repository.classify("牛乳")
        } finally {
            assertTrue(connection.disconnected)
        }
    }
}

private class FakeHttpURLConnection(
    private val responseCodeValue: Int,
    responseBody: String,
    private val outputStreamError: Exception? = null
) : HttpURLConnection(URL("https://example.com")) {

    private val outputBuffer = ByteArrayOutputStream()
    private val inputBytes = responseBody.toByteArray()
    val requestProperties = linkedMapOf<String, String>()
    var disconnected: Boolean = false
    val requestBodyUtf8: String
        get() = outputBuffer.toString(Charsets.UTF_8.name())

    override fun setRequestProperty(key: String?, value: String?) {
        if (key != null && value != null) {
            requestProperties[key] = value
        }
    }

    override fun getOutputStream(): ByteArrayOutputStream {
        outputStreamError?.let { throw it }
        return outputBuffer
    }

    override fun getInputStream(): ByteArrayInputStream = ByteArrayInputStream(inputBytes)

    override fun getErrorStream(): ByteArrayInputStream = ByteArrayInputStream(inputBytes)

    override fun getResponseCode(): Int = responseCodeValue

    override fun disconnect() {
        disconnected = true
    }

    override fun usingProxy(): Boolean = false

    override fun connect() {}
}

private class FakeExternalApiErrorReporter : ExternalApiErrorReporter {
    val executionErrors = mutableListOf<RecordedExecutionError>()
    val responseErrors = mutableListOf<RecordedResponseError>()

    override fun recordExecutionError(
        apiName: String,
        operation: String,
        throwable: Throwable,
        attributes: Map<String, String>
    ) {
        executionErrors += RecordedExecutionError(apiName, operation, throwable, attributes)
    }

    override fun recordResponseError(
        apiName: String,
        operation: String,
        statusCode: Int,
        responseBodyPreview: String?,
        attributes: Map<String, String>
    ) {
        responseErrors += RecordedResponseError(apiName, operation, statusCode, responseBodyPreview, attributes)
    }
}

private data class RecordedExecutionError(
    val apiName: String,
    val operation: String,
    val throwable: Throwable,
    val attributes: Map<String, String>
)

private data class RecordedResponseError(
    val apiName: String,
    val operation: String,
    val statusCode: Int,
    val responseBodyPreview: String?,
    val attributes: Map<String, String>
)

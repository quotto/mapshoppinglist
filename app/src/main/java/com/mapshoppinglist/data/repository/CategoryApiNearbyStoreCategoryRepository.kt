package com.mapshoppinglist.data.repository

import android.util.Log
import com.mapshoppinglist.domain.model.NearbyStoreCategory
import com.mapshoppinglist.domain.repository.NearbyStoreCategoryRepository
import com.mapshoppinglist.monitoring.ExternalApiErrorReporter
import com.mapshoppinglist.monitoring.NoOpExternalApiErrorReporter
import java.io.BufferedWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CategoryApiNearbyStoreCategoryRepository(
    private val endpoint: String,
    private val apiKey: String,
    private val errorReporter: ExternalApiErrorReporter = NoOpExternalApiErrorReporter,
    private val openConnection: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    }
) : NearbyStoreCategoryRepository {

    override suspend fun classify(itemTitle: String, maxCategories: Int): List<NearbyStoreCategory> =
        withContext(Dispatchers.IO) {
            val normalizedItemTitle = itemTitle.trim()
            if (endpoint.isBlank() || apiKey.isBlank() || normalizedItemTitle.isBlank()) {
                logWarn(TAG, "Category API not configured: endpointOrKeyMissing=${endpoint.isBlank() || apiKey.isBlank()} itemTitleBlank=${normalizedItemTitle.isBlank()}")
                return@withContext emptyList()
            }

            val connection = openConnection(URL(endpoint))
            return@withContext try {
                logInfo(
                    TAG,
                    "Calling category API: endpoint=$endpoint itemTitle=$normalizedItemTitle maxCategories=${maxCategories.coerceIn(1, MAX_CATEGORIES)}"
                )
                connection.requestMethod = "POST"
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.doInput = true
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty(HEADER_API_KEY, apiKey)

                BufferedWriter(connection.outputStream.writer()).use { writer ->
                    writer.write(
                        JSONObject()
                            .put("itemName", normalizedItemTitle)
                            .put("locale", DEFAULT_LOCALE)
                            .put("country", DEFAULT_COUNTRY)
                            .put("maxCategories", maxCategories.coerceIn(1, MAX_CATEGORIES))
                            .toString()
                    )
                }

                val responseBody = (
                    if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                )?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (connection.responseCode !in 200..299) {
                    logWarn(
                        TAG,
                        "Category API returned non-success status=${connection.responseCode} body=$responseBody"
                    )
                    errorReporter.recordResponseError(
                        apiName = API_NAME,
                        operation = "classify",
                        statusCode = connection.responseCode,
                        responseBodyPreview = responseBody.take(RESPONSE_PREVIEW_LIMIT),
                        attributes = mapOf(
                            "has_endpoint" to endpoint.isNotBlank().toString(),
                            "max_categories" to maxCategories.coerceIn(1, MAX_CATEGORIES).toString()
                        )
                    )
                    emptyList()
                } else {
                    parseCategories(responseBody).also { categories ->
                        logInfo(
                            TAG,
                            "Category API returned ${categories.size} categories for itemTitle=$normalizedItemTitle values=${categories.joinToString("|") { "${it.placeType}:${it.confidence ?: "na"}" }}"
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logWarn(TAG, "Category API classification failed", error)
                errorReporter.recordExecutionError(
                    apiName = API_NAME,
                    operation = "classify",
                    throwable = error,
                    attributes = mapOf(
                        "has_endpoint" to endpoint.isNotBlank().toString(),
                        "max_categories" to maxCategories.coerceIn(1, MAX_CATEGORIES).toString()
                    )
                )
                emptyList()
            } finally {
                connection.disconnect()
            }
        }

    private fun parseCategories(responseBody: String): List<NearbyStoreCategory> {
        if (responseBody.isBlank()) return emptyList()
        val json = JSONObject(responseBody)
        val categories = json.optJSONArray("categories") ?: JSONArray()
        return buildList {
            for (index in 0 until categories.length()) {
                val item = categories.optJSONObject(index) ?: continue
                val placeType = item.optString("placeType").trim()
                if (placeType.isBlank()) continue
                add(
                    NearbyStoreCategory(
                        placeType = placeType,
                        confidence = item.optDoubleOrNull("confidence"),
                        reason = item.optString("reason").takeIf { it.isNotBlank() }
                    )
                )
            }
        }.distinctBy { it.placeType }
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? {
        return if (has(name) && !isNull(name)) optDouble(name) else null
    }

    companion object {
        private const val TAG = "CategoryApiStoreCatRepo"
        private const val HEADER_API_KEY = "X-Api-Key"
        private const val DEFAULT_LOCALE = "ja-JP"
        private const val DEFAULT_COUNTRY = "JP"
        private const val API_NAME = "nearby_category_api"
        private const val MAX_CATEGORIES = 5
        private const val CONNECT_TIMEOUT_MILLIS = 10_000
        private const val READ_TIMEOUT_MILLIS = 10_000
        private const val RESPONSE_PREVIEW_LIMIT = 256
    }

    private fun logInfo(tag: String, message: String) {
        runCatching { Log.i(tag, message) }
    }

    private fun logWarn(tag: String, message: String, error: Throwable? = null) {
        runCatching {
            if (error == null) {
                Log.w(tag, message)
            } else {
                Log.w(tag, message, error)
            }
        }
    }
}

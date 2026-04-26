package com.mapshoppinglist.monitoring

import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseExternalApiErrorReporter(
    private val crashlyticsProvider: () -> FirebaseCrashlytics = { FirebaseCrashlytics.getInstance() }
) : ExternalApiErrorReporter {

    override fun recordExecutionError(
        apiName: String,
        operation: String,
        throwable: Throwable,
        attributes: Map<String, String>
    ) {
        val crashlytics = crashlyticsProvider()
        crashlytics.log("external_api_execution_error api=$apiName operation=$operation")
        crashlytics.recordException(
            ExternalApiExecutionException(apiName, operation, throwable),
            buildKeys(
                apiName = apiName,
                operation = operation,
                attributes = attributes
            )
        )
    }

    override fun recordResponseError(
        apiName: String,
        operation: String,
        statusCode: Int,
        responseBodyPreview: String?,
        attributes: Map<String, String>
    ) {
        val crashlytics = crashlyticsProvider()
        crashlytics.log("external_api_response_error api=$apiName operation=$operation statusCode=$statusCode")
        crashlytics.recordException(
            ExternalApiResponseException(
                apiName = apiName,
                operation = operation,
                statusCode = statusCode,
                responseBodyPreview = responseBodyPreview
            ),
            buildKeys(
                apiName = apiName,
                operation = operation,
                attributes = attributes + mapOf("status_code" to statusCode.toString())
            )
        )
    }

    private fun buildKeys(
        apiName: String,
        operation: String,
        attributes: Map<String, String>
    ): CustomKeysAndValues {
        val builder = CustomKeysAndValues.Builder()
            .putString("external_api_name", apiName.take(MAX_VALUE_LENGTH))
            .putString("external_api_operation", operation.take(MAX_VALUE_LENGTH))
        attributes
            .toSortedMap()
            .forEach { (key, value) ->
                builder.putString(
                    "external_api_${key.take(MAX_KEY_LENGTH)}",
                    value.take(MAX_VALUE_LENGTH)
                )
            }
        return builder.build()
    }

    private companion object {
        private const val MAX_KEY_LENGTH = 48
        private const val MAX_VALUE_LENGTH = 256
    }
}

private class ExternalApiExecutionException(
    apiName: String,
    operation: String,
    cause: Throwable
) : RuntimeException("External API execution failed: api=$apiName operation=$operation", cause)

private class ExternalApiResponseException(
    apiName: String,
    operation: String,
    statusCode: Int,
    responseBodyPreview: String?
) : RuntimeException(
    buildString {
        append("External API response failed: api=")
        append(apiName)
        append(" operation=")
        append(operation)
        append(" statusCode=")
        append(statusCode)
        if (!responseBodyPreview.isNullOrBlank()) {
            append(" body=")
            append(responseBodyPreview.take(256))
        }
    }
)

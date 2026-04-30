package com.mapshoppinglist.monitoring

interface ExternalApiErrorReporter {

    fun recordExecutionError(
        apiName: String,
        operation: String,
        throwable: Throwable,
        attributes: Map<String, String> = emptyMap()
    )

    fun recordResponseError(
        apiName: String,
        operation: String,
        statusCode: Int,
        responseBodyPreview: String? = null,
        attributes: Map<String, String> = emptyMap()
    )
}

object NoOpExternalApiErrorReporter : ExternalApiErrorReporter {
    override fun recordExecutionError(
        apiName: String,
        operation: String,
        throwable: Throwable,
        attributes: Map<String, String>
    ) = Unit

    override fun recordResponseError(
        apiName: String,
        operation: String,
        statusCode: Int,
        responseBodyPreview: String?,
        attributes: Map<String, String>
    ) = Unit
}

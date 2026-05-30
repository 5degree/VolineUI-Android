@file:Suppress("unused")

package com.cropintellix.volinecore.remotelogger

/**
 * Complete API call log entry containing both request and response data.
 *
 * Use this with [VolineLogger.api] to log a full API round-trip in one call.
 *
 * @property request The outgoing request details.
 * @property response The incoming response details (null if the request failed before a response).
 * @property durationMs Round-trip time in milliseconds.
 * @property error Error message if the call failed (e.g., timeout, DNS resolution failure).
 */
data class ApiLogEntry(
    val request: ApiRequestData,
    val response: ApiResponseData? = null,
    val durationMs: Long,
    val error: String? = null
)

/**
 * Captures all relevant details of an outgoing HTTP request.
 *
 * @property url Full request URL including scheme and path.
 * @property method HTTP method (GET, POST, PUT, DELETE, PATCH, etc.).
 * @property headers Request headers as key-value pairs.
 *                    Sensitive headers are automatically redacted by the logger.
 * @property queryParams URL query parameters (separate from the URL for clarity).
 * @property body Request body string (typically JSON). May be null for GET requests.
 * @property contentType Content-Type header value for convenience.
 * @property timestamp ISO 8601 timestamp when the request was sent.
 */
data class ApiRequestData(
    val url: String,
    val method: String,
    val headers: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String>? = null,
    val body: String? = null,
    val contentType: String? = null,
    val timestamp: String? = null
)

/**
 * Captures all relevant details of an incoming HTTP response.
 *
 * @property statusCode HTTP status code (e.g., 200, 404, 500).
 * @property statusMessage HTTP status message (e.g., "OK", "Not Found").
 * @property headers Response headers as key-value pairs.
 * @property body Response body string. May be truncated for very large responses.
 * @property contentType Content-Type header value for convenience.
 * @property contentLength Response body size in bytes.
 * @property timestamp ISO 8601 timestamp when the response was received.
 */
data class ApiResponseData(
    val statusCode: Int,
    val statusMessage: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val contentType: String? = null,
    val contentLength: Long? = null,
    val timestamp: String? = null
)

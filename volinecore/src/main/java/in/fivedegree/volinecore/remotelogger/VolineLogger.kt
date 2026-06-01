@file:Suppress("unused")

package `in`.fivedegree.volinecore.remotelogger

import android.app.Application
import android.util.Log
import com.google.gson.Gson
import java.io.PrintWriter
import java.io.StringWriter
import java.util.UUID

/**
 * VolineLogger — A reusable, scalable remote logging system for Android.
 *
 * Pushes structured log entries to Firebase Realtime Database with automatic
 * device metadata enrichment, offline queuing, and sensitive header redaction.
 *
 * ## Features
 * - **One-time initialization** in your Application class
 * - **Universal access** — call from any module/class without passing context
 * - **Automatic metadata** — device info, session ID, timestamps on every log
 * - **Multiple log types** — debug, info, warning, error, critical, API, event, crash
 * - **API logging** — dedicated methods for request/response with header redaction
 * - **Offline resilience** — Room-backed queue with auto-sync on connectivity change
 * - **Crash capture** — automatic uncaught exception logging
 * - **Logcat mirroring** — optional local output for development
 * - **Level filtering** — configurable minimum log level for remote push
 *
 * ## Quick Start
 * ```kotlin
 * // 1. Initialize once in Application.onCreate()
 * VolineLogger.init(this, LoggerConfig(appId = "my-app"))
 *
 * // 2. Log from anywhere
 * VolineLogger.d("MyTag", "Something happened")
 * VolineLogger.api(apiLogEntry)
 * ```
 *
 * ## Firebase Database Structure
 * ```
 * /{databaseRef}/{appId}/{logType}/{pushId} → { ...LogEntry }
 * ```
 */
object VolineLogger {

    private const val TAG = "VolineLogger"

    @Volatile
    private var isInitialized = false

    private lateinit var config: LoggerConfig
    private lateinit var enricher: MetadataEnricher
    private lateinit var dispatcher: LogDispatcher
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var sessionId: String

    private val gson = Gson()

    // ═══════════════════════════════════════════════════════════════════════
    //  INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Initialize the remote logger. Must be called once in [Application.onCreate].
     *
     * @param application Application instance.
     * @param config Logger configuration. See [LoggerConfig] for options.
     * @throws LoggerException if [config] is invalid.
     */
    @JvmStatic
    fun init(application: Application, config: LoggerConfig) {
        if (isInitialized) {
            Log.w(TAG, "VolineLogger is already initialized. Ignoring duplicate init().")
            return
        }

        synchronized(this) {
            if (isInitialized) return

            this.config = config
            this.sessionId = "sess_${UUID.randomUUID().toString().take(8)}"

            // Initialize device info collector
            DeviceInfoCollector.init(application)

            // Initialize metadata enricher
            enricher = MetadataEnricher(config, sessionId)

            // Initialize connectivity observer
            connectivityObserver = ConnectivityObserver(application) {
                // On connectivity restored → flush offline queue
                dispatcher.flushQueue()
            }

            // Initialize log dispatcher
            dispatcher = LogDispatcher(application, config, connectivityObserver)

            // Start connectivity monitoring (for offline queue sync)
            if (config.enableOfflineQueue) {
                connectivityObserver.start()
            }

            // Install crash handler
            if (config.enableCrashLogging) {
                CrashHandler().install()
            }

            isInitialized = true
            Log.i(TAG, "VolineLogger initialized — appId=${config.appId}, session=$sessionId")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GENERAL LOGGING METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Log a **debug** message.
     *
     * @param tag Source identifier (e.g., class name, feature area).
     * @param message Human-readable log message.
     * @param extras Optional key-value data to attach.
     */
    @JvmStatic
    @JvmOverloads
    fun d(tag: String, message: String, extras: Map<String, Any?>? = null) {
        log(LogLevel.DEBUG, "debug", tag, message, extras)
    }

    /**
     * Log an **info** message.
     *
     * @param tag Source identifier.
     * @param message Human-readable log message.
     * @param extras Optional key-value data to attach.
     */
    @JvmStatic
    @JvmOverloads
    fun i(tag: String, message: String, extras: Map<String, Any?>? = null) {
        log(LogLevel.INFO, "info", tag, message, extras)
    }

    /**
     * Log a **warning** message.
     *
     * @param tag Source identifier.
     * @param message Human-readable log message.
     * @param extras Optional key-value data to attach.
     */
    @JvmStatic
    @JvmOverloads
    fun w(tag: String, message: String, extras: Map<String, Any?>? = null) {
        log(LogLevel.WARNING, "warning", tag, message, extras)
    }

    /**
     * Log an **error** message, optionally with a [Throwable].
     *
     * @param tag Source identifier.
     * @param message Human-readable log message.
     * @param throwable Optional exception to include stack trace.
     * @param extras Optional key-value data to attach.
     */
    @JvmStatic
    @JvmOverloads
    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        extras: Map<String, Any?>? = null
    ) {
        val enrichedExtras = buildThrowableExtras(throwable, extras)
        log(LogLevel.ERROR, "error", tag, message, enrichedExtras)
    }

    /**
     * Log a **critical / WTF** (What a Terrible Failure) message.
     *
     * @param tag Source identifier.
     * @param message Human-readable log message.
     * @param throwable Optional exception to include stack trace.
     */
    @JvmStatic
    @JvmOverloads
    fun wtf(tag: String, message: String, throwable: Throwable? = null) {
        val extras = buildThrowableExtras(throwable, null)
        log(LogLevel.CRITICAL, "critical", tag, message, extras)
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STRUCTURED DATA LOGGING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Log an analytics/business **event** with structured properties.
     *
     * Events are pushed under the `event` log type in Firebase.
     *
     * @param name Event name (e.g., "purchase_completed", "screen_viewed").
     * @param properties Key-value properties for the event.
     */
    @JvmStatic
    fun event(name: String, properties: Map<String, Any?> = emptyMap()) {
        log(LogLevel.INFO, "event", name, "Event: $name", properties)
    }

    /**
     * Log a raw **JSON string** for inspection.
     *
     * @param tag Source identifier.
     * @param jsonString Raw JSON string to log.
     */
    @JvmStatic
    fun json(tag: String, jsonString: String) {
        val parsed: Map<String, Any?> = try {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(jsonString, Map::class.java) as Map<String, Any?>
        } catch (_: Exception) {
            mapOf("raw_json" to jsonString)
        }
        log(LogLevel.DEBUG, "debug", tag, "JSON data", parsed)
    }

    /**
     * Log a **key-value map** for inspection.
     *
     * @param tag Source identifier.
     * @param data Key-value pairs to log.
     */
    @JvmStatic
    fun map(tag: String, data: Map<String, Any?>) {
        log(LogLevel.DEBUG, "debug", tag, "Map data", data)
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  API LOGGING (DEDICATED)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Log a complete API call (request + response) using an [ApiLogEntry].
     *
     * Sensitive headers are automatically redacted based on [LoggerConfig.sensitiveHeaders].
     *
     * @param apiLog Complete API log entry.
     */
    @JvmStatic
    fun api(apiLog: ApiLogEntry) {
        ensureInitialized()

        val redactedEntry = apiLog.copy(
            request = apiLog.request.copy(
                headers = HeaderRedactor.redact(apiLog.request.headers, config.sensitiveHeaders)
            ),
            response = apiLog.response?.copy(
                headers = HeaderRedactor.redact(
                    apiLog.response.headers,
                    config.sensitiveHeaders
                )
            )
        )

        val extras = mapOf(
            "request" to mapOf(
                "url" to redactedEntry.request.url,
                "method" to redactedEntry.request.method,
                "headers" to redactedEntry.request.headers,
                "query_params" to redactedEntry.request.queryParams,
                "body" to redactedEntry.request.body,
                "content_type" to redactedEntry.request.contentType,
                "timestamp" to redactedEntry.request.timestamp
            ),
            "response" to redactedEntry.response?.let { resp ->
                mapOf(
                    "status_code" to resp.statusCode,
                    "status_message" to resp.statusMessage,
                    "headers" to resp.headers,
                    "body" to resp.body,
                    "content_type" to resp.contentType,
                    "content_length" to resp.contentLength,
                    "timestamp" to resp.timestamp
                )
            },
            "duration_ms" to redactedEntry.durationMs,
            "error" to redactedEntry.error
        )

        val statusCode = redactedEntry.response?.statusCode
        val statusInfo = if (statusCode != null) " [$statusCode]" else ""
        val message = "${redactedEntry.request.method} ${redactedEntry.request.url}$statusInfo"

        logDirect(LogLevel.INFO, "api", "API", message, extras)

        if (config.enableLocalLogcat) {
            Log.i(TAG, "API: $message (${redactedEntry.durationMs}ms)")
        }
    }

    /**
     * Log an outgoing **API request** independently.
     *
     * @param url Request URL.
     * @param method HTTP method.
     * @param headers Request headers (sensitive ones will be redacted).
     * @param body Request body.
     * @param params Query parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun apiRequest(
        url: String,
        method: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        params: Map<String, String>? = null
    ) {
        ensureInitialized()

        val redactedHeaders = HeaderRedactor.redact(headers, config.sensitiveHeaders)
        val extras = mapOf<String, Any?>(
            "direction" to "request",
            "url" to url,
            "method" to method,
            "headers" to redactedHeaders,
            "query_params" to params,
            "body" to body
        )

        logDirect(LogLevel.INFO, "api", "API.Request", "$method $url", extras)
    }

    /**
     * Log an incoming **API response** independently.
     *
     * @param url Response URL (same as the request URL).
     * @param statusCode HTTP status code.
     * @param headers Response headers.
     * @param body Response body.
     * @param durationMs Round-trip duration in milliseconds.
     */
    @JvmStatic
    @JvmOverloads
    fun apiResponse(
        url: String,
        statusCode: Int,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        durationMs: Long = 0
    ) {
        ensureInitialized()

        val redactedHeaders = HeaderRedactor.redact(headers, config.sensitiveHeaders)
        val extras = mapOf<String, Any?>(
            "direction" to "response",
            "url" to url,
            "status_code" to statusCode,
            "headers" to redactedHeaders,
            "body" to body,
            "duration_ms" to durationMs
        )

        logDirect(
            LogLevel.INFO, "api", "API.Response",
            "$url [$statusCode] (${durationMs}ms)", extras
        )
    }

    /**
     * Convenience method to log a complete API request and response in one call.
     *
     * @param request Outgoing request data.
     * @param response Incoming response data.
     * @param durationMs Round-trip duration in milliseconds.
     */
    @JvmStatic
    fun apiCall(request: ApiRequestData, response: ApiResponseData, durationMs: Long) {
        api(ApiLogEntry(request = request, response = response, durationMs = durationMs))
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CRASH LOGGING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Log a **crash** / uncaught exception.
     *
     * Called automatically by [CrashHandler] when [LoggerConfig.enableCrashLogging]
     * is enabled. Can also be called manually for caught exceptions you want
     * to treat as crash-level severity.
     *
     * @param throwable The exception to log.
     * @param extras Optional extra data to attach.
     */
    @JvmStatic
    @JvmOverloads
    fun crash(throwable: Throwable, extras: Map<String, Any?>? = null) {
        val enrichedExtras = buildThrowableExtras(throwable, extras).orEmpty().toMutableMap()
        enrichedExtras["is_fatal"] = true

        logDirect(
            LogLevel.CRITICAL,
            "crash",
            "CRASH",
            "${throwable.javaClass.name}: ${throwable.message}",
            enrichedExtras
        )

        if (config.enableLocalLogcat) {
            Log.e(TAG, "CRASH: ${throwable.message}", throwable)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LIFECYCLE & CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update the user ID attached to subsequent log entries.
     *
     * Useful after user login/logout to correlate logs to a specific user.
     *
     * @param userId New user identifier.
     */
    @JvmStatic
    fun setUserId(userId: String) {
        ensureInitialized()
        enricher.setUserId(userId)
        i(TAG, "User ID updated", mapOf("user_id" to userId))
    }

    /**
     * Add a custom identifier that will be attached to all subsequent log entries.
     *
     * @param key Identifier key.
     * @param value Identifier value.
     */
    @JvmStatic
    fun addIdentifier(key: String, value: String) {
        ensureInitialized()
        enricher.addIdentifier(key, value)
    }

    /**
     * Force-flush the offline log queue.
     *
     * Normally, flushing happens automatically when connectivity is restored.
     * This method allows manual triggering (e.g., before app logout).
     */
    @JvmStatic
    fun flush() {
        ensureInitialized()
        dispatcher.flushQueue()
    }

    /**
     * Change the minimum log level for remote pushing at runtime.
     *
     * Logs below this level will only go to Logcat (if enabled) and won't
     * be pushed to Firebase.
     *
     * @param level New minimum log level.
     */
    @JvmStatic
    fun setMinLogLevel(level: LogLevel) {
        ensureInitialized()
        config = config.copy(minLogLevel = level)
    }

    /**
     * Get the current session ID.
     */
    @JvmStatic
    fun getSessionId(): String {
        ensureInitialized()
        return sessionId
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INTERNAL
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Core logging method. Handles Logcat mirroring, level filtering,
     * enrichment, and dispatch.
     */
    private fun log(
        level: LogLevel,
        logType: String,
        tag: String,
        message: String,
        extras: Map<String, Any?>? = null
    ) {
        ensureInitialized()

        // Mirror to Logcat
        if (config.enableLocalLogcat) {
            logToLogcat(level, tag, message)
        }

        // Check minimum level filter
        if (level.priority < config.minLogLevel.priority) return

        logDirect(level, logType, tag, message, extras)
    }

    /**
     * Directly enrich and dispatch a log entry (bypasses Logcat and level check).
     * Used by API and crash methods that handle their own Logcat output.
     */
    private fun logDirect(
        level: LogLevel,
        logType: String,
        tag: String,
        message: String,
        extras: Map<String, Any?>? = null
    ) {
        if (!isInitialized) return

        val entry = enricher.enrich(
            logLevel = level,
            logType = logType,
            tag = tag,
            message = message,
            extras = extras
        )
        dispatcher.dispatch(entry)
    }

    /**
     * Mirror log output to Android Logcat at the appropriate level.
     */
    private fun logToLogcat(level: LogLevel, tag: String, message: String) {
        val logcatTag = "$TAG.$tag"
        when (level) {
            LogLevel.DEBUG -> Log.d(logcatTag, message)
            LogLevel.INFO -> Log.i(logcatTag, message)
            LogLevel.WARNING -> Log.w(logcatTag, message)
            LogLevel.ERROR -> Log.e(logcatTag, message)
            LogLevel.CRITICAL -> Log.wtf(logcatTag, message)
        }
    }

    /**
     * Build extras map enriched with throwable stack trace if present.
     */
    private fun buildThrowableExtras(
        throwable: Throwable?,
        existingExtras: Map<String, Any?>?
    ): Map<String, Any?>? {
        if (throwable == null) return existingExtras

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        val throwableData = mapOf<String, Any?>(
            "exception_class" to throwable.javaClass.name,
            "exception_message" to throwable.message,
            "stack_trace" to sw.toString(),
            "cause" to throwable.cause?.let { "${it.javaClass.name}: ${it.message}" }
        )

        return if (existingExtras != null) {
            existingExtras + throwableData
        } else {
            throwableData
        }
    }

    /**
     * Ensures the logger is initialized before any operation.
     */
    private fun ensureInitialized() {
        if (!isInitialized) {
            throw LoggerException(LoggerException.ERROR_NOT_INITIALIZED)
        }
    }
}

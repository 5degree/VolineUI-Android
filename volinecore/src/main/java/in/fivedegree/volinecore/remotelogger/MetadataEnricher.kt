package `in`.fivedegree.volinecore.remotelogger

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Enriches raw log data with device metadata, session info, timestamps,
 * and user identifiers to produce a fully-formed [LogEntry].
 *
 * This is an internal component — callers interact with [VolineLogger] only.
 */
internal class MetadataEnricher(
    private val config: LoggerConfig,
    private val sessionId: String
) {

    @Volatile
    private var userId: String? = config.userId

    private val customIdentifiers: MutableMap<String, String> =
        config.customIdentifiers.toMutableMap()

    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    /**
     * Update the user ID that gets attached to subsequent log entries.
     */
    fun setUserId(userId: String) {
        this.userId = userId
    }

    /**
     * Add or update a custom identifier attached to subsequent log entries.
     */
    fun addIdentifier(key: String, value: String) {
        customIdentifiers[key] = value
    }

    /**
     * Enriches raw log parameters into a complete [LogEntry].
     *
     * @param logLevel Severity level.
     * @param logType Firebase child key (e.g., "debug", "api").
     * @param tag Developer-provided tag.
     * @param message Log message.
     * @param extras Optional extra data.
     */
    fun enrich(
        logLevel: LogLevel,
        logType: String,
        tag: String,
        message: String,
        extras: Map<String, Any?>? = null
    ): LogEntry {
        val now = System.currentTimeMillis()
        return LogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = iso8601Format.format(Date(now)),
            timestampMillis = now,
            logLevel = logLevel.name,
            logType = logType,
            tag = tag,
            message = message,
            extras = extras,
            deviceInfo = DeviceInfoCollector.collect(),
            sessionId = sessionId,
            appId = config.appId,
            userId = userId,
            customIdentifiers = customIdentifiers.toMap()
        )
    }
}

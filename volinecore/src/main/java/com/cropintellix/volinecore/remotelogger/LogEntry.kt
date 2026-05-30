package com.cropintellix.volinecore.remotelogger

/**
 * Internal representation of a fully enriched log entry.
 *
 * Created by [MetadataEnricher] and consumed by [LogDispatcher].
 * This is the complete data model that gets serialized to JSON
 * and pushed to Firebase Realtime Database.
 *
 * @property id Unique UUID for this log entry.
 * @property timestamp ISO 8601 formatted timestamp.
 * @property timestampMillis Unix epoch milliseconds for sorting.
 * @property logLevel Severity level of this log.
 * @property logType Firebase child path key (e.g., "debug", "api", "crash").
 * @property tag Developer-provided tag for categorizing the log source.
 * @property message Human-readable log message.
 * @property extras Optional arbitrary key-value data attached by the developer.
 * @property deviceInfo Auto-collected device and app metadata.
 * @property sessionId Unique identifier for the current app session.
 * @property appId Application identifier from [LoggerConfig].
 * @property userId Optional user identifier.
 * @property customIdentifiers Additional identifiers from [LoggerConfig].
 */
internal data class LogEntry(
    val id: String,
    val timestamp: String,
    val timestampMillis: Long,
    val logLevel: String,
    val logType: String,
    val tag: String,
    val message: String,
    val extras: Map<String, Any?>? = null,
    val deviceInfo: DeviceInfo,
    val sessionId: String,
    val appId: String,
    val userId: String? = null,
    val customIdentifiers: Map<String, String> = emptyMap()
)

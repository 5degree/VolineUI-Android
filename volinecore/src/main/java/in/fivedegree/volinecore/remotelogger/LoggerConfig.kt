@file:Suppress("unused")

package `in`.fivedegree.volinecore.remotelogger

/**
 * Configuration for the remote logger.
 *
 * Passed once during [VolineLogger.init] to configure behavior, identifiers,
 * and filtering for the entire logging session.
 *
 * Example:
 * ```kotlin
 * VolineLogger.init(application, LoggerConfig(
 *     appId = "my-app",
 *     userId = "user_123",
 *     customIdentifiers = mapOf("env" to "staging"),
 *     minLogLevel = LogLevel.DEBUG
 * ))
 * ```
 *
 * @property appId Unique application identifier (e.g., package name or a custom slug).
 *                 Used as the top-level Firebase path key.
 * @property userId Optional user identifier for correlating logs to a specific user.
 *                  Can be updated later via [VolineLogger.setUserId].
 * @property customIdentifiers Additional key-value pairs attached to every log entry
 *                             (e.g., environment, organization, build flavor).
 * @property minLogLevel Minimum severity level for remote logging. Logs below this
 *                       level are still printed to Logcat (if enabled) but not pushed.
 * @property enableLocalLogcat Whether to mirror all log calls to Android Logcat.
 * @property enableCrashLogging Whether to automatically capture uncaught exceptions.
 * @property enableOfflineQueue Whether to queue logs in a local Room database when offline.
 * @property databaseReference Root Firebase Realtime Database path for all logs.
 * @property maxOfflineQueueSize Maximum number of log entries to retain in the offline queue.
 *                               Oldest entries are purged when the limit is exceeded.
 * @property sensitiveHeaders Set of HTTP header names whose values will be redacted
 *                            in API log entries (case-insensitive comparison).
 */
data class LoggerConfig(
    val appId: String,
    val userId: String? = null,
    val customIdentifiers: Map<String, String> = emptyMap(),
    val minLogLevel: LogLevel = LogLevel.DEBUG,
    val enableLocalLogcat: Boolean = false,
    val enableCrashLogging: Boolean = false,
    val enableOfflineQueue: Boolean = true,
    val databaseReference: String = "logs",
    val maxOfflineQueueSize: Int = 1000,
    val sensitiveHeaders: Set<String> = setOf(
        "Authorization",
        "Cookie",
        "Set-Cookie",
        "X-Api-Key",
        "X-Auth-Token",
        "Proxy-Authorization"
    )
) {
    init {
        require(appId.isNotBlank()) { "appId must not be blank" }
        require(maxOfflineQueueSize > 0) { "maxOfflineQueueSize must be > 0" }
        require(databaseReference.isNotBlank()) { "databaseReference must not be blank" }
    }
}

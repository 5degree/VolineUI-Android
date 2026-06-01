@file:Suppress("unused")

package `in`.fivedegree.volinecore.remotelogger

/**
 * Defines the severity levels for log entries.
 *
 * Used for both categorizing logs and filtering (via [LoggerConfig.minLogLevel]).
 * Logs with a priority below the configured minimum level are silently dropped.
 *
 * @property priority Numeric priority used for level comparison.
 *                    Higher values represent more severe/important logs.
 */
enum class LogLevel(val priority: Int) {
    /** Fine-grained debug information for development */
    DEBUG(0),

    /** General informational messages */
    INFO(1),

    /** Potentially harmful situations or noteworthy events */
    WARNING(2),

    /** Error conditions that might still allow the app to continue */
    ERROR(3),

    /** Critical failures — "What a Terrible Failure" */
    CRITICAL(4);

    companion object {
        /**
         * Returns the [LogLevel] matching the given name (case-insensitive),
         * or [DEBUG] if no match is found.
         */
        @JvmStatic
        fun fromName(name: String): LogLevel =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEBUG
    }
}

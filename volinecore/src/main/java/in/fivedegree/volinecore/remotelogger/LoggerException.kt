@file:Suppress("unused")

package `in`.fivedegree.volinecore.remotelogger

/**
 * Exception thrown by the remote logging system for configuration
 * and lifecycle errors.
 *
 * @param message Human-readable description of the error.
 */
class LoggerException(message: String) : RuntimeException(message) {

    companion object {
        /** Thrown when [VolineLogger.instance] is accessed before [VolineLogger.init]. */
        const val ERROR_NOT_INITIALIZED =
            "VolineLogger is not initialized. Call VolineLogger.init(application, config) " +
                    "in your Application.onCreate() first."

        /** Thrown when [VolineLogger.init] is called with an invalid configuration. */
        const val ERROR_INVALID_CONFIG =
            "Invalid LoggerConfig provided. Ensure appId is not blank."

        /** Thrown when Firebase Realtime Database is unreachable and offline queue is disabled. */
        const val ERROR_FIREBASE_UNAVAILABLE =
            "Firebase Realtime Database is not available and offline queue is disabled."
    }
}

package `in`.fivedegree.volinecore.remotelogger

import android.util.Log

/**
 * Captures uncaught exceptions and logs them via [VolineLogger.crash]
 * before delegating to the previous default handler.
 *
 * This ensures that the app still crashes normally (so crash reporters
 * like Firebase Crashlytics continue to function), while also pushing
 * the crash data to Firebase Realtime Database.
 *
 * Installed automatically when [LoggerConfig.enableCrashLogging] is `true`.
 */
internal class CrashHandler : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "VolineLogger.Crash"
    }

    private val previousHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    /**
     * Install this handler as the default uncaught exception handler.
     */
    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Attempt to log the crash — use a blocking approach since
            // the process is about to terminate
            VolineLogger.crash(
                throwable = throwable,
                extras = mapOf(
                    "thread_name" to thread.name,
                    "thread_id" to thread.id,
                    "is_main_thread" to (thread == android.os.Looper.getMainLooper().thread)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log crash to remote logger", e)
        }

        // Delegate to the previous handler (Firebase Crashlytics, etc.)
        previousHandler?.uncaughtException(thread, throwable)
    }
}

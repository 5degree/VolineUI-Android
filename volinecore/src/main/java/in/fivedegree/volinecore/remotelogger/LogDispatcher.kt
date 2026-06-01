package `in`.fivedegree.volinecore.remotelogger

import android.app.Application
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import `in`.fivedegree.volinecore.remotelogger.db.LogDatabase
import `in`.fivedegree.volinecore.remotelogger.db.LogQueueEntity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Responsible for pushing [LogEntry] data to Firebase Realtime Database
 * with automatic offline fallback to Room.
 *
 * ## Lifecycle
 * 1. Receives enriched [LogEntry] via [dispatch].
 * 2. Attempts to push to Firebase at path `/{databaseRef}/{appId}/{logType}/{pushId}`.
 * 3. On failure (offline/error): queues in Room if [LoggerConfig.enableOfflineQueue] is true.
 * 4. When connectivity is restored: [flushQueue] drains all pending entries.
 *
 * All operations run on [Dispatchers.IO] within a [SupervisorJob] scope,
 * so individual failures don't cancel sibling dispatches.
 */
internal class LogDispatcher(
    private val application: Application,
    private val config: LoggerConfig,
    private val connectivityObserver: ConnectivityObserver
) {

    companion object {
        private const val TAG = "VolineLogger.Dispatch"
        private const val FLUSH_BATCH_SIZE = 50
    }

    private val gson = Gson()

    private val dao by lazy { LogDatabase.getInstance(application).logQueueDao() }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Dispatch coroutine failed", throwable)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    @Volatile
    private var isFlushing = false

    /**
     * Dispatch a log entry to Firebase (or queue it offline).
     *
     * This is fire-and-forget — returns immediately.
     * The actual push happens on a background coroutine.
     */
    fun dispatch(entry: LogEntry) {
        scope.launch {
            try {
                if (connectivityObserver.isOnline()) {
                    val success = pushToFirebase(entry)
                    if (!success && config.enableOfflineQueue) {
                        enqueueLocally(entry)
                    }
                } else if (config.enableOfflineQueue) {
                    enqueueLocally(entry)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch log entry: ${entry.id}", e)
                if (config.enableOfflineQueue) {
                    try {
                        enqueueLocally(entry)
                    } catch (qe: Exception) {
                        Log.e(TAG, "Failed to queue log locally: ${entry.id}", qe)
                    }
                }
            }
        }
    }

    /**
     * Flush all queued log entries to Firebase.
     *
     * Called automatically by [ConnectivityObserver] when the device comes back online.
     * Can also be called manually via [VolineLogger.flush].
     */
    fun flushQueue() {
        if (!config.enableOfflineQueue || isFlushing) return

        scope.launch {
            if (isFlushing) return@launch
            isFlushing = true

            try {
                while (true) {
                    val batch = dao.getOldest(FLUSH_BATCH_SIZE)
                    if (batch.isEmpty()) break

                    for (entity in batch) {
                        try {
                            val success = pushJsonToFirebase(
                                jsonPayload = entity.jsonPayload,
                                appId = entity.appId,
                                logType = entity.logType
                            )
                            if (success) {
                                dao.deleteById(entity.id)
                            } else {
                                // If push fails, stop flushing — we're probably offline again
                                return@launch
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to flush queued entry: ${entity.id}", e)
                            return@launch
                        }
                    }
                }
            } finally {
                isFlushing = false
            }
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────

    /**
     * Push a [LogEntry] to Firebase Realtime Database.
     *
     * @return `true` if the push was confirmed successful.
     */
    private suspend fun pushToFirebase(entry: LogEntry): Boolean {
        val entryMap = gson.fromJson(gson.toJson(entry), Map::class.java)
        return pushMapToFirebase(entryMap, entry.appId, entry.logType)
    }

    /**
     * Push a pre-serialized JSON payload to Firebase.
     * Used when flushing the offline queue.
     */
    private suspend fun pushJsonToFirebase(
        jsonPayload: String,
        appId: String,
        logType: String
    ): Boolean {
        val entryMap = gson.fromJson(jsonPayload, Map::class.java)
        return pushMapToFirebase(entryMap, appId, logType)
    }

    /**
     * Core Firebase push operation.
     *
     * Writes to: `/{databaseRef}/{appId}/{logType}/{auto-generated push key}`
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun pushMapToFirebase(
        data: Map<*, *>,
        appId: String,
        logType: String
    ): Boolean = suspendCoroutine { continuation ->
        try {
            val ref = FirebaseDatabase.getInstance()
                .getReference(config.databaseReference)
                .child(appId)
                .child(logType)
                .push()

            ref.setValue(data as Map<String, Any?>)
                .addOnSuccessListener { continuation.resume(true) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Firebase push failed for $logType", e)
                    continuation.resume(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase reference error", e)
            continuation.resume(false)
        }
    }

    /**
     * Store a log entry in the local Room queue for later sync.
     */
    private suspend fun enqueueLocally(entry: LogEntry) {
        val entity = LogQueueEntity(
            id = entry.id,
            jsonPayload = gson.toJson(entry),
            logType = entry.logType,
            appId = entry.appId,
            createdAt = entry.timestampMillis
        )
        dao.insert(entity)

        // Trim queue if it exceeds the configured max size
        val count = dao.count()
        if (count > config.maxOfflineQueueSize) {
            dao.trimToSize(config.maxOfflineQueueSize)
        }
    }
}

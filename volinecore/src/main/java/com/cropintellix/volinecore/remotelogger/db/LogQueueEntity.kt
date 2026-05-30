package com.cropintellix.volinecore.remotelogger.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a log entry queued for offline sync.
 *
 * When Firebase is unreachable, [LogDispatcher] serializes a [LogEntry]
 * to JSON and stores it here. On connectivity restoration, queued entries
 * are pushed to Firebase and then deleted.
 *
 * @property id Unique identifier (same UUID as the original LogEntry).
 * @property jsonPayload Complete LogEntry serialized as JSON string.
 * @property logType Firebase child path key (e.g., "debug", "api").
 * @property appId Application identifier for Firebase path routing.
 * @property createdAt Epoch millis when the entry was queued.
 * @property retryCount Number of failed push attempts for this entry.
 */
@Entity(tableName = "log_queue")
data class LogQueueEntity(
    @PrimaryKey
    val id: String,
    val jsonPayload: String,
    val logType: String,
    val appId: String,
    val createdAt: Long,
    val retryCount: Int = 0
)

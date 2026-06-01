package `in`.fivedegree.volinecore.remotelogger.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for the offline log queue.
 *
 * Provides operations to insert, retrieve, and manage queued log entries
 * that couldn't be pushed to Firebase due to network unavailability.
 */
@Dao
internal interface LogQueueDao {

    /**
     * Insert a single log entry into the queue.
     * If an entry with the same ID already exists, it is replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LogQueueEntity)

    /**
     * Retrieve all queued entries ordered by creation time (oldest first).
     */
    @Query("SELECT * FROM log_queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<LogQueueEntity>

    /**
     * Retrieve the oldest [limit] entries for batch processing.
     */
    @Query("SELECT * FROM log_queue ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getOldest(limit: Int): List<LogQueueEntity>

    /**
     * Delete a single entry by its ID (after successful push).
     */
    @Query("DELETE FROM log_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Delete all entries from the queue.
     */
    @Query("DELETE FROM log_queue")
    suspend fun deleteAll()

    /**
     * Get the total count of queued entries.
     */
    @Query("SELECT COUNT(*) FROM log_queue")
    suspend fun count(): Int

    /**
     * Delete entries older than the given timestamp.
     * Used for queue size management.
     */
    @Query("DELETE FROM log_queue WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Delete the oldest entries to keep the queue at or below [maxSize].
     */
    @Query(
        """
        DELETE FROM log_queue WHERE id IN (
            SELECT id FROM log_queue ORDER BY createdAt ASC
            LIMIT MAX(0, (SELECT COUNT(*) FROM log_queue) - :maxSize)
        )
        """
    )
    suspend fun trimToSize(maxSize: Int)
}

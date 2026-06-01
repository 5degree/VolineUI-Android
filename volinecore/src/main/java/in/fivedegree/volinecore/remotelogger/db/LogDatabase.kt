package `in`.fivedegree.volinecore.remotelogger.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for the offline log queue.
 *
 * Holds a single table ([LogQueueEntity]) for log entries that failed
 * to push to Firebase due to network issues.
 *
 * Uses a singleton pattern to ensure only one database instance exists.
 */
@Database(entities = [LogQueueEntity::class], version = 1, exportSchema = false)
internal abstract class LogDatabase : RoomDatabase() {

    abstract fun logQueueDao(): LogQueueDao

    companion object {
        @Volatile
        private var INSTANCE: LogDatabase? = null

        /**
         * Returns the singleton [LogDatabase] instance, creating it if necessary.
         *
         * @param context Application context (avoids activity/fragment leaks).
         */
        fun getInstance(context: Context): LogDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LogDatabase::class.java,
                    "voline_remote_log_queue.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

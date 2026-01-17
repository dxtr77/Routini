package com.dxtr.routini.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Routine::class, RoutineTask::class, StandaloneTask::class, RoutineHistory::class],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun standaloneTaskDao(): StandaloneTaskDao
    abstract fun routineHistoryDao(): RoutineHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "routini_database"
                )
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE routine_history ADD COLUMN taskType TEXT NOT NULL DEFAULT 'ROUTINE'")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS index_routine_tasks_routineId ON routine_tasks (routineId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_routine_history_taskId ON routine_history (taskId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_routine_history_completionDate ON routine_history (completionDate)")
            }
        }
    }
}
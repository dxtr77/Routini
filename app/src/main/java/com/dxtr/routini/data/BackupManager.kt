package com.dxtr.routini.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class BackupManager(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
        .create()

    data class FullBackupData(
        val routines: List<Routine>,
        val routineTasks: List<RoutineTask>,
        val standaloneTasks: List<StandaloneTask>,
        val history: List<RoutineHistory>
    )

    data class SingleRoutineBackup(
        val routine: Routine,
        val tasks: List<RoutineTask>
    )

    suspend fun exportData(uri: Uri) = withContext(Dispatchers.IO) {
        val routines = db.routineDao().getAllRoutinesSuspend()
        val routineTasks = db.routineDao().getAllRoutineTasks()
        val standaloneTasks = db.standaloneTaskDao().getAllStandaloneTasksSuspend()
        val history = db.routineHistoryDao().getAllHistorySuspend()

        val backup = FullBackupData(routines, routineTasks, standaloneTasks, history)
        val json = gson.toJson(backup)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(json)
            }
        }
    }

    suspend fun importData(uri: Uri) = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            InputStreamReader(inputStream).readText()
        } ?: return@withContext

        val backup = gson.fromJson(json, FullBackupData::class.java)

        db.withTransaction {
            val supportDb = db.openHelper.writableDatabase
            supportDb.execSQL("DELETE FROM routine_history")
            supportDb.execSQL("DELETE FROM standalone_tasks")
            supportDb.execSQL("DELETE FROM routine_tasks")
            supportDb.execSQL("DELETE FROM routines")

            for (routine in backup.routines) {
                db.routineDao().insertRoutine(routine)
            }
            for (task in backup.routineTasks) {
                db.routineDao().insertRoutineTask(task)
            }
            for (task in backup.standaloneTasks) {
                db.standaloneTaskDao().insertStandaloneTask(task)
            }
            for (item in backup.history) {
                db.routineHistoryDao().insert(item)
            }
        }
    }

    suspend fun exportRoutine(routineId: Int, uri: Uri) = withContext(Dispatchers.IO) {
        val routine = db.routineDao().getRoutineById(routineId) ?: return@withContext
        val tasks = db.routineDao().getTasksForRoutineSuspend(routineId)
        
        val backup = SingleRoutineBackup(routine, tasks)
        val json = gson.toJson(backup)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(json)
            }
        }
    }

    private class LocalDateAdapter : TypeAdapter<LocalDate>() {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        override fun write(out: JsonWriter, value: LocalDate?) {
            if (value == null) {
                out.nullValue()
            } else {
                out.value(value.format(formatter))
            }
        }
        override fun read(reader: JsonReader): LocalDate? {
            val s = reader.nextString()
            return if (s == null) null else LocalDate.parse(s, formatter)
        }
    }

    private class LocalTimeAdapter : TypeAdapter<LocalTime>() {
        private val formatter = DateTimeFormatter.ISO_LOCAL_TIME
        override fun write(out: JsonWriter, value: LocalTime?) {
            if (value == null) {
                out.nullValue()
            } else {
                out.value(value.format(formatter))
            }
        }
        override fun read(reader: JsonReader): LocalTime? {
            val s = reader.nextString()
            return if (s == null) null else LocalTime.parse(s, formatter)
        }
    }
}

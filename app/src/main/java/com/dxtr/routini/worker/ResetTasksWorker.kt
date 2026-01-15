package com.dxtr.routini.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dxtr.routini.data.AppDatabase

class ResetTasksWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            db.routineDao().resetAllTasks()
            db.standaloneTaskDao().resetAllTasks()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

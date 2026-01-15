package com.dxtr.routini.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MidnightResetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // No-op. The isDone state is now calculated dynamically.
        return Result.success()
    }
}

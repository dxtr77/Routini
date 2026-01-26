package com.dxtr.routini

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dxtr.routini.worker.ResetTasksWorker
import java.util.concurrent.TimeUnit

class RoutiniApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupResetTasksWorker()
    }

    private fun setupResetTasksWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .build()

        val resetRequest = PeriodicWorkRequestBuilder<ResetTasksWorker>(
            1, TimeUnit.DAYS
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ResetTasksWorker",
            ExistingPeriodicWorkPolicy.KEEP, 
            resetRequest
        )
    }
}
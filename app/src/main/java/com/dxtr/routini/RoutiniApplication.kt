package com.dxtr.routini

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dxtr.routini.worker.MidnightResetWorker
import java.util.concurrent.TimeUnit

class RoutiniApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupMidnightResetWorker()
    }

    private fun setupMidnightResetWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .build()

        val midnightRequest = PeriodicWorkRequestBuilder<MidnightResetWorker>(
            24, TimeUnit.HOURS
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MidnightResetWorker",
            ExistingPeriodicWorkPolicy.KEEP, 
            midnightRequest
        )
    }
}
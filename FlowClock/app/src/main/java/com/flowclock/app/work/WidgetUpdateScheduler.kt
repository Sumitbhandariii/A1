package com.flowclock.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetUpdateScheduler {
    private const val WORK_NAME = "flowclock_widget_refresh"

    /** 15 minutes is the minimum interval WorkManager honors for periodic work. */
    fun schedule(context: Context) {
        val constraints = Constraints.Builder().build()
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

package com.flowclock.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flowclock.app.widget.HabitWidgetProvider

/**
 * Runs every 15 minutes via WorkManager (never a foreground service or exact
 * alarm) to refresh widget content: applies the daily reset and keeps the
 * progress bar / remaining-count accurate even if the app stays closed.
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            HabitWidgetProvider.refreshAllWidgets(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

package com.flowclock.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.flowclock.app.MainActivity
import com.flowclock.app.R
import com.flowclock.app.data.HabitDao
import com.flowclock.app.data.HabitDatabase
import com.flowclock.app.data.HabitEntity
import com.flowclock.app.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AppWidgetProvider for FlowClock's home-screen widget.
 *
 * Live updates come from three sources, none of which require restricted
 * permissions:
 *  1. The main app calling [refreshAllWidgets] right after a data change.
 *  2. A tap on a habit row, delivered here as [ACTION_TOGGLE_HABIT] via a
 *     PendingIntent template on the widget's ListView.
 *  3. A WorkManager PeriodicWorkRequest firing every 15 minutes
 *     (see WidgetUpdateWorker), which keeps the "remaining tasks" count and
 *     the daily reset accurate even if the app is never opened.
 */
class HabitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_HABIT -> handleToggle(context, intent)
            ACTION_REFRESH_WIDGET -> refreshAllWidgets(context)
        }
    }

    /**
     * Handles a tap on a single habit row. Runs the DB write inside goAsync()
     * so the BroadcastReceiver is kept alive long enough to finish safely,
     * without needing a foreground service.
     */
    private fun handleToggle(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = HabitDatabase.getInstance(context).habitDao()
                val habit = dao.getHabitByIdSync(habitId)
                if (habit != null) {
                    val updated = habit.copy(
                        isCompletedToday = !habit.isCompletedToday,
                        lastCompletedDate = DateUtils.today()
                    )
                    dao.updateSync(updated)
                }
                withContext(Dispatchers.Main) {
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java))
                    manager.notifyAppWidgetViewDataChanged(ids, R.id.list_habits)
                    for (id in ids) updateAppWidget(context, manager, id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_HABIT = "com.flowclock.app.ACTION_TOGGLE_HABIT"
        const val ACTION_REFRESH_WIDGET = "com.flowclock.app.ACTION_REFRESH_WIDGET"
        const val EXTRA_HABIT_ID = "extra_habit_id"

        /** Called from the app (after add/edit/delete) and from WorkManager. */
        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java))
            if (ids.isEmpty()) return
            manager.notifyAppWidgetViewDataChanged(ids, R.id.list_habits)
            for (id in ids) updateAppWidget(context, manager, id)
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val dao = HabitDatabase.getInstance(context).habitDao()
            val habits = dao.getHabitsSync().map { resetIfStale(it, dao) }
            val total = habits.size
            val completed = habits.count { it.isCompletedToday }
            val remaining = total - completed

            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            views.setTextViewText(R.id.text_date, dateFormat.format(Date()))

            if (total == 0) {
                views.setViewVisibility(R.id.list_habits, View.GONE)
                views.setViewVisibility(R.id.empty_view, View.VISIBLE)
                views.setTextViewText(R.id.text_remaining, context.getString(R.string.widget_no_habits))
                views.setProgressBar(R.id.progress_bar, 100, 0, false)
            } else {
                views.setViewVisibility(R.id.list_habits, View.VISIBLE)
                views.setViewVisibility(R.id.empty_view, View.GONE)

                val remainingText = context.resources.getQuantityString(
                    R.plurals.tasks_remaining, remaining, remaining
                )
                views.setTextViewText(R.id.text_remaining, remainingText)
                views.setProgressBar(R.id.progress_bar, total, completed, false)

                // RemoteViewsService supplies the actual rows (see HabitWidgetService).
                val serviceIntent = Intent(context, HabitWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("content://com.flowclock.app.widget/$appWidgetId")
                }
                views.setRemoteAdapter(R.id.list_habits, serviceIntent)
                views.setEmptyView(R.id.list_habits, R.id.empty_view)

                // A single PendingIntent template + per-row fillInIntent lets every
                // row toggle independently without allocating one PendingIntent each.
                val toggleTemplateIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_HABIT
                }
                val togglePendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    toggleTemplateIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setPendingIntentTemplate(R.id.list_habits, togglePendingIntent)
            }

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.button_add, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.text_title, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /** Lazily rolls a habit's "completed today" flag back to false on a new day. */
        private fun resetIfStale(habit: HabitEntity, dao: HabitDao): HabitEntity {
            val today = DateUtils.today()
            return if (habit.isCompletedToday && habit.lastCompletedDate != today) {
                val reset = habit.copy(isCompletedToday = false)
                dao.updateSync(reset)
                reset
            } else habit
        }
    }
}

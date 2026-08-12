package com.flowclock.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.flowclock.app.R
import com.flowclock.app.data.HabitDao
import com.flowclock.app.data.HabitDatabase
import com.flowclock.app.data.HabitEntity
import com.flowclock.app.util.DateUtils

class HabitRemoteViewsFactory(
    private val context: Context,
    @Suppress("UNUSED_PARAMETER") private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var habits: List<HabitEntity> = emptyList()
    private val dao: HabitDao by lazy { HabitDatabase.getInstance(context).habitDao() }

    override fun onCreate() {
        // No-op: data is loaded in onDataSetChanged, called before the first getViewAt.
    }

    override fun onDataSetChanged() {
        val today = DateUtils.today()
        habits = dao.getHabitsSync().map { habit ->
            if (habit.isCompletedToday && habit.lastCompletedDate != today) {
                val reset = habit.copy(isCompletedToday = false)
                dao.updateSync(reset)
                reset
            } else habit
        }
    }

    override fun onDestroy() {
        habits = emptyList()
    }

    override fun getCount(): Int = habits.size

    override fun getViewAt(position: Int): RemoteViews {
        val habit = habits[position]
        val views = RemoteViews(context.packageName, R.layout.widget_habit_item)

        views.setTextViewText(R.id.item_habit_name, habit.name)

        val tagColor = try {
            Color.parseColor(habit.colorHex)
        } catch (e: IllegalArgumentException) {
            Color.parseColor("#6750A4")
        }
        views.setInt(R.id.item_color_tag, "setBackgroundColor", tagColor)

        val iconRes = if (habit.isCompletedToday) {
            R.drawable.ic_check_circle
        } else {
            R.drawable.ic_circle_outline
        }
        views.setImageViewResource(R.id.item_toggle_icon, iconRes)

        val paintFlags = if (habit.isCompletedToday) {
            Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
        } else {
            Paint.ANTI_ALIAS_FLAG
        }
        views.setInt(R.id.item_habit_name, "setPaintFlags", paintFlags)

        // fillInIntent is merged with the ListView's PendingIntentTemplate on click,
        // so each row can carry its own habit id without a unique PendingIntent.
        val fillInIntent = Intent().apply {
            putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, habit.id)
        }
        views.setOnClickFillInIntent(R.id.item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = habits[position].id
    override fun hasStableIds(): Boolean = true
}

package com.flowclock.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flowclock.app.work.WidgetUpdateScheduler

/** Re-arms the periodic WorkManager refresh after a device reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WidgetUpdateScheduler.schedule(context)
            HabitWidgetProvider.refreshAllWidgets(context)
        }
    }
}

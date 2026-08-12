package com.flowclock.app

import android.app.Application
import com.flowclock.app.work.WidgetUpdateScheduler

class FlowClockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Schedule the 15-minute WorkManager refresh as soon as the app process starts.
        WidgetUpdateScheduler.schedule(this)
    }
}

package com.flowclock.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    /** Returns today's date as "yyyy-MM-dd", used to detect daily habit resets. */
    fun today(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }
}

package com.dxtr.routini.utils

import android.content.Context
import android.text.format.DateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object TimeUtils {
    /**
     * Returns the appropriate time pattern based on the system's 12/24-hour setting.
     */
    fun getTimePattern(context: Context): String {
        return if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
    }

    /**
     * Formats a LocalTime object according to the system's 12/24-hour setting.
     */
    fun formatTime(context: Context, time: LocalTime): String {
        return time.format(DateTimeFormatter.ofPattern(getTimePattern(context)))
    }
}

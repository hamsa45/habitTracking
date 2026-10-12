/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.receivers.StudyReminderReceiver
import java.util.Calendar

/**
 * Schedules study reminders via AlarmManager. Cancels previous alarms before rescheduling
 * to avoid duplicates after settings changes.
 */
class StudyReminderScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAll() {
        cancelAll()
        val prefs = HabitsApplication.component.preferences
        if (!prefs.studyReminderEnabled) return

        val primary = nextTriggerMillis(prefs.studyReminderHour, prefs.studyReminderMinute)
        scheduleAlarm(REQUEST_PRIMARY, primary, StudyReminderReceiver.ACTION_PRIMARY)

        if (prefs.studySecondReminderEnabled) {
            val cal = Calendar.getInstance().apply { timeInMillis = primary }
            cal.add(Calendar.MINUTE, prefs.studySecondReminderOffsetMinutes)
            scheduleAlarm(REQUEST_SECOND, cal.timeInMillis, StudyReminderReceiver.ACTION_SECOND)
        }

        if (prefs.weeklyReviewReminderEnabled) {
            // Sunday evening at reminder time as a gentle weekly nudge
            val weekly = nextWeeklyMillis(prefs.studyReminderHour, prefs.studyReminderMinute)
            scheduleAlarm(REQUEST_WEEKLY, weekly, StudyReminderReceiver.ACTION_WEEKLY)
        }
    }

    fun cancelAll() {
        alarmManager.cancel(pendingIntent(REQUEST_PRIMARY, StudyReminderReceiver.ACTION_PRIMARY))
        alarmManager.cancel(pendingIntent(REQUEST_SECOND, StudyReminderReceiver.ACTION_SECOND))
        alarmManager.cancel(pendingIntent(REQUEST_WEEKLY, StudyReminderReceiver.ACTION_WEEKLY))
    }

    private fun scheduleAlarm(requestCode: Int, triggerAt: Long, action: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission missing; using inexact alarm")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(requestCode, action))
            return
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(requestCode, action)
        )
        Log.i(TAG, "Scheduled $action at $triggerAt")
    }

    private fun pendingIntent(requestCode: Int, action: String): PendingIntent {
        val intent = Intent(context, StudyReminderReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun nextWeeklyMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    companion object {
        private const val TAG = "StudyReminderScheduler"
        private const val REQUEST_PRIMARY = 71001
        private const val REQUEST_SECOND = 71002
        private const val REQUEST_WEEKLY = 71003
    }
}

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
package org.isoron.uhabits.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import org.isoron.platform.time.getToday
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.learning.LearningMainActivity
import org.isoron.uhabits.activities.learning.WeeklyReviewActivity
import org.isoron.uhabits.notifications.StudyReminderScheduler

class StudyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action == null) return
        Log.i(TAG, "Received ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> {
                StudyReminderScheduler(context).scheduleAll()
                return
            }
        }

        val app = context.applicationContext as HabitsApplication
        val facade = app.component.learningFacade
        val prefs = app.component.preferences

        // Reschedule next occurrence after firing
        StudyReminderScheduler(context).scheduleAll()

        when (intent.action) {
            ACTION_PRIMARY -> {
                if (!prefs.studyReminderEnabled) return
                if (!facade.weekdayAllowed(getToday().dayOfWeek)) return
                if (facade.isStudyDayCompleteForReminders()) return
                showNotification(
                    context,
                    NOTIFICATION_PRIMARY,
                    context.getString(R.string.learning_notification_title),
                    context.getString(R.string.learning_notification_body),
                    LearningMainActivity::class.java
                )
            }
            ACTION_SECOND -> {
                if (!prefs.studySecondReminderEnabled) return
                if (!facade.weekdayAllowed(getToday().dayOfWeek)) return
                if (facade.isStudyDayCompleteForReminders()) return
                showNotification(
                    context,
                    NOTIFICATION_SECOND,
                    context.getString(R.string.learning_notification_title),
                    context.getString(R.string.learning_notification_missed_body),
                    LearningMainActivity::class.java
                )
            }
            ACTION_WEEKLY -> {
                if (!prefs.weeklyReviewReminderEnabled) return
                showNotification(
                    context,
                    NOTIFICATION_WEEKLY,
                    context.getString(R.string.learning_weekly_review),
                    context.getString(R.string.learning_review_q6),
                    WeeklyReviewActivity::class.java
                )
            }
        }
    }

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        body: String,
        activityClass: Class<*>
    ) {
        ensureChannel(context)
        val open = Intent(context, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.learning_notification_title),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_PRIMARY = "org.isoron.uhabits.ACTION_STUDY_REMINDER"
        const val ACTION_SECOND = "org.isoron.uhabits.ACTION_STUDY_REMINDER_SECOND"
        const val ACTION_WEEKLY = "org.isoron.uhabits.ACTION_STUDY_WEEKLY_REVIEW"
        private const val CHANNEL_ID = "study_reminders"
        private const val NOTIFICATION_PRIMARY = 72001
        private const val NOTIFICATION_SECOND = 72002
        private const val NOTIFICATION_WEEKLY = 72003
        private const val TAG = "StudyReminderReceiver"
    }
}

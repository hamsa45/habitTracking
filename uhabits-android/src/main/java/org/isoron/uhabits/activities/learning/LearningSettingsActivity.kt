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
package org.isoron.uhabits.activities.learning

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.databinding.ActivityLearningSettingsBinding
import org.isoron.uhabits.notifications.StudyReminderScheduler
import org.isoron.uhabits.utils.applyRootViewInsets

class LearningSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearningSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearningSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyRootViewInsets()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.learning_settings)

        val prefs = HabitsApplication.component.preferences
        binding.settingsTargetMinutes.setText(prefs.dailyStudyTargetMinutes.toString())
        binding.settingsMinMinutes.setText(prefs.minStudySessionMinutes.toString())
        binding.settingsRequireNote.isChecked = prefs.requireMeaningfulNoteForCompletion
        binding.settingsReminderEnabled.isChecked = prefs.studyReminderEnabled
        binding.settingsReminderTime.hour = prefs.studyReminderHour
        binding.settingsReminderTime.minute = prefs.studyReminderMinute
        binding.settingsSecondReminder.isChecked = prefs.studySecondReminderEnabled
        binding.settingsWeeklyReminder.isChecked = prefs.weeklyReviewReminderEnabled

        binding.settingsSaveButton.setOnClickListener {
            prefs.dailyStudyTargetMinutes =
                binding.settingsTargetMinutes.text.toString().toIntOrNull() ?: 45
            prefs.minStudySessionMinutes =
                binding.settingsMinMinutes.text.toString().toIntOrNull() ?: 10
            prefs.requireMeaningfulNoteForCompletion = binding.settingsRequireNote.isChecked
            prefs.studyReminderEnabled = binding.settingsReminderEnabled.isChecked
            prefs.studyReminderHour = binding.settingsReminderTime.hour
            prefs.studyReminderMinute = binding.settingsReminderTime.minute
            prefs.studySecondReminderEnabled = binding.settingsSecondReminder.isChecked
            prefs.weeklyReviewReminderEnabled = binding.settingsWeeklyReminder.isChecked
            StudyReminderScheduler(this).scheduleAll()
            Toast.makeText(this, R.string.save, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

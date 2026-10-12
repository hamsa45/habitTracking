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
import org.isoron.uhabits.core.learning.LearningDateUtils
import org.isoron.uhabits.core.learning.StudyStatsCalculator
import org.isoron.uhabits.core.learning.WeeklyReview
import org.isoron.uhabits.databinding.ActivityWeeklyReviewBinding
import org.isoron.uhabits.utils.applyRootViewInsets

class WeeklyReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeeklyReviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeeklyReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyRootViewInsets()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.learning_weekly_review)

        val facade = HabitsApplication.component.learningFacade
        val prefs = HabitsApplication.component.preferences
        val today = LearningDateUtils.todayIso()
        val weekStart = LearningDateUtils.weekStart(today, prefs.firstWeekday)
        val snap = facade.progressSnapshot()
        binding.weeklySummaryText.text = buildString {
            appendLine("Week of $weekStart")
            appendLine("Actual: ${snap.weekActualMinutes} min · Planned: ${snap.weekPlannedMinutes} min")
            appendLine("Completion rate: ${(snap.weeklyCompletionRate * 100).toInt()}%")
            appendLine("Streak: ${snap.currentStreak} · Overdue revisions: ${snap.overdueRevisions}")
        }

        val existing = facade.repository.findWeeklyReview(weekStart)
        if (existing != null) {
            binding.weeklyQ1.setText(existing.completedSummary)
            binding.weeklyQ2.setText(existing.minutesReflection)
            binding.weeklyQ3.setText(existing.explainImplement)
            binding.weeklyQ4.setText(existing.needsRevision)
            binding.weeklyQ5.setText(existing.distractions)
            binding.weeklyQ6.setText(existing.nextPriority)
            binding.weeklyQ7.setText(existing.adjustment)
        } else {
            binding.weeklyQ2.setText(
                "Studied ${snap.weekActualMinutes} of ${snap.weekPlannedMinutes} planned minutes " +
                    "(${StudyStatsCalculator.minutesFromMs(snap.weekStudyMs)} from saved sessions)."
            )
        }

        binding.weeklySaveButton.setOnClickListener {
            val now = System.currentTimeMillis()
            if (existing != null) {
                existing.completedSummary = binding.weeklyQ1.text.toString()
                existing.minutesReflection = binding.weeklyQ2.text.toString()
                existing.explainImplement = binding.weeklyQ3.text.toString()
                existing.needsRevision = binding.weeklyQ4.text.toString()
                existing.distractions = binding.weeklyQ5.text.toString()
                existing.nextPriority = binding.weeklyQ6.text.toString()
                existing.adjustment = binding.weeklyQ7.text.toString()
                existing.updatedAt = now
                facade.repository.updateWeeklyReview(existing)
            } else {
                facade.repository.insertWeeklyReview(
                    WeeklyReview(
                        weekStartDate = weekStart,
                        completedSummary = binding.weeklyQ1.text.toString(),
                        minutesReflection = binding.weeklyQ2.text.toString(),
                        explainImplement = binding.weeklyQ3.text.toString(),
                        needsRevision = binding.weeklyQ4.text.toString(),
                        distractions = binding.weeklyQ5.text.toString(),
                        nextPriority = binding.weeklyQ6.text.toString(),
                        adjustment = binding.weeklyQ7.text.toString(),
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            Toast.makeText(this, R.string.learning_save_review, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

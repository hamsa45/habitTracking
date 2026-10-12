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

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.isoron.platform.time.getToday
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.learning.LearningDateUtils
import org.isoron.uhabits.core.learning.StudyStatsCalculator
import org.isoron.uhabits.core.learning.TopicStatus
import org.isoron.uhabits.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val facade = HabitsApplication.component.learningFacade
        binding.historyHeader.learningHeaderSubtitle.text = getString(R.string.learning_nav_history)

        val snap = facade.progressSnapshot()
        val prefs = HabitsApplication.component.preferences
        val targetWeek = prefs.dailyStudyTargetMinutes * 5
        val weekMin = snap.weekActualMinutes.toInt()
        val planned = snap.weekPlannedMinutes.coerceAtLeast(targetWeek)
        val pct = if (planned == 0) 0 else ((weekMin * 100) / planned).coerceAtMost(100)

        binding.historyWeekBadge.text = "WEEK"
        binding.historyWeekMinutes.text =
            getString(R.string.learning_week_planned_fmt, weekMin, planned)
        binding.historyWeekProgress.progress = pct
        binding.historyWeekMeta.text =
            "${snap.completedStudyDays} study days · $pct% of weekly target · ${snap.overdueRevisions} overdue"
        binding.historyPaceCard.text =
            "Pace: ${prefs.dailyStudyTargetMinutes} min / day\n${getString(R.string.learning_pace_hint)}"

        binding.historyCurrentStreak.text = "${snap.currentStreak} days"
        binding.historyLongestStreak.text = "${snap.longestStreak} days"

        renderHeatmap(facade)
        val topics = facade.repository.findAllTopics()
        binding.historyHealthInProgress.text =
            "${topics.count { it.status == TopicStatus.IN_PROGRESS }}\nIn Progress"
        binding.historyHealthMastered.text =
            "${topics.count { it.status == TopicStatus.COMPLETED }}\nMastered"
        binding.historyHealthReview.text =
            "${topics.count { it.status == TopicStatus.NEEDS_REVIEW } + snap.overdueRevisions}\nDue Review"

        val sessions = facade.sessionService.recentSessions(12)
        binding.historySessionsContainer.removeAllViews()
        binding.historyEmptyText.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        sessions.forEach { s ->
            val tv = TextView(requireContext()).apply {
                text = "${s.calendarDate}  ·  ${s.durationMinutes}m\n${s.objective.ifBlank { s.learned.take(60) }}"
                setBackgroundResource(R.drawable.learning_card_bg)
                setPadding(36, 28, 36, 28)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.learning_secondary))
                textSize = 14f
            }
            val lp = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = (8 * resources.displayMetrics.density).toInt()
            binding.historySessionsContainer.addView(tv, lp)
        }
    }

    private fun renderHeatmap(facade: org.isoron.uhabits.core.learning.LearningFacade) {
        val grid = binding.historyHeatmap
        grid.removeAllViews()
        val today = getToday()
        val start = today.minus(27)
        val cell = (resources.displayMetrics.widthPixels - (48 * resources.displayMetrics.density)).toInt() / 7
        var todayMinutes = 0L
        for (i in 0 until 28) {
            val date = start.plus(i)
            val iso = LearningDateUtils.format(date)
            val minutes = StudyStatsCalculator.minutesFromMs(
                StudyStatsCalculator.dailyStudyMs(facade.sessionService.sessionsForDate(iso), iso)
            )
            if (date == today) todayMinutes = minutes
            val level = when {
                minutes <= 0 -> 0
                minutes < 15 -> 1
                minutes < 30 -> 2
                minutes < 45 -> 3
                else -> 4
            }
            val color = when (level) {
                1 -> R.color.learning_heatmap_1
                2 -> R.color.learning_heatmap_2
                3 -> R.color.learning_heatmap_3
                4 -> R.color.learning_heatmap_4
                else -> R.color.learning_heatmap_0
            }
            val view = View(requireContext())
            val bg = GradientDrawable().apply {
                cornerRadius = 4 * resources.displayMetrics.density
                setColor(ContextCompat.getColor(requireContext(), color))
                if (date == today) {
                    setStroke((2 * resources.displayMetrics.density).toInt(),
                        ContextCompat.getColor(requireContext(), R.color.learning_secondary))
                }
            }
            view.background = bg
            view.contentDescription = "$iso: $minutes min"
            val params = GridLayout.LayoutParams().apply {
                width = cell - 8
                height = cell - 8
                setMargins(4, 4, 4, 4)
            }
            grid.addView(view, params)
        }
        binding.historyHeatmapFooter.text =
            "Today, ${LearningDateUtils.todayIso()}  ·  ${todayMinutes} min logged"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

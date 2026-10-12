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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.learning.LearningDateUtils
import org.isoron.uhabits.core.learning.LearningDay
import org.isoron.uhabits.core.learning.PlanStatus
import org.isoron.uhabits.databinding.FragmentPlanBinding

class PlanFragment : Fragment() {

    private var _binding: FragmentPlanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanBinding.inflate(inflater, container, false)
        binding.planDaysList.layoutManager = LinearLayoutManager(requireContext())
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        bind()
    }

    private fun bind() {
        val facade = HabitsApplication.component.learningFacade
        val plan = facade.ensurePlanReady()
        val days = facade.planService.getAllDays(plan)

        binding.planHeader.learningHeaderSubtitle.text = getString(R.string.learning_nav_plan)
        binding.planHeaderText.text = plan.name
        binding.planStatusText.text =
            "Started ${plan.startDate} · Day ${plan.currentDayIndex} of 30 ${plan.status.value}"

        binding.planPauseButton.text = if (plan.status == PlanStatus.PAUSED) {
            getString(R.string.learning_resume_plan)
        } else {
            getString(R.string.learning_pause_plan)
        }
        binding.planPauseButton.setOnClickListener {
            if (plan.status == PlanStatus.PAUSED) facade.planService.resumePlan(plan)
            else facade.planService.pausePlan(plan)
            bind()
        }
        binding.planRescheduleButton.setOnClickListener {
            facade.planService.rescheduleForward(plan, 1, LearningDateUtils::addDays)
            bind()
        }

        val pct = ((plan.currentDayIndex - 1) * 100 / 30).coerceIn(0, 100)
        binding.planProgressBadge.text = "Day ${plan.currentDayIndex} of 30 ($pct%)"
        binding.planProgressBar.progress = pct
        binding.planMilestoneText.text = weekTitleForDay(plan.currentDayIndex)
        binding.planRemainingText.text =
            getString(R.string.learning_days_remaining, (30 - plan.currentDayIndex + 1).coerceAtLeast(0))

        val items = mutableListOf<PlanListItem>()
        val weeks = listOf(
            Triple(1, 7, "Week 1: OOP Foundations"),
            Triple(8, 14, "Week 2: OOP & Polymorphism"),
            Triple(15, 21, "Week 3: Collections"),
            Triple(22, 30, "Week 4: Modern Java & Project")
        )
        for ((start, end, title) in weeks) {
            items.add(PlanListItem.WeekHeader(title, "DAYS $start–$end"))
            days.filter { it.dayIndex in start..end }.forEach { day ->
                items.add(PlanListItem.DayRow(day, day.dayIndex == plan.currentDayIndex))
            }
        }

        binding.planDaysList.adapter = PlanDayAdapter(items) { day ->
            showDayDialog(day, plan.currentDayIndex) { updated ->
                if (updated == "current") {
                    facade.planService.setCurrentDayIndex(plan, day.dayIndex)
                }
                bind()
            }
        }
    }

    private fun showDayDialog(day: LearningDay, currentIndex: Int, onDone: (String) -> Unit) {
        val facade = HabitsApplication.component.learningFacade
        val input = EditText(requireContext()).apply {
            setText(day.title)
            minHeight = (48 * resources.displayMetrics.density).toInt()
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Day ${day.dayIndex}")
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                day.title = input.text.toString().trim().ifEmpty { day.title }
                facade.planService.updateDay(day)
                onDone("saved")
            }
            .setNeutralButton("Make current") { _, _ -> onDone("current") }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun weekTitleForDay(dayIndex: Int): String = when (dayIndex) {
        in 1..7 -> "Milestone: OOP Foundations"
        in 8..14 -> "Milestone: OOP & Polymorphism"
        in 15..21 -> "Milestone: Collections"
        else -> "Milestone: Modern Java & Project"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

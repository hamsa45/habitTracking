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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.learning.DayStatus
import org.isoron.uhabits.core.learning.PlanStatus
import org.isoron.uhabits.core.learning.StudyStatsCalculator
import org.isoron.uhabits.databinding.FragmentTodayBinding
import org.isoron.uhabits.databinding.ItemLearningTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        bind()
    }

    private fun bind() {
        val facade = HabitsApplication.component.learningFacade
        val vm = facade.todayViewModel()
        val day = vm.day

        binding.todayHeader.learningHeaderSubtitle.text = getString(R.string.learning_nav_today)
        binding.todayDateText.text = friendlyDate(vm.calendarDate) +
            " · " + getString(R.string.learning_plan_day, vm.plan.currentDayIndex)

        binding.todayTopicText.text = day?.title ?: getString(R.string.learning_no_plan_empty)
        binding.todayDescriptionText.text = day?.description.orEmpty()
        binding.todayStatusText.text = statusLabel(vm.dayStatus)
        binding.todayStatusText.setBackgroundResource(
            if (vm.dayStatus == DayStatus.COMPLETED) R.drawable.learning_status_success_bg
            else R.drawable.learning_status_progress_bg
        )
        binding.todayStatusText.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (vm.dayStatus == DayStatus.COMPLETED) R.color.learning_success
                else R.color.learning_primary
            )
        )

        val studiedMin = StudyStatsCalculator.minutesFromMs(vm.studyMsToday).toInt()
        val targetMin = (vm.targetMs / 60_000L).toInt().coerceAtLeast(1)
        val progress = ((studiedMin * 100) / targetMin).coerceAtMost(100)
        binding.todayRingProgress.progress = progress
        binding.todayRingPercent.text = "$progress%"
        binding.todayProgressLabel.text = getString(R.string.learning_progress_time, studiedMin, targetMin)
        binding.todayTasksSummary.text = getString(
            R.string.learning_tasks_remaining,
            (vm.tasksTotal - vm.tasksCompleted).coerceAtLeast(0)
        )
        binding.todayTagChip.text = getString(R.string.learning_evening_deep_work)
        binding.todayMilestoneCount.text =
            getString(R.string.learning_milestones_count, vm.tasksCompleted, vm.tasksTotal)

        val next = vm.nextTask
        if (next != null) {
            binding.todayNextActionCard.visibility = View.VISIBLE
            binding.todayNextActionText.text = next.title
            binding.todayNextActionMeta.text = next.deliverable.ifBlank { next.description }
        } else if (vm.recentNextAction.isNotBlank()) {
            binding.todayNextActionCard.visibility = View.VISIBLE
            binding.todayNextActionText.text = vm.recentNextAction
            binding.todayNextActionMeta.text = getString(R.string.learning_what_next)
        } else {
            binding.todayNextActionCard.visibility = View.GONE
        }

        binding.todayTasksContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        day?.tasks?.forEach { task ->
            val row = ItemLearningTaskBinding.inflate(inflater, binding.todayTasksContainer, false)
            row.taskTitle.text = task.title
            row.taskSubtitle.text = task.deliverable.ifBlank { task.description }
            row.taskCheck.isChecked = task.isDone
            row.taskCheck.setOnCheckedChangeListener { _, checked ->
                task.isDone = checked
                facade.planService.updateTask(task)
                bind()
            }
            row.root.setOnClickListener { row.taskCheck.isChecked = !row.taskCheck.isChecked }
            val lp = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = (8 * resources.displayMetrics.density).toInt()
            binding.todayTasksContainer.addView(row.root, lp)
        }

        binding.todayStartButton.text = if (vm.hasActiveSession) {
            getString(R.string.learning_resume_studying)
        } else {
            getString(R.string.learning_start_studying)
        }
        binding.todayStartButton.setOnClickListener {
            startActivity(Intent(requireContext(), StudySessionActivity::class.java))
        }
        binding.todaySkipButton.setOnClickListener {
            if (day == null) return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.learning_mark_rest_or_skip)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    facade.planService.markDaySkipped(day, vm.plan)
                    bind()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        val empty = day == null
        binding.todayEmptyText.visibility = if (empty) View.VISIBLE else View.GONE
        binding.todayStartButton.isEnabled = !empty && vm.plan.status != PlanStatus.PAUSED
    }

    private fun friendlyDate(iso: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)
            SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(parsed ?: Date())
        } catch (_: Exception) {
            iso
        }
    }

    private fun statusLabel(status: DayStatus): String = when (status) {
        DayStatus.PENDING -> getString(R.string.learning_status_pending)
        DayStatus.IN_PROGRESS -> getString(R.string.learning_status_in_progress)
        DayStatus.COMPLETED -> getString(R.string.learning_status_completed)
        DayStatus.SKIPPED -> getString(R.string.learning_status_skipped)
        DayStatus.MISSED -> getString(R.string.learning_status_missed)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

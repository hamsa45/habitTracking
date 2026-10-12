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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.isoron.uhabits.R
import org.isoron.uhabits.core.learning.DayStatus
import org.isoron.uhabits.core.learning.LearningDay
import org.isoron.uhabits.databinding.ItemPlanDayBinding
import org.isoron.uhabits.databinding.ItemPlanWeekHeaderBinding

sealed class PlanListItem {
    data class WeekHeader(val title: String, val range: String) : PlanListItem()
    data class DayRow(val day: LearningDay, val isCurrent: Boolean) : PlanListItem()
}

class PlanDayAdapter(
    private val items: List<PlanListItem>,
    private val onDayClick: (LearningDay) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is PlanListItem.WeekHeader -> 0
        is PlanListItem.DayRow -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            WeekVH(ItemPlanWeekHeaderBinding.inflate(inflater, parent, false))
        } else {
            DayVH(ItemPlanDayBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PlanListItem.WeekHeader -> (holder as WeekVH).bind(item)
            is PlanListItem.DayRow -> (holder as DayVH).bind(item, onDayClick)
        }
    }

    class WeekVH(private val binding: ItemPlanWeekHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlanListItem.WeekHeader) {
            binding.weekTitle.text = item.title
            binding.weekRange.text = item.range
        }
    }

    class DayVH(private val binding: ItemPlanDayBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlanListItem.DayRow, onDayClick: (LearningDay) -> Unit) {
            val day = item.day
            val ctx = binding.root.context
            binding.planDayBadge.text = "DAY %02d".format(day.dayIndex)
            binding.planDayTitle.text = day.title
            binding.planDayMeta.text = buildString {
                append("${day.estimatedMinutes}m estimated")
                if (day.tasks.isNotEmpty()) append(" · ${day.tasks.size} tasks")
                if (day.isRestDay) append(" · rest")
            }
            val statusLabel = when {
                day.isRestDay -> "Rest day"
                day.status == DayStatus.IN_PROGRESS || item.isCurrent -> "In progress"
                day.status == DayStatus.COMPLETED -> "Completed"
                day.status == DayStatus.SKIPPED -> "Skipped"
                else -> "Pending"
            }
            binding.planDayStatus.text = statusLabel

            if (day.isRestDay) {
                binding.planDayRoot.setBackgroundResource(R.drawable.learning_rest_soft_bg)
                binding.planDayAccent.visibility = View.GONE
            } else if (item.isCurrent || day.status == DayStatus.IN_PROGRESS) {
                binding.planDayRoot.setBackgroundResource(R.drawable.learning_card_bg)
                binding.planDayAccent.visibility = View.VISIBLE
                binding.planDayStatus.setTextColor(
                    ContextCompat.getColor(ctx, R.color.learning_primary)
                )
            } else {
                binding.planDayRoot.setBackgroundResource(R.drawable.learning_card_bg)
                binding.planDayAccent.visibility = View.GONE
            }
            binding.root.setOnClickListener { onDayClick(day) }
        }
    }
}

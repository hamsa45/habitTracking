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
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.learning.LearningDateUtils
import org.isoron.uhabits.core.learning.LearningTopic
import org.isoron.uhabits.core.learning.MasteryChecklist
import org.isoron.uhabits.core.learning.RevisionStatus
import org.isoron.uhabits.core.learning.TopicStatus
import org.isoron.uhabits.databinding.FragmentTopicsBinding
import org.isoron.uhabits.databinding.ItemTopicCardBinding

class TopicsFragment : Fragment() {

    private var _binding: FragmentTopicsBinding? = null
    private val binding get() = _binding!!
    private var filter: TopicStatus? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopicsBinding.inflate(inflater, container, false)
        binding.topicsList.layoutManager = LinearLayoutManager(requireContext())
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        bind()
    }

    private fun bind() {
        val facade = HabitsApplication.component.learningFacade
        facade.ensurePlanReady()
        binding.topicsHeader.learningHeaderSubtitle.text = getString(R.string.learning_nav_topics)

        val topics = facade.repository.findAllTopics()
        val overdue = facade.revisionScheduler.overdueCount(LearningDateUtils.todayIso())
        val mastered = topics.count { it.status == TopicStatus.COMPLETED }
        val active = topics.count { it.status == TopicStatus.IN_PROGRESS || it.status == TopicStatus.NEEDS_REVIEW }
        val queued = topics.count { it.status == TopicStatus.TO_LEARN }
        val retention = if (topics.isEmpty()) 0 else (mastered * 100 / topics.size)

        binding.topicsRetentionText.text = "$retention% retention index"
        binding.topicsMasteryBar.progress = retention
        binding.topicsMasteryBreakdown.text = getString(R.string.learning_mastered_count, mastered) +
            "  ·  " + getString(R.string.learning_active_count, active) +
            "  ·  " + getString(R.string.learning_queued_count, queued)
        binding.topicsRevisionBadge.text = getString(R.string.learning_revisions_due, overdue)
        binding.topicsRevisionBadge.visibility = if (overdue > 0) View.VISIBLE else View.GONE

        renderFilters(topics)
        val filtered = topics.filter { filter == null || it.status == filter }
            .sortedWith(compareBy({ it.category }, { it.name }))
        binding.topicsEmptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.topicsList.adapter = TopicAdapter(filtered, facade) { topic ->
            startActivity(
                Intent(requireContext(), TopicDetailActivity::class.java)
                    .putExtra(TopicDetailActivity.EXTRA_TOPIC_ID, topic.id!!)
            )
        }
    }

    private fun renderFilters(topics: List<LearningTopic>) {
        binding.topicsFilterRow.removeAllViews()
        data class Chip(val label: String, val status: TopicStatus?)
        val chips = listOf(
            Chip(getString(R.string.learning_filter_all) + " (${topics.size})", null),
            Chip(
                getString(R.string.learning_filter_to_learn) +
                    " (${topics.count { it.status == TopicStatus.TO_LEARN }})",
                TopicStatus.TO_LEARN
            ),
            Chip(
                getString(R.string.learning_filter_in_progress) +
                    " (${topics.count { it.status == TopicStatus.IN_PROGRESS }})",
                TopicStatus.IN_PROGRESS
            ),
            Chip(
                getString(R.string.learning_filter_needs_review) +
                    " (${topics.count { it.status == TopicStatus.NEEDS_REVIEW }})",
                TopicStatus.NEEDS_REVIEW
            ),
            Chip(
                getString(R.string.learning_filter_completed) +
                    " (${topics.count { it.status == TopicStatus.COMPLETED }})",
                TopicStatus.COMPLETED
            )
        )
        chips.forEach { chip ->
            val tv = TextView(requireContext()).apply {
                text = chip.label
                setPadding(28, 16, 28, 16)
                textSize = 13f
                val selected = filter == chip.status
                setBackgroundResource(
                    if (selected) R.drawable.learning_chip_selected_bg else R.drawable.learning_chip_bg
                )
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (selected) R.color.learning_on_primary else R.color.learning_neutral
                    )
                )
                setOnClickListener {
                    filter = chip.status
                    bind()
                }
            }
            val lp = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = (8 * resources.displayMetrics.density).toInt()
            binding.topicsFilterRow.addView(tv, lp)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class TopicAdapter(
    private val topics: List<LearningTopic>,
    private val facade: org.isoron.uhabits.core.learning.LearningFacade,
    private val onClick: (LearningTopic) -> Unit
) : RecyclerView.Adapter<TopicAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTopicCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = topics.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(topics[position])

    inner class VH(private val binding: ItemTopicCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(topic: LearningTopic) {
            binding.topicName.text = topic.name
            binding.topicCategoryChip.text = topic.category.ifBlank { "General" }
            binding.topicStatusChip.text = when (topic.status) {
                TopicStatus.TO_LEARN -> "To Learn"
                TopicStatus.IN_PROGRESS -> "In Progress"
                TopicStatus.NEEDS_REVIEW -> "Needs Review"
                TopicStatus.COMPLETED -> "Completed"
            }
            val filled = MasteryChecklist.ALL.count { topic.hasMastery(it) }
            binding.topicMasteryDots.text = (0 until 5).joinToString(" ") { i ->
                if (i < filled) "●" else "○"
            }
            val open = facade.repository.findRevisionsForTopic(topic.id!!)
                .any { it.status == RevisionStatus.PENDING || it.status == RevisionStatus.OVERDUE }
            binding.topicReviewChip.visibility = if (open) View.VISIBLE else View.GONE
            binding.topicReviewChip.text = if (open) "Review due" else ""
            binding.topicFooter.text = when {
                topic.lastReviewedAt != null -> "Practiced"
                topic.status == TopicStatus.TO_LEARN -> "Unstarted"
                else -> topic.status.value
            }
            binding.root.setOnClickListener { onClick(topic) }
        }
    }
}

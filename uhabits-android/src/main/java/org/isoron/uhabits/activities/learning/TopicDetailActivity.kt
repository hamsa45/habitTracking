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
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.learning.LearningDateUtils
import org.isoron.uhabits.core.learning.MasteryChecklist
import org.isoron.uhabits.core.learning.RevisionStatus
import org.isoron.uhabits.core.learning.TopicStatus
import org.isoron.uhabits.databinding.ActivityTopicDetailBinding
import org.isoron.uhabits.utils.applyRootViewInsets

class TopicDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTopicDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyRootViewInsets()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val topicId = intent.getLongExtra(EXTRA_TOPIC_ID, -1)
        val facade = HabitsApplication.component.learningFacade
        val topic = facade.repository.findTopicById(topicId) ?: run {
            finish()
            return
        }

        title = topic.name
        binding.topicNameText.text = topic.name
        binding.topicCategoryText.text = topic.category

        val statuses = TopicStatus.entries
        binding.topicStatusSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statuses.map { it.value }
        )
        binding.topicStatusSpinner.setSelection(statuses.indexOf(topic.status).coerceAtLeast(0))

        val boxes = listOf(
            binding.mastery0, binding.mastery1, binding.mastery2, binding.mastery3, binding.mastery4
        )
        MasteryChecklist.ALL.forEachIndexed { index, bit ->
            boxes[index].isChecked = topic.hasMastery(bit)
        }
        binding.topicNotes.setText(topic.notes)

        val revisions = facade.repository.findRevisionsForTopic(topicId)
        binding.topicRevisionsText.text = if (revisions.isEmpty()) {
            "No revision tasks scheduled."
        } else {
            revisions.joinToString("\n") {
                "Interval ${it.intervalDays}d · due ${it.dueDate} · ${it.status.value}"
            }
        }

        binding.topicSaveButton.setOnClickListener {
            topic.status = statuses[binding.topicStatusSpinner.selectedItemPosition]
            MasteryChecklist.ALL.forEachIndexed { index, bit ->
                topic.withMastery(bit, boxes[index].isChecked)
            }
            topic.notes = binding.topicNotes.text.toString()
            facade.repository.updateTopic(topic)

            // Allow completing an overdue/pending revision from detail when status set to needs_review after review
            revisions.filter { it.status != RevisionStatus.DONE }.firstOrNull()?.let { task ->
                if (topic.status == TopicStatus.NEEDS_REVIEW || topic.status == TopicStatus.COMPLETED) {
                    // leave as-is unless user marks completed via status Completed after review
                }
            }
            // Mark first open revision done if user sets Completed
            if (topic.status == TopicStatus.COMPLETED) {
                revisions.firstOrNull { it.status != RevisionStatus.DONE }?.let {
                    facade.revisionScheduler.completeRevision(
                        it,
                        LearningDateUtils.todayIso(),
                        System.currentTimeMillis()
                    )
                }
            }

            Toast.makeText(this, R.string.save, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_TOPIC_ID = "topic_id"
    }
}

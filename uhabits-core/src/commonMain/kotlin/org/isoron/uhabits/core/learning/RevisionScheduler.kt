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
package org.isoron.uhabits.core.learning

/**
 * Simple spaced revision: default intervals 1, 3, and 7 days after initial study.
 */
class RevisionScheduler(
    private val repository: LearningRepository,
    private val intervals: List<Int> = listOf(1, 3, 7)
) {

    fun scheduleAfterStudy(topicId: Long, studyDate: String, addDays: (String, Int) -> String) {
        for (interval in intervals) {
            if (repository.findPendingRevision(topicId, interval) != null) continue
            repository.insertRevision(
                RevisionTask(
                    topicId = topicId,
                    intervalDays = interval,
                    dueDate = addDays(studyDate, interval),
                    status = RevisionStatus.PENDING
                )
            )
        }
    }

    fun markOverdue(today: String) {
        for (task in repository.findOpenRevisions()) {
            if (task.status == RevisionStatus.PENDING && task.dueDate < today) {
                task.status = RevisionStatus.OVERDUE
                repository.updateRevision(task)
            }
        }
    }

    fun completeRevision(task: RevisionTask, today: String, nowMillis: Long) {
        task.status = RevisionStatus.DONE
        repository.updateRevision(task)
        val topic = repository.findTopicById(task.topicId) ?: return
        topic.lastReviewedAt = nowMillis
        topic.status = TopicStatus.NEEDS_REVIEW
        // Schedule next interval only if not already pending for a later interval
        val nextInterval = intervals.firstOrNull { it > task.intervalDays }
        if (nextInterval != null && repository.findPendingRevision(topic.id!!, nextInterval) == null) {
            // keep existing pending later intervals; optionally bump next_review_at
            val open = repository.findOpenRevisions().filter { it.topicId == topic.id }
            topic.nextReviewAt = open.minOfOrNull { parseDateMillis(it.dueDate) }
        } else {
            topic.nextReviewAt = null
        }
        repository.updateTopic(topic)
    }

    fun overdueCount(today: String): Int {
        markOverdue(today)
        return repository.findOpenRevisions().count { it.status == RevisionStatus.OVERDUE || it.dueDate < today }
    }

    private fun parseDateMillis(date: String): Long {
        // YYYY-MM-DD as lexicographic proxy; callers using nextReviewAt for display only
        return date.replace("-", "").toLongOrNull() ?: 0L
    }
}

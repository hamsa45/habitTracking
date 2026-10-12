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

enum class PlanStatus(val value: String) {
    ACTIVE("active"),
    PAUSED("paused"),
    COMPLETED("completed");

    companion object {
        fun from(value: String): PlanStatus =
            entries.firstOrNull { it.value == value } ?: ACTIVE
    }
}

enum class DayStatus(val value: String) {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    SKIPPED("skipped"),
    MISSED("missed");

    companion object {
        fun from(value: String): DayStatus =
            entries.firstOrNull { it.value == value } ?: PENDING
    }
}

enum class TopicStatus(val value: String) {
    TO_LEARN("to_learn"),
    IN_PROGRESS("in_progress"),
    NEEDS_REVIEW("needs_review"),
    COMPLETED("completed");

    companion object {
        fun from(value: String): TopicStatus =
            entries.firstOrNull { it.value == value } ?: TO_LEARN
    }
}

enum class RevisionStatus(val value: String) {
    PENDING("pending"),
    DONE("done"),
    OVERDUE("overdue");

    companion object {
        fun from(value: String): RevisionStatus =
            entries.firstOrNull { it.value == value } ?: PENDING
    }
}

enum class SessionTimingSource(val value: String) {
    TIMER("timer"),
    MANUAL("manual");

    companion object {
        fun from(value: String): SessionTimingSource =
            entries.firstOrNull { it.value == value } ?: TIMER
    }
}

enum class ActiveSessionState(val value: String) {
    RUNNING("running"),
    PAUSED("paused");

    companion object {
        fun from(value: String): ActiveSessionState =
            entries.firstOrNull { it.value == value } ?: RUNNING
    }
}

data class LearningPlan(
    var id: Long? = null,
    var name: String = "Java Backend 30-Day Plan",
    var startDate: String = "",
    var currentDayIndex: Int = 1,
    var status: PlanStatus = PlanStatus.ACTIVE,
    var createdAt: Long = 0
)

data class LearningDay(
    var id: Long? = null,
    var planId: Long = 0,
    var dayIndex: Int = 1,
    var title: String = "",
    var description: String = "",
    var estimatedMinutes: Int = 45,
    var isRestDay: Boolean = false,
    var status: DayStatus = DayStatus.PENDING,
    var scheduledDate: String? = null,
    var tasks: MutableList<LearningTask> = mutableListOf()
)

data class LearningTask(
    var id: Long? = null,
    var dayId: Long = 0,
    var position: Int = 0,
    var title: String = "",
    var description: String = "",
    var deliverable: String = "",
    var isDone: Boolean = false
)

data class StudySession(
    var id: Long? = null,
    var uuid: String = "",
    var startedAt: Long = 0,
    var endedAt: Long = 0,
    var durationMs: Long = 0,
    var pausedMs: Long = 0,
    var timingSource: SessionTimingSource = SessionTimingSource.TIMER,
    var objective: String = "",
    var learned: String = "",
    var attempted: String = "",
    var unclear: String = "",
    var nextAction: String = "",
    var planDayId: Long? = null,
    var topicId: Long? = null,
    var calendarDate: String = ""
) {
    val durationMinutes: Long get() = (durationMs.coerceAtLeast(0) / 60_000L)
    val isMeaningful: Boolean get() = learned.trim().isNotEmpty()
}

data class ActiveStudySession(
    var startedElapsedRealtime: Long = 0,
    var accumulatedMs: Long = 0,
    var pauseStartedElapsed: Long? = null,
    var state: ActiveSessionState = ActiveSessionState.RUNNING,
    var objective: String = "",
    var dayId: Long? = null,
    var topicId: Long? = null,
    var wallStartedAt: Long = 0
)

data class LearningTopic(
    var id: Long? = null,
    var name: String = "",
    var category: String = "",
    var status: TopicStatus = TopicStatus.TO_LEARN,
    var firstStudiedAt: Long? = null,
    var lastReviewedAt: Long? = null,
    var nextReviewAt: Long? = null,
    var notes: String = "",
    var masteryFlags: Int = 0,
    var rating: Int = 0
) {
    fun hasMastery(bit: Int): Boolean = (masteryFlags and (1 shl bit)) != 0
    fun withMastery(bit: Int, enabled: Boolean): LearningTopic {
        val mask = 1 shl bit
        masteryFlags = if (enabled) masteryFlags or mask else masteryFlags and mask.inv()
        return this
    }
}

data class RevisionTask(
    var id: Long? = null,
    var topicId: Long = 0,
    var intervalDays: Int = 1,
    var dueDate: String = "",
    var status: RevisionStatus = RevisionStatus.PENDING
)

data class WeeklyReview(
    var id: Long? = null,
    var weekStartDate: String = "",
    var completedSummary: String = "",
    var minutesReflection: String = "",
    var explainImplement: String = "",
    var needsRevision: String = "",
    var distractions: String = "",
    var nextPriority: String = "",
    var adjustment: String = "",
    var createdAt: Long = 0,
    var updatedAt: Long = 0
)

object MasteryChecklist {
    const val EXPLAIN_PLAIN = 0
    const val EXPLAIN_TECHNICAL = 1
    const val IMPLEMENT_BASIC = 2
    const val SOLVE_VARIATION = 3
    const val IDENTIFY_TRADEOFFS = 4
    val ALL = listOf(
        EXPLAIN_PLAIN,
        EXPLAIN_TECHNICAL,
        IMPLEMENT_BASIC,
        SOLVE_VARIATION,
        IDENTIFY_TRADEOFFS
    )
}

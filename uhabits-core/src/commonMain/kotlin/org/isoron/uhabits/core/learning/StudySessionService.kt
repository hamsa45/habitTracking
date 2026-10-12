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

import kotlin.random.Random

class StudySessionService(
    private val repository: LearningRepository,
    private val planService: LearningPlanService,
    private val revisionScheduler: RevisionScheduler,
    private val timer: StudySessionTimer = StudySessionTimer()
) {

    fun getActive(): ActiveStudySession? = repository.findActiveSession()

    fun start(
        elapsedRealtime: Long,
        wallClockNow: Long,
        objective: String,
        dayId: Long?,
        topicId: Long?
    ): ActiveStudySession {
        val active = timer.start(elapsedRealtime, wallClockNow, objective, dayId, topicId)
        repository.saveActiveSession(active)
        dayId?.let { id ->
            repository.findDayById(id)?.let { planService.markDayInProgress(it) }
        }
        return active
    }

    fun pause(elapsedRealtime: Long): ActiveStudySession? {
        val active = repository.findActiveSession() ?: return null
        val paused = timer.pause(active, elapsedRealtime)
        repository.saveActiveSession(paused)
        return paused
    }

    fun resume(elapsedRealtime: Long): ActiveStudySession? {
        val active = repository.findActiveSession() ?: return null
        val resumed = timer.resume(active, elapsedRealtime)
        repository.saveActiveSession(resumed)
        return resumed
    }

    fun elapsedMs(elapsedRealtime: Long): Long {
        val active = repository.findActiveSession() ?: return 0
        return timer.elapsedMs(active, elapsedRealtime)
    }

    fun persistActive(active: ActiveStudySession) = repository.saveActiveSession(active)

    /**
     * Saves a session. Completion of the plan day requires a meaningful learning note
     * when [requireMeaningfulForCompletion] is true. Starting a timer alone never completes a day.
     */
    fun finishAndSave(
        elapsedRealtime: Long,
        wallEndedAt: Long,
        calendarDate: String,
        learned: String,
        attempted: String,
        unclear: String,
        nextAction: String,
        durationOverrideMs: Long? = null,
        timingSource: SessionTimingSource = SessionTimingSource.TIMER,
        requireMeaningfulForCompletion: Boolean = true,
        addDays: (String, Int) -> String
    ): StudySession {
        val active = repository.findActiveSession()
            ?: throw IllegalStateException("No active session to save")

        val durationMs = timer.validateDuration(
            durationOverrideMs ?: timer.elapsedMs(active, elapsedRealtime)
        )
        val pausedMs = if (durationOverrideMs != null) {
            0L
        } else {
            timer.pausedMs(active, elapsedRealtime, wallEndedAt)
        }

        val session = StudySession(
            uuid = generateUuid(),
            startedAt = active.wallStartedAt,
            endedAt = wallEndedAt,
            durationMs = durationMs,
            pausedMs = pausedMs,
            timingSource = if (durationOverrideMs != null) SessionTimingSource.MANUAL else timingSource,
            objective = active.objective,
            learned = learned,
            attempted = attempted,
            unclear = unclear,
            nextAction = nextAction,
            planDayId = active.dayId,
            topicId = active.topicId,
            calendarDate = calendarDate
        )
        repository.insertSession(session)
        repository.clearActiveSession()

        val meaningful = session.isMeaningful
        active.dayId?.let { dayId ->
            val day = repository.findDayById(dayId)
            val plan = planService.getActivePlan()
            if (day != null && plan != null) {
                if (!requireMeaningfulForCompletion || meaningful) {
                    planService.markDayCompleted(day, plan)
                } else {
                    planService.markDayInProgress(day)
                }
            }
        }

        val topicId = active.topicId ?: active.dayId?.let { resolveTopicIdForDay(it) }
        if (topicId != null && meaningful) {
            val topic = repository.findTopicById(topicId)
            if (topic != null) {
                if (topic.firstStudiedAt == null) topic.firstStudiedAt = wallEndedAt
                topic.lastReviewedAt = wallEndedAt
                if (topic.status == TopicStatus.TO_LEARN) {
                    topic.status = TopicStatus.IN_PROGRESS
                }
                repository.updateTopic(topic)
                revisionScheduler.scheduleAfterStudy(topic.id!!, calendarDate, addDays)
            }
        }

        return session
    }

    fun saveManualSession(
        calendarDate: String,
        wallStartedAt: Long,
        wallEndedAt: Long,
        durationMs: Long,
        objective: String,
        learned: String,
        attempted: String,
        unclear: String,
        nextAction: String,
        dayId: Long?,
        topicId: Long?,
        requireMeaningfulForCompletion: Boolean,
        addDays: (String, Int) -> String
    ): StudySession {
        val session = StudySession(
            uuid = generateUuid(),
            startedAt = wallStartedAt,
            endedAt = wallEndedAt,
            durationMs = timer.validateDuration(durationMs),
            pausedMs = 0,
            timingSource = SessionTimingSource.MANUAL,
            objective = objective,
            learned = learned,
            attempted = attempted,
            unclear = unclear,
            nextAction = nextAction,
            planDayId = dayId,
            topicId = topicId,
            calendarDate = calendarDate
        )
        repository.insertSession(session)
        if (session.isMeaningful || !requireMeaningfulForCompletion) {
            dayId?.let { id ->
                val day = repository.findDayById(id)
                val plan = planService.getActivePlan()
                if (day != null && plan != null && session.isMeaningful) {
                    planService.markDayCompleted(day, plan)
                }
            }
        }
        topicId?.let { tid ->
            if (session.isMeaningful) {
                repository.findTopicById(tid)?.let { topic ->
                    if (topic.firstStudiedAt == null) topic.firstStudiedAt = wallEndedAt
                    topic.lastReviewedAt = wallEndedAt
                    if (topic.status == TopicStatus.TO_LEARN) topic.status = TopicStatus.IN_PROGRESS
                    repository.updateTopic(topic)
                    revisionScheduler.scheduleAfterStudy(topic.id!!, calendarDate, addDays)
                }
            }
        }
        return session
    }

    fun updateSession(session: StudySession) = repository.updateSession(session)

    fun deleteSession(id: Long) = repository.deleteSession(id)

    fun sessionsForDate(date: String) = repository.findSessionsByDate(date)

    fun recentSessions(limit: Int = 20) = repository.findRecentSessions(limit)

    fun sessionsBetween(start: String, end: String) = repository.findSessionsBetween(start, end)

    private fun resolveTopicIdForDay(dayId: Long): Long? {
        val day = repository.findDayById(dayId) ?: return null
        return repository.findTopicByName(day.title)?.id
    }

    private fun generateUuid(): String {
        val hex = "0123456789abcdef"
        return buildString(36) {
            repeat(36) { i ->
                append(
                    when (i) {
                        8, 13, 18, 23 -> '-'
                        else -> hex[Random.nextInt(16)]
                    }
                )
            }
        }
    }
}

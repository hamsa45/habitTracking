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

import org.isoron.platform.io.Database
import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.preferences.Preferences

/**
 * App-facing facade that wires learning repositories and services.
 */
class LearningFacade(
    db: Database,
    private val preferences: Preferences
) {
    val repository = LearningRepository(db)
    val planService = LearningPlanService(repository)
    val revisionScheduler = RevisionScheduler(repository)
    val sessionService = StudySessionService(repository, planService, revisionScheduler)

    fun ensurePlanReady(): LearningPlan {
        val today = LearningDateUtils.todayIso()
        val plan = planService.ensurePlan(today, getToday().unixTime)
        preferences.learningPlanSeeded = true
        revisionScheduler.markOverdue(today)
        return plan
    }

    fun todayViewModel(): TodayViewModel {
        val plan = ensurePlanReady()
        val day = planService.getCurrentDay(plan)
        val today = LearningDateUtils.todayIso()
        val sessions = sessionService.sessionsForDate(today)
        val studyMs = StudyStatsCalculator.dailyStudyMs(sessions, today)
        val targetMs = preferences.dailyStudyTargetMinutes * 60_000L
        val completedByPolicy = StudyStatsCalculator.isDayCompleted(
            sessions,
            preferences.requireMeaningfulNoteForCompletion
        )
        val status = when {
            day == null -> DayStatus.PENDING
            day.isRestDay -> DayStatus.SKIPPED
            day.status == DayStatus.COMPLETED || completedByPolicy -> DayStatus.COMPLETED
            day.status == DayStatus.SKIPPED -> DayStatus.SKIPPED
            sessionService.getActive() != null -> DayStatus.IN_PROGRESS
            studyMs > 0 -> DayStatus.IN_PROGRESS
            else -> day.status
        }
        val nextTask = day?.tasks?.firstOrNull { !it.isDone }
        return TodayViewModel(
            calendarDate = today,
            plan = plan,
            day = day,
            studyMsToday = studyMs,
            targetMs = targetMs,
            timeGoalMet = studyMs >= targetMs,
            tasksCompleted = day?.tasks?.count { it.isDone } ?: 0,
            tasksTotal = day?.tasks?.size ?: 0,
            nextTask = nextTask,
            dayStatus = status,
            hasActiveSession = sessionService.getActive() != null,
            recentNextAction = sessions.lastOrNull()?.nextAction.orEmpty()
        )
    }

    fun progressSnapshot(): ProgressSnapshot {
        val plan = ensurePlanReady()
        val days = planService.getAllDays(plan)
        val today = LearningDateUtils.todayIso()
        val firstWeekday = preferences.firstWeekday
        val weekStart = LearningDateUtils.weekStart(today, firstWeekday)
        val weekEnd = LearningDateUtils.weekEnd(weekStart)
        val allSessions = repository.findRecentSessions(10_000)
        val weekSessions = sessionService.sessionsBetween(weekStart, weekEnd)
        val weekDayStatuses = days
            .filter { d ->
                val scheduled = d.scheduledDate
                scheduled != null && scheduled in weekStart..weekEnd
            }
            .map { it.isRestDay to it.status }
            .ifEmpty {
                // Fallback: use current week slice of plan days around current index
                days.filter {
                    it.dayIndex in (plan.currentDayIndex - 3)..(plan.currentDayIndex + 3)
                }.map { it.isRestDay to it.status }
            }

        val history = days.map { it.isRestDay to it.status }
        val newestFirst = days
            .filter { it.dayIndex <= plan.currentDayIndex }
            .sortedByDescending { it.dayIndex }
            .map { it.isRestDay to it.status }

        val weekPlanned = days
            .filter { !it.isRestDay }
            .filter {
                val s = it.scheduledDate
                s == null || s in weekStart..weekEnd
            }
            .take(7)
            .sumOf { it.estimatedMinutes }

        return StudyStatsCalculator.buildSnapshot(
            allSessions = allSessions,
            weekSessions = weekSessions,
            weekDayStatuses = weekDayStatuses,
            streakDaysNewestFirst = newestFirst,
            historyDaysOldestFirst = history,
            topics = repository.findAllTopics(),
            overdueRevisions = revisionScheduler.overdueCount(today),
            weekPlannedMinutes = weekPlanned
        )
    }

    fun isStudyDayCompleteForReminders(): Boolean {
        val vm = todayViewModel()
        return vm.day?.isRestDay == true ||
            vm.dayStatus == DayStatus.COMPLETED ||
            vm.dayStatus == DayStatus.SKIPPED
    }

    fun weekdayAllowed(dayOfWeek: DayOfWeek): Boolean {
        val bit = when (dayOfWeek) {
            DayOfWeek.SUNDAY -> 0
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
        }
        return (preferences.studyReminderDaysMask and (1 shl bit)) != 0
    }
}

data class TodayViewModel(
    val calendarDate: String,
    val plan: LearningPlan,
    val day: LearningDay?,
    val studyMsToday: Long,
    val targetMs: Long,
    val timeGoalMet: Boolean,
    val tasksCompleted: Int,
    val tasksTotal: Int,
    val nextTask: LearningTask?,
    val dayStatus: DayStatus,
    val hasActiveSession: Boolean,
    val recentNextAction: String
)

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
 * Statistics formulas (local calendar dates, ISO-like YYYY-MM-DD):
 *
 * - Actual study time = sum of saved session durationMs (paused time already excluded).
 * - Daily study time = sum of sessions whose calendarDate equals the selected local date.
 *   Sessions that cross midnight are attributed to the calendarDate stored at save time
 *   (local date when the session ended / was saved), not split across days.
 * - Weekly actual time = sum of session durations with calendarDate in [weekStart, weekEnd].
 * - Weekly completion rate = completed scheduled study days / scheduled study days
 *   (rest days excluded from both numerator and denominator).
 * - Streak = consecutive qualifying scheduled study days walking backward from today.
 *   Rest days do not break a streak. Missed/pending past scheduled days break it.
 *   A completed or intentionally skipped day counts as qualifying for streak continuity
 *   only when completed (meaningful session saved). Skipped/rest do not add to streak length.
 */
data class ProgressSnapshot(
    val totalStudyMs: Long,
    val weekStudyMs: Long,
    val completedStudyDays: Int,
    val weeklyCompletionRate: Double,
    val currentStreak: Int,
    val longestStreak: Int,
    val topicsCompleted: Int,
    val topicsNeedsReview: Int,
    val overdueRevisions: Int,
    val weekPlannedMinutes: Int,
    val weekActualMinutes: Long
)

object StudyStatsCalculator {

    fun sumDurationMs(sessions: List<StudySession>): Long =
        sessions.sumOf { it.durationMs.coerceAtLeast(0) }

    fun dailyStudyMs(sessions: List<StudySession>, calendarDate: String): Long =
        sumDurationMs(sessions.filter { it.calendarDate == calendarDate })

    fun weeklyStudyMs(sessions: List<StudySession>, weekStart: String, weekEnd: String): Long =
        sumDurationMs(sessions.filter { it.calendarDate in weekStart..weekEnd })

    fun isDayCompleted(sessions: List<StudySession>, requireMeaningful: Boolean = true): Boolean {
        if (sessions.isEmpty()) return false
        return if (requireMeaningful) sessions.any { it.isMeaningful } else true
    }

    fun weeklyCompletionRate(
        dayStatuses: List<Pair<Boolean, DayStatus>>
    ): Double {
        val scheduled = dayStatuses.filter { !it.first }
        if (scheduled.isEmpty()) return 0.0
        val completed = scheduled.count { it.second == DayStatus.COMPLETED }
        return completed.toDouble() / scheduled.size
    }

    /**
     * @param days Chronological list of (isRestDay, status) for scheduled plan days that have
     *             a calendar association ending at [todayIndex] inclusive.
     */
    fun currentStreak(daysNewestFirst: List<Pair<Boolean, DayStatus>>): Int {
        var streak = 0
        for ((isRest, status) in daysNewestFirst) {
            if (isRest) continue
            when (status) {
                DayStatus.COMPLETED -> streak++
                DayStatus.SKIPPED -> continue
                else -> break
            }
        }
        return streak
    }

    fun longestStreak(daysOldestFirst: List<Pair<Boolean, DayStatus>>): Int {
        var best = 0
        var current = 0
        for ((isRest, status) in daysOldestFirst) {
            if (isRest) continue
            when (status) {
                DayStatus.COMPLETED -> {
                    current++
                    if (current > best) best = current
                }
                DayStatus.SKIPPED -> { /* skip without breaking */ }
                else -> current = 0
            }
        }
        return best
    }

    fun minutesFromMs(ms: Long): Long = ms.coerceAtLeast(0) / 60_000L

    fun buildSnapshot(
        allSessions: List<StudySession>,
        weekSessions: List<StudySession>,
        weekDayStatuses: List<Pair<Boolean, DayStatus>>,
        streakDaysNewestFirst: List<Pair<Boolean, DayStatus>>,
        historyDaysOldestFirst: List<Pair<Boolean, DayStatus>>,
        topics: List<LearningTopic>,
        overdueRevisions: Int,
        weekPlannedMinutes: Int
    ): ProgressSnapshot {
        val weekMs = sumDurationMs(weekSessions)
        return ProgressSnapshot(
            totalStudyMs = sumDurationMs(allSessions),
            weekStudyMs = weekMs,
            completedStudyDays = historyDaysOldestFirst.count {
                !it.first && it.second == DayStatus.COMPLETED
            },
            weeklyCompletionRate = weeklyCompletionRate(weekDayStatuses),
            currentStreak = currentStreak(streakDaysNewestFirst),
            longestStreak = longestStreak(historyDaysOldestFirst),
            topicsCompleted = topics.count { it.status == TopicStatus.COMPLETED },
            topicsNeedsReview = topics.count { it.status == TopicStatus.NEEDS_REVIEW },
            overdueRevisions = overdueRevisions,
            weekPlannedMinutes = weekPlannedMinutes,
            weekActualMinutes = minutesFromMs(weekMs)
        )
    }
}

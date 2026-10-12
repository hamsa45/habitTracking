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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudyStatsCalculatorTest {

    private fun session(date: String, durationMs: Long, learned: String = "notes") =
        StudySession(
            uuid = "u-$date-$durationMs",
            startedAt = 0,
            endedAt = durationMs,
            durationMs = durationMs,
            calendarDate = date,
            learned = learned
        )

    @Test
    fun dailyAggregationSumsMultipleSessions() {
        val sessions = listOf(
            session("2026-10-11", 10 * 60_000),
            session("2026-10-11", 20 * 60_000),
            session("2026-10-12", 5 * 60_000)
        )
        assertEquals(30 * 60_000L, StudyStatsCalculator.dailyStudyMs(sessions, "2026-10-11"))
    }

    @Test
    fun pausedTimeIsExcludedFromDurationField() {
        // durationMs already excludes pause; calculator sums durationMs only
        val sessions = listOf(
            StudySession(
                uuid = "a",
                durationMs = 12 * 60_000,
                pausedMs = 8 * 60_000,
                calendarDate = "2026-10-11",
                learned = "x"
            )
        )
        assertEquals(12 * 60_000L, StudyStatsCalculator.sumDurationMs(sessions))
    }

    @Test
    fun midnightCrossingAttributedToSavedCalendarDate() {
        val sessions = listOf(
            session("2026-10-12", 30 * 60_000) // saved after midnight
        )
        assertEquals(0L, StudyStatsCalculator.dailyStudyMs(sessions, "2026-10-11"))
        assertEquals(30 * 60_000L, StudyStatsCalculator.dailyStudyMs(sessions, "2026-10-12"))
    }

    @Test
    fun weeklyAggregation() {
        val sessions = listOf(
            session("2026-10-06", 10 * 60_000),
            session("2026-10-11", 20 * 60_000),
            session("2026-10-13", 5 * 60_000)
        )
        assertEquals(
            30 * 60_000L,
            StudyStatsCalculator.weeklyStudyMs(sessions, "2026-10-06", "2026-10-12")
        )
    }

    @Test
    fun completionRateExcludesRestDays() {
        val days = listOf(
            false to DayStatus.COMPLETED,
            false to DayStatus.MISSED,
            true to DayStatus.SKIPPED,
            false to DayStatus.COMPLETED
        )
        assertEquals(2.0 / 3.0, StudyStatsCalculator.weeklyCompletionRate(days), 1e-9)
    }

    @Test
    fun streakIgnoresRestDaysAndBreaksOnMiss() {
        val newestFirst = listOf(
            true to DayStatus.SKIPPED,
            false to DayStatus.COMPLETED,
            false to DayStatus.COMPLETED,
            false to DayStatus.MISSED,
            false to DayStatus.COMPLETED
        )
        assertEquals(2, StudyStatsCalculator.currentStreak(newestFirst))
    }

    @Test
    fun longestStreak() {
        val oldestFirst = listOf(
            false to DayStatus.COMPLETED,
            false to DayStatus.COMPLETED,
            false to DayStatus.MISSED,
            false to DayStatus.COMPLETED,
            true to DayStatus.SKIPPED,
            false to DayStatus.COMPLETED,
            false to DayStatus.COMPLETED,
            false to DayStatus.COMPLETED
        )
        // Rest days do not break streaks, so C + rest + C + C + C => 4
        assertEquals(4, StudyStatsCalculator.longestStreak(oldestFirst))
    }

    @Test
    fun meaningfulCompletionRequiresLearnedNote() {
        assertFalse(
            StudyStatsCalculator.isDayCompleted(listOf(session("2026-10-11", 1000, learned = "")))
        )
        assertTrue(
            StudyStatsCalculator.isDayCompleted(listOf(session("2026-10-11", 1000, learned = "maps")))
        )
    }

    @Test
    fun invalidDurationRejectedByTimer() {
        val timer = StudySessionTimer()
        assertFailsWith<IllegalArgumentException> { timer.validateDuration(-1) }
    }

    @Test
    fun timerExcludesPausedTime() {
        val timer = StudySessionTimer()
        var active = timer.start(1000, 0, "obj", 1, null)
        active = timer.pause(active, 1000 + 5 * 60_000)
        active = timer.resume(active, 1000 + 10 * 60_000)
        val elapsed = timer.elapsedMs(active, 1000 + 12 * 60_000)
        assertEquals(7 * 60_000L, elapsed)
    }
}

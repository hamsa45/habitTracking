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
 * Pure timer logic using a monotonic clock (elapsedRealtime).
 * Wall-clock times are only used for started/ended timestamps on save.
 */
class StudySessionTimer {

    fun start(
        elapsedRealtime: Long,
        wallClockNow: Long,
        objective: String,
        dayId: Long?,
        topicId: Long?
    ): ActiveStudySession {
        return ActiveStudySession(
            startedElapsedRealtime = elapsedRealtime,
            accumulatedMs = 0,
            pauseStartedElapsed = null,
            state = ActiveSessionState.RUNNING,
            objective = objective,
            dayId = dayId,
            topicId = topicId,
            wallStartedAt = wallClockNow
        )
    }

    fun pause(active: ActiveStudySession, elapsedRealtime: Long): ActiveStudySession {
        if (active.state == ActiveSessionState.PAUSED) return active
        val segment = (elapsedRealtime - active.startedElapsedRealtime).coerceAtLeast(0)
        return active.copy(
            accumulatedMs = active.accumulatedMs + segment,
            pauseStartedElapsed = elapsedRealtime,
            state = ActiveSessionState.PAUSED,
            startedElapsedRealtime = elapsedRealtime
        )
    }

    fun resume(active: ActiveStudySession, elapsedRealtime: Long): ActiveStudySession {
        if (active.state == ActiveSessionState.RUNNING) return active
        return active.copy(
            state = ActiveSessionState.RUNNING,
            pauseStartedElapsed = null,
            startedElapsedRealtime = elapsedRealtime
        )
    }

    /**
     * Returns active study duration excluding paused time.
     */
    fun elapsedMs(active: ActiveStudySession, elapsedRealtime: Long): Long {
        return when (active.state) {
            ActiveSessionState.RUNNING ->
                (active.accumulatedMs + (elapsedRealtime - active.startedElapsedRealtime)).coerceAtLeast(0)
            ActiveSessionState.PAUSED ->
                active.accumulatedMs.coerceAtLeast(0)
        }
    }

    fun pausedMs(active: ActiveStudySession, elapsedRealtime: Long, wallEndedAt: Long): Long {
        val totalWall = (wallEndedAt - active.wallStartedAt).coerceAtLeast(0)
        val studied = elapsedMs(active, elapsedRealtime)
        return (totalWall - studied).coerceAtLeast(0)
    }

    fun validateDuration(durationMs: Long): Long {
        require(durationMs >= 0) { "Duration must not be negative" }
        return durationMs.coerceAtMost(24L * 60 * 60 * 1000)
    }
}

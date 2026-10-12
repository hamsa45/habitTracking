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

import kotlinx.coroutines.runBlocking
import org.isoron.platform.io.TestDatabaseHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningRepositoryTest {

    private fun withRepo(block: (LearningRepository) -> Unit) = runBlocking {
        val db = TestDatabaseHelper.createEmptyDatabase()
        block(LearningRepository(db))
        db.close()
    }

    @Test
    fun seedPlanAndSessionsPersist() = withRepo { repo ->
        val plan = JavaLearningPlanSeeder.ensureSeeded(repo, "2026-10-11", 1_000)
        assertEquals(30, repo.findDaysForPlan(plan.id!!).size)
        assertNotNull(repo.findDayByPlanAndIndex(plan.id!!, 17))

        val sessionService = StudySessionService(
            repo,
            LearningPlanService(repo),
            RevisionScheduler(repo)
        )
        sessionService.start(0, 1000, "HashMap", repo.findDayByPlanAndIndex(plan.id!!, 17)!!.id, null)
        val saved = sessionService.finishAndSave(
            elapsedRealtime = 12 * 60_000L,
            wallEndedAt = 1000 + 12 * 60_000L,
            calendarDate = "2026-10-11",
            learned = "HashMap stores key-value pairs",
            attempted = "frequency counter",
            unclear = "",
            nextAction = "solve variation",
            addDays = LearningDateUtils::addDays
        )
        assertTrue(saved.id!! > 0)
        assertNull(repo.findActiveSession())
        assertEquals(1, repo.findSessionsByDate("2026-10-11").size)
    }

    @Test
    fun editAndDeleteSessionUpdatesAggregates() = withRepo { repo ->
        val plan = JavaLearningPlanSeeder.ensureSeeded(repo, "2026-10-11", 1_000)
        val dayId = repo.findDayByPlanAndIndex(plan.id!!, 2)!!.id
        val svc = StudySessionService(repo, LearningPlanService(repo), RevisionScheduler(repo))
        svc.start(0, 1000, "BankAccount", dayId, null)
        val s = svc.finishAndSave(
            5 * 60_000,
            1000 + 5 * 60_000,
            "2026-10-11",
            "classes",
            "BankAccount",
            "",
            "encapsulation",
            addDays = LearningDateUtils::addDays
        )
        s.durationMs = 15 * 60_000
        repo.updateSession(s)
        assertEquals(15 * 60_000L, StudyStatsCalculator.dailyStudyMs(repo.findSessionsByDate("2026-10-11"), "2026-10-11"))
        repo.deleteSession(s.id!!)
        assertEquals(0L, StudyStatsCalculator.dailyStudyMs(repo.findSessionsByDate("2026-10-11"), "2026-10-11"))
    }

    @Test
    fun revisionSchedulingNoDuplicates() = withRepo { repo ->
        val topicId = repo.insertTopic(LearningTopic(name = "HashMap", category = "Collections"))
        val scheduler = RevisionScheduler(repo)
        scheduler.scheduleAfterStudy(topicId, "2026-10-11", LearningDateUtils::addDays)
        scheduler.scheduleAfterStudy(topicId, "2026-10-11", LearningDateUtils::addDays)
        assertEquals(3, repo.findRevisionsForTopic(topicId).size)
        scheduler.markOverdue("2026-10-20")
        assertTrue(repo.findOpenRevisions().any { it.status == RevisionStatus.OVERDUE })
    }

    @Test
    fun planDoesNotAutoAdvanceOnDateChange() = withRepo { repo ->
        val plan = JavaLearningPlanSeeder.ensureSeeded(repo, "2026-10-01", 1_000)
        assertEquals(1, plan.currentDayIndex)
        val reloaded = repo.findActivePlan()!!
        assertEquals(1, reloaded.currentDayIndex)
        assertEquals("2026-10-01", reloaded.startDate)
    }
}

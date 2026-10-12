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
import org.isoron.platform.io.StepResult
import org.isoron.platform.io.queryLong
import org.isoron.platform.io.run

class LearningRepository(private val db: Database) {

    fun findActivePlan(): LearningPlan? {
        val stmt = db.prepareStatement(
            """SELECT id, name, start_date, current_day_index, status, created_at
               FROM LearningPlans WHERE status IN ('active','paused')
               ORDER BY created_at DESC LIMIT 1"""
        )
        return try {
            if (stmt.step() == StepResult.ROW) readPlan(stmt) else null
        } finally {
            stmt.finalize()
        }
    }

    fun findPlanById(id: Long): LearningPlan? {
        val stmt = db.prepareStatement(
            """SELECT id, name, start_date, current_day_index, status, created_at
               FROM LearningPlans WHERE id = ?"""
        )
        return try {
            stmt.bindLong(1, id)
            if (stmt.step() == StepResult.ROW) readPlan(stmt) else null
        } finally {
            stmt.finalize()
        }
    }

    fun insertPlan(plan: LearningPlan): Long {
        db.run(
            """INSERT INTO LearningPlans(name, start_date, current_day_index, status, created_at)
               VALUES (?, ?, ?, ?, ?)"""
        ) {
            bindText(1, plan.name)
            bindText(2, plan.startDate)
            bindInt(3, plan.currentDayIndex)
            bindText(4, plan.status.value)
            bindLong(5, plan.createdAt)
        }
        val id = db.queryLong("SELECT last_insert_rowid()")
        plan.id = id
        return id
    }

    fun updatePlan(plan: LearningPlan) {
        db.run(
            """UPDATE LearningPlans SET name=?, start_date=?, current_day_index=?, status=?
               WHERE id=?"""
        ) {
            bindText(1, plan.name)
            bindText(2, plan.startDate)
            bindInt(3, plan.currentDayIndex)
            bindText(4, plan.status.value)
            bindLong(5, plan.id!!)
        }
    }

    fun insertDay(day: LearningDay): Long {
        db.run(
            """INSERT INTO LearningDays(plan_id, day_index, title, description, estimated_minutes,
               is_rest_day, status, scheduled_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"""
        ) {
            bindLong(1, day.planId)
            bindInt(2, day.dayIndex)
            bindText(3, day.title)
            bindText(4, day.description)
            bindInt(5, day.estimatedMinutes)
            bindInt(6, if (day.isRestDay) 1 else 0)
            bindText(7, day.status.value)
            if (day.scheduledDate != null) bindText(8, day.scheduledDate!!) else bindNull(8)
        }
        val id = db.queryLong("SELECT last_insert_rowid()")
        day.id = id
        return id
    }

    fun updateDay(day: LearningDay) {
        db.run(
            """UPDATE LearningDays SET title=?, description=?, estimated_minutes=?,
               is_rest_day=?, status=?, scheduled_date=? WHERE id=?"""
        ) {
            bindText(1, day.title)
            bindText(2, day.description)
            bindInt(3, day.estimatedMinutes)
            bindInt(4, if (day.isRestDay) 1 else 0)
            bindText(5, day.status.value)
            if (day.scheduledDate != null) bindText(6, day.scheduledDate!!) else bindNull(6)
            bindLong(7, day.id!!)
        }
    }

    fun findDaysForPlan(planId: Long): List<LearningDay> {
        val stmt = db.prepareStatement(
            """SELECT id, plan_id, day_index, title, description, estimated_minutes,
               is_rest_day, status, scheduled_date
               FROM LearningDays WHERE plan_id = ? ORDER BY day_index"""
        )
        val days = mutableListOf<LearningDay>()
        try {
            stmt.bindLong(1, planId)
            while (stmt.step() == StepResult.ROW) {
                val day = readDay(stmt)
                day.tasks = findTasksForDay(day.id!!).toMutableList()
                days.add(day)
            }
        } finally {
            stmt.finalize()
        }
        return days
    }

    fun findDayById(id: Long): LearningDay? {
        val stmt = db.prepareStatement(
            """SELECT id, plan_id, day_index, title, description, estimated_minutes,
               is_rest_day, status, scheduled_date FROM LearningDays WHERE id = ?"""
        )
        return try {
            stmt.bindLong(1, id)
            if (stmt.step() == StepResult.ROW) {
                val day = readDay(stmt)
                day.tasks = findTasksForDay(day.id!!).toMutableList()
                day
            } else null
        } finally {
            stmt.finalize()
        }
    }

    fun findDayByPlanAndIndex(planId: Long, dayIndex: Int): LearningDay? {
        val stmt = db.prepareStatement(
            """SELECT id, plan_id, day_index, title, description, estimated_minutes,
               is_rest_day, status, scheduled_date
               FROM LearningDays WHERE plan_id = ? AND day_index = ?"""
        )
        return try {
            stmt.bindLong(1, planId)
            stmt.bindInt(2, dayIndex)
            if (stmt.step() == StepResult.ROW) {
                val day = readDay(stmt)
                day.tasks = findTasksForDay(day.id!!).toMutableList()
                day
            } else null
        } finally {
            stmt.finalize()
        }
    }

    fun insertTask(task: LearningTask): Long {
        db.run(
            """INSERT INTO LearningTasks(day_id, position, title, description, deliverable, is_done)
               VALUES (?, ?, ?, ?, ?, ?)"""
        ) {
            bindLong(1, task.dayId)
            bindInt(2, task.position)
            bindText(3, task.title)
            bindText(4, task.description)
            bindText(5, task.deliverable)
            bindInt(6, if (task.isDone) 1 else 0)
        }
        val id = db.queryLong("SELECT last_insert_rowid()")
        task.id = id
        return id
    }

    fun updateTask(task: LearningTask) {
        db.run(
            """UPDATE LearningTasks SET position=?, title=?, description=?, deliverable=?, is_done=?
               WHERE id=?"""
        ) {
            bindInt(1, task.position)
            bindText(2, task.title)
            bindText(3, task.description)
            bindText(4, task.deliverable)
            bindInt(5, if (task.isDone) 1 else 0)
            bindLong(6, task.id!!)
        }
    }

    fun findTasksForDay(dayId: Long): List<LearningTask> {
        val stmt = db.prepareStatement(
            """SELECT id, day_id, position, title, description, deliverable, is_done
               FROM LearningTasks WHERE day_id = ? ORDER BY position"""
        )
        val tasks = mutableListOf<LearningTask>()
        try {
            stmt.bindLong(1, dayId)
            while (stmt.step() == StepResult.ROW) {
                tasks.add(readTask(stmt))
            }
        } finally {
            stmt.finalize()
        }
        return tasks
    }

    fun insertSession(session: StudySession): Long {
        require(session.durationMs >= 0) { "Duration must not be negative" }
        db.run(
            """INSERT INTO StudySessions(uuid, started_at, ended_at, duration_ms, paused_ms,
               timing_source, objective, learned, attempted, unclear, next_action,
               plan_day_id, topic_id, calendar_date)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        ) {
            bindText(1, session.uuid)
            bindLong(2, session.startedAt)
            bindLong(3, session.endedAt)
            bindLong(4, session.durationMs.coerceAtLeast(0))
            bindLong(5, session.pausedMs.coerceAtLeast(0))
            bindText(6, session.timingSource.value)
            bindText(7, session.objective.take(500))
            bindText(8, session.learned.take(2000))
            bindText(9, session.attempted.take(2000))
            bindText(10, session.unclear.take(2000))
            bindText(11, session.nextAction.take(500))
            if (session.planDayId != null) bindLong(12, session.planDayId!!) else bindNull(12)
            if (session.topicId != null) bindLong(13, session.topicId!!) else bindNull(13)
            bindText(14, session.calendarDate)
        }
        val id = db.queryLong("SELECT last_insert_rowid()")
        session.id = id
        return id
    }

    fun updateSession(session: StudySession) {
        require(session.durationMs >= 0) { "Duration must not be negative" }
        db.run(
            """UPDATE StudySessions SET started_at=?, ended_at=?, duration_ms=?, paused_ms=?,
               timing_source=?, objective=?, learned=?, attempted=?, unclear=?, next_action=?,
               plan_day_id=?, topic_id=?, calendar_date=? WHERE id=?"""
        ) {
            bindLong(1, session.startedAt)
            bindLong(2, session.endedAt)
            bindLong(3, session.durationMs.coerceAtLeast(0))
            bindLong(4, session.pausedMs.coerceAtLeast(0))
            bindText(5, session.timingSource.value)
            bindText(6, session.objective.take(500))
            bindText(7, session.learned.take(2000))
            bindText(8, session.attempted.take(2000))
            bindText(9, session.unclear.take(2000))
            bindText(10, session.nextAction.take(500))
            if (session.planDayId != null) bindLong(11, session.planDayId!!) else bindNull(11)
            if (session.topicId != null) bindLong(12, session.topicId!!) else bindNull(12)
            bindText(13, session.calendarDate)
            bindLong(14, session.id!!)
        }
    }

    fun deleteSession(id: Long) {
        db.run("DELETE FROM StudySessions WHERE id = ?") {
            bindLong(1, id)
        }
    }

    fun findSessionById(id: Long): StudySession? {
        val stmt = db.prepareStatement(
            """SELECT id, uuid, started_at, ended_at, duration_ms, paused_ms, timing_source,
               objective, learned, attempted, unclear, next_action, plan_day_id, topic_id, calendar_date
               FROM StudySessions WHERE id = ?"""
        )
        return try {
            stmt.bindLong(1, id)
            if (stmt.step() == StepResult.ROW) readSession(stmt) else null
        } finally {
            stmt.finalize()
        }
    }

    fun findSessionsByDate(calendarDate: String): List<StudySession> {
        val stmt = db.prepareStatement(
            """SELECT id, uuid, started_at, ended_at, duration_ms, paused_ms, timing_source,
               objective, learned, attempted, unclear, next_action, plan_day_id, topic_id, calendar_date
               FROM StudySessions WHERE calendar_date = ? ORDER BY started_at"""
        )
        val results = mutableListOf<StudySession>()
        try {
            stmt.bindText(1, calendarDate)
            while (stmt.step() == StepResult.ROW) results.add(readSession(stmt))
        } finally {
            stmt.finalize()
        }
        return results
    }

    fun findSessionsBetween(startDate: String, endDate: String): List<StudySession> {
        val stmt = db.prepareStatement(
            """SELECT id, uuid, started_at, ended_at, duration_ms, paused_ms, timing_source,
               objective, learned, attempted, unclear, next_action, plan_day_id, topic_id, calendar_date
               FROM StudySessions WHERE calendar_date >= ? AND calendar_date <= ?
               ORDER BY started_at"""
        )
        val results = mutableListOf<StudySession>()
        try {
            stmt.bindText(1, startDate)
            stmt.bindText(2, endDate)
            while (stmt.step() == StepResult.ROW) results.add(readSession(stmt))
        } finally {
            stmt.finalize()
        }
        return results
    }

    fun findRecentSessions(limit: Int = 20): List<StudySession> {
        val stmt = db.prepareStatement(
            """SELECT id, uuid, started_at, ended_at, duration_ms, paused_ms, timing_source,
               objective, learned, attempted, unclear, next_action, plan_day_id, topic_id, calendar_date
               FROM StudySessions ORDER BY started_at DESC LIMIT ?"""
        )
        val results = mutableListOf<StudySession>()
        try {
            stmt.bindInt(1, limit)
            while (stmt.step() == StepResult.ROW) results.add(readSession(stmt))
        } finally {
            stmt.finalize()
        }
        return results
    }

    fun findSessionsForTopic(topicId: Long): List<StudySession> {
        val stmt = db.prepareStatement(
            """SELECT id, uuid, started_at, ended_at, duration_ms, paused_ms, timing_source,
               objective, learned, attempted, unclear, next_action, plan_day_id, topic_id, calendar_date
               FROM StudySessions WHERE topic_id = ? ORDER BY started_at DESC"""
        )
        val results = mutableListOf<StudySession>()
        try {
            stmt.bindLong(1, topicId)
            while (stmt.step() == StepResult.ROW) results.add(readSession(stmt))
        } finally {
            stmt.finalize()
        }
        return results
    }

    fun saveActiveSession(active: ActiveStudySession) {
        db.run("DELETE FROM ActiveStudySession")
        db.run(
            """INSERT INTO ActiveStudySession(id, started_elapsed_realtime, accumulated_ms,
               pause_started_elapsed, state, objective, day_id, topic_id, wall_started_at)
               VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?)"""
        ) {
            bindLong(1, active.startedElapsedRealtime)
            bindLong(2, active.accumulatedMs)
            if (active.pauseStartedElapsed != null) {
                bindLong(3, active.pauseStartedElapsed!!)
            } else {
                bindNull(3)
            }
            bindText(4, active.state.value)
            bindText(5, active.objective)
            if (active.dayId != null) bindLong(6, active.dayId!!) else bindNull(6)
            if (active.topicId != null) bindLong(7, active.topicId!!) else bindNull(7)
            bindLong(8, active.wallStartedAt)
        }
    }

    fun findActiveSession(): ActiveStudySession? {
        val stmt = db.prepareStatement(
            """SELECT started_elapsed_realtime, accumulated_ms, pause_started_elapsed, state,
               objective, day_id, topic_id, wall_started_at FROM ActiveStudySession WHERE id = 1"""
        )
        return try {
            if (stmt.step() == StepResult.ROW) {
                ActiveStudySession(
                    startedElapsedRealtime = stmt.getLong(0),
                    accumulatedMs = stmt.getLong(1),
                    pauseStartedElapsed = stmt.getLongOrNull(2),
                    state = ActiveSessionState.from(stmt.getText(3)),
                    objective = stmt.getText(4),
                    dayId = stmt.getLongOrNull(5),
                    topicId = stmt.getLongOrNull(6),
                    wallStartedAt = stmt.getLong(7)
                )
            } else null
        } finally {
            stmt.finalize()
        }
    }

    fun clearActiveSession() {
        db.run("DELETE FROM ActiveStudySession")
    }

    fun insertTopic(topic: LearningTopic): Long {
        db.run(
            """INSERT INTO LearningTopics(name, category, status, first_studied_at, last_reviewed_at,
               next_review_at, notes, mastery_flags, rating)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        ) {
            bindText(1, topic.name)
            bindText(2, topic.category)
            bindText(3, topic.status.value)
            if (topic.firstStudiedAt != null) bindLong(4, topic.firstStudiedAt!!) else bindNull(4)
            if (topic.lastReviewedAt != null) bindLong(5, topic.lastReviewedAt!!) else bindNull(5)
            if (topic.nextReviewAt != null) bindLong(6, topic.nextReviewAt!!) else bindNull(6)
            bindText(7, topic.notes)
            bindInt(8, topic.masteryFlags)
            bindInt(9, topic.rating)
        }
        val id = db.queryLong("SELECT last_insert_rowid()")
        topic.id = id
        return id
    }

    fun updateTopic(topic: LearningTopic) {
        db.run(
            """UPDATE LearningTopics SET name=?, category=?, status=?, first_studied_at=?,
               last_reviewed_at=?, next_review_at=?, notes=?, mastery_flags=?, rating=?
               WHERE id=?"""
        ) {
            bindText(1, topic.name)
            bindText(2, topic.category)
            bindText(3, topic.status.value)
            if (topic.firstStudiedAt != null) bindLong(4, topic.firstStudiedAt!!) else bindNull(4)
            if (topic.lastReviewedAt != null) bindLong(5, topic.lastReviewedAt!!) else bindNull(5)
            if (topic.nextReviewAt != null) bindLong(6, topic.nextReviewAt!!) else bindNull(6)
            bindText(7, topic.notes)
            bindInt(8, topic.masteryFlags)
            bindInt(9, topic.rating)
            bindLong(10, topic.id!!)
        }
    }

    fun findAllTopics(): List<LearningTopic> {
        val stmt = db.prepareStatement(
            """SELECT id, name, category, status, first_studied_at, last_reviewed_at,
               next_review_at, notes, mastery_flags, rating FROM LearningTopics ORDER BY name"""
        )
        val results = mutableListOf<LearningTopic>()
        try {
            while (stmt.step() == StepResult.ROW) results.add(readTopic(stmt))
        } finally {
            stmt.finalize()
        }
        return results
    }

    fun findTopicById(id: Long): LearningTopic? {
        val stmt = db.prepareStatement(
            """SELECT id, name, category, status, first_studied_at, last_reviewed_at,
               next_review_at, notes, mastery_flags, rating FROM LearningTopics WHERE id = ?"""
        )
        return try {
            stmt.bindLong(1, id)
            if (stmt.step() == StepResult.ROW) readTopic(stmt) else null
        } finally {
            stmt.finalize()
        }
    }

    fun findTopicByName(name: String): LearningTopic? {
        val stmt = db.prepareStatement(
            """SELECT id, name, category, status, first_studied_at, last_reviewed_at,
               next_review_at, notes, mastery_flags, rating FROM LearningTopics WHERE name = ? LIMIT 1"""
        )
        return try {
            stmt.bindText(1, name)
            if (stmt.step() == StepResult.ROW) readTopic(stmt) else null
        } finally {
            stmt.finalize()
        }
    }

    fun insertRevision(task: RevisionTask): Long {
        db.run(
            """INSERT INTO RevisionTasks(topic_id, interval_days, due_date, status)
               VALUES (?, ?, ?, ?)"""
        ) {
            bindLong(1, task.topicId)
            bindInt(2, task.intervalDays)
            bindText(3, task.dueDate)
            bindText(4, task.status.value)
        }
        val id = db.queryLong("SELECT last_insert_rowid()")
        task.id = id
        return id
    }

    fun updateRevision(task: RevisionTask) {
        db.run(
            """UPDATE RevisionTasks SET due_date=?, status=? WHERE id=?"""
        ) {
            bindText(1, task.dueDate)
            bindText(2, task.status.value)
            bindLong(3, task.id!!)
        }
    }

    fun findPendingRevision(topicId: Long, intervalDays: Int): RevisionTask? {
        val stmt = db.prepareStatement(
            """SELECT id, topic_id, interval_days, due_date, status
               FROM RevisionTasks WHERE topic_id = ? AND interval_days = ? AND status = 'pending'"""
        )
        return try {
            stmt.bindLong(1, topicId)
            stmt.bindInt(2, intervalDays)
            if (stmt.step() == StepResult.ROW) readRevision(stmt) else null
        } finally {
            stmt.finalize()
        }
    }

    fun findRevisionsForTopic(topicId: Long): List<RevisionTask> {
        val stmt = db.prepareStatement(
            """SELECT id, topic_id, interval_days, due_date, status
               FROM RevisionTasks WHERE topic_id = ? ORDER BY interval_days"""
        )
        val results = mutableListOf<RevisionTask>()
        try {
            stmt.bindLong(1, topicId)
            while (stmt.step() == StepResult.ROW) results.add(readRevision(stmt))
        } finally {
            stmt.finalize()
        }
        return results
    }

    fun findOpenRevisions(): List<RevisionTask> {
        val stmt = db.prepareStatement(
            """SELECT id, topic_id, interval_days, due_date, status
               FROM RevisionTasks WHERE status IN ('pending','overdue') ORDER BY due_date"""
        )
        val results = mutableListOf<RevisionTask>()
        try {
            while (stmt.step() == StepResult.ROW) results.add(readRevision(stmt))
        } finally {
            stmt.finalize()
        }
        return results
    }

    fun insertWeeklyReview(review: WeeklyReview): Long {
        db.run(
            """INSERT INTO WeeklyReviews(week_start_date, completed_summary, minutes_reflection,
               explain_implement, needs_revision, distractions, next_priority, adjustment,
               created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        ) {
            bindText(1, review.weekStartDate)
            bindText(2, review.completedSummary.take(2000))
            bindText(3, review.minutesReflection.take(2000))
            bindText(4, review.explainImplement.take(2000))
            bindText(5, review.needsRevision.take(2000))
            bindText(6, review.distractions.take(2000))
            bindText(7, review.nextPriority.take(500))
            bindText(8, review.adjustment.take(500))
            bindLong(9, review.createdAt)
            bindLong(10, review.updatedAt)
        }
        val id = db.queryLong("SELECT last_insert_rowid()")
        review.id = id
        return id
    }

    fun updateWeeklyReview(review: WeeklyReview) {
        db.run(
            """UPDATE WeeklyReviews SET completed_summary=?, minutes_reflection=?,
               explain_implement=?, needs_revision=?, distractions=?, next_priority=?,
               adjustment=?, updated_at=? WHERE id=?"""
        ) {
            bindText(1, review.completedSummary.take(2000))
            bindText(2, review.minutesReflection.take(2000))
            bindText(3, review.explainImplement.take(2000))
            bindText(4, review.needsRevision.take(2000))
            bindText(5, review.distractions.take(2000))
            bindText(6, review.nextPriority.take(500))
            bindText(7, review.adjustment.take(500))
            bindLong(8, review.updatedAt)
            bindLong(9, review.id!!)
        }
    }

    fun findWeeklyReview(weekStartDate: String): WeeklyReview? {
        val stmt = db.prepareStatement(
            """SELECT id, week_start_date, completed_summary, minutes_reflection, explain_implement,
               needs_revision, distractions, next_priority, adjustment, created_at, updated_at
               FROM WeeklyReviews WHERE week_start_date = ?"""
        )
        return try {
            stmt.bindText(1, weekStartDate)
            if (stmt.step() == StepResult.ROW) readWeeklyReview(stmt) else null
        } finally {
            stmt.finalize()
        }
    }

    fun findAllWeeklyReviews(): List<WeeklyReview> {
        val stmt = db.prepareStatement(
            """SELECT id, week_start_date, completed_summary, minutes_reflection, explain_implement,
               needs_revision, distractions, next_priority, adjustment, created_at, updated_at
               FROM WeeklyReviews ORDER BY week_start_date DESC"""
        )
        val results = mutableListOf<WeeklyReview>()
        try {
            while (stmt.step() == StepResult.ROW) results.add(readWeeklyReview(stmt))
        } finally {
            stmt.finalize()
        }
        return results
    }

    private fun readPlan(stmt: org.isoron.platform.io.PreparedStatement) = LearningPlan(
        id = stmt.getLong(0),
        name = stmt.getText(1),
        startDate = stmt.getText(2),
        currentDayIndex = stmt.getInt(3),
        status = PlanStatus.from(stmt.getText(4)),
        createdAt = stmt.getLong(5)
    )

    private fun readDay(stmt: org.isoron.platform.io.PreparedStatement) = LearningDay(
        id = stmt.getLong(0),
        planId = stmt.getLong(1),
        dayIndex = stmt.getInt(2),
        title = stmt.getText(3),
        description = stmt.getText(4),
        estimatedMinutes = stmt.getInt(5),
        isRestDay = stmt.getInt(6) == 1,
        status = DayStatus.from(stmt.getText(7)),
        scheduledDate = stmt.getTextOrNull(8)
    )

    private fun readTask(stmt: org.isoron.platform.io.PreparedStatement) = LearningTask(
        id = stmt.getLong(0),
        dayId = stmt.getLong(1),
        position = stmt.getInt(2),
        title = stmt.getText(3),
        description = stmt.getText(4),
        deliverable = stmt.getText(5),
        isDone = stmt.getInt(6) == 1
    )

    private fun readSession(stmt: org.isoron.platform.io.PreparedStatement) = StudySession(
        id = stmt.getLong(0),
        uuid = stmt.getText(1),
        startedAt = stmt.getLong(2),
        endedAt = stmt.getLong(3),
        durationMs = stmt.getLong(4),
        pausedMs = stmt.getLong(5),
        timingSource = SessionTimingSource.from(stmt.getText(6)),
        objective = stmt.getText(7),
        learned = stmt.getText(8),
        attempted = stmt.getText(9),
        unclear = stmt.getText(10),
        nextAction = stmt.getText(11),
        planDayId = stmt.getLongOrNull(12),
        topicId = stmt.getLongOrNull(13),
        calendarDate = stmt.getText(14)
    )

    private fun readTopic(stmt: org.isoron.platform.io.PreparedStatement) = LearningTopic(
        id = stmt.getLong(0),
        name = stmt.getText(1),
        category = stmt.getText(2),
        status = TopicStatus.from(stmt.getText(3)),
        firstStudiedAt = stmt.getLongOrNull(4),
        lastReviewedAt = stmt.getLongOrNull(5),
        nextReviewAt = stmt.getLongOrNull(6),
        notes = stmt.getText(7),
        masteryFlags = stmt.getInt(8),
        rating = stmt.getInt(9)
    )

    private fun readRevision(stmt: org.isoron.platform.io.PreparedStatement) = RevisionTask(
        id = stmt.getLong(0),
        topicId = stmt.getLong(1),
        intervalDays = stmt.getInt(2),
        dueDate = stmt.getText(3),
        status = RevisionStatus.from(stmt.getText(4))
    )

    private fun readWeeklyReview(stmt: org.isoron.platform.io.PreparedStatement) = WeeklyReview(
        id = stmt.getLong(0),
        weekStartDate = stmt.getText(1),
        completedSummary = stmt.getText(2),
        minutesReflection = stmt.getText(3),
        explainImplement = stmt.getText(4),
        needsRevision = stmt.getText(5),
        distractions = stmt.getText(6),
        nextPriority = stmt.getText(7),
        adjustment = stmt.getText(8),
        createdAt = stmt.getLong(9),
        updatedAt = stmt.getLong(10)
    )
}

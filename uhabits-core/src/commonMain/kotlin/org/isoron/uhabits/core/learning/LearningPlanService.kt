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

class LearningPlanService(private val repository: LearningRepository) {

    fun ensurePlan(startDate: String, nowMillis: Long): LearningPlan =
        JavaLearningPlanSeeder.ensureSeeded(repository, startDate, nowMillis)

    fun getActivePlan(): LearningPlan? = repository.findActivePlan()

    fun getCurrentDay(plan: LearningPlan): LearningDay? =
        repository.findDayByPlanAndIndex(plan.id!!, plan.currentDayIndex)

    fun getAllDays(plan: LearningPlan): List<LearningDay> =
        repository.findDaysForPlan(plan.id!!)

    fun pausePlan(plan: LearningPlan) {
        plan.status = PlanStatus.PAUSED
        repository.updatePlan(plan)
    }

    fun resumePlan(plan: LearningPlan) {
        plan.status = PlanStatus.ACTIVE
        repository.updatePlan(plan)
    }

    fun setStartDate(plan: LearningPlan, startDate: String) {
        plan.startDate = startDate
        repository.updatePlan(plan)
    }

    fun updateDay(day: LearningDay) = repository.updateDay(day)

    fun updateTask(task: LearningTask) = repository.updateTask(task)

    fun markDaySkipped(day: LearningDay, plan: LearningPlan) {
        day.status = DayStatus.SKIPPED
        repository.updateDay(day)
        advanceIfNeeded(plan, day)
    }

    fun markDayCompleted(day: LearningDay, plan: LearningPlan) {
        day.status = DayStatus.COMPLETED
        repository.updateDay(day)
        advanceIfNeeded(plan, day)
    }

    fun markDayInProgress(day: LearningDay) {
        if (day.status == DayStatus.PENDING || day.status == DayStatus.MISSED) {
            day.status = DayStatus.IN_PROGRESS
            repository.updateDay(day)
        }
    }

    /**
     * Explicit reschedule: shift pending/future days' scheduledDate forward by [daysForward].
     * Does not delete sessions or change completed history.
     */
    fun rescheduleForward(plan: LearningPlan, daysForward: Int, addDays: (String, Int) -> String) {
        if (daysForward <= 0) return
        for (day in repository.findDaysForPlan(plan.id!!)) {
            if (day.status == DayStatus.COMPLETED || day.status == DayStatus.SKIPPED) continue
            val base = day.scheduledDate ?: plan.startDate
            day.scheduledDate = addDays(base, daysForward)
            repository.updateDay(day)
        }
    }

    fun setCurrentDayIndex(plan: LearningPlan, dayIndex: Int) {
        plan.currentDayIndex = dayIndex.coerceIn(1, 30)
        repository.updatePlan(plan)
        val day = repository.findDayByPlanAndIndex(plan.id!!, plan.currentDayIndex)
        if (day != null && day.status == DayStatus.PENDING) {
            markDayInProgress(day)
        }
    }

    private fun advanceIfNeeded(plan: LearningPlan, day: LearningDay) {
        if (day.dayIndex != plan.currentDayIndex) return
        val next = repository.findDayByPlanAndIndex(plan.id!!, day.dayIndex + 1)
        if (next != null) {
            plan.currentDayIndex = next.dayIndex
            repository.updatePlan(plan)
            if (next.status == DayStatus.PENDING) markDayInProgress(next)
        } else {
            plan.status = PlanStatus.COMPLETED
            repository.updatePlan(plan)
        }
    }
}

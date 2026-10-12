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

import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday

object LearningDateUtils {

    fun format(date: LocalDate): String {
        val y = date.year
        val m = date.month.toString().padStart(2, '0')
        val d = date.day.toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    fun parse(iso: String): LocalDate {
        val parts = iso.split("-")
        require(parts.size == 3) { "Invalid date: $iso" }
        return LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }

    fun todayIso(): String = format(getToday())

    fun addDays(iso: String, days: Int): String = format(parse(iso).plus(days))

    fun weekStart(iso: String, firstWeekday: DayOfWeek): String =
        format(parse(iso).startOfWeek(firstWeekday))

    fun weekEnd(weekStartIso: String): String = addDays(weekStartIso, 6)

    fun displayLabel(iso: String): String {
        val d = parse(iso)
        return "${d.year}-${d.month.toString().padStart(2, '0')}-${d.day.toString().padStart(2, '0')}"
    }
}

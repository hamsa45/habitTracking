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
package org.isoron.uhabits.activities.learning

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.learning.ActiveSessionState
import org.isoron.uhabits.core.learning.LearningDateUtils
import org.isoron.uhabits.databinding.ActivityStudySessionBinding
import org.isoron.uhabits.utils.applyRootViewInsets

class StudySessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudySessionBinding
    private val handler = Handler(Looper.getMainLooper())
    private var finishing = false

    private val tick = object : Runnable {
        override fun run() {
            updateTimer()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudySessionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyRootViewInsets()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.learning_start_studying)

        val facade = HabitsApplication.component.learningFacade
        val prefs = HabitsApplication.component.preferences
        val vm = facade.todayViewModel()
        val day = vm.day
        binding.sessionTopicText.text = day?.title ?: ""

        val taskTitles = mutableListOf(getString(R.string.learning_select_task))
        day?.tasks?.forEach { taskTitles.add(it.title) }
        binding.sessionTaskSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, taskTitles)

        val existing = facade.sessionService.getActive()
        if (existing == null && day != null) {
            val objective = day.tasks.firstOrNull { !it.isDone }?.title ?: day.title
            binding.sessionCustomObjective.setText(objective)
            facade.sessionService.start(
                SystemClock.elapsedRealtime(),
                System.currentTimeMillis(),
                objective,
                day.id,
                facade.repository.findTopicByName(day.title)?.id
            )
        } else if (existing != null) {
            binding.sessionCustomObjective.setText(existing.objective)
        }

        binding.sessionPauseButton.setOnClickListener {
            val active = facade.sessionService.getActive() ?: return@setOnClickListener
            if (active.state == ActiveSessionState.RUNNING) {
                facade.sessionService.pause(SystemClock.elapsedRealtime())
                binding.sessionPauseButton.text = getString(R.string.learning_resume)
            } else {
                facade.sessionService.resume(SystemClock.elapsedRealtime())
                binding.sessionPauseButton.text = getString(R.string.learning_pause)
            }
            updateTimer()
        }

        binding.sessionFinishButton.setOnClickListener {
            finishing = true
            handler.removeCallbacks(tick)
            binding.sessionSaveForm.visibility = View.VISIBLE
            val minutes = facade.sessionService.elapsedMs(SystemClock.elapsedRealtime()) / 60_000L
            binding.sessionDurationMinutes.setText(minutes.toString())
            val selected = binding.sessionTaskSpinner.selectedItemPosition
            if (selected > 0) {
                binding.sessionCustomObjective.setText(taskTitles[selected])
            }
        }

        binding.sessionSaveButton.setOnClickListener {
            val minutesText = binding.sessionDurationMinutes.text?.toString().orEmpty()
            val minutes = minutesText.toLongOrNull()
            if (minutes == null || minutes < 0) {
                binding.sessionValidationError.visibility = View.VISIBLE
                binding.sessionValidationError.text = getString(R.string.learning_duration_invalid)
                return@setOnClickListener
            }
            val objective = binding.sessionCustomObjective.text?.toString()?.trim().orEmpty()
            val active = facade.sessionService.getActive()
            if (active != null && objective.isNotEmpty()) {
                active.objective = objective
                facade.sessionService.persistActive(active)
            }
            try {
                facade.sessionService.finishAndSave(
                    elapsedRealtime = SystemClock.elapsedRealtime(),
                    wallEndedAt = System.currentTimeMillis(),
                    calendarDate = LearningDateUtils.todayIso(),
                    learned = binding.sessionLearned.text?.toString().orEmpty(),
                    attempted = binding.sessionAttempted.text?.toString().orEmpty(),
                    unclear = binding.sessionUnclear.text?.toString().orEmpty(),
                    nextAction = binding.sessionNextAction.text?.toString().orEmpty(),
                    durationOverrideMs = minutes * 60_000L,
                    requireMeaningfulForCompletion = prefs.requireMeaningfulNoteForCompletion,
                    addDays = LearningDateUtils::addDays
                )
                Toast.makeText(this, R.string.learning_save_session, Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                binding.sessionValidationError.visibility = View.VISIBLE
                binding.sessionValidationError.text = e.message
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!finishing) handler.post(tick)
        val active = HabitsApplication.component.learningFacade.sessionService.getActive()
        binding.sessionPauseButton.text = if (active?.state == ActiveSessionState.PAUSED) {
            getString(R.string.learning_resume)
        } else {
            getString(R.string.learning_pause)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    private fun updateTimer() {
        val ms = HabitsApplication.component.learningFacade.sessionService
            .elapsedMs(SystemClock.elapsedRealtime())
        val totalSec = (ms / 1000).toInt()
        val min = totalSec / 60
        val sec = totalSec % 60
        binding.sessionTimerText.text = "%02d:%02d".format(min, sec)
        binding.sessionTimerText.contentDescription = binding.sessionTimerText.text
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

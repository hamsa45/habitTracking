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

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.databinding.ActivityLearningMainBinding
import org.isoron.uhabits.notifications.StudyReminderScheduler
import org.isoron.uhabits.utils.applyRootViewInsets

class LearningMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearningMainBinding
    private lateinit var prefs: Preferences
    private var permissionAsked = false

    private val permissionLauncher =
        registerForActivityResult(RequestPermission()) { granted ->
            if (granted) {
                StudyReminderScheduler(this).scheduleAll()
            } else {
                Log.i("LearningMainActivity", "POST_NOTIFICATIONS denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_JavaLearning)
        super.onCreate(savedInstanceState)
        prefs = HabitsApplication.component.preferences
        HabitsApplication.component.learningFacade.ensurePlanReady()

        binding = ActivityLearningMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyRootViewInsets()

        if (savedInstanceState == null) {
            openFragment(TodayFragment())
            binding.learningBottomNav.selectedItemId = R.id.nav_today
        }

        binding.learningBottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_today -> TodayFragment()
                R.id.nav_plan -> PlanFragment()
                R.id.nav_topics -> TopicsFragment()
                R.id.nav_history -> HistoryFragment()
                R.id.nav_more -> MoreFragment()
                else -> return@setOnItemSelectedListener false
            }
            openFragment(fragment)
            true
        }

        maybeRequestNotificationPermission()
        StudyReminderScheduler(this).scheduleAll()
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.learningFragmentContainer, fragment)
            .commit()
    }

    private fun maybeRequestNotificationPermission() {
        if (!prefs.studyReminderEnabled) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(this, POST_NOTIFICATIONS) == PERMISSION_GRANTED) return
        if (permissionAsked) return
        permissionAsked = true
        AlertDialog.Builder(this)
            .setMessage(R.string.learning_notification_rationale)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                permissionLauncher.launch(POST_NOTIFICATIONS)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val EXTRA_OPEN_TODAY = "open_today"
    }
}

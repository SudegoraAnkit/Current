package com.sudegoratechglobal.current.util

import com.sudegoratechglobal.current.data.local.TaskEntity
import java.util.Calendar

object StreakEngine {

    /**
     * Calculates the consecutive daily streak based on task completion times.
     * A streak is active if the user completed a task today or yesterday.
     * The streak is incremented for each consecutive prior day that contains at least one task completion.
     */
    fun calculateStreak(completedTasks: List<TaskEntity>): Int {
        val completedWithTime = completedTasks.filter { it.isCompleted && it.completionTime != null && it.completionTime > 0 }
        if (completedWithTime.isEmpty()) return 0

        // Map task completion times to start of day epoch days
        val completedDays = completedWithTime
            .map { task ->
                val cal = Calendar.getInstance().apply { timeInMillis = task.completionTime!! }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis / (24 * 60 * 60 * 1000)
            }
            .toSet()
            .sortedDescending() // Newest completed days first

        if (completedDays.isEmpty()) return 0

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayEpochDay = todayCal.timeInMillis / (24 * 60 * 60 * 1000)
        val yesterdayEpochDay = todayEpochDay - 1

        val hasCompletedToday = completedDays.contains(todayEpochDay)
        val hasCompletedYesterday = completedDays.contains(yesterdayEpochDay)

        // If nothing was completed today and yesterday, streak is broken
        if (!hasCompletedToday && !hasCompletedYesterday) {
            return 0
        }

        var currentCheckDay = if (hasCompletedToday) todayEpochDay else yesterdayEpochDay
        var streak = 0

        // Walk backwards in time to count consecutive days
        while (completedDays.contains(currentCheckDay)) {
            streak++
            currentCheckDay--
        }

        return streak
    }
}

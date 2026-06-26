package com.sudegoratechglobal.current.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val scheduledTime: Long? = null, // Epoch timestamp in ms, null for Notes
    val isCompleted: Boolean = false,
    val priority: Int = 2, // 1 = High, 2 = Medium, 3 = Low
    val executionStyle: String = "POMODORO", // POMODORO, TIME_BOXING, EAT_THE_FROG
    val durationMinutes: Int? = 25, // Null for Notes
    val isLocked: Boolean = false, // Procrastination Tax lock
    val accountabilityContact: String? = null, // Accountability SMS recipient
    val completionTime: Long? = null,
    val elapsedTime: Long = 0, // In seconds
    val vibe: String? = null, // DEEP_FOCUS, LOW_ENERGY, HOME_BASE
    val focusInterruptionCount: Int = 0 // Schema field only for now
) {
    val isNote: Boolean
        get() = scheduledTime == null && durationMinutes == null
}

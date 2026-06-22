package com.sudegoratechglobal.current.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val scheduledTime: Long, // Epoch timestamp in ms
    val isCompleted: Boolean = false,
    val priority: Int = 2, // 1 = High, 2 = Medium, 3 = Low
    val executionStyle: String = "POMODORO", // POMODORO, TIME_BOXING, EAT_THE_FROG
    val durationMinutes: Int = 25,
    val isLocked: Boolean = false, // Procrastination Tax lock
    val accountabilityContact: String? = null, // Accountability SMS recipient
    val completionTime: Long? = null,
    val elapsedTime: Long = 0 // In seconds
)

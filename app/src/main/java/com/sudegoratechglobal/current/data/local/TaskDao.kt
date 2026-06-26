package com.sudegoratechglobal.current.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY scheduledTime ASC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND (scheduledTime IS NOT NULL OR durationMinutes IS NOT NULL) ORDER BY priority ASC, scheduledTime ASC")
    fun getActiveTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND scheduledTime IS NULL AND durationMinutes IS NULL ORDER BY id DESC")
    fun getNotesFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND completionTime >= :startOfDay AND completionTime <= :endOfDay ORDER BY completionTime DESC")
    fun getCompletedTodayFlow(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET isCompleted = 0, completionTime = null WHERE id = :id")
    suspend fun uncompleteTask(id: Long)

    @Query("SELECT * FROM tasks WHERE scheduledTime >= :startOfDay AND scheduledTime <= :endOfDay ORDER BY scheduledTime ASC")
    fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND isLocked = 1 AND scheduledTime < :time")
    suspend fun getOverdueLockedTasks(time: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}

package com.sudegoratechglobal.current.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY scheduledTime ASC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority ASC, scheduledTime ASC")
    fun getActiveTasksFlow(): Flow<List<TaskEntity>>

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

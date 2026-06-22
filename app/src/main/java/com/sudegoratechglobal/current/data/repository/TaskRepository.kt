package com.sudegoratechglobal.current.data.repository

import android.content.Context
import com.sudegoratechglobal.current.data.local.TaskDao
import com.sudegoratechglobal.current.data.local.TaskEntity
import com.sudegoratechglobal.current.data.remote.GoogleDriveService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class TaskRepository(
    private val taskDao: TaskDao,
    private val scope: CoroutineScope
) {
    fun getActiveTasksFlow(): Flow<List<TaskEntity>> = taskDao.getActiveTasksFlow()

    fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>> =
        taskDao.getTasksForDateRange(startOfDay, endOfDay)

    fun getAllTasksFlow(): Flow<List<TaskEntity>> = taskDao.getAllTasksFlow()

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    suspend fun insertTask(context: Context, task: TaskEntity): Long {
        val id = taskDao.insertTask(task)
        triggerBackup(context)
        return id
    }

    suspend fun updateTask(context: Context, task: TaskEntity) {
        taskDao.updateTask(task)
        triggerBackup(context)
    }

    suspend fun deleteTask(context: Context, task: TaskEntity) {
        taskDao.deleteTask(task)
        triggerBackup(context)
    }

    // Google Drive sync helper
    fun triggerBackup(context: Context) {
        scope.launch(Dispatchers.IO) {
            if (GoogleDriveService.isLinked.value) {
                val allTasks = taskDao.getAllTasksFlow().firstOrNull() ?: emptyList()
                val json = Json.encodeToString(ListSerializer(TaskEntity.serializer()), allTasks)
                GoogleDriveService.backup(context, json)
            }
        }
    }

    suspend fun restoreFromBackup(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            val json = GoogleDriveService.restore(context)
            if (json != null) {
                try {
                    val tasks = Json.decodeFromString(ListSerializer(TaskEntity.serializer()), json)
                    for (task in tasks) {
                        taskDao.insertTask(task)
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            } else {
                false
            }
        }
    }

    // Low friction Team Sync (Single-Link Space)
    suspend fun syncTeamSpace(context: Context, urlString: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val jsonBuilder = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        jsonBuilder.append(line)
                    }
                    reader.close()

                    val tasksJson = jsonBuilder.toString()
                    val tasks = Json.decodeFromString(ListSerializer(TaskEntity.serializer()), tasksJson)
                    var count = 0
                    for (task in tasks) {
                        // Insert task as a fresh item in local database
                        val taskToInsert = task.copy(id = 0, isCompleted = false)
                        taskDao.insertTask(taskToInsert)
                        count++
                    }
                    triggerBackup(context)
                    Result.success(count)
                } else {
                    Result.failure(Exception("Server returned HTTP code $responseCode"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

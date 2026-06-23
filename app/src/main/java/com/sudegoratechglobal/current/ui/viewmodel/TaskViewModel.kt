package com.sudegoratechglobal.current.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sudegoratechglobal.current.data.local.AppDatabase
import com.sudegoratechglobal.current.data.local.TaskEntity
import com.sudegoratechglobal.current.data.remote.GoogleDriveService
import com.sudegoratechglobal.current.data.repository.TaskRepository
import com.sudegoratechglobal.current.util.AlarmReceiver
import com.sudegoratechglobal.current.util.NlpParser
import com.sudegoratechglobal.current.widget.AestheticTimerWidget
import com.sudegoratechglobal.current.util.StreakEngine
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("current_app_prefs", Context.MODE_PRIVATE)

    // DB Initialization states
    private val _dbInitialized = MutableStateFlow(false)
    val dbInitialized: StateFlow<Boolean> = _dbInitialized

    private val _dbError = MutableStateFlow<String?>(null)
    val dbError: StateFlow<String?> = _dbError

    @Volatile
    private var repository: TaskRepository? = null

    // Data Flows mapped reactively after initialization
    val activeTasks: StateFlow<List<TaskEntity>>
    val allTasks: StateFlow<List<TaskEntity>>
    val streakCount: StateFlow<Int>

    // Onboarding Flows
    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    // Google Drive state delegation
    val isDriveLinked: StateFlow<Boolean> = GoogleDriveService.isLinked
    val driveSyncState: StateFlow<GoogleDriveService.SyncState> = GoogleDriveService.syncState

    // Active Timer Flows
    private val _activeTimerTask = MutableStateFlow<TaskEntity?>(null)
    val activeTimerTask: StateFlow<TaskEntity?> = _activeTimerTask

    private val _timerRemainingSeconds = MutableStateFlow(0)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds

    private val _timerIsRunning = MutableStateFlow(false)
    val timerIsRunning: StateFlow<Boolean> = _timerIsRunning

    private val _timerType = MutableStateFlow("WORK") // WORK, BREAK, TIME_BOX, FROG
    val timerType: StateFlow<String> = _timerType

    private val _timerTotalDuration = MutableStateFlow(25 * 60) // in seconds

    private var timerJob: Job? = null

    init {
        // Asynchronously initialize database off the main thread to prevent startup crash
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(application)
                repository = TaskRepository(database.taskDao(), viewModelScope)
                GoogleDriveService.init(application)
                _dbInitialized.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _dbError.value = e.localizedMessage ?: e.toString()
            }
        }

        activeTasks = _dbInitialized
            .flatMapLatest { initialized ->
                if (initialized) {
                    repository?.getActiveTasksFlow() ?: flowOf(emptyList())
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allTasks = _dbInitialized
            .flatMapLatest { initialized ->
                if (initialized) {
                    repository?.getAllTasksFlow() ?: flowOf(emptyList())
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        streakCount = allTasks
            .map { tasks -> StreakEngine.calculateStreak(tasks) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        _onboardingCompleted.value = sharedPrefs.getBoolean("onboarding_done", false)
        _userName.value = sharedPrefs.getString("user_name", "") ?: ""
    }

    /**
     * Helper function that suspends until database initialization completes,
     * ensuring repository operations don't run on null references.
     */
    private suspend fun getSafeRepository(): TaskRepository? {
        _dbInitialized.first { it }
        return repository
    }

    // Onboarding Actions
    fun saveOnboardingName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            sharedPrefs.edit()
                .putString("user_name", trimmed)
                .putBoolean("onboarding_done", true)
                .apply()
            _userName.value = trimmed
            _onboardingCompleted.value = true
        }
    }

    // Task Operations
    fun addTaskFromNlp(input: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = getSafeRepository() ?: return@launch
            val parsed = NlpParser.parse(input)
            val task = TaskEntity(
                title = parsed.title,
                scheduledTime = parsed.scheduledTime,
                priority = parsed.priority,
                isLocked = parsed.isLocked,
                accountabilityContact = parsed.accountabilityContact,
                executionStyle = "POMODORO" // Default execution style
            )
            val insertedId = repo.insertTask(getApplication(), task)
            
            if (task.isLocked) {
                val createdTask = task.copy(id = insertedId)
                scheduleProcrastinationAlarm(createdTask)
            }
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = getSafeRepository() ?: return@launch
            val completedTask = task.copy(
                isCompleted = true,
                completionTime = System.currentTimeMillis()
            )
            repo.updateTask(getApplication(), completedTask)
            cancelProcrastinationAlarm(task)
            // If the completed task is currently timed, stop timer
            if (_activeTimerTask.value?.id == task.id) {
                stopTimer()
            }
        }
    }

    fun updateTaskExecutionStyle(task: TaskEntity, style: String, durationMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = getSafeRepository() ?: return@launch
            val updated = task.copy(
                executionStyle = style,
                durationMinutes = durationMinutes
            )
            repo.updateTask(getApplication(), updated)
        }
    }

    fun updateTaskDuration(task: TaskEntity, durationMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = getSafeRepository() ?: return@launch
            val updated = task.copy(durationMinutes = durationMinutes)
            repo.updateTask(getApplication(), updated)
        }
    }

    fun rescheduleTask(task: TaskEntity, newTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = getSafeRepository() ?: return@launch
            val updated = task.copy(scheduledTime = newTime)
            repo.updateTask(getApplication(), updated)
            if (task.isLocked) {
                cancelProcrastinationAlarm(task)
                scheduleProcrastinationAlarm(updated)
            }
        }
    }

    fun swipeTaskToTomorrow(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = getSafeRepository() ?: return@launch
            val calendar = Calendar.getInstance().apply {
                timeInMillis = task.scheduledTime
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val updated = task.copy(scheduledTime = calendar.timeInMillis)
            repo.updateTask(getApplication(), updated)
            if (task.isLocked) {
                cancelProcrastinationAlarm(task)
                scheduleProcrastinationAlarm(updated)
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = getSafeRepository() ?: return@launch
            repo.deleteTask(getApplication(), task)
            cancelProcrastinationAlarm(task)
            if (_activeTimerTask.value?.id == task.id) {
                stopTimer()
            }
        }
    }

    // Google Drive Backup Actions
    fun linkGoogleDrive() {
        viewModelScope.launch {
            val repo = getSafeRepository() ?: return@launch
            val success = GoogleDriveService.link(getApplication(), _userName.value)
            if (success) {
                repo.triggerBackup(getApplication())
            }
        }
    }

    fun unlinkGoogleDrive() {
        GoogleDriveService.unlink(getApplication())
    }

    // Low-Friction Team Sync (Deep-Link imports)
    fun importTeamTasks(urlString: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val repo = getSafeRepository()
            if (repo == null) {
                onResult("Database not initialized")
                return@launch
            }
            val result = repo.syncTeamSpace(getApplication(), urlString)
            result.onSuccess { count ->
                onResult("Synced Team Space: added $count items to your Focus Zone!")
            }.onFailure { exception ->
                onResult("Sync failed: ${exception.localizedMessage ?: "Network error"}")
            }
        }
    }

    // Widget refresh helper
    private fun updateWidgetState(taskTitle: String, timeText: String, isRunning: Boolean) {
        sharedPrefs.edit()
            .putString("widget_task_title", taskTitle)
            .putString("widget_time_text", timeText)
            .putBoolean("widget_timer_running", isRunning)
            .apply()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                AestheticTimerWidget().updateAll(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Timer Execution Engine
    fun startTimer(task: TaskEntity, type: String) {
        timerJob?.cancel()
        _activeTimerTask.value = task
        _timerType.value = type

        val durationSec = task.durationMinutes * 60
        _timerTotalDuration.value = durationSec
        _timerRemainingSeconds.value = durationSec
        _timerIsRunning.value = true

        val initialTimeText = "${durationSec / 60}:00"
        updateWidgetState(task.title, initialTimeText, true)

        timerJob = viewModelScope.launch {
            var seconds = durationSec
            while (seconds > 0) {
                delay(1000)
                if (_timerIsRunning.value) {
                    seconds--
                    _timerRemainingSeconds.value = seconds
                    val timeText = "${seconds / 60}:${String.format("%02d", seconds % 60)}"
                    updateWidgetState(task.title, timeText, true)

                    // Accumulate elapsed time on task
                    val currentTask = _activeTimerTask.value
                    if (currentTask != null) {
                        val updatedTask = currentTask.copy(elapsedTime = currentTask.elapsedTime + 1)
                        _activeTimerTask.value = updatedTask
                        repository?.updateTask(getApplication(), updatedTask)
                    }
                }
            }
            onTimerComplete()
        }
    }

    fun pauseTimer() {
        _timerIsRunning.value = false
        val seconds = _timerRemainingSeconds.value
        val timeText = "${seconds / 60}:${String.format("%02d", seconds % 60)}"
        updateWidgetState(_activeTimerTask.value?.title ?: "No Active Session", timeText, false)
    }

    fun resumeTimer() {
        _timerIsRunning.value = true
        val seconds = _timerRemainingSeconds.value
        val timeText = "${seconds / 60}:${String.format("%02d", seconds % 60)}"
        updateWidgetState(_activeTimerTask.value?.title ?: "No Active Session", timeText, true)
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _activeTimerTask.value = null
        _timerIsRunning.value = false
        _timerRemainingSeconds.value = 0
        updateWidgetState("Lock-in Clear", "Focus Mode", false)
    }

    private fun onTimerComplete() {
        val task = _activeTimerTask.value ?: return
        val type = _timerType.value
        
        // Broadcast completion intent
        val intent = Intent(getApplication(), AlarmReceiver::class.java).apply {
            action = "com.sudegoratechglobal.current.ACTION_TIMER_COMPLETE"
            putExtra("task_title", task.title)
            putExtra("timer_type", type)
        }
        getApplication<Application>().sendBroadcast(intent)

        if (type == "POMODORO") {
            // Trigger 5-minute break automatically
            startTimer(task.copy(durationMinutes = 5), "BREAK")
        } else {
            stopTimer()
        }
    }

    fun getTimerProgress(): Float {
        val total = _timerTotalDuration.value.toFloat()
        if (total == 0f) return 0f
        return (_timerRemainingSeconds.value.toFloat() / total).coerceIn(0f, 1f)
    }

    // AlarmManager exact alarms for locked tasks
    private fun scheduleProcrastinationAlarm(task: TaskEntity) {
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(getApplication(), AlarmReceiver::class.java).apply {
            action = "com.sudegoratechglobal.current.ACTION_PROCRASTINATION_TAX"
            putExtra("task_id", task.id)
            putExtra("task_title", task.title)
            putExtra("contact", task.accountabilityContact)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.scheduledTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, task.scheduledTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.scheduledTime, pendingIntent)
        }
    }

    private fun cancelProcrastinationAlarm(task: TaskEntity) {
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(getApplication(), AlarmReceiver::class.java).apply {
            action = "com.sudegoratechglobal.current.ACTION_PROCRASTINATION_TAX"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

package com.sudegoratechglobal.current.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

object GoogleDriveService {

    private const val PREFS_NAME = "current_drive_prefs"
    private const val KEY_IS_LINKED = "is_linked"
    private const val KEY_LAST_SYNC = "last_sync_time"

    private val _isLinked = MutableStateFlow(false)
    val isLinked: StateFlow<Boolean> = _isLinked

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        object Success : SyncState()
        data class Error(val message: String) : SyncState()
    }

    fun init(context: Context) {
        val prefs = getPrefs(context)
        _isLinked.value = prefs.getBoolean(KEY_IS_LINKED, false)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun link(context: Context, userName: String): Boolean {
        _syncState.value = SyncState.Syncing
        return withContext(Dispatchers.IO) {
            // Simulate Google OAuth2 authentication flow
            delay(1500)
            getPrefs(context).edit()
                .putBoolean(KEY_IS_LINKED, true)
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .apply()
            _isLinked.value = true
            _syncState.value = SyncState.Success
            true
        }
    }

    fun unlink(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LINKED, false)
            .remove(KEY_LAST_SYNC)
            .apply()
        _isLinked.value = false
        _syncState.value = SyncState.Idle

        // Delete simulated remote file
        val backupFile = File(context.filesDir, "simulated_gdrive_backup.json")
        if (backupFile.exists()) {
            backupFile.delete()
        }
    }

    suspend fun backup(context: Context, tasksJson: String): Boolean {
        if (!_isLinked.value) return false
        _syncState.value = SyncState.Syncing
        return withContext(Dispatchers.IO) {
            try {
                // Simulate network latency
                delay(800)
                // Save JSON to simulated drive folder (isolated file in app private files)
                val backupFile = File(context.filesDir, "simulated_gdrive_backup.json")
                backupFile.writeText(tasksJson)
                
                getPrefs(context).edit()
                    .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                    .apply()
                _syncState.value = SyncState.Success
                true
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.localizedMessage ?: "Backup failed")
                false
            }
        }
    }

    suspend fun restore(context: Context): String? {
        if (!_isLinked.value) return null
        _syncState.value = SyncState.Syncing
        return withContext(Dispatchers.IO) {
            try {
                delay(1000)
                val backupFile = File(context.filesDir, "simulated_gdrive_backup.json")
                if (backupFile.exists()) {
                    _syncState.value = SyncState.Success
                    backupFile.readText()
                } else {
                    _syncState.value = SyncState.Idle
                    null
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.localizedMessage ?: "Restore failed")
                null
            }
        }
    }

    fun getLastSyncTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SYNC, 0L)
    }
}

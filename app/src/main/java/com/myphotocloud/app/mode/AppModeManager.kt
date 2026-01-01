// AppModeManager.kt
package com.myphotocloud.app.mode

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.myphotocloud.app.model.AppMode
import com.myphotocloud.app.receiver.AutoSyncAlarmReceiver
import com.myphotocloud.app.server.BackupServerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 앱 모드 관리
 */
class AppModeManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("app_mode", Context.MODE_PRIVATE)
    
    private val _currentMode = MutableStateFlow(loadMode())
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()
    
    var mode: AppMode
        get() = _currentMode.value
        set(value) {
            _currentMode.value = value
            saveMode(value)
            applyMode(value)
        }
    
    private fun loadMode(): AppMode {
        val modeName = prefs.getString(KEY_MODE, AppMode.STANDALONE.name)
        return try {
            AppMode.valueOf(modeName!!)
        } catch (e: Exception) {
            AppMode.STANDALONE
        }
    }
    
    private fun saveMode(mode: AppMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }
    
    private fun applyMode(mode: AppMode) {
        when (mode) {
            AppMode.CLIENT_ONLY -> {
                stopServer()
                enableBackup()
            }
            AppMode.SERVER_ONLY -> {
                startServer()
                disableBackup()
            }
            AppMode.STANDALONE -> {
                startServer()
                enableBackup()
            }
        }
    }
    
    private fun startServer() {
        try {
            val intent = Intent(context, BackupServerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopServer() {
        try {
            val intent = Intent(context, BackupServerService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun enableBackup() {
        AutoSyncAlarmReceiver.schedule(context)
    }
    
    private fun disableBackup() {
        AutoSyncAlarmReceiver.cancel(context)
    }
    
    companion object {
        private const val KEY_MODE = "mode"
    }
}

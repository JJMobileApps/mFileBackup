// MainActivity.kt
package com.myphotocloud.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.myphotocloud.app.mode.AppModeManager
import com.myphotocloud.app.ui.MainScreen
import com.myphotocloud.app.ui.WelcomeScreen
import com.myphotocloud.app.ui.theme.MyPhotoCloudTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var modeManager: AppModeManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        modeManager = AppModeManager(this)
        
        setContent {
            MyPhotoCloudTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }
    
    @Composable
    private fun AppContent() {
        val context = LocalContext.current
        val prefs = remember {
            context.getSharedPreferences("app_prefs", MODE_PRIVATE)
        }
        
        var isFirstRun by remember {
            mutableStateOf(prefs.getBoolean("first_run", true))
        }
        
        var showSettings by remember { mutableStateOf(false) }
        
        when {
            // 첫 실행 - 환영 화면
            isFirstRun -> {
                WelcomeScreen(
                    onModeSelected = { mode ->
                        modeManager.mode = mode
                        prefs.edit().putBoolean("first_run", false).apply()
                        isFirstRun = false
                    }
                )
            }
            // 설정 화면
            showSettings -> {
                val currentMode by modeManager.currentMode.collectAsState()
                com.myphotocloud.app.ui.SettingsScreen(
                    currentMode = currentMode,
                    onModeChanged = { newMode ->
                        modeManager.mode = newMode
                        showSettings = false
                        // 모드 변경 시 앱 재시작 (선택사항)
                        // recreate()
                    },
                    onBackClick = {
                        showSettings = false
                    }
                )
            }
            // 메인 화면
            else -> {
                val currentMode by modeManager.currentMode.collectAsState()
                MainScreen(
                    mode = currentMode,
                    onSettingsClick = {
                        showSettings = true
                    }
                )
            }
        }
    }
}

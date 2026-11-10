package com.vibecode.minitodo

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.vibecode.minitodo.data.prefs.AppSettings
import com.vibecode.minitodo.ui.AppNavHost
import com.vibecode.minitodo.ui.theme.MiniTodoTheme
import com.vibecode.minitodo.ui.theme.applySystemBarStyle
import com.vibecode.minitodo.ui.theme.resolveDarkTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private var latestIntent by mutableStateOf<Intent?>(null)
    private var lastExactAlarmAllowed: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            latestIntent = intent
        }

        val app = application as MiniTodoApp
        setContent {
            val settingsFlow = remember(app.container.settingsRepository) {
                app.container.settingsRepository.settings
                    .map<AppSettings, AppSettings?> { it }
            }
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            val currentSettings = settings
            if (currentSettings != null) {
                val systemDark = isSystemInDarkTheme()
                val darkTheme = resolveDarkTheme(currentSettings.themeMode, systemDark)

                LaunchedEffect(darkTheme) {
                    this@MainActivity.applySystemBarStyle(darkTheme)
                }

                MiniTodoTheme(
                    darkTheme = darkTheme,
                    palette = currentSettings.themePalette,
                ) {
                    val navController = rememberNavController()
                    LaunchedEffect(latestIntent) {
                        latestIntent?.let { intent ->
                            navController.handleDeepLink(intent)
                            latestIntent = null
                        }
                    }
                    AppNavHost(
                        navController = navController,
                        container = app.container,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        latestIntent = intent
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val allowed = getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        val previous = lastExactAlarmAllowed
        if (previous != null && previous != allowed) {
            (application as MiniTodoApp).container.todoRepository.requestFullRefresh()
        }
        lastExactAlarmAllowed = allowed
    }
}

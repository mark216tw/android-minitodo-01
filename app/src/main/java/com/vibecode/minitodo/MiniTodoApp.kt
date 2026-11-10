package com.vibecode.minitodo

import android.app.Application
import android.content.Context
import com.vibecode.minitodo.data.backup.BackupManager
import com.vibecode.minitodo.data.local.AppDatabase
import com.vibecode.minitodo.data.prefs.SettingsRepository
import com.vibecode.minitodo.data.repository.TodoRepository
import com.vibecode.minitodo.notification.NotificationHelper
import com.vibecode.minitodo.notification.ReminderScheduler
import com.vibecode.minitodo.widget.WidgetRefreshWorker
import com.vibecode.minitodo.widget.WidgetStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database = AppDatabase.create(
        context = appContext,
        allowDestructiveMigration = BuildConfig.DEBUG,
    )

    val todoRepository = TodoRepository(database)
    val settingsRepository = SettingsRepository(appContext)
    val backupManager = BackupManager(appContext, todoRepository)
    val widgetStore = WidgetStore(appContext)
    val reminderScheduler = ReminderScheduler(appContext, todoRepository)
}

class MiniTodoApp : Application() {

    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
        WidgetRefreshWorker.schedule(this)
        container.todoRepository.requestFullRefresh()
        appScope.launch {
            SideEffectCoordinator(
                context = this@MiniTodoApp,
                repository = container.todoRepository,
                settingsRepository = container.settingsRepository,
                reminderScheduler = container.reminderScheduler,
            ).run()
        }
    }
}

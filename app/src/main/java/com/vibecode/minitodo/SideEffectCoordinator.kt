package com.vibecode.minitodo

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.vibecode.minitodo.data.prefs.SettingsRepository
import com.vibecode.minitodo.data.repository.TodoRepository
import com.vibecode.minitodo.notification.ReminderScheduler
import com.vibecode.minitodo.widget.TodoWidgetReceiver
import com.vibecode.minitodo.widget.TodoWidgetUpdater
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SideEffectCoordinator(
    private val context: Context,
    private val repository: TodoRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) {

    suspend fun run() {
        coroutineScope {
            launch { observeDataChanges() }
            launch { observePaletteChanges() }
        }
    }

    private suspend fun observeDataChanges() {
        repository.changes.collect {
            delay(DEBOUNCE_MILLIS)
            val changes = repository.takeChanges()
            if (changes.isEmpty) return@collect
            if (changes.reminders) {
                reminderScheduler.rescheduleAll()
            }
            if (changes.widget && hasWidgets()) {
                TodoWidgetUpdater.updateAll(context)
            }
        }
    }

    private suspend fun observePaletteChanges() {
        settingsRepository.settings
            .map { it.themePalette }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                if (hasWidgets()) {
                    TodoWidgetUpdater.updateAll(context)
                }
            }
    }

    private fun hasWidgets(): Boolean {
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, TodoWidgetReceiver::class.java))
        return ids.isNotEmpty()
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 400L
    }
}

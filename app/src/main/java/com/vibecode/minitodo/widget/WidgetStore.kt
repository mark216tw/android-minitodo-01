package com.vibecode.minitodo.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.flow.first

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widgets")
internal val selectedTodoIdKey = longPreferencesKey("selected_todo_id")

class WidgetStore(private val context: Context) {

    private fun legacyKey(widgetId: Int) = longPreferencesKey("widget_todo_$widgetId")

    suspend fun getTodoId(glanceId: GlanceId, legacyWidgetId: Int): Long? {
        val selectedTodoId = getAppWidgetState(
            context,
            PreferencesGlanceStateDefinition,
            glanceId,
        )[selectedTodoIdKey]
        if (selectedTodoId != null) return selectedTodoId

        val legacyTodoId = getLegacyTodoId(legacyWidgetId)
        if (legacyTodoId != null) setTodoId(glanceId, legacyTodoId)
        return legacyTodoId
    }

    suspend fun getLegacyTodoId(widgetId: Int): Long? =
        context.widgetDataStore.data.first()[legacyKey(widgetId)]
            ?: WidgetOptions.read(context, widgetId)

    suspend fun setTodoId(glanceId: GlanceId, todoId: Long) {
        updateAppWidgetState(context, glanceId) { preferences ->
            preferences[selectedTodoIdKey] = todoId
        }
    }

    suspend fun removeWidget(widgetId: Int) {
        context.widgetDataStore.edit { it.remove(legacyKey(widgetId)) }
    }
}

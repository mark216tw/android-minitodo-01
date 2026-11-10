package com.vibecode.minitodo.widget

import android.appwidget.AppWidgetManager
import android.content.Context

object WidgetOptions {

    private const val KEY_SELECTED_TODO = "selected_todo_id"

    fun read(context: Context, appWidgetId: Int): Long? = runCatching {
        val options = AppWidgetManager.getInstance(context)
            .getAppWidgetOptions(appWidgetId)
        options.getLong(KEY_SELECTED_TODO, 0L).takeIf { it > 0L }
    }.getOrNull()
}

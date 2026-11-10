package com.vibecode.minitodo.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.vibecode.minitodo.MiniTodoApp
import kotlinx.coroutines.launch

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = TodoWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val app = context.applicationContext as? MiniTodoApp ?: return
        val ids = appWidgetIds.toList()
        app.appScope.launch {
            ids.forEach { app.container.widgetStore.removeWidget(it) }
        }
    }
}

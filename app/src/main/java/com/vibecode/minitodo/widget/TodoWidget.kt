package com.vibecode.minitodo.widget

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vibecode.minitodo.MiniTodoApp
import com.vibecode.minitodo.R
import com.vibecode.minitodo.data.prefs.AppSettings
import com.vibecode.minitodo.domain.ThemeMode
import com.vibecode.minitodo.domain.Todo
import com.vibecode.minitodo.domain.TodoItem
import com.vibecode.minitodo.ui.theme.colorSchemes
import com.vibecode.minitodo.ui.theme.effectivePalette
import com.vibecode.minitodo.ui.theme.todoColor
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class TodoWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val app = context.applicationContext as MiniTodoApp
        val legacyTodoId = app.container.widgetStore.getLegacyTodoId(appWidgetId)
        val repository = app.container.todoRepository
        val settingsRepository = app.container.settingsRepository

        provideContent {
            val preferences = currentState<Preferences>()
            val todoId = preferences[selectedTodoIdKey] ?: legacyTodoId
            val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
            val stateFlow = remember(todoId) {
                if (todoId == null) {
                    flowOf<WidgetTodoState>(WidgetTodoState.Unselected)
                } else {
                    repository.observeTodo(todoId)
                        .map<Todo?, WidgetTodoState> { todo ->
                            todo?.takeIf { it.archivedAt == null }
                                ?.let(WidgetTodoState::Ready)
                                ?: WidgetTodoState.Missing
                        }
                        .onStart { emit(WidgetTodoState.Loading) }
                }
            }
            val initialState = if (todoId == null) {
                WidgetTodoState.Unselected
            } else {
                WidgetTodoState.Loading
            }
            val widgetState by stateFlow.collectAsState(initial = initialState)
            val (lightScheme, darkScheme) = settings.themePalette
                .effectivePalette()
                .colorSchemes(context)
            val colors = when (settings.themeMode) {
                ThemeMode.SYSTEM -> ColorProviders(lightScheme, darkScheme)
                ThemeMode.LIGHT -> ColorProviders(lightScheme, lightScheme)
                ThemeMode.DARK -> ColorProviders(darkScheme, darkScheme)
            }

            GlanceTheme(colors = colors) {
                WidgetContent(widgetState)
            }
        }
    }
}

private sealed interface WidgetTodoState {
    data object Unselected : WidgetTodoState
    data object Loading : WidgetTodoState
    data object Missing : WidgetTodoState
    data class Ready(val todo: Todo) : WidgetTodoState
}

private val itemIdKey = ActionParameters.Key<Long>("item_id")
private val checkedKey = ActionParameters.Key<Boolean>("checked")

@Keep
class ToggleItemAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val itemId = parameters[itemIdKey] ?: return
        val checked = parameters[checkedKey] ?: return
        val app = context.applicationContext as? MiniTodoApp ?: return
        app.container.todoRepository.setItemCompleted(itemId, checked)
    }
}

@Composable
private fun WidgetContent(state: WidgetTodoState) {
    val context = LocalContext.current
    Column(
        modifier = androidx.glance.GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp),
    ) {
        when (state) {
            WidgetTodoState.Unselected -> WidgetEmptyState(
                message = context.getString(R.string.widget_no_todo),
                hint = context.getString(R.string.widget_no_todo_hint),
            )
            WidgetTodoState.Loading -> WidgetLoadingState()
            WidgetTodoState.Missing -> WidgetEmptyState(
                message = context.getString(R.string.widget_deleted_todo),
                hint = context.getString(R.string.widget_no_todo_hint),
            )
            is WidgetTodoState.Ready -> WidgetTodoContent(state.todo)
        }
    }
}

@Composable
private fun WidgetLoadingState() {
    Column(
        modifier = androidx.glance.GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = LocalContext.current.getString(R.string.widget_loading),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
private fun WidgetEmptyState(
    message: String,
    hint: String,
) {
    Column(
        modifier = androidx.glance.GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 2,
        )
        Spacer(modifier = androidx.glance.GlanceModifier.height(6.dp))
        Text(
            text = hint,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            maxLines = 3,
        )
    }
}

@Composable
private fun WidgetTodoContent(
    todo: Todo,
) {
    val context = LocalContext.current
    val openAppIntent = Intent(context, com.vibecode.minitodo.MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        action = Intent.ACTION_VIEW
        data = "minitodo://todo/${todo.id}".toUri()
    }
    Row(
        modifier = androidx.glance.GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(openAppIntent)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = androidx.glance.GlanceModifier
                .size(10.dp)
                .cornerRadius(5.dp)
                .background(ColorProvider(todoColor(todo.color))),
        ) {}
        Spacer(modifier = androidx.glance.GlanceModifier.width(8.dp))
        Text(
            text = todo.title.ifBlank { context.getString(R.string.untitled_todo) },
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }

    Spacer(modifier = androidx.glance.GlanceModifier.height(8.dp))

    if (todo.items.isEmpty()) {
        Text(
            text = context.getString(R.string.widget_no_items),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        )
        Spacer(modifier = androidx.glance.GlanceModifier.height(4.dp))
    } else {
        val orderedItems = todo.items.sortedWith(
            compareBy<TodoItem> { it.isCompleted }.thenBy { it.position },
        )
        orderedItems.take(MAX_ITEMS).forEach { item ->
            WidgetItemRow(item)
        }
        if (orderedItems.size > MAX_ITEMS) {
            Text(
                text = context.getString(R.string.widget_more_items, orderedItems.size - MAX_ITEMS),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )
        }
        Spacer(modifier = androidx.glance.GlanceModifier.height(4.dp))
    }

    if (todo.items.isNotEmpty()) {
        Text(
            text = context.getString(
                R.string.widget_completed_progress,
                todo.completedItemCount,
                todo.items.size,
            ),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
private fun WidgetItemRow(item: TodoItem) {
    CheckBox(
        checked = item.isCompleted,
        onCheckedChange = actionRunCallback<ToggleItemAction>(
            actionParametersOf(itemIdKey to item.id, checkedKey to !item.isCompleted),
        ),
        modifier = androidx.glance.GlanceModifier.fillMaxWidth(),
        text = item.title.ifBlank { LocalContext.current.getString(R.string.untitled_item) },
        style = TextStyle(
            color = if (item.isCompleted) {
                GlanceTheme.colors.onSurfaceVariant
            } else {
                GlanceTheme.colors.onSurface
            },
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
        ),
        maxLines = 1,
    )
}

private const val MAX_ITEMS = 5

object TodoWidgetUpdater {
    suspend fun updateAll(context: Context) {
        TodoWidget().updateAll(context)
    }

    suspend fun updateWidget(context: Context, appWidgetId: Int): Boolean = runCatching {
        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        TodoWidget().update(context, glanceId)
    }.isSuccess
}

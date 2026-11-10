package com.vibecode.minitodo.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.vibecode.minitodo.MiniTodoApp
import com.vibecode.minitodo.R
import com.vibecode.minitodo.data.prefs.AppSettings
import com.vibecode.minitodo.domain.Todo
import com.vibecode.minitodo.ui.theme.MiniTodoTheme
import com.vibecode.minitodo.ui.theme.applySystemBarStyle
import com.vibecode.minitodo.ui.theme.resolveDarkTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private var currentAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isSaving by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        currentAppWidgetId = appWidgetId
        setResult(RESULT_CANCELED, resultIntent(appWidgetId))

        setContent {
            val settings by (application as MiniTodoApp).container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = resolveDarkTheme(settings.themeMode, isSystemInDarkTheme())

            LaunchedEffect(darkTheme) {
                this@WidgetConfigActivity.applySystemBarStyle(darkTheme)
            }

            MiniTodoTheme(
                darkTheme = darkTheme,
                palette = settings.themePalette,
            ) {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    isSaving = isSaving,
                    onCancel = ::finishWithCancel,
                    onSaved = ::finishWithSaved,
                )
            }
        }
    }

    private fun resultIntent(appWidgetId: Int): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    private fun finishWithCancel() {
        setResult(RESULT_CANCELED, resultIntent(currentAppWidgetId))
        finish()
    }

    private fun finishWithSaved(appWidgetId: Int, todoId: Long) {
        val app = application as MiniTodoApp
        val context = applicationContext
        if (isSaving) return
        isSaving = true
        lifecycleScope.launch {
            runCatching {
                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                app.container.widgetStore.setTodoId(glanceId, todoId)
                TodoWidget().update(context, glanceId)
            }.onSuccess {
                setResult(RESULT_OK, resultIntent(appWidgetId))
                finish()
            }.onFailure {
                isSaving = false
                Toast.makeText(
                    this@WidgetConfigActivity,
                    R.string.widget_config_save_failed,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    appWidgetId: Int,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSaved: (Int, Long) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MiniTodoApp
    val repository = app.container.todoRepository
    val widgetStore = app.container.widgetStore
    val todos by repository.observeActiveTodos().collectAsStateWithLifecycle(emptyList())
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectionChanged by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(appWidgetId) {
        runCatching {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            widgetStore.getTodoId(glanceId, appWidgetId)
        }.onSuccess { storedId ->
            if (!selectionChanged) selectedId = storedId
        }.onFailure {
            Toast.makeText(
                context,
                R.string.widget_config_load_failed,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.widget_config_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_cancel),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (todos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.widget_config_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(todos, key = { it.id }) { todo ->
                        TodoChoiceRow(
                            todo = todo,
                            selected = selectedId == todo.id,
                            onSelect = {
                                selectionChanged = true
                                selectedId = todo.id
                            },
                        )
                    }
                }
            }
            Button(
                onClick = {
                    selectedId?.let { onSaved(appWidgetId, it) }
                },
                enabled = selectedId != null && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        }
    }
}

@Composable
private fun TodoChoiceRow(
    todo: Todo,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = todo.title.ifBlank { stringResource(R.string.untitled_todo) },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (todo.items.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.todo_progress,
                        todo.completedItemCount,
                        todo.items.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

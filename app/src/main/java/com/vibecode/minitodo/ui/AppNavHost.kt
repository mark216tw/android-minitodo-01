package com.vibecode.minitodo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.vibecode.minitodo.AppContainer
import com.vibecode.minitodo.ui.archive.ArchiveScreen
import com.vibecode.minitodo.ui.archive.ArchiveViewModel
import com.vibecode.minitodo.ui.edit.TodoEditScreen
import com.vibecode.minitodo.ui.edit.TodoEditViewModel
import com.vibecode.minitodo.ui.settings.SettingsScreen
import com.vibecode.minitodo.ui.settings.SettingsViewModel
import com.vibecode.minitodo.ui.todo.TodoListScreen
import com.vibecode.minitodo.ui.todo.TodoListViewModel

object Routes {
    const val LIST = "list"
    const val EDIT = "edit/{todoId}"
    const val ARCHIVE = "archive"
    const val SETTINGS = "settings"

    fun edit(todoId: Long): String = "edit/$todoId"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    container: AppContainer,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LIST,
    ) {
        composable(Routes.LIST) {
            val viewModel: TodoListViewModel = viewModel(
                factory = TodoListViewModel.factory(
                    container.todoRepository,
                    container.settingsRepository,
                ),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TodoListScreen(
                uiState = uiState,
                events = viewModel.events,
                onAdd = { navController.navigate(Routes.edit(-1L)) },
                onOpenTodo = { todoId -> navController.navigate(Routes.edit(todoId)) },
                onOpenArchive = { navController.navigate(Routes.ARCHIVE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onToggleTodo = viewModel::toggleTodoComplete,
                onToggleItem = viewModel::toggleItem,
                onArchiveTodo = viewModel::archiveTodo,
                onRestoreTodo = viewModel::restoreTodo,
                onArchiveCompleted = viewModel::archiveCompleted,
                onSetSortMode = viewModel::setSortMode,
                onReorderTodos = viewModel::reorderTodos,
                onSearchQueryChange = viewModel::setSearchQuery,
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("todoId") { type = NavType.LongType }),
            deepLinks = listOf(navDeepLink { uriPattern = "minitodo://todo/{todoId}" }),
        ) { entry ->
            val todoId = entry.arguments?.getLong("todoId") ?: -1L
            val viewModel: TodoEditViewModel = viewModel(
                key = "edit-$todoId",
                factory = TodoEditViewModel.factory(
                    container.todoRepository,
                    container.settingsRepository,
                    todoId,
                ),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TodoEditScreen(
                uiState = uiState,
                events = viewModel.events,
                onBack = { navController.popBackStack() },
                onSave = viewModel::save,
                onTitleChange = viewModel::setTitle,
                onColorChange = viewModel::setColor,
                onTodoCompletedChange = viewModel::setTodoCompleted,
                onAddItem = viewModel::addItem,
                onRemoveItem = viewModel::removeItem,
                onRestoreItem = viewModel::restoreItem,
                onMoveItem = viewModel::moveItem,
                onItemTitleChange = viewModel::setItemTitle,
                onItemCompletedChange = viewModel::setItemCompleted,
                onStartDateChange = viewModel::setItemStartDate,
                onStartTimeChange = viewModel::setItemStartTime,
                onStartAllDayChange = viewModel::setItemStartAllDay,
                onClearStart = viewModel::clearItemStart,
                onEndDateChange = viewModel::setItemEndDate,
                onEndTimeChange = viewModel::setItemEndTime,
                onEndAllDayChange = viewModel::setItemEndAllDay,
                onClearEnd = viewModel::clearItemEnd,
                onNotificationPermissionAsked = viewModel::markNotificationPermissionAsked,
            )
        }

        composable(Routes.ARCHIVE) {
            val viewModel: ArchiveViewModel = viewModel(
                factory = ArchiveViewModel.factory(container.todoRepository),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ArchiveScreen(
                uiState = uiState,
                events = viewModel.events,
                onBack = { navController.popBackStack() },
                onRestore = viewModel::restore,
                onDelete = viewModel::delete,
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(
                    container.settingsRepository,
                    container.backupManager,
                ),
            )
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            SettingsScreen(
                settings = settings,
                events = viewModel.events,
                onBack = { navController.popBackStack() },
                onSetSortMode = viewModel::setSortMode,
                onSetThemeMode = viewModel::setThemeMode,
                onSetThemePalette = viewModel::setThemePalette,
                onExport = viewModel::export,
                onImport = viewModel::import,
            )
        }
    }
}

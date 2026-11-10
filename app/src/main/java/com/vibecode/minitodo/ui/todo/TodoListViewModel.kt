package com.vibecode.minitodo.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibecode.minitodo.data.prefs.SettingsRepository
import com.vibecode.minitodo.data.repository.TodoRepository
import com.vibecode.minitodo.domain.SortMode
import com.vibecode.minitodo.domain.Todo
import com.vibecode.minitodo.domain.TodoItem
import com.vibecode.minitodo.domain.TodoOrganizer
import com.vibecode.minitodo.domain.TodoSection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodoListUiState(
    val isLoading: Boolean = true,
    val sections: List<TodoSection> = emptyList(),
    val sortMode: SortMode = SortMode.EARLIEST_START,
    val completedCount: Int = 0,
    val searchQuery: String = "",
    val hasError: Boolean = false,
)

sealed interface TodoListEvent {
    data class Archived(val todoId: Long, val title: String) : TodoListEvent
    data class ArchivedCompleted(val count: Int) : TodoListEvent
}

class TodoListViewModel(
    private val repository: TodoRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _events = Channel<TodoListEvent>(Channel.BUFFERED)
    val events: Flow<TodoListEvent> = _events.receiveAsFlow()

    private val searchQuery = MutableStateFlow("")

    private val ticker = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(now)
            delay(60_000L - now % 60_000L)
        }
    }.conflate()

    val uiState = combine(
        repository.observeActiveTodos(),
        settingsRepository.settings,
        searchQuery,
        ticker,
    ) { todos, settings, query, now ->
        val normalizedQuery = query.trim()
        val filtered = if (normalizedQuery.isEmpty()) {
            todos
        } else {
            todos.filter { todo ->
                todo.title.contains(normalizedQuery, ignoreCase = true) ||
                    todo.items.any { it.title.contains(normalizedQuery, ignoreCase = true) }
            }
        }
        TodoListUiState(
            isLoading = false,
            sections = TodoOrganizer.organize(filtered, settings.sortMode, now),
            sortMode = settings.sortMode,
            completedCount = todos.count { it.isCompleted },
            searchQuery = normalizedQuery,
        )
    }
        .catch { emit(TodoListUiState(isLoading = false, hasError = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TodoListUiState(),
        )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortMode(mode: SortMode) {
        viewModelScope.launch { settingsRepository.setSortMode(mode) }
    }

    fun toggleTodoComplete(todo: Todo) {
        viewModelScope.launch { repository.setTodoCompleted(todo.id, !todo.isCompleted) }
    }

    fun toggleItem(item: TodoItem) {
        viewModelScope.launch { repository.setItemCompleted(item.id, !item.isCompleted) }
    }

    fun archiveTodo(todo: Todo) {
        viewModelScope.launch {
            repository.archiveTodo(todo.id)
            _events.send(TodoListEvent.Archived(todo.id, todo.title))
        }
    }

    fun restoreTodo(todoId: Long) {
        viewModelScope.launch { repository.restoreTodo(todoId) }
    }

    fun archiveCompleted() {
        viewModelScope.launch {
            val count = repository.archiveCompleted()
            if (count > 0) _events.send(TodoListEvent.ArchivedCompleted(count))
        }
    }

    fun reorderTodos(orderedIds: List<Long>) {
        viewModelScope.launch { repository.reorderTodos(orderedIds) }
    }

    companion object {
        fun factory(
            repository: TodoRepository,
            settingsRepository: SettingsRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TodoListViewModel(repository, settingsRepository) as T
        }
    }
}

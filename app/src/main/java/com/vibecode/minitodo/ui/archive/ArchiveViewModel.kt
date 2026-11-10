package com.vibecode.minitodo.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibecode.minitodo.data.repository.TodoRepository
import com.vibecode.minitodo.domain.Todo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArchiveUiState(
    val isLoading: Boolean = true,
    val todos: List<Todo> = emptyList(),
)

sealed interface ArchiveEvent {
    data class Deleted(val title: String) : ArchiveEvent
}

class ArchiveViewModel(
    private val repository: TodoRepository,
) : ViewModel() {

    private val _events = Channel<ArchiveEvent>(Channel.BUFFERED)
    val events: Flow<ArchiveEvent> = _events.receiveAsFlow()

    val uiState = repository.observeArchivedTodos()
        .map { ArchiveUiState(isLoading = false, todos = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ArchiveUiState(),
        )

    fun restore(todoId: Long) {
        viewModelScope.launch { repository.restoreTodo(todoId) }
    }

    fun delete(todo: Todo) {
        viewModelScope.launch {
            repository.deleteTodo(todo.id)
            _events.send(ArchiveEvent.Deleted(todo.title))
        }
    }

    companion object {
        fun factory(repository: TodoRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ArchiveViewModel(repository) as T
            }
    }
}

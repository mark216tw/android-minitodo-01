package com.vibecode.minitodo.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibecode.minitodo.data.prefs.SettingsRepository
import com.vibecode.minitodo.data.repository.TodoRepository
import com.vibecode.minitodo.domain.Todo
import com.vibecode.minitodo.domain.TodoColor
import com.vibecode.minitodo.domain.TodoItem
import com.vibecode.minitodo.util.DateTimeUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.UUID

data class ItemDraft(
    val key: String = UUID.randomUUID().toString(),
    val id: Long = 0L,
    val title: String = "",
    val startAt: Long? = null,
    val startAllDay: Boolean = false,
    val endAt: Long? = null,
    val endAllDay: Boolean = false,
    val isCompleted: Boolean = false,
)

data class TodoEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val title: String = "",
    val color: TodoColor = TodoColor.NONE,
    val todoCompleted: Boolean = false,
    val items: List<ItemDraft> = emptyList(),
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val notificationPermissionAsked: Boolean = true,
)

enum class ValidationError {
    EMPTY_TITLE,
    END_BEFORE_START,
}

sealed interface TodoEditEvent {
    data object Saved : TodoEditEvent
    data object NotFound : TodoEditEvent
    data object SaveFailed : TodoEditEvent
    data class Validation(val error: ValidationError) : TodoEditEvent
    data class ItemRemoved(val item: ItemDraft, val index: Int) : TodoEditEvent
}

class TodoEditViewModel(
    private val repository: TodoRepository,
    private val settingsRepository: SettingsRepository,
    private val todoId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoEditUiState())
    val uiState: StateFlow<TodoEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<TodoEditEvent>(Channel.BUFFERED)
    val events: Flow<TodoEditEvent> = _events.receiveAsFlow()

    private var baseline: EditSnapshot = EditSnapshot.empty()

    init {
        viewModelScope.launch {
            settingsRepository.notificationPermissionAsked.collect { asked ->
                _uiState.update { it.copy(notificationPermissionAsked = asked) }
            }
        }
        if (todoId <= 0L) {
            val state = TodoEditUiState(isLoading = false, isNew = true, isDirty = false)
            baseline = snapshotOf(state)
            _uiState.value = state
        } else {
            viewModelScope.launch {
                val todo = repository.getTodo(todoId)
                if (todo == null) {
                    _events.send(TodoEditEvent.NotFound)
                    return@launch
                }
                val state = TodoEditUiState(
                    isLoading = false,
                    isNew = false,
                    title = todo.title,
                    color = todo.color,
                    todoCompleted = todo.isCompleted,
                    items = todo.items.map { item ->
                        ItemDraft(
                            key = "item-${item.id}",
                            id = item.id,
                            title = item.title,
                            startAt = item.startAt,
                            startAllDay = item.startAllDay,
                            endAt = item.endAt,
                            endAllDay = item.endAllDay,
                            isCompleted = item.isCompleted,
                        )
                    },
                    isDirty = false,
                )
                baseline = snapshotOf(state)
                _uiState.value = state
            }
        }
    }

    fun setTitle(value: String) = update { it.copy(title = value) }

    fun setColor(color: TodoColor) = update { it.copy(color = color) }

    fun setTodoCompleted(completed: Boolean) = update { state ->
        state.copy(
            todoCompleted = completed,
            items = if (state.items.isEmpty()) {
                state.items
            } else {
                state.items.map { it.copy(isCompleted = completed) }
            },
        )
    }

    fun addItem() = update { it.copy(items = it.items + ItemDraft()) }

    fun removeItem(key: String) {
        val state = _uiState.value
        val index = state.items.indexOfFirst { it.key == key }
        if (index < 0) return
        val removed = state.items[index]
        update { current ->
            current.copy(items = current.items.filterNot { it.key == key })
        }
        viewModelScope.launch { _events.send(TodoEditEvent.ItemRemoved(removed, index)) }
    }

    fun restoreItem(item: ItemDraft, index: Int) = update { state ->
        val items = state.items.toMutableList()
        items.add(index.coerceIn(0, items.size), item)
        state.copy(items = items)
    }

    fun markNotificationPermissionAsked() {
        viewModelScope.launch { settingsRepository.markNotificationPermissionAsked() }
    }

    fun moveItem(from: Int, to: Int) = update { state ->
        if (from !in state.items.indices || to !in state.items.indices || from == to) {
            state
        } else {
            val items = state.items.toMutableList()
            val item = items.removeAt(from)
            items.add(to, item)
            state.copy(items = items)
        }
    }

    fun setItemTitle(key: String, value: String) = updateItem(key) { it.copy(title = value) }

    fun setItemCompleted(key: String, completed: Boolean) = update { state ->
        val items = state.items.map {
            if (it.key == key) it.copy(isCompleted = completed) else it
        }
        val todoCompleted = if (items.isEmpty()) {
            state.todoCompleted
        } else {
            items.all { it.isCompleted }
        }
        state.copy(items = items, todoCompleted = todoCompleted)
    }

    fun setItemStartDate(key: String, datePickerMillis: Long) = updateItem(key) { item ->
        val date = DateTimeUtils.dateFromPickerMillis(datePickerMillis)
        val time = if (item.startAllDay) {
            LocalTime.MIDNIGHT
        } else {
            item.startAt?.let { DateTimeUtils.localDateTime(it).toLocalTime() }
                ?: LocalTime.of(DateTimeUtils.ALL_DAY_REMINDER_HOUR, 0)
        }
        item.copy(startAt = DateTimeUtils.toMillis(date, time))
    }

    fun setItemStartTime(key: String, hour: Int, minute: Int) = updateItem(key) { item ->
        val date = item.startAt?.let { DateTimeUtils.localDate(it) }
            ?: DateTimeUtils.localDate(System.currentTimeMillis())
        item.copy(
            startAt = DateTimeUtils.toMillis(date, LocalTime.of(hour, minute)),
            startAllDay = false,
        )
    }

    fun setItemStartAllDay(key: String, allDay: Boolean) = updateItem(key) { item ->
        val timestamp = item.startAt ?: return@updateItem item
        val date = DateTimeUtils.localDate(timestamp)
        val newTimestamp = when {
            allDay -> DateTimeUtils.startOfDay(date)
            DateTimeUtils.localDateTime(timestamp).toLocalTime() == LocalTime.MIDNIGHT ->
                DateTimeUtils.toMillis(
                    date,
                    LocalTime.of(DateTimeUtils.ALL_DAY_REMINDER_HOUR, 0),
                )

            else -> timestamp
        }
        item.copy(startAt = newTimestamp, startAllDay = allDay)
    }

    fun clearItemStart(key: String) = updateItem(key) {
        it.copy(startAt = null, startAllDay = false)
    }

    fun setItemEndDate(key: String, datePickerMillis: Long) = updateItem(key) { item ->
        val date = DateTimeUtils.dateFromPickerMillis(datePickerMillis)
        val time = when {
            item.endAt != null && item.endAllDay -> LocalTime.MIDNIGHT
            item.endAt != null -> DateTimeUtils.localDateTime(item.endAt).toLocalTime()
            item.startAt != null && !item.startAllDay ->
                DateTimeUtils.localDateTime(item.startAt).toLocalTime().plusHours(1)

            else -> LocalTime.of(DateTimeUtils.ALL_DAY_REMINDER_HOUR, 0)
        }
        item.copy(endAt = DateTimeUtils.toMillis(date, time))
    }

    fun setItemEndTime(key: String, hour: Int, minute: Int) = updateItem(key) { item ->
        val date = item.endAt?.let { DateTimeUtils.localDate(it) }
            ?: item.startAt?.let { DateTimeUtils.localDate(it) }
            ?: DateTimeUtils.localDate(System.currentTimeMillis())
        item.copy(
            endAt = DateTimeUtils.toMillis(date, LocalTime.of(hour, minute)),
            endAllDay = false,
        )
    }

    fun setItemEndAllDay(key: String, allDay: Boolean) = updateItem(key) { item ->
        val timestamp = item.endAt ?: return@updateItem item
        val date = DateTimeUtils.localDate(timestamp)
        val newTimestamp = when {
            allDay -> DateTimeUtils.startOfDay(date)
            DateTimeUtils.localDateTime(timestamp).toLocalTime() == LocalTime.MIDNIGHT ->
                DateTimeUtils.toMillis(
                    date,
                    LocalTime.of(DateTimeUtils.ALL_DAY_REMINDER_HOUR, 0),
                )

            else -> timestamp
        }
        item.copy(endAt = newTimestamp, endAllDay = allDay)
    }

    fun clearItemEnd(key: String) = updateItem(key) {
        it.copy(endAt = null, endAllDay = false)
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.title.isBlank()) {
            viewModelScope.launch { _events.send(TodoEditEvent.Validation(ValidationError.EMPTY_TITLE)) }
            return
        }

        val items = state.items.filter { it.title.isNotBlank() }
        val hasInvalidRange = items.any { item ->
            val start = item.startAt
            val end = item.endAt
            start != null && end != null &&
                DateTimeUtils.effectiveEnd(end, item.endAllDay) <
                DateTimeUtils.effectiveStart(start, item.startAllDay)
        }
        if (hasInvalidRange) {
            viewModelScope.launch {
                _events.send(TodoEditEvent.Validation(ValidationError.END_BEFORE_START))
            }
            return
        }

        val completed = if (items.isEmpty()) {
            state.todoCompleted
        } else {
            items.all { it.isCompleted }
        }
        val todoItems = items.mapIndexed { index, item ->
            TodoItem(
                id = item.id,
                title = item.title.trim(),
                startAt = item.startAt,
                startAllDay = item.startAllDay,
                endAt = item.endAt,
                endAllDay = item.endAllDay,
                isCompleted = item.isCompleted,
                position = index,
            )
        }
        val todo = Todo(
            id = if (state.isNew) 0L else todoId,
            title = state.title.trim(),
            color = state.color,
            isCompleted = completed,
            items = todoItems,
        )

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                if (state.isNew) {
                    repository.createTodo(todo, todoItems)
                } else {
                    repository.updateTodo(todo, todoItems)
                }
            }.onSuccess {
                _events.send(TodoEditEvent.Saved)
            }.onFailure {
                _uiState.update { current -> current.copy(isSaving = false) }
                _events.send(TodoEditEvent.SaveFailed)
            }
        }
    }

    private fun updateItem(key: String, transform: (ItemDraft) -> ItemDraft) = update { state ->
        state.copy(items = state.items.map { if (it.key == key) transform(it) else it })
    }

    private fun update(transform: (TodoEditUiState) -> TodoEditUiState) {
        val next = transform(_uiState.value)
        _uiState.value = next.copy(isDirty = snapshotOf(next) != baseline)
    }

    private data class EditSnapshot(
        val title: String,
        val color: TodoColor,
        val todoCompleted: Boolean,
        val items: List<ItemDraft>,
    ) {
        companion object {
            fun empty() = EditSnapshot("", TodoColor.NONE, false, emptyList())
        }
    }

    private fun snapshotOf(state: TodoEditUiState): EditSnapshot = EditSnapshot(
        title = state.title,
        color = state.color,
        todoCompleted = state.todoCompleted,
        items = state.items,
    )

    companion object {
        fun factory(
            repository: TodoRepository,
            settingsRepository: SettingsRepository,
            todoId: Long,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TodoEditViewModel(repository, settingsRepository, todoId) as T
        }
    }
}

package com.vibecode.minitodo.data.repository

import androidx.room.withTransaction
import com.vibecode.minitodo.data.local.AppDatabase
import com.vibecode.minitodo.data.local.ReminderEntryRow
import com.vibecode.minitodo.data.local.TodoEntity
import com.vibecode.minitodo.data.local.TodoItemEntity
import com.vibecode.minitodo.data.local.TodoWithItems
import com.vibecode.minitodo.domain.ReminderEntry
import com.vibecode.minitodo.domain.Todo
import com.vibecode.minitodo.domain.TodoColor
import com.vibecode.minitodo.domain.TodoItem
import com.vibecode.minitodo.domain.diffItems
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicReference

data class DataChange(
    val reminders: Boolean,
    val widget: Boolean,
) {
    val isEmpty: Boolean
        get() = !reminders && !widget

    fun merge(other: DataChange): DataChange = DataChange(
        reminders = reminders || other.reminders,
        widget = widget || other.widget,
    )

    companion object {
        val NONE = DataChange(reminders = false, widget = false)
    }
}

class TodoRepository(private val database: AppDatabase) {

    private val dao = database.todoDao()

    private val pendingChanges = AtomicReference(DataChange.NONE)
    private val changeSignals = Channel<Unit>(Channel.CONFLATED)
    val changes: Flow<Unit> = changeSignals.receiveAsFlow()

    fun requestFullRefresh() {
        markChanged(DataChange(reminders = true, widget = true))
    }

    fun takeChanges(): DataChange = pendingChanges.getAndSet(DataChange.NONE)

    fun observeActiveTodos(): Flow<List<Todo>> =
        dao.observeActiveTodos().map { list -> list.map { it.toDomain() } }

    fun observeArchivedTodos(): Flow<List<Todo>> =
        dao.observeArchivedTodos().map { list -> list.map { it.toDomain() } }

    fun observeTodo(todoId: Long): Flow<Todo?> =
        dao.observeTodo(todoId).map { it?.toDomain() }

    suspend fun getTodo(todoId: Long): Todo? = dao.getTodo(todoId)?.toDomain()

    suspend fun getAllTodosForBackup(): List<Todo> =
        dao.getAllTodosForBackup().map { it.toDomain() }

    suspend fun replaceAllTodos(todos: List<Todo>) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            dao.deleteAllTodos()
            todos.forEach { todo ->
                val newId = dao.insertTodo(
                    TodoEntity(
                        title = todo.title.trim(),
                        colorKey = todo.color.name,
                        isCompleted = todo.isCompleted,
                        createdAt = todo.createdAt.takeIf { it > 0L } ?: now,
                        completedAt = todo.completedAt,
                        archivedAt = todo.archivedAt,
                        position = todo.position,
                    ),
                )
                if (todo.items.isNotEmpty()) {
                    dao.insertItems(
                        todo.items.mapIndexed { index, item ->
                            TodoItemEntity(
                                todoId = newId,
                                title = item.title.trim(),
                                startAt = item.startAt,
                                startAllDay = item.startAllDay,
                                endAt = item.endAt,
                                endAllDay = item.endAllDay,
                                isCompleted = item.isCompleted,
                                completedAt = item.completedAt,
                                position = index,
                            )
                        },
                    )
                }
            }
        }
        notifyChanged()
    }

    suspend fun getActiveReminderEntries(): List<ReminderEntry> =
        dao.getActiveReminderEntries().map { it.toDomain() }

    suspend fun getScheduledItemIds(): List<Long> = dao.getScheduledItemIds()

    suspend fun getReminderEntry(itemId: Long): ReminderEntry? =
        dao.getReminderEntry(itemId)?.toDomain()

    suspend fun createTodo(todo: Todo, items: List<TodoItem>): Long {
        val now = System.currentTimeMillis()
        val id = database.withTransaction {
            val newId = dao.insertTodo(
                TodoEntity(
                    title = todo.title.trim(),
                    colorKey = todo.color.name,
                    isCompleted = todo.isCompleted,
                    createdAt = now,
                    completedAt = if (todo.isCompleted) now else null,
                    archivedAt = null,
                    position = dao.maxPosition() + 1,
                ),
            )
            dao.insertItems(items.toEntities(newId, now))
            newId
        }
        notifyChanged()
        return id
    }

    suspend fun updateTodo(todo: Todo, items: List<TodoItem>) {
        database.withTransaction {
            val existing = dao.getTodo(todo.id) ?: return@withTransaction
            val positioned = items.mapIndexed { index, item -> item.copy(position = index) }
            val allCompleted = positioned.isNotEmpty() && positioned.all { it.isCompleted }
            val completed = if (positioned.isEmpty()) todo.isCompleted else allCompleted
            val completionTimestamp = when {
                !completed -> null
                existing.todo.isCompleted -> existing.todo.completedAt ?: System.currentTimeMillis()
                else -> System.currentTimeMillis()
            }
            dao.updateTodo(
                existing.todo.copy(
                    title = todo.title.trim(),
                    colorKey = todo.color.name,
                    isCompleted = completed,
                    completedAt = completionTimestamp,
                ),
            )

            val now = System.currentTimeMillis()
            val existingItems = existing.items.associateBy { it.id }
            val diff = diffItems(existingItems.keys, positioned) { it.id }
            val toInsert = diff.toInsert.map {
                it.toEntity(todoId = todo.id, id = 0L, now = now, existingItems = existingItems)
            }
            val toUpdate = diff.toUpdate.map {
                it.toEntity(todoId = todo.id, id = it.id, now = now, existingItems = existingItems)
            }
            if (toInsert.isNotEmpty()) dao.insertItems(toInsert)
            if (toUpdate.isNotEmpty()) dao.updateItems(toUpdate)
            if (diff.toDeleteIds.isNotEmpty()) dao.deleteItemsByIds(diff.toDeleteIds)
        }
        notifyChanged()
    }

    suspend fun setTodoCompleted(todoId: Long, completed: Boolean) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            dao.updateTodoCompletion(todoId, completed, if (completed) now else null)
            dao.updateAllItemsCompletion(todoId, completed, if (completed) now else null)
        }
        notifyChanged()
    }

    suspend fun setItemCompleted(itemId: Long, completed: Boolean) {
        database.withTransaction {
            val item = dao.getItem(itemId) ?: return@withTransaction
            val now = System.currentTimeMillis()
            dao.updateItemCompletion(itemId, completed, if (completed) now else null)
            val items = dao.getItemsForTodo(item.todoId)
            if (items.isNotEmpty()) {
                val allCompleted = items.all { it.isCompleted }
                val todo = dao.getTodo(item.todoId)?.todo
                val completedAt = if (allCompleted) {
                    if (todo?.isCompleted == true) todo.completedAt ?: now else now
                } else {
                    null
                }
                dao.updateTodoCompletion(item.todoId, allCompleted, completedAt)
            }
        }
        notifyChanged()
    }

    suspend fun archiveTodo(todoId: Long) {
        dao.setArchived(todoId, System.currentTimeMillis())
        notifyChanged()
    }

    suspend fun restoreTodo(todoId: Long) {
        dao.setArchived(todoId, null)
        notifyChanged()
    }

    suspend fun archiveCompleted(): Int {
        val count = dao.countCompletedActive()
        if (count > 0) {
            dao.archiveCompleted(System.currentTimeMillis())
            notifyChanged()
        }
        return count
    }

    suspend fun reorderTodos(orderedIds: List<Long>) {
        database.withTransaction {
            orderedIds.forEachIndexed { index, todoId ->
                dao.updatePosition(todoId, index)
            }
        }
    }

    suspend fun deleteTodo(todoId: Long) {
        dao.deleteTodo(todoId)
        notifyChanged()
    }

    private fun notifyChanged(
        reminders: Boolean = true,
        widget: Boolean = true,
    ) {
        markChanged(DataChange(reminders, widget))
    }

    private fun markChanged(change: DataChange) {
        pendingChanges.updateAndGet { it.merge(change) }
        changeSignals.trySend(Unit)
    }

    private fun List<TodoItem>.toEntities(todoId: Long, now: Long): List<TodoItemEntity> =
        mapIndexed { index, item ->
            TodoItemEntity(
                todoId = todoId,
                title = item.title.trim(),
                startAt = item.startAt,
                startAllDay = item.startAllDay,
                endAt = item.endAt,
                endAllDay = item.endAllDay,
                isCompleted = item.isCompleted,
                completedAt = if (item.isCompleted) item.completedAt ?: now else null,
                position = index,
            )
        }

    private fun TodoItem.toEntity(
        todoId: Long,
        id: Long,
        now: Long,
        existingItems: Map<Long, TodoItemEntity>,
    ): TodoItemEntity {
        val previous = existingItems[id]
        val completedAt = when {
            !isCompleted -> null
            previous?.isCompleted == true -> previous.completedAt ?: now
            else -> now
        }
        return TodoItemEntity(
            id = id,
            todoId = todoId,
            title = title.trim(),
            startAt = startAt,
            startAllDay = startAllDay,
            endAt = endAt,
            endAllDay = endAllDay,
            isCompleted = isCompleted,
            completedAt = completedAt,
            position = position,
        )
    }

    private fun TodoWithItems.toDomain(): Todo = Todo(
        id = todo.id,
        title = todo.title,
        color = TodoColor.entries.firstOrNull { it.name == todo.colorKey } ?: TodoColor.NONE,
        isCompleted = todo.isCompleted,
        createdAt = todo.createdAt,
        completedAt = todo.completedAt,
        archivedAt = todo.archivedAt,
        position = todo.position,
        items = items.sortedBy { it.position }.map { it.toDomain() },
    )

    private fun TodoItemEntity.toDomain(): TodoItem = TodoItem(
        id = id,
        title = title,
        startAt = startAt,
        startAllDay = startAllDay,
        endAt = endAt,
        endAllDay = endAllDay,
        isCompleted = isCompleted,
        completedAt = completedAt,
        position = position,
    )

    private fun ReminderEntryRow.toDomain(): ReminderEntry = ReminderEntry(
        itemId = itemId,
        todoId = todoId,
        todoTitle = todoTitle,
        itemTitle = itemTitle,
        startAt = startAt,
        startAllDay = startAllDay,
        endAt = endAt,
        endAllDay = endAllDay,
        isCompleted = isCompleted,
        archived = archivedAt != null,
    )
}

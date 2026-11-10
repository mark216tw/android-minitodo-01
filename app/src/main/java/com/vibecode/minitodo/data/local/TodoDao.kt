package com.vibecode.minitodo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Transaction
    @Query("SELECT * FROM todos WHERE archivedAt IS NULL")
    fun observeActiveTodos(): Flow<List<TodoWithItems>>

    @Transaction
    @Query("SELECT * FROM todos WHERE archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun observeArchivedTodos(): Flow<List<TodoWithItems>>

    @Transaction
    @Query("SELECT * FROM todos WHERE id = :todoId LIMIT 1")
    fun observeTodo(todoId: Long): Flow<TodoWithItems?>

    @Transaction
    @Query("SELECT * FROM todos WHERE id = :todoId LIMIT 1")
    suspend fun getTodo(todoId: Long): TodoWithItems?

    @Transaction
    @Query("SELECT * FROM todos ORDER BY position ASC, id ASC")
    suspend fun getAllTodosForBackup(): List<TodoWithItems>

    @Query("DELETE FROM todos")
    suspend fun deleteAllTodos()

    @Query(
        """
        SELECT ti.id AS itemId, t.id AS todoId, t.title AS todoTitle,
               ti.title AS itemTitle, ti.startAt AS startAt, ti.startAllDay AS startAllDay,
               ti.endAt AS endAt, ti.endAllDay AS endAllDay, ti.isCompleted AS isCompleted,
               t.archivedAt AS archivedAt
        FROM todo_items ti
        INNER JOIN todos t ON t.id = ti.todoId
        WHERE ti.id = :itemId
        LIMIT 1
        """,
    )
    suspend fun getReminderEntry(itemId: Long): ReminderEntryRow?

    @Query(
        """
        SELECT ti.id AS itemId, t.id AS todoId, t.title AS todoTitle,
               ti.title AS itemTitle, ti.startAt AS startAt, ti.startAllDay AS startAllDay,
               ti.endAt AS endAt, ti.endAllDay AS endAllDay, ti.isCompleted AS isCompleted,
               t.archivedAt AS archivedAt
        FROM todo_items ti
        INNER JOIN todos t ON t.id = ti.todoId
        WHERE t.archivedAt IS NULL
          AND t.isCompleted = 0
          AND ti.isCompleted = 0
          AND (ti.startAt IS NOT NULL OR ti.endAt IS NOT NULL)
        """,
    )
    suspend fun getActiveReminderEntries(): List<ReminderEntryRow>

    @Query("SELECT id FROM todo_items WHERE startAt IS NOT NULL OR endAt IS NOT NULL")
    suspend fun getScheduledItemIds(): List<Long>

    @Query("SELECT COUNT(*) FROM todos WHERE archivedAt IS NULL AND isCompleted = 1")
    suspend fun countCompletedActive(): Int

    @Query("SELECT * FROM todo_items WHERE todoId = :todoId ORDER BY position ASC, id ASC")
    suspend fun getItemsForTodo(todoId: Long): List<TodoItemEntity>

    @Query("SELECT * FROM todo_items WHERE id = :itemId LIMIT 1")
    suspend fun getItem(itemId: Long): TodoItemEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM todos")
    suspend fun maxPosition(): Int

    @Query("UPDATE todos SET position = :position WHERE id = :todoId")
    suspend fun updatePosition(todoId: Long, position: Int)

    @Insert
    suspend fun insertTodo(todo: TodoEntity): Long

    @Insert
    suspend fun insertItems(items: List<TodoItemEntity>)

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Update
    suspend fun updateItems(items: List<TodoItemEntity>)

    @Query("DELETE FROM todo_items WHERE id IN (:ids)")
    suspend fun deleteItemsByIds(ids: List<Long>)

    @Query("UPDATE todos SET isCompleted = :completed, completedAt = :completedAt WHERE id = :todoId")
    suspend fun updateTodoCompletion(todoId: Long, completed: Boolean, completedAt: Long?)

    @Query("UPDATE todo_items SET isCompleted = :completed, completedAt = :completedAt WHERE todoId = :todoId")
    suspend fun updateAllItemsCompletion(todoId: Long, completed: Boolean, completedAt: Long?)

    @Query("UPDATE todo_items SET isCompleted = :completed, completedAt = :completedAt WHERE id = :itemId")
    suspend fun updateItemCompletion(itemId: Long, completed: Boolean, completedAt: Long?)

    @Query("UPDATE todos SET archivedAt = :archivedAt WHERE id = :todoId")
    suspend fun setArchived(todoId: Long, archivedAt: Long?)

    @Query("UPDATE todos SET archivedAt = :archivedAt WHERE archivedAt IS NULL AND isCompleted = 1")
    suspend fun archiveCompleted(archivedAt: Long)

    @Query("DELETE FROM todos WHERE id = :todoId")
    suspend fun deleteTodo(todoId: Long)
}

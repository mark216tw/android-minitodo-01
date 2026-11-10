package com.vibecode.minitodo.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "todos",
    indices = [Index("archivedAt")],
)
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val colorKey: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val completedAt: Long?,
    val archivedAt: Long?,
    val position: Int,
)

@Entity(
    tableName = "todo_items",
    foreignKeys = [
        ForeignKey(
            entity = TodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["todoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("todoId")],
)
data class TodoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val todoId: Long,
    val title: String,
    val startAt: Long?,
    val startAllDay: Boolean,
    val endAt: Long?,
    val endAllDay: Boolean,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val position: Int,
)

data class TodoWithItems(
    @Embedded val todo: TodoEntity,
    @Relation(parentColumn = "id", entityColumn = "todoId")
    val items: List<TodoItemEntity>,
)

data class ReminderEntryRow(
    val itemId: Long,
    val todoId: Long,
    val todoTitle: String,
    val itemTitle: String,
    val startAt: Long?,
    val startAllDay: Boolean,
    val endAt: Long?,
    val endAllDay: Boolean,
    val isCompleted: Boolean,
    val archivedAt: Long?,
)

package com.vibecode.minitodo.domain

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class TodoColor {
    NONE,
    RED,
    ORANGE,
    YELLOW,
    LIME,
    GREEN,
    TEAL,
    CYAN,
    BLUE,
    INDIGO,
    PURPLE,
    DEEP_PURPLE,
    PINK,
    BROWN,
    BLUE_GREY,
    DEEP_ORANGE,
}

enum class SortMode {
    EARLIEST_START,
    CREATED,
    MANUAL,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class SectionType {
    OVERDUE,
    TODAY,
    UPCOMING,
    NO_DATE,
    ALL,
    COMPLETED,
}

@Immutable
data class TodoSection(
    val type: SectionType,
    val todos: List<Todo>,
)

@Immutable
data class TodoItem(
    val id: Long = 0L,
    val title: String = "",
    val startAt: Long? = null,
    val startAllDay: Boolean = false,
    val endAt: Long? = null,
    val endAllDay: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val position: Int = 0,
)

@Immutable
data class Todo(
    val id: Long = 0L,
    val title: String = "",
    val color: TodoColor = TodoColor.NONE,
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val completedAt: Long? = null,
    val archivedAt: Long? = null,
    val position: Int = 0,
    val items: List<TodoItem> = emptyList(),
) {
    val completedItemCount: Int
        get() = items.count { it.isCompleted }

    val hasIncompleteItems: Boolean
        get() = items.any { !it.isCompleted }
}

data class ReminderEntry(
    val itemId: Long,
    val todoId: Long,
    val todoTitle: String,
    val itemTitle: String,
    val startAt: Long?,
    val startAllDay: Boolean,
    val endAt: Long?,
    val endAllDay: Boolean,
    val isCompleted: Boolean,
    val archived: Boolean,
)

object TodoOrganizer {

    fun organize(
        todos: List<Todo>,
        sortMode: SortMode,
        now: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<TodoSection> {
        val active = todos.filter { it.archivedAt == null }
        val completed = active.filter { it.isCompleted }
            .sortedWith(
                compareByDescending<Todo> { it.completedAt ?: Long.MIN_VALUE }
                    .thenByDescending { it.createdAt },
            )
        val incomplete = active.filterNot { it.isCompleted }

        if (sortMode == SortMode.MANUAL) {
            val manualSections = listOf(
                TodoSection(
                    SectionType.ALL,
                    incomplete.sortedWith(comparatorFor(SortMode.MANUAL)),
                ),
                TodoSection(SectionType.COMPLETED, completed),
            )
            return manualSections.filter { it.todos.isNotEmpty() }
        }

        val overdue = mutableListOf<Todo>()
        val today = mutableListOf<Todo>()
        val upcoming = mutableListOf<Todo>()
        val noDate = mutableListOf<Todo>()

        val bounds = dayBounds(now, zoneId)
        incomplete.forEach { todo ->
            when (classify(todo, now, zoneId, bounds)) {
                SectionType.OVERDUE -> overdue += todo
                SectionType.TODAY -> today += todo
                SectionType.UPCOMING -> upcoming += todo
                SectionType.NO_DATE -> noDate += todo
                SectionType.ALL, SectionType.COMPLETED -> Unit
            }
        }

        val comparator = comparatorFor(sortMode)
        val sections = listOf(
            TodoSection(SectionType.OVERDUE, overdue.sortedWith(comparator)),
            TodoSection(SectionType.TODAY, today.sortedWith(comparator)),
            TodoSection(SectionType.UPCOMING, upcoming.sortedWith(comparator)),
            TodoSection(SectionType.NO_DATE, noDate.sortedWith(comparator)),
            TodoSection(SectionType.COMPLETED, completed),
        )
        return sections.filter { it.todos.isNotEmpty() }
    }

    fun classify(
        todo: Todo,
        now: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): SectionType = classify(todo, now, zoneId, dayBounds(now, zoneId))

    private fun classify(
        todo: Todo,
        now: Long,
        zone: ZoneId,
        bounds: DayBounds,
    ): SectionType {
        if (todo.isCompleted) return SectionType.COMPLETED

        val incompleteItems = todo.items.filterNot { it.isCompleted }
        val overdue = incompleteItems.any { item ->
            when {
                item.endAt != null -> {
                    val end = if (item.endAllDay) {
                        Instant.ofEpochMilli(item.endAt).atZone(zone).toLocalDate()
                            .plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    } else {
                        item.endAt
                    }
                    end < now
                }
                item.startAt != null -> item.startAt < bounds.todayStart
                else -> false
            }
        }
        if (overdue) return SectionType.OVERDUE

        val earliestStart = earliestStart(todo) ?: return SectionType.NO_DATE
        return when {
            earliestStart < bounds.tomorrowStart -> SectionType.TODAY
            else -> SectionType.UPCOMING
        }
    }

    private fun dayBounds(now: Long, zone: ZoneId): DayBounds {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return DayBounds(
            todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli(),
            tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
    }

    private data class DayBounds(
        val todayStart: Long,
        val tomorrowStart: Long,
    )

    fun earliestStart(todo: Todo): Long? {
        val incomplete = todo.items.filterNot { it.isCompleted }.mapNotNull { it.startAt }
        if (incomplete.isNotEmpty()) return incomplete.min()
        return todo.items.mapNotNull { it.startAt }.minOrNull()
    }

    private fun comparatorFor(sortMode: SortMode): Comparator<Todo> {
        return when (sortMode) {
            SortMode.EARLIEST_START -> compareBy<Todo> { earliestStart(it) ?: Long.MAX_VALUE }
                .thenBy { it.createdAt }
                .thenBy { it.id }
            SortMode.CREATED -> compareByDescending<Todo> { it.createdAt }
                .thenByDescending { it.id }

            SortMode.MANUAL -> compareBy<Todo> { it.position }
                .thenBy { it.createdAt }
                .thenBy { it.id }
        }
    }
}

fun LocalDate.toEpochMillis(zoneId: ZoneId): Long =
    atStartOfDay(zoneId).toInstant().toEpochMilli()

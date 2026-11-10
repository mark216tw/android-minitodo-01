package com.vibecode.minitodo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TodoOrganizerTest {

    private val zone: ZoneId = ZoneId.of("Asia/Taipei")

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
    ): Long = LocalDateTime.of(year, month, day, hour, minute)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    private fun todo(
        id: Long,
        title: String,
        createdAt: Long = 0L,
        completed: Boolean = false,
        items: List<TodoItem> = emptyList(),
    ): Todo = Todo(
        id = id,
        title = title,
        createdAt = createdAt,
        isCompleted = completed,
        completedAt = if (completed) createdAt else null,
        items = items,
    )

    private fun item(
        id: Long,
        startAt: Long? = null,
        endAt: Long? = null,
        completed: Boolean = false,
        allDay: Boolean = false,
    ): TodoItem = TodoItem(
        id = id,
        title = "item-$id",
        startAt = startAt,
        endAt = endAt,
        isCompleted = completed,
        startAllDay = allDay,
        endAllDay = allDay,
    )

    @Test
    fun `completed todos are placed in completed section`() {
        val now = millis(2026, 11, 9, 12)
        val result = TodoOrganizer.organize(
            todos = listOf(
                todo(1L, "done", completed = true),
                todo(2L, "pending", items = listOf(item(1L, startAt = millis(2026, 11, 9, 14)))),
            ),
            sortMode = SortMode.EARLIEST_START,
            now = now,
            zoneId = zone,
        )
        assertEquals(
            listOf(SectionType.TODAY, SectionType.COMPLETED),
            result.map { it.type },
        )
        assertEquals(1L, result.last().todos.single().id)
    }

    @Test
    fun `overdue end time moves todo to overdue`() {
        val now = millis(2026, 11, 9, 12)
        val result = TodoOrganizer.organize(
            todos = listOf(
                todo(
                    1L,
                    "late",
                    items = listOf(
                        item(
                            1L,
                            startAt = millis(2026, 11, 9, 9),
                            endAt = millis(2026, 11, 9, 10),
                        ),
                    ),
                ),
            ),
            sortMode = SortMode.EARLIEST_START,
            now = now,
            zoneId = zone,
        )
        assertEquals(SectionType.OVERDUE, result.single().type)
    }

    @Test
    fun `all day end is treated as end of the day`() {
        val now = millis(2026, 11, 9, 12)
        val result = TodoOrganizer.organize(
            todos = listOf(
                todo(
                    1L,
                    "all-day",
                    items = listOf(
                        item(
                            1L,
                            startAt = millis(2026, 11, 9),
                            endAt = millis(2026, 11, 9),
                            allDay = true,
                        ),
                    ),
                ),
            ),
            sortMode = SortMode.EARLIEST_START,
            now = now,
            zoneId = zone,
        )
        assertEquals(SectionType.TODAY, result.single().type)
    }

    @Test
    fun `todos without dates go to no date section`() {
        val now = millis(2026, 11, 9, 12)
        val result = TodoOrganizer.organize(
            todos = listOf(todo(1L, "no-date")),
            sortMode = SortMode.EARLIEST_START,
            now = now,
            zoneId = zone,
        )
        assertEquals(SectionType.NO_DATE, result.single().type)
    }

    @Test
    fun `earliest start sort puts null start last`() {
        val now = millis(2026, 11, 9, 12)
        val first = todo(
            1L,
            "first",
            createdAt = 100L,
            items = listOf(item(1L, startAt = millis(2026, 11, 9, 13))),
        )
        val second = todo(
            2L,
            "second",
            createdAt = 50L,
            items = listOf(item(2L, startAt = millis(2026, 11, 9, 15))),
        )
        val noStart = todo(3L, "no-start", createdAt = 1L)
        val result = TodoOrganizer.organize(
            todos = listOf(second, noStart, first),
            sortMode = SortMode.EARLIEST_START,
            now = now,
            zoneId = zone,
        )
        val ordered = result.flatMap { it.todos }.map { it.id }
        assertEquals(listOf(1L, 2L, 3L), ordered)
    }

    @Test
    fun `created sort puts newest first`() {
        val now = millis(2026, 11, 9, 12)
        val older = todo(
            1L,
            "older",
            createdAt = 100L,
            items = listOf(item(1L, startAt = millis(2026, 11, 9, 13))),
        )
        val newer = todo(
            2L,
            "newer",
            createdAt = 200L,
            items = listOf(item(2L, startAt = millis(2026, 11, 9, 15))),
        )
        val result = TodoOrganizer.organize(
            todos = listOf(older, newer),
            sortMode = SortMode.CREATED,
            now = now,
            zoneId = zone,
        )
        assertEquals(listOf(2L, 1L), result.single().todos.map { it.id })
    }

    @Test
    fun `earliest start ignores completed items when possible`() {
        val todo = todo(
            1L,
            "todo",
            items = listOf(
                item(1L, startAt = millis(2026, 11, 9, 8), completed = true),
                item(2L, startAt = millis(2026, 11, 9, 15)),
            ),
        )
        assertEquals(millis(2026, 11, 9, 15), TodoOrganizer.earliestStart(todo))
    }

    @Test
    fun `future start is upcoming`() {
        val now = millis(2026, 11, 9, 12)
        val result = TodoOrganizer.organize(
            todos = listOf(
                todo(
                    1L,
                    "future",
                    items = listOf(item(1L, startAt = millis(2026, 11, 10, 9))),
                ),
            ),
            sortMode = SortMode.EARLIEST_START,
            now = now,
            zoneId = zone,
        )
        assertEquals(SectionType.UPCOMING, result.single().type)
        assertTrue(result.single().todos.isNotEmpty())
    }

    @Test
    fun `manual sort uses stored position and single all section`() {
        val now = millis(2026, 11, 9, 12)
        val first = todo(
            1L,
            "first",
            items = listOf(item(1L, startAt = millis(2026, 11, 10, 9))),
        ).copy(position = 0)
        val second = todo(
            2L,
            "second",
            items = listOf(item(2L, startAt = millis(2026, 11, 9, 9))),
        ).copy(position = 1)
        val done = todo(3L, "done", completed = true).copy(position = 2)

        val result = TodoOrganizer.organize(
            todos = listOf(second, done, first),
            sortMode = SortMode.MANUAL,
            now = now,
            zoneId = zone,
        )

        assertEquals(
            listOf(SectionType.ALL, SectionType.COMPLETED),
            result.map { it.type },
        )
        assertEquals(listOf(1L, 2L), result.first().todos.map { it.id })
    }
}

package com.vibecode.minitodo.data.backup

import com.vibecode.minitodo.domain.Todo
import com.vibecode.minitodo.domain.TodoColor
import com.vibecode.minitodo.domain.TodoItem
import org.json.JSONArray
import org.json.JSONObject

internal object BackupJson {

    private const val FORMAT_VERSION = 1

    fun encode(todos: List<Todo>, exportedAt: Long): String {
        val root = JSONObject()
        root.put("app", "MiniTodo")
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", exportedAt)
        val todosArray = JSONArray()
        todos.forEach { todo ->
            todosArray.put(
                JSONObject().apply {
                    put("title", todo.title)
                    put("color", todo.color.name)
                    put("isCompleted", todo.isCompleted)
                    put("createdAt", todo.createdAt)
                    putOpt("completedAt", todo.completedAt)
                    putOpt("archivedAt", todo.archivedAt)
                    put("position", todo.position)
                    put("items", JSONArray().apply {
                        todo.items.forEach { item ->
                            put(
                                JSONObject().apply {
                                    put("title", item.title)
                                    putOpt("startAt", item.startAt)
                                    put("startAllDay", item.startAllDay)
                                    putOpt("endAt", item.endAt)
                                    put("endAllDay", item.endAllDay)
                                    put("isCompleted", item.isCompleted)
                                    putOpt("completedAt", item.completedAt)
                                    put("position", item.position)
                                },
                            )
                        }
                    })
                },
            )
        }
        root.put("todos", todosArray)
        return root.toString(2)
    }

    fun decode(text: String): List<Todo> {
        val root = JSONObject(text)
        val version = root.optInt("version", 0)
        require(version == FORMAT_VERSION) { "Unsupported backup version" }
        val todosArray = root.optJSONArray("todos") ?: JSONArray()
        return buildList {
            for (index in 0 until todosArray.length()) {
                val todoJson = todosArray.getJSONObject(index)
                val itemsJson = todoJson.optJSONArray("items") ?: JSONArray()
                val items = buildList {
                    for (itemIndex in 0 until itemsJson.length()) {
                        val itemJson = itemsJson.getJSONObject(itemIndex)
                        add(
                            TodoItem(
                                title = itemJson.optString("title"),
                                startAt = itemJson.optLongOrNull("startAt"),
                                startAllDay = itemJson.optBoolean("startAllDay"),
                                endAt = itemJson.optLongOrNull("endAt"),
                                endAllDay = itemJson.optBoolean("endAllDay"),
                                isCompleted = itemJson.optBoolean("isCompleted"),
                                completedAt = itemJson.optLongOrNull("completedAt"),
                                position = itemJson.optInt("position", itemIndex),
                            ),
                        )
                    }
                }
                add(
                    Todo(
                        title = todoJson.optString("title"),
                        color = TodoColor.entries.firstOrNull {
                            it.name == todoJson.optString("color")
                        } ?: TodoColor.NONE,
                        isCompleted = todoJson.optBoolean("isCompleted"),
                        createdAt = todoJson.optLong("createdAt", 0L),
                        completedAt = todoJson.optLongOrNull("completedAt"),
                        archivedAt = todoJson.optLongOrNull("archivedAt"),
                        position = todoJson.optInt("position", index),
                        items = items,
                    ),
                )
            }
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null
}

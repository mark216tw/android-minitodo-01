package com.vibecode.minitodo.data.backup

import android.content.Context
import android.net.Uri
import com.vibecode.minitodo.data.repository.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(
    private val context: Context,
    private val repository: TodoRepository,
) {

    suspend fun export(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val todos = repository.getAllTodosForBackup()
            val json = BackupJson.encode(todos, System.currentTimeMillis())
            val output = context.contentResolver.openOutputStream(uri)
                ?: error("Unable to open backup file")
            output.use { stream -> stream.write(json.toByteArray(Charsets.UTF_8)) }
            todos.size
        }
    }

    suspend fun import(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Unable to open backup file")
            val bytes = input.use { stream -> stream.readBytes() }
            require(bytes.size <= MAX_BACKUP_BYTES) { "Backup file is too large" }
            val todos = BackupJson.decode(bytes.toString(Charsets.UTF_8))
            repository.replaceAllTodos(todos)
            todos.size
        }
    }

    private companion object {
        const val MAX_BACKUP_BYTES = 5 * 1024 * 1024
    }
}

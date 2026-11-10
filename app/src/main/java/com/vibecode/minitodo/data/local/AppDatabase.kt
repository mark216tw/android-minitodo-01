package com.vibecode.minitodo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TodoEntity::class, TodoItemEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_todos_archivedAt ON todos(archivedAt)",
                )
            }
        }

        fun create(
            context: Context,
            allowDestructiveMigration: Boolean = false,
        ): AppDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "minitodo.db",
            )
                .addMigrations(MIGRATION_1_2)
            if (allowDestructiveMigration) {
                builder.fallbackToDestructiveMigration(true)
            }
            return builder.build()
        }
    }
}

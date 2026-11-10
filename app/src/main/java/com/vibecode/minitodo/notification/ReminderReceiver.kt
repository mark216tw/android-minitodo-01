package com.vibecode.minitodo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.vibecode.minitodo.MiniTodoApp
import com.vibecode.minitodo.R
import com.vibecode.minitodo.util.DateTimeUtils
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? MiniTodoApp ?: return
        val itemId = intent.getLongExtra(ReminderContract.EXTRA_ITEM_ID, -1L)
        if (itemId <= 0L) return
        val type = intent.getStringExtra(ReminderContract.EXTRA_TYPE)
            ?.let { name -> ReminderType.entries.firstOrNull { it.name == name } }
            ?: return

        when (intent.action) {
            ReminderContract.ACTION_COMPLETE -> handleComplete(context, app, itemId, type)
            ReminderContract.ACTION_SNOOZE -> handleSnooze(context, app, itemId, type)
            ReminderContract.ACTION_REMINDER -> {
                val snoozed = intent.getBooleanExtra(ReminderContract.EXTRA_SNOOZE, false)
                handleReminder(context, app, itemId, type, snoozed)
            }
        }
    }

    private fun handleReminder(
        context: Context,
        app: MiniTodoApp,
        itemId: Long,
        type: ReminderType,
        snoozed: Boolean,
    ) {
        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                val entry = app.container.todoRepository.getReminderEntry(itemId)
                if (entry == null || entry.archived || entry.isCompleted) return@launch

                if (!snoozed) {
                    val expected = when (type) {
                        ReminderType.START -> entry.startAt?.let {
                            DateTimeUtils.reminderTime(it, entry.startAllDay)
                        }
                        ReminderType.END -> entry.endAt?.let {
                            DateTimeUtils.reminderTime(it, entry.endAllDay)
                        }
                    }
                    if (expected == null || expected > System.currentTimeMillis() + 60_000L) {
                        return@launch
                    }
                }
                NotificationHelper.showReminder(context, entry, type)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleComplete(
        context: Context,
        app: MiniTodoApp,
        itemId: Long,
        type: ReminderType,
    ) {
        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                app.container.todoRepository.setItemCompleted(itemId, true)
                NotificationHelper.cancel(context, itemId, type)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSnooze(
        context: Context,
        app: MiniTodoApp,
        itemId: Long,
        type: ReminderType,
    ) {
        app.container.reminderScheduler.scheduleSnooze(itemId, type)
        NotificationHelper.cancel(context, itemId, type)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                context.getString(R.string.notification_snoozed),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}

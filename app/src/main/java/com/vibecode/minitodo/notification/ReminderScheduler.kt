package com.vibecode.minitodo.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.vibecode.minitodo.data.repository.TodoRepository
import com.vibecode.minitodo.domain.ReminderEntry
import com.vibecode.minitodo.util.DateTimeUtils

class ReminderScheduler(
    private val context: Context,
    private val repository: TodoRepository,
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    suspend fun rescheduleAll() {
        val entries = repository.getActiveReminderEntries()
        repository.getScheduledItemIds().forEach { itemId ->
            cancel(itemId, ReminderType.START, snooze = false)
            cancel(itemId, ReminderType.END, snooze = false)
        }

        val now = System.currentTimeMillis()
        entries.forEach { entry ->
            entry.startAt?.let { startAt ->
                schedule(
                    entry = entry,
                    type = ReminderType.START,
                    triggerAt = DateTimeUtils.reminderTime(startAt, entry.startAllDay),
                    now = now,
                )
            }
            entry.endAt?.let { endAt ->
                schedule(
                    entry = entry,
                    type = ReminderType.END,
                    triggerAt = DateTimeUtils.reminderTime(endAt, entry.endAllDay),
                    now = now,
                )
            }
        }
    }

    fun scheduleSnooze(itemId: Long, type: ReminderType) {
        val triggerAt = System.currentTimeMillis() + ReminderContract.SNOOZE_MILLIS
        scheduleExact(triggerAt, pendingIntent(itemId, type, snooze = true))
    }

    private fun schedule(
        entry: ReminderEntry,
        type: ReminderType,
        triggerAt: Long,
        now: Long,
    ) {
        if (triggerAt <= now) return
        val intent = pendingIntent(entry.itemId, type, snooze = false)
        scheduleExact(triggerAt, intent)
    }

    private fun scheduleExact(triggerAt: Long, pendingIntent: PendingIntent) {
        try {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancel(itemId: Long, type: ReminderType, snooze: Boolean) {
        alarmManager.cancel(pendingIntent(itemId, type, snooze))
    }

    private fun pendingIntent(
        itemId: Long,
        type: ReminderType,
        snooze: Boolean,
    ): PendingIntent {
        val uri = if (snooze) {
            ReminderContract.snoozeUri(itemId, type)
        } else {
            ReminderContract.reminderUri(itemId, type)
        }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderContract.ACTION_REMINDER
            data = uri
            putExtra(ReminderContract.EXTRA_ITEM_ID, itemId)
            putExtra(ReminderContract.EXTRA_TYPE, type.name)
            putExtra(ReminderContract.EXTRA_SNOOZE, snooze)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

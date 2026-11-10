package com.vibecode.minitodo.notification

import android.net.Uri
import androidx.core.net.toUri

enum class ReminderType {
    START,
    END,
}

object ReminderContract {
    const val ACTION_REMINDER = "com.vibecode.minitodo.action.REMINDER"
    const val ACTION_COMPLETE = "com.vibecode.minitodo.action.COMPLETE"
    const val ACTION_SNOOZE = "com.vibecode.minitodo.action.SNOOZE"

    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_TYPE = "extra_reminder_type"
    const val EXTRA_SNOOZE = "extra_snooze"

    const val SNOOZE_MILLIS = 10 * 60 * 1000L

    fun reminderUri(itemId: Long, type: ReminderType): Uri =
        "minitodo://alarm/$itemId/${type.name.lowercase()}".toUri()

    fun snoozeUri(itemId: Long, type: ReminderType): Uri =
        "minitodo://alarm/$itemId/${type.name.lowercase()}/snooze".toUri()

    fun actionUri(action: String, itemId: Long, type: ReminderType): Uri =
        "minitodo://action/$action/$itemId/${type.name.lowercase()}".toUri()

    fun notificationId(itemId: Long, type: ReminderType): Int {
        val base = (itemId % 1_000_000L).toInt()
        return base * 10 + type.ordinal + 1
    }
}

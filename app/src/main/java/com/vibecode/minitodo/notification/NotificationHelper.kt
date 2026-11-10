package com.vibecode.minitodo.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.vibecode.minitodo.MainActivity
import com.vibecode.minitodo.R
import com.vibecode.minitodo.domain.ReminderEntry

object NotificationHelper {

    const val CHANNEL_ID = "reminders"
    private const val GROUP_KEY = "com.vibecode.minitodo.reminders"

    fun createChannel(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        )
            .setName(context.getString(R.string.notification_channel_name))
            .setDescription(context.getString(R.string.notification_channel_description))
            .build()
        manager.createNotificationChannel(channel)
    }

    fun showReminder(
        context: Context,
        entry: ReminderEntry,
        type: ReminderType,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val itemTitle = entry.itemTitle.ifBlank { context.getString(R.string.untitled_item) }
        val todoTitle = entry.todoTitle.ifBlank { context.getString(R.string.untitled_todo) }
        val eventText = context.getString(
            if (type == ReminderType.START) {
                R.string.notification_start_text
            } else {
                R.string.notification_end_text
            },
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(itemTitle)
            .setContentText(context.getString(R.string.notification_content, eventText, todoTitle))
            .setSubText(context.getString(R.string.notification_todo_label, todoTitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(GROUP_KEY)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, entry.todoId, entry.itemId, type))
            .addAction(
                0,
                context.getString(R.string.notification_complete),
                actionIntent(context, ReminderContract.ACTION_COMPLETE, entry.itemId, type),
            )
            .addAction(
                0,
                context.getString(R.string.notification_snooze),
                actionIntent(context, ReminderContract.ACTION_SNOOZE, entry.itemId, type),
            )
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(ReminderContract.notificationId(entry.itemId, type), notification)
        } catch (_: SecurityException) {
            // Notification permission can be revoked between the check and notify call.
        }
    }

    fun cancel(context: Context, itemId: Long, type: ReminderType) {
        NotificationManagerCompat.from(context)
            .cancel(ReminderContract.notificationId(itemId, type))
    }

    private fun contentIntent(
        context: Context,
        todoId: Long,
        itemId: Long,
        type: ReminderType,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = "minitodo://todo/$todoId".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            ReminderContract.notificationId(itemId, type),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(
        context: Context,
        action: String,
        itemId: Long,
        type: ReminderType,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            data = ReminderContract.actionUri(action, itemId, type)
            putExtra(ReminderContract.EXTRA_ITEM_ID, itemId)
            putExtra(ReminderContract.EXTRA_TYPE, type.name)
        }
        return PendingIntent.getBroadcast(
            context,
            ReminderContract.notificationId(itemId, type),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

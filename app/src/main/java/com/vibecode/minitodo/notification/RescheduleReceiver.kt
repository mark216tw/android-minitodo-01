package com.vibecode.minitodo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vibecode.minitodo.MiniTodoApp
import kotlinx.coroutines.launch

class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in ALLOWED_ACTIONS) return

        val app = context.applicationContext as? MiniTodoApp ?: return
        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                app.container.reminderScheduler.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val ALLOWED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

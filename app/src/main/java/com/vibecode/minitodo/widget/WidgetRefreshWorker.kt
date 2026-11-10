package com.vibecode.minitodo.widget

import android.content.Context
import androidx.annotation.Keep
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vibecode.minitodo.util.DateTimeUtils
import java.util.concurrent.TimeUnit

@Keep
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        TodoWidgetUpdater.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "widget_refresh"

        fun schedule(context: Context) {
            val now = System.currentTimeMillis()
            val nextMidnight = DateTimeUtils.startOfTomorrow(now) + 5 * 60 * 1000L
            val delay = (nextMidnight - now).coerceAtLeast(0L)
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}

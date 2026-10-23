package com.lyulf.mind.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderPollWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        IncomingSync.poll(applicationContext)
        BackgroundSync.scheduleChain(applicationContext)
        return Result.success()
    }
}

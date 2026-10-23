package com.lyulf.mind.notify

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lyulf.mind.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 无常驻通知：靠进程内 WS + 15s 轮询（需自启动 / 省电无限制）。
 * WorkManager 仅作进程被杀后的弱兜底。
 */
object BackgroundSync {
    private const val TAG = "MindBg"
    private const val UNIQUE_PERIODIC = "mind_poll_periodic"
    private const val UNIQUE_CHAIN = "mind_poll_chain"
    private const val TAG_POLL = "mind_poll"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var aliveJob: Job? = null
    private var started = false

    fun start(context: Context, appContainer: AppContainer) {
        val appCtx = context.applicationContext
        MindNotifier.ensureChannel(appCtx)
        ensurePeriodic(appCtx)
        scheduleChain(appCtx)
        if (started) {
            Log.d(TAG, "already started")
            return
        }
        started = true
        aliveJob = scope.launch {
            Log.i(TAG, "alive loop start (ws + 15s poll, no FGS)")
            val wsJob = launch {
                appContainer.realtime.events.collect {
                    Log.i(TAG, "ws tick → poll")
                    IncomingSync.poll(appCtx)
                }
            }
            try {
                while (isActive) {
                    IncomingSync.poll(appCtx)
                    delay(15_000)
                }
            } finally {
                wsJob.cancel()
                Log.w(TAG, "alive loop ended")
            }
        }
    }

    fun stop(context: Context) {
        aliveJob?.cancel()
        aliveJob = null
        started = false
        WorkManager.getInstance(context.applicationContext).apply {
            cancelUniqueWork(UNIQUE_CHAIN)
            cancelUniqueWork(UNIQUE_PERIODIC)
        }
    }

    fun scheduleChain(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val req = OneTimeWorkRequestBuilder<ReminderPollWorker>()
            .setInitialDelay(2, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(TAG_POLL)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_CHAIN, ExistingWorkPolicy.REPLACE, req)
    }

    private fun ensurePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val req = PeriodicWorkRequestBuilder<ReminderPollWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(TAG_POLL)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
    }
}

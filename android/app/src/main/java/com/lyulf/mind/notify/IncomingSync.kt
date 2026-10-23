package com.lyulf.mind.notify

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.lyulf.mind.MindApp
import com.lyulf.mind.data.remote.ReminderDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 统一处理「对方发来」的新提醒。
 *
 * 决策：
 * - 未打底 / primed 但 csv 空：只打底，不弹（防历史狂刷）
 * - 有 fresh id：后台弹系统通知；前台（RESUMED）不弹，只记 seen（列表会自己刷）
 *
 * 日志 tag = MindPoll，排查时看 reason= 那一行。
 */
object IncomingSync {
    private const val TAG = "MindPoll"
    private val mutex = Mutex()

    fun isAppForeground(): Boolean =
        ProcessLifecycleOwner.get()
            .lifecycle
            .currentState
            .isAtLeast(Lifecycle.State.RESUMED)

    suspend fun poll(context: Context): List<ReminderDto> = mutex.withLock {
        val app = context.applicationContext as? MindApp ?: run {
            Log.w(TAG, "skip reason=no_mind_app")
            return emptyList()
        }
        return try {
            val session = app.container.sessionStore.session.first()
            if (!session.paired || session.token.isNullOrBlank()) {
                Log.d(TAG, "skip reason=not_paired")
                return emptyList()
            }

            val items = app.container.repository.listReminders(session.bearer())
            val incoming = items.filter { it.dir == "in" }
            val seen = app.container.seenReminders
            val known = seen.knownIds()
            val primed = seen.isPrimed()
            val fg = isAppForeground()

            Log.d(
                TAG,
                "poll ok in=${incoming.size} known=${known.size} primed=$primed fg=$fg feed=${NotifyGate.feedResumed}",
            )

            // 升级后 primed=true 但 csv 空：当未打底，避免整段历史当新消息
            if (!primed || (known.isEmpty() && incoming.isNotEmpty())) {
                seen.markSeen(incoming.map { it.id }, prime = true)
                Log.i(
                    TAG,
                    "skip reason=prime_baseline count=${incoming.size} migrated=${primed && known.isEmpty()}",
                )
                return emptyList()
            }

            val fresh = incoming.filter { it.id !in known }
            if (fresh.isEmpty()) {
                Log.d(TAG, "skip reason=no_fresh")
                return emptyList()
            }

            val freshTypes = fresh.joinToString(",") { "${it.type}:${it.id.take(6)}" }
            if (fg) {
                // 前台：列表/WS 已在刷，不弹系统通知，但仍记 seen，避免退后台后补炸
                seen.markSeen(fresh.map { it.id }, prime = true)
                Log.i(TAG, "skip reason=foreground_suppress fresh=[$freshTypes]")
                return fresh
            }

            Log.i(TAG, "notify reason=background_fresh fresh=[$freshTypes]")
            fresh.take(5).forEach { item ->
                MindNotifier.notifyIncoming(
                    context.applicationContext,
                    item,
                    session.partnerPronounLabel,
                )
            }
            seen.markSeen(fresh.map { it.id }, prime = true)
            fresh
        } catch (t: Throwable) {
            Log.w(TAG, "skip reason=poll_failed msg=${t.message}", t)
            emptyList()
        }
    }
}

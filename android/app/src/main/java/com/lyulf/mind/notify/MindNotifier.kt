package com.lyulf.mind.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lyulf.mind.MainActivity
import com.lyulf.mind.R
import com.lyulf.mind.data.remote.ReminderDto
import kotlin.random.Random

object MindNotifier {
    private const val TAG = "MindNotif"
    // v2：旧 mind_care 渠道创建后系统不再改震动开关；换 id 才能默认开震动
    const val CHANNEL_ID = "mind_care_v2"
    private const val CHANNEL_NAME = "惦记提醒"
    private val VIBRATE_PATTERN = longArrayOf(0, 280, 160, 280)
    private val notifySeq = java.util.concurrent.atomic.AtomicInteger(1000)

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching { mgr.deleteNotificationChannel("mind_care") }
        val ch = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "对方发来的惦记与戳一下"
            enableVibration(true)
            vibrationPattern = VIBRATE_PATTERN
            setShowBadge(true)
        }
        mgr.createNotificationChannel(ch)
    }

    fun notifyIncoming(
        context: Context,
        item: ReminderDto,
        pronoun: String,
    ) {
        ensureChannel(context)
        val title = "惦 · Mind"
        val body = buildBody(item, pronoun)

        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notify", true)
            putExtra("reminder_id", item.id)
        }
        val reqCode = notifySeq.incrementAndGet()
        val pi = PendingIntent.getActivity(
            context,
            reqCode,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mind)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setOnlyAlertOnce(false)
            .build()

        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        try {
            NotificationManagerCompat.from(context).notify(reqCode, notif)
            Log.i(TAG, "posted id=$reqCode type=${item.type} enabled=$enabled body=$body")
        } catch (se: SecurityException) {
            Log.e(TAG, "post failed SecurityException enabled=$enabled", se)
        } catch (t: Throwable) {
            Log.e(TAG, "post failed ${t.message}", t)
        }
    }

    /** 主文案随机；约每 4～5 条带一句括号彩蛋 */
    private fun buildBody(item: ReminderDto, pronoun: String): String {
        val main = when (item.type) {
            "drink" -> pick(
                "该喝水了",
                "喝水了吗？",
                "喝口水呗",
            )
            "stand" -> pick(
                "该起身活动一下了",
                "起身走走？",
                "坐太久啦，起来转转",
            )
            "kegel" -> pick(
                "该做提肛了",
                "提肛打卡一下？",
                "提肛时间到",
            )
            "poke" -> pick(
                "${pronoun}戳了你一下",
                "${pronoun}戳了戳你",
                "被${pronoun}戳了一下",
            )
            "custom" -> {
                // 贴贴：通知直接用那句话，不加彩蛋
                return item.title?.trim()?.takeIf { it.isNotBlank() }
                    ?: item.content?.trim()?.takeIf { it.isNotBlank() }
                    ?: "${pronoun}想贴贴你"
            }
            else -> {
                // care：通知直接用正文，不加彩蛋
                return item.content?.trim()?.takeIf { it.isNotBlank() }
                    ?: item.title?.trim()?.takeIf { it.isNotBlank() }
                    ?: "${pronoun}想贴贴你"
            }
        }
        return if (Random.nextInt(5) == 0) {
            "$main（${pickAside(pronoun)}）"
        } else {
            main
        }
    }

    private fun pickAside(pronoun: String): String = pick(
        "${pronoun}可能是想你了",
        "${pronoun}一定很爱你",
        "本意是想和你贴贴",
        "查岗！",
        "不听话打你哦",
        "来都来了",
        "比心比心",
    )

    private fun pick(vararg lines: String): String =
        lines[Random.nextInt(lines.size)]
}

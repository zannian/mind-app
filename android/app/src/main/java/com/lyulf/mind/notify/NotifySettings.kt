package com.lyulf.mind.notify

import android.content.Context
import android.content.Intent
import android.provider.Settings

/** 跳到系统「惦记提醒」渠道页（小米上可开悬浮通知 / 震动） */
object NotifySettings {
    fun openChannel(context: Context) {
        MindNotifier.ensureChannel(context)
        val pkg = context.packageName
        val intents = listOf(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
                putExtra(Settings.EXTRA_CHANNEL_ID, MindNotifier.CHANNEL_ID)
            },
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", pkg, null)
            },
        )
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Throwable) {
            }
        }
    }
}

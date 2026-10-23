package com.lyulf.mind.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lyulf.mind.MindApp

/** 开机后把后台轮询重新挂上 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? MindApp ?: return
        BackgroundSync.start(app, app.container)
    }
}

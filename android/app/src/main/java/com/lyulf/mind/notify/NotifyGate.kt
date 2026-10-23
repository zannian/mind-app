package com.lyulf.mind.notify

/**
 * 互动 Tab 是否正对着用户（前台 + Feed 可见）。
 * 仅在这种情况下不弹系统通知（列表会自己更新）。
 */
object NotifyGate {
    @Volatile
    var feedResumed: Boolean = false
}

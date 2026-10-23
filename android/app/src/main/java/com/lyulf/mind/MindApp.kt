package com.lyulf.mind

import android.app.Application
import com.lyulf.mind.data.AppContainer
import com.lyulf.mind.notify.BackgroundSync

class MindApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BackgroundSync.start(this, container)
    }
}

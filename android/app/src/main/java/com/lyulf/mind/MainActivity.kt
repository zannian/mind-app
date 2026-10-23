package com.lyulf.mind

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lyulf.mind.ui.MindRoot
import com.lyulf.mind.ui.theme.MindTheme

class MainActivity : ComponentActivity() {
    private val requestNotify = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 拒了也能用，只是没有本地通知 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureNotifyPermission()
        val app = application as MindApp
        setContent {
            MindTheme {
                MindRoot(container = app.container)
            }
        }
    }

    private fun ensureNotifyPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

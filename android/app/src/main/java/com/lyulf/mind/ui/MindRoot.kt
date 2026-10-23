package com.lyulf.mind.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lyulf.mind.data.AppContainer
import com.lyulf.mind.ui.onboarding.OnboardingScreen
import com.lyulf.mind.ui.pair.WaitingPairScreen
import kotlinx.coroutines.delay

@Composable
fun MindRoot(container: AppContainer) {
    val session by container.sessionStore.session.collectAsState(initial = null)
    // 品牌页至少停一小会儿，避免会话秒回时 splash 一闪而过
    var holdSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(900)
        holdSplash = false
    }

    DisposableEffect(session?.token, session?.paired) {
        val s = session
        if (s != null && s.paired && !s.token.isNullOrBlank()) {
            container.realtime.connect(s.token)
        } else {
            container.realtime.disconnect()
        }
        // 不解绑不断开：退后台仍尽量保 WS（靠省电白名单）
        onDispose { }
    }

    when {
        session == null || holdSplash -> SplashPane()
        !session!!.hasDevice -> OnboardingScreen(repo = container.repository)
        !session!!.paired -> WaitingPairScreen(session = session!!, repo = container.repository)
        else -> MainShell(
            session = session!!,
            repo = container.repository,
            realtime = container.realtime,
        )
    }
}

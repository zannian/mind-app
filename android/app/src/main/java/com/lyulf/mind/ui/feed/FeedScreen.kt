package com.lyulf.mind.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lyulf.mind.data.MindRealtime
import com.lyulf.mind.data.MindRepository
import com.lyulf.mind.data.SessionStore
import com.lyulf.mind.data.apiMessage
import com.lyulf.mind.data.remote.ReminderDto
import com.lyulf.mind.notify.IncomingSync
import com.lyulf.mind.notify.NotifyGate
import com.lyulf.mind.ui.components.ComposeSendSheet
import com.lyulf.mind.ui.components.DetailSheet
import com.lyulf.mind.ui.components.ReminderRow
import com.lyulf.mind.ui.components.RespondSheet
import com.lyulf.mind.ui.theme.MindBg
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindPrimary
import com.lyulf.mind.ui.theme.MindSurface
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PokeBackWindowMs = 5_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    session: SessionStore.Session,
    repo: MindRepository,
    realtime: MindRealtime,
) {
    var items by remember { mutableStateOf<List<ReminderDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSend by remember { mutableStateOf(false) }
    var detailTarget by remember { mutableStateOf<ReminderDto?>(null) }
    var respondTarget by remember { mutableStateOf<ReminderDto?>(null) }
    var peekPoke by remember { mutableStateOf(false) }
    var knownIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var primed by remember { mutableStateOf(false) }
    var isPokeAnimating by remember { mutableStateOf(false) }
    var showPokeBackFab by remember { mutableStateOf(false) }
    var pokeBackJob by remember { mutableStateOf<Job?>(null) }
    var showJumpBottom by remember { mutableStateOf(false) }
    val shakeX = remember { Animatable(0f) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val bearer = session.bearer()
    val pronoun = session.partnerPronounLabel
    val lifecycleOwner = LocalLifecycleOwner.current

    /** 滚到列表末尾占位（不是最后一条消息本身，否则会被顶到视口上方） */
    suspend fun scrollToBottom(animate: Boolean = true) {
        if (items.isEmpty()) return
        // items 之后还有一个 bottom spacer，index == items.size
        val end = items.size
        if (animate) listState.animateScrollToItem(end) else listState.scrollToItem(end)
    }

    // 告诉后台：用户正在看互动流（此时不弹系统通知）
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> NotifyGate.feedResumed = true
                Lifecycle.Event.ON_PAUSE -> NotifyGate.feedResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            NotifyGate.feedResumed = true
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            NotifyGate.feedResumed = false
        }
    }

    fun hidePokeBack() {
        pokeBackJob?.cancel()
        pokeBackJob = null
        showPokeBackFab = false
    }

    fun offerPokeBackWindow() {
        pokeBackJob?.cancel()
        pokeBackJob = scope.launch {
            while (isPokeAnimating) delay(16)
            showPokeBackFab = true
            delay(PokeBackWindowMs)
            showPokeBackFab = false
        }
    }

    suspend fun playPokeShake() {
        isPokeAnimating = true
        showPokeBackFab = false
        try {
            val amp = with(density) { 10.dp.toPx() }
            repeat(3) {
                shakeX.animateTo(amp, tween(45))
                shakeX.animateTo(-amp, tween(45))
            }
            shakeX.animateTo(0f, tween(45))
        } finally {
            isPokeAnimating = false
            shakeX.snapTo(0f)
        }
    }

    suspend fun applyList(next: List<ReminderDto>) {
        val foreground = IncomingSync.isAppForeground()
        val anyNew = if (primed) next.any { it.id !in knownIds } else false
        val firstPaint = !primed
        // API 是新→旧；互动流按聊天习惯：旧在上、新在下
        val chronological = next.asReversed()

        if (primed) {
            val newPokes = next.filter { it.type == "poke" && it.id !in knownIds }
            val gotPoked = newPokes.any { it.dir == "in" }
            val iPoked = newPokes.any { it.dir == "out" }
            if (foreground && (gotPoked || iPoked)) {
                scope.launch {
                    playPokeShake()
                    if (gotPoked) offerPokeBackWindow()
                }
            }
        }

        knownIds = next.map { it.id }.toSet()
        primed = true
        items = chronological

        // 已见 / 系统通知一律交给 IncomingSync，避免这里抢先 markSeen 把通知吃掉
        if (firstPaint && chronological.isNotEmpty()) {
            // 等一帧让 LazyColumn 吃到新列表再滚到底
            delay(32)
            scrollToBottom(animate = false)
            showJumpBottom = false
        } else if (foreground && anyNew) {
            val atBottom = !listState.canScrollForward
            if (atBottom) {
                scrollToBottom()
                showJumpBottom = false
            } else {
                showJumpBottom = true
            }
        }
    }

    /** 静默拉取，不动下拉刷新转圈 */
    suspend fun refreshQuiet() {
        try {
            applyList(repo.listReminders(bearer))
            error = null
        } catch (t: Throwable) {
            error = t.apiMessage()
        } finally {
            loading = false
        }
    }

    suspend fun refreshFromPull() {
        refreshing = true
        try {
            applyList(repo.listReminders(bearer))
            error = null
        } catch (t: Throwable) {
            error = t.apiMessage()
        } finally {
            loading = false
            refreshing = false
        }
    }

    suspend fun refreshAndScrollBottom() {
        refreshQuiet()
        scrollToBottom()
    }

    LaunchedEffect(bearer) {
        refreshQuiet()
        // WS 断线兜底
        while (true) {
            delay(30_000)
            try {
                applyList(repo.listReminders(bearer))
            } catch (_: Throwable) {
            }
        }
    }

    LaunchedEffect(realtime) {
        realtime.events.collect {
            try {
                applyList(repo.listReminders(bearer))
            } catch (_: Throwable) {
            }
        }
    }

    // 滑到底部附近时收起「有新消息」
    LaunchedEffect(listState) {
        snapshotFlow { !listState.canScrollForward }
            .distinctUntilChanged()
            .collect { atBottom ->
                if (atBottom) showJumpBottom = false
            }
    }

    val today = items.filter { it.isToday }
    val careCount = today.count { it.type != "poke" }
    val pokeCount = today.count { it.type == "poke" }
    val showPokeBack = showPokeBackFab && !isPokeAnimating

    Box(Modifier.fillMaxSize().background(MindBg)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text("互动", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = MindInk)
                Text(
                    text = buildString {
                        append("今天 · 你们贴贴了 $careCount 次")
                        if (peekPoke) append("，相互戳了 $pokeCount 次")
                    },
                    fontSize = 11.5.sp,
                    color = MindInk3,
                    modifier = Modifier.clickable {
                        peekPoke = true
                        scope.launch {
                            delay(3000)
                            peekPoke = false
                        }
                    },
                )
            }
            when {
                loading && items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MindPrimary)
                }
                error != null && items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = MindPrimary)
                }
                else -> Box(Modifier.fillMaxSize()) {
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = { scope.launch { refreshFromPull() } },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = 100.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .offset { IntOffset(shakeX.value.roundToInt(), 0) },
                        ) {
                            items(items, key = { it.id }) { item ->
                                ReminderRow(
                                    item = item,
                                    pronoun = pronoun,
                                    onOpen = { detailTarget = item },
                                    onRespond = { respondTarget = item },
                                    onAck = {
                                        scope.launch {
                                            try {
                                                repo.ack(bearer, item.id)
                                                refreshQuiet()
                                            } catch (t: Throwable) {
                                                error = t.apiMessage()
                                            }
                                        }
                                    },
                                )
                            }
                            // 末尾锚点：scrollToBottom 滚到这里，最后一条才真正露在 FAB 上方
                            item(key = "feed_end") { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                    // 显式用 animation 版，避免吃到外层 ColumnScope 扩展
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showJumpBottom,
                        enter = fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.92f),
                        exit = fadeOut(tween(100)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MindPrimary,
                            shadowElevation = 4.dp,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    scrollToBottom()
                                    showJumpBottom = false
                                }
                            },
                        ) {
                            Text(
                                "有新消息 · 回到底部",
                                color = MindSurface,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showPokeBack,
            enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.85f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 88.dp, bottom = 28.dp),
        ) {
            FloatingActionButton(
                onClick = {
                    hidePokeBack()
                    scope.launch {
                        try {
                            repo.poke(bearer)
                            refreshAndScrollBottom()
                        } catch (t: Throwable) {
                            error = t.apiMessage()
                        }
                    }
                },
                shape = RoundedCornerShape(999.dp),
                containerColor = MindSurface,
                contentColor = MindPrimary,
            ) {
                Text(
                    "回戳",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
        }

        FloatingActionButton(
            onClick = { showSend = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            shape = CircleShape,
            containerColor = MindPrimary,
            contentColor = MindSurface,
        ) {
            Icon(Icons.Default.Add, contentDescription = "发送")
        }
    }

    if (showSend) {
        ComposeSendSheet(
            pronoun = pronoun,
            bearer = bearer,
            repo = repo,
            onDismiss = { showSend = false },
            onSent = { scope.launch { refreshAndScrollBottom() } },
        )
    }
    detailTarget?.let { target ->
        DetailSheet(
            item = target,
            pronoun = pronoun,
            onDismiss = { detailTarget = null },
            onRespond = {
                detailTarget = null
                respondTarget = target
            },
            onAck = {
                scope.launch {
                    try {
                        repo.ack(bearer, target.id)
                        detailTarget = null
                        refreshQuiet()
                    } catch (t: Throwable) {
                        error = t.apiMessage()
                    }
                }
            },
            onOpened = {
                scope.launch {
                    try {
                        repo.markRead(bearer, target.id)
                        refreshQuiet()
                    } catch (_: Throwable) {
                    }
                }
            },
        )
    }
    respondTarget?.let { target ->
        RespondSheet(
            target = target,
            bearer = bearer,
            repo = repo,
            onDismiss = { respondTarget = null },
            onDone = { scope.launch { refreshQuiet() } },
        )
    }
}

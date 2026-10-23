package com.lyulf.mind.ui.mind

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyulf.mind.data.MindRealtime
import com.lyulf.mind.data.MindRepository
import com.lyulf.mind.data.SessionStore
import com.lyulf.mind.data.apiMessage
import com.lyulf.mind.data.remote.ReminderDto
import com.lyulf.mind.ui.components.ComposeSendSheet
import com.lyulf.mind.ui.components.DetailSheet
import com.lyulf.mind.ui.components.RespondSheet
import com.lyulf.mind.ui.theme.MindBg
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk2
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindPending
import com.lyulf.mind.ui.theme.MindPrimary
import com.lyulf.mind.ui.theme.MindPrimarySoft
import com.lyulf.mind.ui.theme.MindSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MindScreen(
    session: SessionStore.Session,
    repo: MindRepository,
    realtime: MindRealtime,
) {
    var sideFrom by remember { mutableStateOf(true) }
    var fromItems by remember { mutableStateOf<List<ReminderDto>>(emptyList()) }
    var toItems by remember { mutableStateOf<List<ReminderDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSend by remember { mutableStateOf(false) }
    var detailTarget by remember { mutableStateOf<ReminderDto?>(null) }
    var respondTarget by remember { mutableStateOf<ReminderDto?>(null) }
    val scope = rememberCoroutineScope()
    val bearer = session.bearer()
    val pronoun = session.partnerPronounLabel

    suspend fun refresh() {
        try {
            val today = repo.today(bearer)
            fromItems = today.fromPartner.filter { it.type != "poke" }
            toItems = today.toPartner.filter { it.type != "poke" }
            error = null
        } catch (t: Throwable) {
            error = t.apiMessage()
        } finally {
            loading = false
        }
    }

    LaunchedEffect(bearer) {
        refresh()
        while (true) {
            delay(30_000)
            try {
                refresh()
            } catch (_: Throwable) {
            }
        }
    }

    LaunchedEffect(realtime) {
        realtime.events.collect {
            try {
                refresh()
            } catch (_: Throwable) {
            }
        }
    }

    val items = if (sideFrom) fromItems else toItems
    val hero = if (sideFrom) {
        if (fromItems.isEmpty()) "${pronoun}今天还没惦记你哦"
        else "${pronoun}今天惦记了你 ${fromItems.size} 次"
    } else {
        if (toItems.isEmpty()) "你今天还没惦记${pronoun}呢"
        else "你今天惦记了 TA ${toItems.size} 次"
    }

    Box(Modifier.fillMaxSize().background(MindBg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("惦", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = MindInk)
            }

            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFEDE4DE))
                    .padding(3.dp),
            ) {
                SegBtn("From $pronoun", fromItems.size, sideFrom) { sideFrom = true }
                SegBtn("To $pronoun", toItems.size, !sideFrom) { sideFrom = false }
            }

            if (loading && fromItems.isEmpty() && toItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MindPrimary)
                }
            } else if (error != null && items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = MindPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    // 用 contentPadding，最后一条才能滚到加号上方完整露出
                    contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp),
                ) {
                    item {
                        Text(
                            hero,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = MindInk2,
                        )
                    }
                    if (items.isEmpty()) {
                        item {
                            Column(
                                Modifier.fillMaxWidth().padding(top = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    if (sideFrom) "${pronoun}今天还没惦记你哦" else "你今天还没惦记${pronoun}呢",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MindInk,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (sideFrom) "等一会儿就好，或者你先开口" else "一条提醒只要三秒",
                                    fontSize = 13.sp,
                                    color = MindInk3,
                                )
                                if (!sideFrom) {
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick = { showSend = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
                                        shape = RoundedCornerShape(999.dp),
                                    ) { Text("贴贴 $pronoun 一下") }
                                }
                            }
                        }
                    } else {
                        items(items, key = { it.id }) { r ->
                            Column {
                                MindRow(
                                    item = r,
                                    isFrom = sideFrom,
                                    onOpen = { detailTarget = r },
                                    onRespond = { respondTarget = r },
                                    onAck = {
                                        scope.launch {
                                            try {
                                                repo.ack(bearer, r.id)
                                                refresh()
                                            } catch (t: Throwable) {
                                                error = t.apiMessage()
                                            }
                                        }
                                    },
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showSend = true; sideFrom = false },
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
            onSent = {
                sideFrom = false
                scope.launch { refresh() }
            },
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
                        refresh()
                    } catch (t: Throwable) {
                        error = t.apiMessage()
                    }
                }
            },
            onOpened = {
                scope.launch {
                    try {
                        repo.markRead(bearer, target.id)
                        refresh()
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
            onDone = { scope.launch { refresh() } },
        )
    }
}

@Composable
private fun SegBtn(label: String, count: Int, on: Boolean, click: () -> Unit) {
    Text(
        text = "$label  $count",
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) MindSurface else Color.Transparent)
            .clickable(onClick = click)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (on) MindInk else MindInk2,
    )
}

@Composable
private fun MindRow(
    item: ReminderDto,
    isFrom: Boolean,
    onOpen: () -> Unit,
    onRespond: () -> Unit,
    onAck: () -> Unit,
) {
    val (icon, name) = when (item.type) {
        "drink" -> "💧" to "喝水"
        "stand" -> "🧍" to "起身"
        "kegel" -> "🌿" to "提肛"
        "custom" -> "✎" to (item.title ?: "贴贴")
        else -> "·" to item.type
    }
    val pending = isFrom && item.state == "todo" && !item.read
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (pending) Color(0xFFFFFBF7) else MindSurface)
            .border(
                width = if (pending) 1.5.dp else 1.dp,
                color = if (pending) MindPending.copy(alpha = 0.55f) else Color(0x141C1C1E),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onOpen)
            .then(
                if (item.foldCorner) {
                    Modifier.drawBehind {
                        val s = 14.dp.toPx()
                        val path = Path().apply {
                            moveTo(size.width - s, size.height)
                            lineTo(size.width, size.height - s)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(path, MindPrimary.copy(alpha = 0.5f))
                        drawLine(
                            Color.White.copy(alpha = 0.5f),
                            Offset(size.width - s, size.height),
                            Offset(size.width, size.height - s),
                            1.dp.toPx(),
                        )
                    }
                } else Modifier,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MindPrimarySoft),
            contentAlignment = Alignment.Center,
        ) { Text(icon) }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Row {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MindInk)
                Spacer(Modifier.weight(1f))
                Text(item.timeLabel, fontSize = 11.sp, color = MindInk3)
            }
            if (!item.content.isNullOrBlank()) {
                Text(item.content, fontSize = 12.5.sp, color = MindInk2, maxLines = 2)
            }
            if (isFrom && item.state == "todo") {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRespond,
                        colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.height(34.dp),
                    ) { Text("回应", fontSize = 12.sp) }
                    if (!item.read) {
                        TextButton(onClick = onAck) { Text("知道了", fontSize = 12.sp, color = MindInk2) }
                    }
                }
            }
        }
        if (!isFrom && item.listStatus != null) {
            Text(
                item.listStatus,
                fontSize = 10.sp,
                color = if (item.listStatus == "未读") MindPrimary else MindInk3,
                fontWeight = if (item.listStatus == "未读") FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

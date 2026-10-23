package com.lyulf.mind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyulf.mind.data.MindRealtime
import com.lyulf.mind.data.MindRepository
import com.lyulf.mind.data.SessionStore
import com.lyulf.mind.ui.daily.DailyScreen
import com.lyulf.mind.ui.feed.FeedScreen
import com.lyulf.mind.ui.mind.MindScreen
import com.lyulf.mind.ui.theme.MindBg
import com.lyulf.mind.ui.theme.MindIcons
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindPrimary
import com.lyulf.mind.ui.theme.MindSurface
import kotlinx.coroutines.delay

@Composable
fun MainShell(
    session: SessionStore.Session,
    repo: MindRepository,
    realtime: MindRealtime,
) {
    var tab by remember { mutableIntStateOf(0) }
    var mindBadge by remember { mutableIntStateOf(0) }
    val bearer = session.bearer()

    suspend fun refreshBadge() {
        try {
            val today = repo.today(bearer)
            mindBadge = today.fromPartner.count { it.type != "poke" && it.state == "todo" }
        } catch (_: Throwable) {
        }
    }

    LaunchedEffect(bearer) {
        refreshBadge()
        // 兜底：WS 断了也不至于角标永远旧
        while (true) {
            delay(30_000)
            refreshBadge()
        }
    }

    LaunchedEffect(realtime) {
        realtime.events.collect { refreshBadge() }
    }

    data class Tab(val label: String, val icon: ImageVector)

    val tabs = listOf(
        Tab("互动", MindIcons.Chat),
        Tab("惦", MindIcons.Heart),
        Tab("日常", MindIcons.Stats),
    )

    Scaffold(
        containerColor = MindBg,
        bottomBar = {
            NavigationBar(containerColor = MindSurface) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            Box {
                                Icon(
                                    t.icon,
                                    contentDescription = t.label,
                                    modifier = Modifier.size(22.dp),
                                )
                                if (i == 1 && mindBadge > 0) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 3.dp, y = (-2).dp)
                                            .size(8.dp)
                                            .background(Color(0xFFFF3B30), CircleShape),
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                t.label,
                                fontSize = 10.5.sp,
                                fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MindPrimary,
                            selectedTextColor = MindPrimary,
                            unselectedIconColor = MindInk3,
                            unselectedTextColor = MindInk3,
                            indicatorColor = MindSurface,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> FeedScreen(session = session, repo = repo, realtime = realtime)
                1 -> MindScreen(
                    session = session,
                    repo = repo,
                    realtime = realtime,
                )
                else -> DailyScreen(session = session, repo = repo)
            }
        }
    }
}

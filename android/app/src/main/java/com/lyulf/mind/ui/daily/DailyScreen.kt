package com.lyulf.mind.ui.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyulf.mind.data.MindRepository
import com.lyulf.mind.data.SessionStore
import com.lyulf.mind.data.apiMessage
import com.lyulf.mind.data.remote.HabitModuleDto
import com.lyulf.mind.ui.settings.SettingsScreen
import com.lyulf.mind.ui.theme.MindBg
import com.lyulf.mind.ui.theme.MindDone
import com.lyulf.mind.ui.theme.MindIcons
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk2
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindPrimary
import com.lyulf.mind.ui.theme.MindPrimarySoft
import com.lyulf.mind.ui.theme.MindSurface
import kotlinx.coroutines.launch

@Composable
fun DailyScreen(
    session: SessionStore.Session,
    repo: MindRepository,
) {
    var showSettings by remember { mutableStateOf(false) }
    var modules by remember { mutableStateOf<List<HabitModuleDto>>(emptyList()) }
    var date by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var trendModule by remember { mutableStateOf<HabitModuleDto?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var customMl by remember { mutableStateOf<HabitModuleDto?>(null) }
    val scope = rememberCoroutineScope()
    val bearer = session.bearer()

    suspend fun refresh() {
        try {
            val d = repo.daily(bearer)
            modules = d.modules
            date = d.date
            error = null
        } catch (t: Throwable) {
            error = t.apiMessage()
        } finally {
            loading = false
        }
    }

    LaunchedEffect(bearer) { refresh() }

    if (showSettings) {
        SettingsScreen(
            session = session,
            repo = repo,
            onBack = { showSettings = false },
        )
        return
    }

    if (trendModule != null) {
        TrendScreen(
            module = trendModule!!,
            bearer = bearer,
            repo = repo,
            onBack = { trendModule = null },
        )
        return
    }

    Column(Modifier.fillMaxSize().background(MindBg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("日常", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = MindInk)
                Text(
                    if (date.isNotBlank()) "今天 · $date" else "今天",
                    fontSize = 11.5.sp,
                    color = MindInk3,
                )
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    MindIcons.Gear,
                    contentDescription = "设置",
                    tint = MindInk2,
                )
            }
        }

        when {
            loading && modules.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MindPrimary)
            }
            error != null && modules.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = MindPrimary)
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(modules, key = { it.id }) { m ->
                    ModuleCard(
                        module = m,
                        onDrink = { ml ->
                            scope.launch {
                                try {
                                    repo.logDrink(bearer, ml)
                                    refresh()
                                } catch (t: Throwable) {
                                    error = t.apiMessage()
                                }
                            }
                        },
                        onCustomDrink = { customMl = m },
                        onTick = {
                            scope.launch {
                                try {
                                    repo.logActivity(bearer, m.id, m.moduleType)
                                    refresh()
                                } catch (t: Throwable) {
                                    error = t.apiMessage()
                                }
                            }
                        },
                        onTrend = { trendModule = m },
                        onDelete = if (m.moduleType == "custom") {
                            {
                                scope.launch {
                                    try {
                                        repo.deleteModule(bearer, m.id)
                                        refresh()
                                    } catch (t: Throwable) {
                                        error = t.apiMessage()
                                    }
                                }
                            }
                        } else null,
                    )
                }
                item {
                    TextButton(
                        onClick = { showAdd = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
                    ) {
                        Text("＋ 添加模块", color = MindPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("3") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加模块") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 8) name = it },
                        label = { Text("名字 ≤8 字") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("每天目标次数") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                repo.addModule(bearer, name.trim(), target.toIntOrNull() ?: 3)
                                showAdd = false
                                refresh()
                            } catch (t: Throwable) {
                                error = t.apiMessage()
                            }
                        }
                    },
                    enabled = name.isNotBlank(),
                ) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } },
        )
    }

    if (customMl != null) {
        var mlText by remember { mutableStateOf("250") }
        AlertDialog(
            onDismissRequest = { customMl = null },
            title = { Text("自定义水量") },
            text = {
                OutlinedTextField(
                    value = mlText,
                    onValueChange = { mlText = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("ml") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val n = mlText.toIntOrNull() ?: return@TextButton
                        scope.launch {
                            try {
                                repo.logDrink(bearer, n)
                                customMl = null
                                refresh()
                            } catch (t: Throwable) {
                                error = t.apiMessage()
                            }
                        }
                    },
                ) { Text("记录") }
            },
            dismissButton = { TextButton(onClick = { customMl = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun ModuleCard(
    module: HabitModuleDto,
    onDrink: (Int) -> Unit,
    onCustomDrink: () -> Unit,
    onTick: () -> Unit,
    onTrend: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val isDrink = module.moduleType == "drink"
    val icon = when (module.moduleType) {
        "drink" -> "💧"
        "stand" -> "🧍"
        "kegel" -> "🌿"
        else -> "📖"
    }
    val pct = if (module.dailyTarget <= 0) 0f
    else (module.todayValue.toFloat() / module.dailyTarget).coerceIn(0f, 1f)
    val done = module.todayValue >= module.dailyTarget
    val over = module.todayValue - module.dailyTarget
    val unit = if (isDrink) "ml" else "次"
    val timesLine = if (module.todayTimes.isEmpty()) {
        "今天还没记"
    } else if (isDrink) {
        "今天 · " + module.todayTimes.take(4).joinToString(" · ") { "${it.at} +${it.value}ml" }
    } else {
        "今天 · " + module.todayTimes.take(4).joinToString(" · ") { it.at }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MindSurface)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 22.sp)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(module.title, fontWeight = FontWeight.SemiBold, color = MindInk)
                Text(
                    if (isDrink) "今天 ${module.todayTimes.size} 次记录" else "自己加的习惯",
                    fontSize = 11.5.sp,
                    color = MindInk3,
                )
            }
            Text(
                buildString {
                    append(module.todayValue)
                    append("/")
                    append(module.dailyTarget)
                    append(" ")
                    append(unit)
                },
                fontWeight = FontWeight.SemiBold,
                color = MindInk,
                fontSize = 13.sp,
            )
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text("×", color = MindInk3) }
            }
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
            color = MindPrimary,
            trackColor = MindPrimarySoft,
        )
        Spacer(Modifier.height(10.dp))
        if (isDrink) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(200, 300, 500).forEach { ml ->
                    TextButton(
                        onClick = { onDrink(ml) },
                        colors = ButtonDefaults.textButtonColors(containerColor = MindPrimarySoft),
                        shape = RoundedCornerShape(999.dp),
                    ) { Text("+$ml", color = MindPrimary, fontSize = 12.sp) }
                }
                TextButton(
                    onClick = onCustomDrink,
                    colors = ButtonDefaults.textButtonColors(containerColor = MindPrimarySoft),
                    shape = RoundedCornerShape(999.dp),
                ) { Text("自定义", color = MindPrimary, fontSize = 12.sp) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(timesLine, fontSize = 11.5.sp, color = MindInk2, modifier = Modifier.weight(1f))
            TextButton(onClick = onTrend) {
                Text("趋势 ›", color = MindInk2, fontSize = 12.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    done && over > 0 -> "已完成 · 多做 $over 次"
                    done -> "已完成"
                    isDrink -> "达标进度 ${(pct * 100).toInt()}%"
                    else -> "还差 ${module.dailyTarget - module.todayValue} 次"
                },
                fontSize = 12.sp,
                color = if (done) MindDone else MindInk2,
                modifier = Modifier.weight(1f),
            )
            if (!isDrink) {
                Button(
                    onClick = onTick,
                    colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
                    shape = RoundedCornerShape(999.dp),
                ) { Text("记一次", fontSize = 12.5.sp) }
            }
        }
    }
}

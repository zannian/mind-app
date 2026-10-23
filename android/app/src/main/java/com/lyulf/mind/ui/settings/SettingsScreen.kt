package com.lyulf.mind.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyulf.mind.data.MindRepository
import com.lyulf.mind.data.SessionStore
import com.lyulf.mind.data.apiMessage
import com.lyulf.mind.data.remote.HabitModuleDto
import com.lyulf.mind.notify.NotifySettings
import com.lyulf.mind.ui.components.CopyableCode
import com.lyulf.mind.ui.components.PronounPicker
import com.lyulf.mind.ui.theme.MindBg
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk2
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindPrimary
import com.lyulf.mind.ui.theme.MindSurface
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    session: SessionStore.Session,
    repo: MindRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val bearer = session.bearer()
    var pronoun by remember { mutableStateOf(session.partnerPronoun ?: "ta") }
    var modules by remember { mutableStateOf<List<HabitModuleDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var editModule by remember { mutableStateOf<HabitModuleDto?>(null) }
    var confirmUnbind by remember { mutableStateOf(false) }
    LaunchedEffect(bearer) {
        try {
            modules = repo.daily(bearer).modules
        } catch (t: Throwable) {
            error = t.apiMessage()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MindBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("‹ 返回", color = MindInk2) }
            Text(
                "设置",
                Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MindInk,
            )
            Spacer(Modifier.padding(horizontal = 28.dp))
        }

        Spacer(Modifier.height(12.dp))
        SectionTitle("称呼")
        CardBlock {
            PronounPicker(
                pronoun = pronoun,
                onChange = { code ->
                    pronoun = code
                    scope.launch {
                        error = null
                        try {
                            repo.updatePronoun(bearer, code)
                        } catch (t: Throwable) {
                            error = t.apiMessage()
                        }
                    }
                },
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("每日目标")
        CardBlock {
            modules.forEachIndexed { i, m ->
                if (i > 0) Spacer(Modifier.height(2.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { editModule = m }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(m.title, color = MindInk, fontSize = 14.sp)
                    Text(
                        if (m.moduleType == "drink") "${m.dailyTarget} ml" else "${m.dailyTarget} 次",
                        color = MindInk2,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (modules.isEmpty()) {
                Text("加载中…", color = MindInk3, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("配对")
        CardBlock {
            InfoRow("身份", if (session.side == "a") "创建方 · a" else "加入方 · b")
            InfoRow("称呼对方", session.partnerPronounLabel)
            if (!session.recoveryCode.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                CopyableCode(
                    "本机保存的恢复码",
                    session.recoveryCode,
                    codeSize = 14.sp,
                    codeColor = MindInk,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("通知")
        CardBlock {
            Text(
                "小米上「悬浮通知」「震动」默认常关，App 无法代开，需在系统里手动打开。",
                fontSize = 13.sp,
                color = MindInk2,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { NotifySettings.openChannel(context) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
            ) {
                Text("打开「惦记提醒」设置", fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            "后台靠：自启动 / 省电无限制（小米：应用管理 → 省电策略）。",
            fontSize = 11.sp,
            color = MindInk3,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("关于")
        CardBlock {
            InfoRow("惦 · Mind", "0.1.0")
            InfoRow("提醒", "后台轮询 + 本地通知")
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = MindPrimary, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = { confirmUnbind = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MindPrimary),
        ) {
            Text("解绑这对关系", fontWeight = FontWeight.SemiBold)
        }
        Text(
            "会删除双方设备身份与全部惦记数据，不可恢复。",
            fontSize = 11.sp,
            color = MindInk3,
            modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
        )
    }

    editModule?.let { m ->
        var input by remember(m.id) { mutableStateOf(m.dailyTarget.toString()) }
        AlertDialog(
            onDismissRequest = { editModule = null },
            title = { Text("改「${m.title}」目标") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() }.take(5) },
                    label = {
                        Text(if (m.moduleType == "drink") "每天目标 ml" else "每天目标次数")
                    },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val v = input.toIntOrNull() ?: return@Button
                        scope.launch {
                            try {
                                repo.updateModuleTarget(bearer, m.id, v)
                                modules = repo.daily(bearer).modules
                                editModule = null
                            } catch (t: Throwable) {
                                error = t.apiMessage()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
                    enabled = (input.toIntOrNull() ?: 0) > 0,
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editModule = null }) { Text("取消") }
            },
        )
    }

    if (confirmUnbind) {
        AlertDialog(
            onDismissRequest = { confirmUnbind = false },
            title = { Text("确认解绑？") },
            text = { Text("双方都会回到未配对状态，历史与打卡一并清空。") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                repo.unbind(bearer)
                                confirmUnbind = false
                            } catch (t: Throwable) {
                                error = t.apiMessage()
                                confirmUnbind = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
                ) { Text("解绑") }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnbind = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = MindInk3,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
    )
}

@Composable
private fun CardBlock(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MindSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        content()
    }
}

@Composable
private fun InfoRow(k: String, v: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(k, color = MindInk2, fontSize = 14.sp)
        Text(v, color = MindInk, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

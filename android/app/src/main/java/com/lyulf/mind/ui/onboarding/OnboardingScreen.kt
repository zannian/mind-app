package com.lyulf.mind.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyulf.mind.data.MindRepository
import com.lyulf.mind.data.apiMessage
import com.lyulf.mind.ui.components.CopyableCode
import com.lyulf.mind.ui.components.PronounPicker
import com.lyulf.mind.ui.theme.MindBg
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk2
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindPrimary
import com.lyulf.mind.ui.theme.MindSurface
import kotlinx.coroutines.launch

private enum class Mode { Home, Create, Join, Recover }

@Composable
fun OnboardingScreen(repo: MindRepository) {
    var mode by remember { mutableStateOf(Mode.Home) }
    var pronoun by remember { mutableStateOf("she") }
    var inviteInput by remember { mutableStateOf("") }
    var recoveryInput by remember { mutableStateOf("") }
    var recoverSide by remember { mutableStateOf("a") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var shownRecovery by remember { mutableStateOf<String?>(null) }
    var shownInvite by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(MindBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("惦 · Mind", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = MindInk)
        Text("你在惦谁", fontSize = 13.sp, color = MindInk3, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Text("不用账号密码。邀请码成对，换机用恢复码。", fontSize = 13.sp, color = MindInk2)
        Spacer(Modifier.height(24.dp))

        when (mode) {
            Mode.Home -> {
                Button(
                    onClick = { mode = Mode.Create; error = null },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
                    shape = RoundedCornerShape(999.dp),
                ) { Text("创建一对（生成邀请码）") }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { mode = Mode.Join; error = null },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
                    shape = RoundedCornerShape(999.dp),
                ) { Text("输入邀请码加入") }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { mode = Mode.Recover; error = null }) {
                    Text("换机恢复", color = MindInk2)
                }
            }

            Mode.Create -> {
                PronounPicker(pronoun) { pronoun = it }
                Spacer(Modifier.height(16.dp))
                if (shownInvite != null) {
                    CopyableCode("邀请码（给对方，一次有效）", shownInvite!!, codeSize = 28.sp)
                    Spacer(Modifier.height(12.dp))
                    if (!shownRecovery.isNullOrBlank()) {
                        CopyableCode(
                            "恢复码（请双方抄下来，换机用）",
                            shownRecovery!!,
                            codeSize = 16.sp,
                            codeColor = MindInk,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("已保存到本机。对方加入后会自动进入。", fontSize = 12.sp, color = MindInk2)
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                loading = true
                                error = null
                                try {
                                    val res = repo.createPair(pronoun)
                                    shownInvite = res.inviteCode
                                    shownRecovery = res.recoveryCode
                                } catch (t: Throwable) {
                                    error = t.apiMessage()
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
                        shape = RoundedCornerShape(999.dp),
                    ) { Text(if (loading) "…" else "生成") }
                }
                TextButton(onClick = { mode = Mode.Home; shownInvite = null; shownRecovery = null }) {
                    Text("返回", color = MindInk3)
                }
            }

            Mode.Join -> {
                PronounPicker(pronoun) { pronoun = it }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = inviteInput,
                    onValueChange = { inviteInput = it.uppercase() },
                    label = { Text("邀请码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                repo.joinPair(inviteInput, pronoun)
                            } catch (t: Throwable) {
                                error = t.apiMessage()
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading && inviteInput.length >= 4,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
                    shape = RoundedCornerShape(999.dp),
                ) { Text("加入") }
                TextButton(onClick = { mode = Mode.Home }) { Text("返回", color = MindInk3) }
            }

            Mode.Recover -> {
                Text("我是哪一边？", fontSize = 13.sp, color = MindInk2)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("a" to "创建方", "b" to "加入方").forEach { (code, label) ->
                        val on = recoverSide == code
                        Text(
                            text = label,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (on) MindPrimary else MindInk3.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .background(if (on) MindSurface else MindBg, RoundedCornerShape(12.dp))
                                .clickable { recoverSide = code }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            color = if (on) MindPrimary else MindInk2,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = recoveryInput,
                    onValueChange = { recoveryInput = it.uppercase() },
                    label = { Text("恢复码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                repo.recoverPair(recoveryInput, recoverSide, null)
                            } catch (t: Throwable) {
                                error = t.apiMessage()
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading && recoveryInput.length >= 8,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
                    shape = RoundedCornerShape(999.dp),
                ) { Text("恢复到本机") }
                TextButton(onClick = { mode = Mode.Home }) { Text("返回", color = MindInk3) }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = MindPrimary, fontSize = 13.sp)
        }
    }
}


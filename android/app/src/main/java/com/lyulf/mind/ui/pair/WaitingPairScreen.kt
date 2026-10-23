package com.lyulf.mind.ui.pair

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyulf.mind.data.MindRepository
import com.lyulf.mind.data.SessionStore
import com.lyulf.mind.data.apiMessage
import com.lyulf.mind.ui.components.CopyableCode
import com.lyulf.mind.ui.theme.MindBg
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk2
import com.lyulf.mind.ui.theme.MindPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WaitingPairScreen(
    session: SessionStore.Session,
    repo: MindRepository,
) {
    var hint by remember { mutableStateOf("等待对方输入邀请码…") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val bearer = session.bearer()

    LaunchedEffect(bearer) {
        while (true) {
            try {
                if (repo.refreshMe(bearer)) {
                    hint = "对方已加入，正在进入…"
                    break
                }
            } catch (_: Throwable) {
            }
            delay(2500)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MindBg)
            .statusBarsPadding()
            .padding(24.dp),
    ) {
        Text("等待配对", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = MindInk)
        Text(hint, fontSize = 13.sp, color = MindPrimary, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        if (!session.inviteCode.isNullOrBlank()) {
            CopyableCode("邀请码", session.inviteCode, codeSize = 32.sp)
        }
        if (!session.recoveryCode.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            CopyableCode(
                "恢复码（请抄下）",
                session.recoveryCode,
                codeSize = 15.sp,
                codeColor = MindInk,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                scope.launch {
                    try {
                        repo.refreshMe(bearer)
                    } catch (t: Throwable) {
                        error = t.apiMessage()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MindPrimary),
            shape = RoundedCornerShape(999.dp),
        ) { Text("刷新") }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = MindPrimary, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = {
                scope.launch {
                    try {
                        repo.unbind(bearer)
                    } catch (_: Throwable) {
                        repo.clearLocal()
                    }
                }
            },
        ) {
            Text("取消并解散", color = MindInk2)
        }
    }
}

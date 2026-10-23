package com.lyulf.mind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk2
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindPrimary
import com.lyulf.mind.ui.theme.MindSurface
import kotlinx.coroutines.launch

private data class SendType(val id: String, val icon: String, val label: String)

private val SEND_TYPES = listOf(
    SendType("drink", "💧", "喝水"),
    SendType("stand", "🧍", "起身"),
    SendType("kegel", "🌿", "提肛"),
    SendType("custom", "✎", "贴贴"),
    SendType("poke", "👉", "戳一下"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeSendSheet(
    pronoun: String,
    bearer: String,
    repo: MindRepository,
    onDismiss: () -> Unit,
    onSent: () -> Unit,
) {
    var type by remember { mutableStateOf("drink") }
    var content by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = MindSurface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("贴贴 $pronoun", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = MindInk)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SEND_TYPES.forEach { t ->
                    val on = type == t.id
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (on) MindPrimary else MindInk3.copy(alpha = 0.3f),
                                RoundedCornerShape(14.dp),
                            )
                            .background(if (on) MindPrimary.copy(alpha = 0.08f) else MindSurface, RoundedCornerShape(14.dp))
                            .clickable { type = t.id }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        Text(t.icon, fontSize = 18.sp)
                        Text(
                            t.label,
                            fontSize = 11.sp,
                            color = if (on) MindPrimary else MindInk2,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            when (type) {
                "poke" -> Text("戳一下不需要正文，发出去她手机响一下。", fontSize = 13.sp, color = MindInk2)
                "custom" -> OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 15) title = it },
                    label = { Text("跟 $pronoun 说一句…（≤15字）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                else -> OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("可选一句话") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MindPrimary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            when (type) {
                                "poke" -> repo.poke(bearer)
                                "custom" -> {
                                    if (title.isBlank()) {
                                        error = "先写一句贴贴的话"
                                        return@launch
                                    }
                                    repo.sendReminder(bearer, "custom", title = title.trim())
                                }
                                else -> repo.sendReminder(
                                    bearer,
                                    type,
                                    content = content.trim().ifBlank { null },
                                )
                            }
                            onSent()
                            onDismiss()
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
            ) {
                Text(if (loading) "…" else "发送")
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

package com.lyulf.mind.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lyulf.mind.data.MindRepository
import com.lyulf.mind.data.apiMessage
import com.lyulf.mind.data.remote.ReminderDto
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk2
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindPending
import com.lyulf.mind.ui.theme.MindPrimarySoft
import com.lyulf.mind.ui.theme.MindSurface
import com.lyulf.mind.util.ImageCompress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RespondSheet(
    target: ReminderDto,
    bearer: String,
    repo: MindRepository,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    var amount by remember { mutableIntStateOf(300) }
    var customMode by remember { mutableStateOf(false) }
    var customMl by remember { mutableStateOf("") }
    var count by remember { mutableIntStateOf(1) }
    var reply by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) imageUri = uri }

    val (icon, name) = when (target.type) {
        "drink" -> "💧" to "喝水"
        "stand" -> "🧍" to "起身"
        "kegel" -> "🌿" to "提肛"
        "custom" -> "✎" to (target.title ?: "贴贴")
        else -> "·" to target.type
    }
    val sub = buildString {
        append("$icon $name · ${target.timeLabel}")
        if (!target.content.isNullOrBlank()) append(" · ${target.content}")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = MindSurface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("回应", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MindInk)
            Text(sub, fontSize = 12.5.sp, color = MindInk3, modifier = Modifier.padding(top = 6.dp))

            when (target.type) {
                "drink" -> {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(200, 300, 500).forEach { ml ->
                            Chip(
                                label = "$ml ml",
                                selected = !customMode && amount == ml,
                                onClick = {
                                    customMode = false
                                    amount = ml
                                },
                            )
                        }
                        Chip(
                            label = "自定义",
                            selected = customMode,
                            onClick = { customMode = true },
                        )
                    }
                    if (customMode) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = customMl,
                            onValueChange = { customMl = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text("ml") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MindPending,
                                cursorColor = MindPending,
                            ),
                        )
                    }
                }
                "stand" -> {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("起身 1 次", selected = count == 1) { count = 1 }
                        Chip("记 5 分钟", selected = count == 5) { count = 5 }
                    }
                }
                "kegel" -> {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("做 1 组", selected = count == 1) { count = 1 }
                        Chip("做 3 组", selected = count == 3) { count = 3 }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = reply,
                onValueChange = { reply = it },
                placeholder = { Text("附一句？（可选）", color = MindInk3) },
                modifier = Modifier.fillMaxWidth().height(88.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MindPending,
                    cursorColor = MindPending,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MindPrimarySoft)
                    .clickable {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
                    .then(
                        if (imageUri != null) Modifier.height(140.dp) else Modifier.padding(vertical = 14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "已选图片",
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text("＋ 加一张图（可选）", fontSize = 13.sp, color = MindInk2)
                }
            }
            if (imageUri != null) {
                Text(
                    "点图片可重选 · 取消请清掉再发",
                    fontSize = 11.sp,
                    color = MindInk3,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { imageUri = null },
                )
            }
            if (error != null) {
                Text(error!!, color = MindPending, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            var uploaded: String? = null
                            val uri = imageUri
                            if (uri != null) {
                                val compressed = withContext(Dispatchers.IO) {
                                    runCatching { ImageCompress.compress(context, uri) }
                                }.getOrElse {
                                    error = "图片处理失败"
                                    return@launch
                                }
                                if (compressed.bytes.size > 8 * 1024 * 1024) {
                                    error = "图片太大（最多 8MB）"
                                    return@launch
                                }
                                uploaded = repo.uploadImage(
                                    bearer,
                                    compressed.bytes,
                                    mime = compressed.mime,
                                    filename = compressed.filename,
                                ).url
                            }
                            val note = reply.ifBlank { null }
                            when (target.type) {
                                "drink" -> {
                                    val ml = if (customMode) customMl.toIntOrNull() else amount
                                    if (ml == null || ml <= 0) {
                                        error = "请选择或输入水量"
                                        return@launch
                                    }
                                    repo.respond(
                                        bearer,
                                        target.id,
                                        amountMl = ml,
                                        reply = note,
                                        imageUrl = uploaded,
                                    )
                                }
                                "stand", "kegel" ->
                                    repo.respond(
                                        bearer,
                                        target.id,
                                        count = count,
                                        reply = note,
                                        imageUrl = uploaded,
                                    )
                                else ->
                                    repo.respond(
                                        bearer,
                                        target.id,
                                        reply = note,
                                        imageUrl = uploaded,
                                    )
                            }
                            onDone()
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
                colors = ButtonDefaults.buttonColors(containerColor = MindPending),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(if (loading) "…" else "回应")
            }
            Text(
                "确认后 TA 会收到回执，这一条就完成了",
                fontSize = 11.5.sp,
                color = MindInk3,
                modifier = Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) MindSurface else MindInk2,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MindPending else MindPrimarySoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

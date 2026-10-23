package com.lyulf.mind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lyulf.mind.BuildConfig
import com.lyulf.mind.data.remote.ReminderDto
import com.lyulf.mind.ui.theme.MindDone
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk2
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindOutBorder
import com.lyulf.mind.ui.theme.MindPending
import com.lyulf.mind.ui.theme.MindPrimary
import com.lyulf.mind.ui.theme.MindPrimarySoft
import com.lyulf.mind.ui.theme.MindSurface


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(
    item: ReminderDto,
    pronoun: String,
    onDismiss: () -> Unit,
    onRespond: () -> Unit,
    onAck: () -> Unit,
    onOpened: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(item.id) {
        // 点开即已读（对方发来的）
        if (item.dir == "in" && !item.read && item.state == "todo") {
            onOpened()
        }
    }

    val (icon, name) = when (item.type) {
        "drink" -> "💧" to "喝水"
        "stand" -> "🧍" to "起身"
        "kegel" -> "🌿" to "提肛"
        "custom" -> "✎" to (item.title ?: "贴贴")
        else -> "·" to item.type
    }
    val dirLabel = if (item.dir == "in") "${pronoun}发给你的" else "你发给${pronoun}的"
    val status = item.detailStatus ?: when {
        item.state == "done" -> "已回应"
        item.read -> "仅读"
        else -> "未读"
    }
    val statusBg = when (status) {
        "未读" -> Color(0xFFFFEDE8)
        "已回应" -> Color(0xFFE7F8EC)
        else -> Color(0xFFF1EFED)
    }
    val statusFg = when (status) {
        "未读" -> MindPrimary
        "已回应" -> MindDone
        else -> MindInk2
    }
    val actable = item.dir == "in" && item.state == "todo" && item.isToday
    var previewUrl by remember { mutableStateOf<String?>(null) }

    previewUrl?.let { url ->
        FullImageDialog(model = url, onDismiss = { previewUrl = null })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = MindSurface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("$icon $name", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MindInk)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text("$dirLabel · ${item.timeLabel}", fontSize = 12.5.sp, color = MindInk3)
                Text(
                    status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusFg,
                    modifier = Modifier
                        .padding(start = 7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(statusBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            if (!item.content.isNullOrBlank()) {
                Text(
                    item.content,
                    fontSize = 15.sp,
                    color = MindInk,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            if (item.foldCorner && (!item.reply.isNullOrBlank() || item.amountMl != null || item.count != null || item.hasImage)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFBF7F4))
                        .border(1.dp, MindOutBorder, RoundedCornerShape(12.dp))
                        .padding(11.dp),
                ) {
                    if (!item.reply.isNullOrBlank()) {
                        Text("“${item.reply}”", fontSize = 13.sp, color = MindInk2, lineHeight = 20.sp)
                    }
                    if (item.hasImage || !item.imageUrl.isNullOrBlank()) {
                        val url = absoluteMediaUrl(item.imageUrl)
                        if (url != null) {
                            AsyncImage(
                                model = url,
                                contentDescription = "回应图片，点开看全图",
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { previewUrl = url },
                                contentScale = ContentScale.Crop,
                            )
                            Text(
                                "点图看全貌",
                                fontSize = 11.sp,
                                color = MindInk3,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        } else {
                            Box(
                                Modifier
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MindPrimarySoft)
                                    .padding(horizontal = 28.dp, vertical = 20.dp),
                            ) { Text("📷") }
                        }
                    }
                }
            }
            if (item.state == "done") {
                val note = when {
                    item.type == "drink" && item.amountMl != null -> "记了 ${item.amountMl}ml"
                    item.type == "stand" && item.count != null -> "起身 ${item.count} 次"
                    item.type == "kegel" && item.count != null -> "做了 ${item.count} 组"
                    else -> null
                }
                if (note != null) {
                    Text(note, fontSize = 12.sp, color = MindInk3, modifier = Modifier.padding(top = 10.dp))
                }
            }
            if (actable) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRespond,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MindPending),
                        shape = RoundedCornerShape(999.dp),
                    ) { Text("回应") }
                    if (!item.read) {
                        OutlinedButton(
                            onClick = onAck,
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(999.dp),
                        ) { Text("知道了", color = MindInk2) }
                    }
                }
            }
        }
    }
}

private fun absoluteMediaUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}

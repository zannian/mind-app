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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyulf.mind.data.remote.ReminderDto
import com.lyulf.mind.ui.theme.MindDone
import com.lyulf.mind.ui.theme.MindHair
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk2
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindOutBorder
import com.lyulf.mind.ui.theme.MindPending
import com.lyulf.mind.ui.theme.MindPrimary
import com.lyulf.mind.ui.theme.MindPrimarySoft
import com.lyulf.mind.ui.theme.MindSurface

private fun typeMeta(type: String, title: String?): Pair<String, String> = when (type) {
    "drink" -> "💧" to "喝水"
    "stand" -> "🧍" to "起身"
    "kegel" -> "🌿" to "提肛"
    "custom" -> "✎" to (title ?: "贴贴")
    "poke" -> "👉" to "戳一下"
    else -> "·" to type
}

@Composable
fun ReminderRow(
    item: ReminderDto,
    pronoun: String,
    onOpen: () -> Unit = {},
    onRespond: () -> Unit,
    onAck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (item.type == "poke") {
        PokeLine(item, modifier)
        return
    }

    val incoming = item.dir == "in"
    val shape = if (incoming) {
        RoundedCornerShape(topStart = 7.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 7.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    }
    val bg = if (incoming) MindSurface else MindPrimarySoft
    val border = if (incoming) MindHair else MindOutBorder
    val (icon, name) = typeMeta(item.type, item.title)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (incoming) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!incoming && item.listStatus != null) {
            Text(
                text = item.listStatus,
                color = if (item.listStatus == "未读") MindPrimary else MindInk3,
                fontSize = 10.sp,
                fontWeight = if (item.listStatus == "未读") FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.04.sp,
                modifier = Modifier.padding(end = 7.dp, bottom = 2.dp),
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = if (incoming) 290.dp else 270.dp)
                .clip(shape)
                .background(bg)
                .border(1.dp, border, shape)
                .clickable(onClick = onOpen)
                .then(
                    if (item.foldCorner) {
                        Modifier.drawBehind {
                            val s = 17.dp.toPx()
                            val path = Path().apply {
                                moveTo(size.width - s, size.height)
                                lineTo(size.width, size.height - s)
                                lineTo(size.width, size.height)
                                close()
                            }
                            drawPath(path, color = MindPrimary.copy(alpha = 0.55f))
                            drawLine(
                                color = Color.White.copy(alpha = 0.55f),
                                start = Offset(size.width - s, size.height),
                                end = Offset(size.width, size.height - s),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                    } else Modifier,
                )
                .padding(horizontal = 13.dp, vertical = 11.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 13.sp)
                    Text(
                        text = name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = MindInk2,
                        letterSpacing = 0.02.sp,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(item.timeLabel, fontSize = 11.5.sp, color = MindInk3)
                }
                if (!item.content.isNullOrBlank()) {
                    Text(
                        text = item.content,
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        color = MindInk,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                if (item.foldCorner && (!item.reply.isNullOrBlank() || item.amountMl != null || item.count != null)) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (incoming) Color(0xFFFBF7F4) else MindSurface)
                            .border(1.dp, MindOutBorder, RoundedCornerShape(12.dp))
                            .padding(9.dp, 8.dp),
                    ) {
                        when {
                            item.amountMl != null ->
                                Text("喝了 ${item.amountMl} ml", fontSize = 12.sp, color = MindDone, fontWeight = FontWeight.SemiBold)
                            item.count != null ->
                                Text("做了 ${item.count} 次", fontSize = 12.sp, color = MindDone, fontWeight = FontWeight.SemiBold)
                        }
                        if (!item.reply.isNullOrBlank()) {
                            Text(item.reply, fontSize = 13.sp, color = MindInk2, lineHeight = 18.sp)
                        }
                    }
                }
                if (incoming && item.state == "todo") {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRespond,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MindPending),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text("回应", fontSize = 12.5.sp)
                        }
                        if (!item.read) {
                            OutlinedButton(
                                onClick = onAck,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text("知道了", fontSize = 12.5.sp, color = MindInk2)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PokeLine(
    item: ReminderDto,
    modifier: Modifier = Modifier,
) {
    // 我发出的：卡靠右，👈 在卡内靠左（指向对方）
    // 对方发来的：卡靠左，👉 在卡内靠右（指向我）
    val mine = item.dir == "out"
    val pillBg = if (mine) Color(0xFFFBF3EF) else Color(0xFFF6F2EF)
    val finger = if (mine) "👈" else "👉"
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.60f)
                .clip(RoundedCornerShape(14.dp))
                .background(pillBg)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = if (mine) Alignment.CenterStart else Alignment.CenterEnd,
        ) {
            Text(finger, fontSize = 22.sp)
        }
    }
}

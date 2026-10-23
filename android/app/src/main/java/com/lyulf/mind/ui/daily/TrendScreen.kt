package com.lyulf.mind.ui.daily

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyulf.mind.data.MindRepository
import com.lyulf.mind.data.apiMessage
import com.lyulf.mind.data.remote.HabitModuleDto
import com.lyulf.mind.data.remote.TrendResponse
import com.lyulf.mind.ui.theme.MindBg
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk2
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindPrimary
import com.lyulf.mind.ui.theme.MindPrimarySoft

@Composable
fun TrendScreen(
    module: HabitModuleDto,
    bearer: String,
    repo: MindRepository,
    onBack: () -> Unit,
) {
    var data by remember { mutableStateOf<TrendResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(module.id) {
        try {
            data = repo.trends(bearer, module.id, 7)
        } catch (t: Throwable) {
            error = t.apiMessage()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MindBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回", color = MindInk2) }
            Text(
                "${module.title} · 趋势",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MindInk,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        when {
            data == null && error == null -> {
                CircularProgressIndicator(color = MindPrimary, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            error != null -> Text(error!!, color = MindPrimary)
            else -> {
                val t = data!!
                Text("近 ${t.days} 天", fontSize = 12.sp, color = MindInk3)
                Spacer(Modifier.height(12.dp))
                BarChart(
                    values = t.series.map { it.value },
                    target = t.module.dailyTarget,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    t.series.forEach { p ->
                        Text(
                            p.date.takeLast(5).replace("-", "/"),
                            fontSize = 9.sp,
                            color = MindInk3,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "日均 ${t.average} ${t.unit} · ${t.hitDays}/${t.days} 天达标",
                    fontSize = 13.sp,
                    color = MindInk2,
                )
                Text(
                    "最多 ${t.max.value}${t.unit}（${t.max.date ?: "-"}） · 最少 ${t.min.value}${t.unit}（${t.min.date ?: "-"}）",
                    fontSize = 12.sp,
                    color = MindInk3,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text("今天的每一条", fontWeight = FontWeight.SemiBold, color = MindInk)
                Spacer(Modifier.height(8.dp))
                if (t.todayEntries.isEmpty()) {
                    Text("今天还没有记录", fontSize = 13.sp, color = MindInk3)
                } else {
                    t.todayEntries.forEachIndexed { i, e ->
                        Text(
                            if (t.unit == "ml") "${e.at}  +${e.value}ml"
                            else "${e.at}  第 ${e.index ?: (i + 1)} 次",
                            fontSize = 13.sp,
                            color = MindInk2,
                            modifier = Modifier.padding(vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "今日合计 ${t.todayTotal} ${t.unit} / 目标 ${t.module.dailyTarget} ${t.unit}",
                    fontWeight = FontWeight.SemiBold,
                    color = MindInk,
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun BarChart(
    values: List<Int>,
    target: Int,
    modifier: Modifier = Modifier,
) {
    val maxY = (values.maxOrNull() ?: 0).coerceAtLeast(target).coerceAtLeast(1).toFloat()
    Canvas(modifier) {
        val n = values.size.coerceAtLeast(1)
        val gap = 10.dp.toPx()
        val barW = (size.width - gap * (n + 1)) / n
        val targetY = size.height * (1f - target / maxY)

        drawLine(
            color = MindInk3.copy(alpha = 0.45f),
            start = Offset(0f, targetY),
            end = Offset(size.width, targetY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
        )

        values.forEachIndexed { i, v ->
            val h = size.height * (v / maxY)
            val x = gap + i * (barW + gap)
            val highlight = i == values.lastIndex
            drawRoundRect(
                color = if (highlight) MindPrimary else MindPrimarySoft,
                topLeft = Offset(x, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
        }
    }
}

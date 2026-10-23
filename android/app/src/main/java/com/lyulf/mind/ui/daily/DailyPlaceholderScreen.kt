package com.lyulf.mind.ui.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyulf.mind.ui.theme.MindBg
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk3

@Composable
fun DailyPlaceholderScreen() {
    Box(
        Modifier.fillMaxSize().background(MindBg).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("日常", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = MindInk)
            Text(
                "下一轮做模块打卡与趋势页。\n只有喝水记水量，其余只记次数。",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MindInk3,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

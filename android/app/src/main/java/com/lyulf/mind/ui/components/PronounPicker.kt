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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyulf.mind.ui.theme.MindBg
import com.lyulf.mind.ui.theme.MindInk2
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindPrimary
import com.lyulf.mind.ui.theme.MindSurface

@Composable
fun PronounPicker(
    pronoun: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Text("我怎么称呼对方", fontSize = 13.sp, color = MindInk2)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("she" to "她", "he" to "他", "ta" to "TA").forEach { (code, label) ->
                val on = pronoun == code
                Text(
                    text = label,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (on) MindPrimary else MindInk3.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp),
                        )
                        .background(if (on) MindSurface else MindBg, RoundedCornerShape(12.dp))
                        .clickable { onChange(code) }
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    color = if (on) MindPrimary else MindInk2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

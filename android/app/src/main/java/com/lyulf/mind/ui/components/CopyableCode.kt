package com.lyulf.mind.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyulf.mind.ui.theme.MindInk
import com.lyulf.mind.ui.theme.MindInk3
import com.lyulf.mind.ui.theme.MindPrimary

@Composable
fun CopyableCode(
    label: String,
    code: String,
    codeSize: TextUnit = 28.sp,
    codeColor: androidx.compose.ui.graphics.Color = MindPrimary,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = MindInk3)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { copyToClipboard(context, label, code) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                code,
                fontSize = codeSize,
                fontWeight = FontWeight.SemiBold,
                color = codeColor,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "复制",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MindPrimary,
            )
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}

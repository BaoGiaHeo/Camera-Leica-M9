package com.example.camui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun LunaDreamDeveloperLogPanel() {
    val context = LocalContext.current
    var logText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        logText = LunaDreamDiagnostics.read(context)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "LOG LunaDream Working",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            "Chỉ dành cho Developer. Ghi nhận lỗi LunaDream 1.5/2.0, số khung hình, công đoạn xử lý, thời gian, bộ nhớ và lỗi crash Kotlin/Java.",
            color = CamColors.Sub,
            fontSize = 11.sp
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            DiagnosticLogAction("REFRESH", Modifier.weight(1f)) {
                logText = LunaDreamDiagnostics.read(context)
            }
            DiagnosticLogAction("COPY LOG", Modifier.weight(1f)) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("LOG LunaDream Working", logText.ifBlank {
                    LunaDreamDiagnostics.read(context)
                }))
                Toast.makeText(context, "Đã sao chép LOG LunaDream Working", Toast.LENGTH_SHORT).show()
            }
            DiagnosticLogAction("CLEAR", Modifier.weight(1f)) {
                val cleared = LunaDreamDiagnostics.clear(context)
                logText = LunaDreamDiagnostics.read(context)
                Toast.makeText(
                    context,
                    if (cleared) "Đã xóa log" else "Không thể xóa log",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        Text(
            "NỘI DUNG LOG · hiển thị phần cuối; COPY LOG sao chép toàn bộ",
            color = FujiSkin.lcdGreen,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF050708), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF344247), RoundedCornerShape(4.dp))
                .padding(10.dp)
        ) {
            Text(
                logText.takeLast(24_000).ifBlank { "Đang tải log…" },
                color = Color(0xFFD5DEDF),
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            "Lưu ý: Android không cho ứng dụng tự đọc mọi tombstone/native crash hoặc biết chắc nguyên nhân khi hệ điều hành kill tiến trình. Các lỗi Kotlin/Java không được xử lý và lỗi LunaDream trong pipeline sẽ được ghi tại đây.",
            color = CamColors.Sub,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun DiagnosticLogAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        label,
        modifier = modifier
            .background(FujiSkin.lcd, RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF526064), RoundedCornerShape(4.dp))
            .animaTClickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        color = Color.White,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold
    )
}

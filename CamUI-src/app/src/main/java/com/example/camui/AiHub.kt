package com.example.camui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HdmpYellow = Color(0xFFF5C518)
private val HdmpMuted = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubBottomSheet(
    onDismiss: () -> Unit,
    uhdEnabled: Boolean,
    onUhdEnabledChange: (Boolean) -> Unit,
    fhdEnabled: Boolean,
    onFhdEnabledChange: (Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161616),
        contentColor = Color.White,
        scrimColor = Color(0x99000000),
        tonalElevation = 0.dp,
        dragHandle = {
            HorizontalDivider(
                modifier = Modifier.width(42.dp).padding(top = 10.dp, bottom = 8.dp),
                thickness = 4.dp,
                color = Color(0xFF666666)
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("HDMP", color = HdmpYellow, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "Xử lý độ phân giải bằng thuật toán nội suy và ghép nhiều khung hình. Không sử dụng mô hình AI hoặc dịch vụ AI.",
                color = HdmpMuted, fontSize = 12.sp
            )
            HdmpToggleRow(
                title = "UHD · 211MP",
                detail = "Khóa zoom 1x, chụp 5 khung hình liên tiếp, căn chỉnh và trung bình pixel để giảm nhiễu, sau đó nội suy lên mục tiêu 211MP.",
                enabled = uhdEnabled, onEnabledChange = onUhdEnabledChange,
                status = if (uhdEnabled) "UHD · ĐANG BẬT" else "UHD · TẮT"
            )
            HorizontalDivider(color = Color(0xFF333333))
            HdmpToggleRow(
                title = "FHD · 108MP",
                detail = "Khóa zoom 1x, chụp 5 khung hình liên tiếp, căn chỉnh và trung bình pixel, sau đó nội suy lên mục tiêu 108MP.",
                enabled = fhdEnabled, onEnabledChange = onFhdEnabledChange,
                status = if (fhdEnabled) "FHD · ĐANG BẬT" else "FHD · TẮT"
            )
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF202020), RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Giới hạn xử lý", color = HdmpYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "211MP và 108MP là số pixel đầu ra do nội suy, không phải chi tiết quang học mới. Kết quả ghép phụ thuộc tốc độ chụp liên tiếp, độ phân giải cảm biến và RAM; nếu thiếu bộ nhớ, ứng dụng giữ ảnh gốc.",
                    color = HdmpMuted, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun HdmpToggleRow(
    title: String, detail: String, enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit, status: String
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = HdmpMuted, fontSize = 11.sp, lineHeight = 15.sp)
            Text(status, color = if (enabled) HdmpYellow else Color(0xFF888888),
                fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled, onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF161616), checkedTrackColor = HdmpYellow,
                uncheckedThumbColor = Color(0xFFBDBDBD), uncheckedTrackColor = Color(0xFF444444),
                checkedBorderColor = HdmpYellow, uncheckedBorderColor = Color(0xFF666666)
            )
        )
    }
}

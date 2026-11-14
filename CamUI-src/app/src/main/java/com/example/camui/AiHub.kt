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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HdmpYellow = Color(0xFFF5C518)
private val HdmpMuted = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HdmpBottomSheet(
    onDismiss: () -> Unit,
    uhdEnabled: Boolean,
    onUhdEnabledChange: (Boolean) -> Unit,
    fhdEnabled: Boolean,
    onFhdEnabledChange: (Boolean) -> Unit,
    uhdPlusEnabled: Boolean,
    onUhdPlusEnabledChange: (Boolean) -> Unit,
    nightShotEnabled: Boolean,
    nightShotDurationSeconds: Int,
    onCycleNightShot: () -> Unit
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
                "HDMP điều phối NAFNet width64 → NAFNetLib width32 dự phòng · model chỉ hỗ trợ khử nhiễu; HDMP kiểm soát ảnh cuối",
                color = HdmpMuted, fontSize = 12.sp
            )
            HdmpToggleRow(
                title = "UHD · 108MP",
                enabled = uhdEnabled, onEnabledChange = onUhdEnabledChange,
                status = if (uhdEnabled) "ĐANG BẬT · 8 TÁC VỤ" else "TẮT"
            )
            HorizontalDivider(color = Color(0xFF333333))
            HdmpToggleRow(
                title = "FHD · 60MP",
                enabled = fhdEnabled, onEnabledChange = onFhdEnabledChange,
                status = if (fhdEnabled) "ĐANG BẬT · 8 TÁC VỤ" else "TẮT"
            )
            HorizontalDivider(color = Color(0xFF333333))
            HdmpToggleRow(
                title = "UHD+ · 250MP",
                enabled = uhdPlusEnabled, onEnabledChange = onUhdPlusEnabledChange,
                status = if (uhdPlusEnabled) "ĐANG BẬT · 8 TÁC VỤ" else "TẮT"
            )
            HorizontalDivider(color = Color(0xFF333333))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF202020)).animaTClickable(onClick = onCycleNightShot).padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Night Shot", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Chạm lần lượt: 5s → 10s → 3s → Tắt. Mục tiêu 30 FPS: 5s/150 khung, 10s/300 khung, 3s/90 khung. Tự thu nhanh nhất camera cho phép rồi mới căn chỉnh và ghép; đầu ra tối đa 60MP.",
                        color = HdmpMuted, fontSize = 11.sp, lineHeight = 15.sp
                    )
                    Text(
                        if (nightShotEnabled) "ĐANG BẬT · ${nightShotDurationSeconds}s" else "NIGHT SHOT · TẮT",
                        color = if (nightShotEnabled) HdmpYellow else Color(0xFF888888),
                        fontSize = 10.sp, fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (nightShotEnabled) "${nightShotDurationSeconds}s" else "OFF",
                    color = if (nightShotEnabled) HdmpYellow else Color(0xFFAAAAAA),
                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF202020), RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Cách phân bổ xử lý", color = HdmpYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "FHD, UHD và UHD+ dùng cùng pipeline phục hồi chi tiết với 8 tác vụ. Night Shot giới hạn 60MP; Photo thường không chạy HDMP.",
                    color = HdmpMuted, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun HdmpToggleRow(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    status: String
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(status, color = if (enabled) HdmpYellow else Color(0xFF888888),
                fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF161616), checkedTrackColor = HdmpYellow,
                uncheckedThumbColor = Color(0xFFBDBDBD), uncheckedTrackColor = Color(0xFF444444),
                checkedBorderColor = HdmpYellow, uncheckedBorderColor = Color(0xFF666666)
            )
        )
    }
}

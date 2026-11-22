package com.example.camui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

private val LunaDreamYellow = Color(0xFFF5C518)
private val LunaDreamMuted = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunaDreamBottomSheet(
    onDismiss: () -> Unit,
    uhdEnabled: Boolean,
    onUhdEnabledChange: (Boolean) -> Unit,
    fhdEnabled: Boolean,
    onFhdEnabledChange: (Boolean) -> Unit,
    uhdPlusEnabled: Boolean,
    onUhdPlusEnabledChange: (Boolean) -> Unit,
    nightShotEnabled: Boolean,
    nightShotDurationSeconds: Int,
    onCycleNightShot: () -> Unit,
    renderProfile: String,
    onRenderProfileChange: (String) -> Unit
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
            Text("LunaDream", color = LunaDreamYellow, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "LunaDream điều phối LunaDream AI 2 và LunaDream AI 2 Fast; LunaDream kiểm tra và hoàn tất ảnh cuối.",
                color = LunaDreamMuted, fontSize = 12.sp
            )
            Text("Chế độ xử lý LunaDream AI", color = LunaDreamYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "quality" to "Chất lượng",
                    "balanced" to "Cân bằng",
                    "speed" to "Tốc độ"
                ).forEach { (value, title) ->
                    val selected = renderProfile == value
                    Box(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) LunaDreamYellow else Color(0xFF292929))
                            .clickable { onRenderProfileChange(value) }
                            .padding(vertical = 11.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            color = if (selected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Text(
                when (renderProfile) {
                    "quality" -> "Ưu tiên LunaDream AI 2; số luồng vẫn tự giới hạn theo CPU, RAM và nhiệt độ."
                    "speed" -> "Bắt đầu trực tiếp bằng LunaDream AI 2 Fast để rút ngắn thời gian xử lý."
                    else -> "Ưu tiên LunaDream AI 2 với mức tải cân bằng; hệ thống tự điều chỉnh theo thiết bị."
                },
                color = LunaDreamMuted, fontSize = 11.sp, lineHeight = 16.sp
            )
            LunaDreamToggleRow(
                title = "UHD · 108MP",
                enabled = uhdEnabled, onEnabledChange = onUhdEnabledChange,
                status = if (uhdEnabled) "ĐANG BẬT · TỰ ĐIỀU CHỈNH" else "TẮT"
            )
            HorizontalDivider(color = Color(0xFF333333))
            LunaDreamToggleRow(
                title = "FHD · 60MP",
                enabled = fhdEnabled, onEnabledChange = onFhdEnabledChange,
                status = if (fhdEnabled) "ĐANG BẬT · TỰ ĐIỀU CHỈNH" else "TẮT"
            )
            HorizontalDivider(color = Color(0xFF333333))
            LunaDreamToggleRow(
                title = "UHD+ · 250MP",
                enabled = uhdPlusEnabled, onEnabledChange = onUhdPlusEnabledChange,
                status = if (uhdPlusEnabled) "ĐANG BẬT · TỰ ĐIỀU CHỈNH" else "TẮT"
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
                        color = LunaDreamMuted, fontSize = 11.sp, lineHeight = 15.sp
                    )
                    Text(
                        if (nightShotEnabled) "ĐANG BẬT · ${nightShotDurationSeconds}s" else "NIGHT SHOT · TẮT",
                        color = if (nightShotEnabled) LunaDreamYellow else Color(0xFF888888),
                        fontSize = 10.sp, fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (nightShotEnabled) "${nightShotDurationSeconds}s" else "OFF",
                    color = if (nightShotEnabled) LunaDreamYellow else Color(0xFFAAAAAA),
                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF202020), RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Cách phân bổ xử lý", color = LunaDreamYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "FHD, UHD và UHD+ dùng pipeline LunaDream với số luồng tự điều chỉnh theo CPU, RAM và trạng thái nhiệt. Night Shot có trần 60MP nhưng tự hạ độ phân giải nếu RAM không đủ.",
                    color = LunaDreamMuted, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun LunaDreamToggleRow(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    status: String
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(status, color = if (enabled) LunaDreamYellow else Color(0xFF888888),
                fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF161616), checkedTrackColor = LunaDreamYellow,
                uncheckedThumbColor = Color(0xFFBDBDBD), uncheckedTrackColor = Color(0xFF444444),
                checkedBorderColor = LunaDreamYellow, uncheckedBorderColor = Color(0xFF666666)
            )
        )
    }
}

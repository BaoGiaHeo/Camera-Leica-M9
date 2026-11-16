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
    nightShotEnabled: Boolean,
    nightShotDurationSeconds: Int,
    onCycleNightShot: () -> Unit,
    renderProfile: String,
    onRenderProfileChange: (String) -> Unit,
    outputMegapixels: Int,
    onOutputMegapixelsChange: (Int) -> Unit
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
                "Chọn HDMP AI 2, HDMP AI 1.5, HDMP LunaDream 2.0 hoặc OFF trước khi chụp. Mỗi chế độ giữ đúng thuật toán đã chọn.",
                color = HdmpMuted, fontSize = 12.sp, lineHeight = 17.sp
            )
            Text("CHẾ ĐỘ XỬ LÝ HDMP", color = HdmpYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "ai2" to "HDMP AI 2",
                    "ai15" to "HDMP AI 1.5",
                    "luna" to "LunaDream 2.0",
                    "off" to "OFF"
                ).forEach { (value, title) ->
                    val selected = renderProfile == value
                    Box(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) HdmpYellow else Color(0xFF292929))
                            .clickable { onRenderProfileChange(value) }
                            .padding(vertical = 13.dp, horizontal = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            color = if (selected) Color.Black else Color.White,
                            fontSize = if (value == "luna") 8.sp else 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                when (HdmpSelectionPolicy.normalizeModel(renderProfile)) {
                    "off" -> "OFF HDMP AI · chụp thường, không chạy mô hình AI và không nội suy HDMP."
                    "ai15" -> "HDMP AI 1.5 · width32 · ưu tiên tốc độ, khử nhiễu và phục hồi chi tiết. Mục tiêu 10 giây."
                    "luna" -> "HDMP LunaDream 2.0 · thuật toán ghép đa khung riêng, không dùng NAFNet; căn chỉnh Lucas–Kanade, khử nhiễu theo chuyển động và phục hồi độ nét. Mục tiêu tối đa 60 giây."
                    else -> "HDMP AI 2 · width64 · ưu tiên chất lượng, khử nhiễu và phục hồi chi tiết. Mục tiêu 20 giây."
                },
                color = HdmpMuted, fontSize = 11.sp, lineHeight = 16.sp
            )
            HorizontalDivider(color = Color(0xFF333333))
            if (HdmpSelectionPolicy.normalizeModel(renderProfile) != "luna") {
                Text("ĐỘ PHÂN GIẢI ĐẦU RA", color = HdmpYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(24, 48, 60, 108, 250).forEach { mp ->
                        val selected = outputMegapixels == mp
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) HdmpYellow else Color(0xFF292929))
                                .clickable { onOutputMegapixelsChange(mp) }
                                .padding(vertical = 12.dp, horizontal = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${mp}MP",
                                color = if (selected) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    "AI 1.5 và AI 2 xử lý ảnh đơn theo độ phân giải đã chọn. Số megapixel thực tế có thể bị giới hạn bởi cảm biến hoặc RAM.",
                    color = HdmpMuted, fontSize = 11.sp, lineHeight = 16.sp
                )
            } else {
                Text(
                    "LunaDream 2.0 ghép tối đa 8 khung hình, giữ tỷ lệ gốc và giới hạn tối đa 60MP; độ phân giải có thể tự giảm khi RAM không đủ.",
                    color = HdmpMuted, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
            HorizontalDivider(color = Color(0xFF333333))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF202020)).animaTClickable(onClick = onCycleNightShot).padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("NightShot", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "NightShot có pipeline ghép ảnh đêm và tăng sáng riêng. LunaDream 2.0 là chế độ ghép đa khung HDMP độc lập.",
                        color = HdmpMuted, fontSize = 11.sp, lineHeight = 15.sp
                    )
                    Text(
                        if (nightShotEnabled) "ĐANG BẬT · ${nightShotDurationSeconds}s" else "NIGHTSHOT · TẮT",
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
            Text(
                "Milky Way được bật/tắt trong Cài đặt → NightShot. Khi HDMP bắt đầu xử lý sau khi đã thu/ghép xong, có thể di chuyển điện thoại tự do.",
                color = HdmpMuted, fontSize = 11.sp, lineHeight = 16.sp
            )
        }
    }
}

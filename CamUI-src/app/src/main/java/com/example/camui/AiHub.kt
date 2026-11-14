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

private val AiHubYellow = Color(0xFFF5C518)
private val AiHubSurface = Color(0xFF202020)
private val AiHubMuted = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubBottomSheet(
    onDismiss: () -> Unit,
    compositionEnabled: Boolean,
    onCompositionChanged: (Boolean) -> Unit,
    directorEnabled: Boolean,
    onDirectorChanged: (Boolean) -> Unit,
    uhdEnabled: Boolean,
    onUhdEnabledChange: (Boolean) -> Unit,
    fhdEnabled: Boolean,
    onFhdEnabledChange: (Boolean) -> Unit,
portraitEnabled: Boolean,
    onPortraitChanged: (Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161616),
        contentColor = Color.White,
        scrimColor = Color(0x99000000),
        tonalElevation = 0.dp,
        dragHandle = {
            HorizontalDivider(
                modifier = Modifier
                    .width(42.dp)
                    .padding(top = 10.dp, bottom = 8.dp),
                thickness = 4.dp,
                color = Color(0xFF666666)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "✨ AI",
                color = AiHubYellow,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Trung tâm điều khiển AI trên thiết bị",
                color = AiHubMuted,
                fontSize = 12.sp
            )
            AiHubToggleRow(
                title = "AI Bố cục & Tạo dáng",
                detail = "Nhận diện khuôn mặt và Pose Detection 33 điểm để gợi ý bố cục, vai, tay và tư thế ngay trên khung ngắm.",
                enabled = compositionEnabled,
                onEnabledChange = onCompositionChanged,
                status = "Face detect"
            )
            HorizontalDivider(color = Color(0xFF333333))
            AiHubToggleRow(
                title = "Trợ lý Đạo diễn AI",
                detail = "Nhận diện cảnh trực tiếp trên thiết bị và gợi ý góc chụp, ánh sáng, khoảng trống chủ thể.",
                enabled = directorEnabled,
                onEnabledChange = onDirectorChanged,
                status = if (directorEnabled) "Scene AI · ĐANG BẬT" else "Scene AI · SẴN SÀNG"
            )
            HorizontalDivider(color = Color(0xFF333333))
            AiHubToggleRow(
                title = "UHD · AI Photo 211MP",
                detail = "Khóa zoom 1x, ghép 5 khung, NAFNet khử nhiễu và Real-ESRGAN x4plus. Độ phân giải đầu ra tự điều chỉnh theo RAM trống nếu cần.",
                enabled = uhdEnabled,
                onEnabledChange = onUhdEnabledChange,
                status = if (uhdEnabled) "UHD AI · ĐANG BẬT" else "UHD AI · SẴN SÀNG"
            )
            HorizontalDivider(color = Color(0xFF333333))
            AiHubToggleRow(
                title = "FHD · AI Photo 108MP",
                detail = "Khóa zoom 1x, ghép 5 khung từ camera (12.5MP/khung nếu cảm biến cung cấp), NAFNet khử nhiễu và Real-ESRGAN x4plus lên mục tiêu 108MP.",
                enabled = fhdEnabled,
                onEnabledChange = onFhdEnabledChange,
                status = if (fhdEnabled) "FHD AI · 108MP · ĐANG BẬT" else "FHD AI · SẴN SÀNG"
            )
            HorizontalDivider(color = Color(0xFF333333))
            AiHubToggleRow(
                title = "Xóa phông AI Chiều sâu",
                detail = "Depth Anything V2 và BRIA RMBG chưa được đóng gói; công tắc chỉ lưu trạng thái thử nghiệm.",
                enabled = portraitEnabled,
                onEnabledChange = onPortraitChanged,
                status = "Chưa tích hợp"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AiHubSurface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Ghi chú bản thử nghiệm",
                    color = AiHubYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "UHD/FHD ghép 5 khung, chạy NAFNet và Real-ESRGAN x4plus; UHD tự hạ mục tiêu nếu RAM trống không đủ. Khi hai chế độ tắt, ảnh 1x có thể nội suy đến 211MP (tăng số pixel, không tăng chi tiết quang học). AI Bố cục/Tạo dáng dùng Pose Detection 33 điểm; AI Director phân tích nhãn cảnh trên thiết bị. Moondream VLM và AI xóa phông chiều sâu chưa tích hợp.",
                    color = AiHubMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun AiHubToggleRow(
    title: String,
    detail: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    status: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                detail,
                color = AiHubMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Text(
                status,
                color = if (enabled) AiHubYellow else Color(0xFF888888),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF161616),
                checkedTrackColor = AiHubYellow,
                uncheckedThumbColor = Color(0xFFBDBDBD),
                uncheckedTrackColor = Color(0xFF444444),
                checkedBorderColor = AiHubYellow,
                uncheckedBorderColor = Color(0xFF666666)
            )
        )
    }
}

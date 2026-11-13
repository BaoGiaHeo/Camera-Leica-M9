package com.example.camui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class SettingItem(val title: String, val subtitle: String)

private val settingItems = listOf(
    SettingItem("Assistive Tools", "Grid Lines · Focus Peaking · Histogram"),
    SettingItem("Focus & Lens", "Autofocus · Manual focus · Lens behavior"),
    SettingItem("Capture & Storage", "Aspect ratio · Photo quality · Save location"),
    SettingItem("Photo Mode Settings", "Photo output · Sharpening · Noise reduction"),
    SettingItem("About", "Fujifilm Mode · app-debug")
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    ratioIdx: Int,
    onRatio: (Int) -> Unit,
    showGrid: Boolean,
    onShowGrid: (Boolean) -> Unit,
    showHist: Boolean,
    onShowHist: (Boolean) -> Unit,
    focusPeaking: Boolean,
    onFocusPeaking: (Boolean) -> Unit
) {
    var selectedPage by remember { mutableStateOf<String?>(null) }
    if (selectedPage != null) {
        SettingsDetailPage(
            title = selectedPage!!,
            onBack = { selectedPage = null },
            ratioIdx = ratioIdx,
            onRatio = onRatio,
            showGrid = showGrid,
            onShowGrid = onShowGrid,
            showHist = showHist,
            onShowHist = onShowHist,
            focusPeaking = focusPeaking,
            onFocusPeaking = onFocusPeaking
        )
        return
    }
    Column(Modifier.fillMaxSize().background(CamColors.Bg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionTitle("Capture") }
            items(settingItems.take(3)) { item -> SettingCard(item) { selectedPage = item.title } }
            item { SectionTitle("Imaging") }
            items(settingItems.drop(3)) { item -> SettingCard(item) { selectedPage = item.title } }
        }
    }
}

@Composable
private fun SettingsDetailPage(
    title: String,
    onBack: () -> Unit,
    ratioIdx: Int,
    onRatio: (Int) -> Unit,
    showGrid: Boolean,
    onShowGrid: (Boolean) -> Unit,
    showHist: Boolean,
    onShowHist: (Boolean) -> Unit,
    focusPeaking: Boolean,
    onFocusPeaking: (Boolean) -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(CamColors.Bg).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
        when (title) {
            "Assistive Tools" -> {
                Text("Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm.",
                    color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                SettingsSwitch("Grid Lines", "Thirds composition guide", showGrid, onShowGrid)
                SettingsSwitch("Histogram", "Live luminance histogram", showHist, onShowHist)
                SettingsSwitch("Focus Peaking", "Highlight high-contrast focus edges", focusPeaking, onFocusPeaking)
            }
            "Focus & Lens" -> {
                Text("Autofocus", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Chạm vào vị trí cần nét trên kính ngắm để CameraX chạy AF + AE tại điểm chạm. Trong chế độ Pro, mục AF mở lấy nét thủ công khi ống kính báo hỗ trợ LENS_INFO_MINIMUM_FOCUS_DISTANCE.",
                    color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
                Text("Giới hạn", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Eye AF và hiệu chỉnh méo ống kính phụ thuộc các chế độ mà camera HAL của thiết bị cung cấp; app không bật giả khi HAL không hỗ trợ.",
                    color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
            "Capture & Storage" -> {
                Text("Aspect Ratio", color = Color.White, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    aspectRatios.forEachIndexed { index, pair ->
                        Chip(pair.first, index == ratioIdx, { onRatio(index) })
                    }
                }
                Text("Ảnh được lưu vào Pictures/FujifilmMode và xuất hiện trong thư viện ảnh của Android. Tỉ lệ được áp dụng cho cả khung xem trước và ảnh JPEG đã cắt.",
                    color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp))
            }
            "Photo Mode Settings" -> {
                Text("Capture Quality", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("CameraX đang dùng CAPTURE_MODE_MAXIMIZE_QUALITY.", color = CamColors.Sub,
                    fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
                Text("Noise Reduction", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Khử nhiễu ảnh tĩnh do ISP/Camera HAL quyết định. App chỉ hiển thị điều khiển khi thiết bị cung cấp API phù hợp; hiện không có công tắc chung an toàn cho mọi máy.",
                    color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
            else -> {
                Text("Fujifilm Mode (app-debug)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("CameraX 1.4 · Camera2 interop · Experimental build", color = CamColors.Sub, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun SettingsSwitch(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(RoundedCornerShape(16.dp))
            .background(CamColors.Card).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = CamColors.Sub, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = CamColors.Sub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 3.dp))
}

@Composable
private fun SettingCard(item: SettingItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CamColors.Card)
            .clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(item.subtitle, color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open",
            tint = CamColors.Sub)
    }
}

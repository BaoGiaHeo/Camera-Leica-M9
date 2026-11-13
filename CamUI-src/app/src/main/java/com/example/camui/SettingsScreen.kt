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
import androidx.compose.runtime.mutableFloatStateOf
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
    SettingItem("Assistive Tools", "Grid Lines · Horizontal Level · Focus Peaking · Histogram"),
    SettingItem("Focus & Lens", "Autofocus · Manual focus · Lens behavior"),
    SettingItem("Capture & Storage", "Aspect ratio · Photo quality · Save location"),
    SettingItem("Photo Mode Settings", "Photo output · Sharpening · Noise reduction"),
    SettingItem("About", "Fujifilm Mode · app-debug")
)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var selectedPage by remember { mutableStateOf<String?>(null) }
    if (selectedPage != null) {
        SettingsDetailPage(title = selectedPage!!, onBack = { selectedPage = null })
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
private fun SettingsDetailPage(title: String, onBack: () -> Unit) {
    var focusAssist by remember { mutableStateOf(true) }
    var hLevel by remember { mutableStateOf(false) }
    var faceAf by remember { mutableStateOf(true) }
    var distortion by remember { mutableStateOf(false) }
    var autoSave by remember { mutableStateOf(true) }
    var highQuality by remember { mutableStateOf(true) }
    var denoise by remember { mutableStateOf(false) }
    var sharpening by remember { mutableFloatStateOf(0.35f) }

    Column(Modifier.fillMaxSize().background(CamColors.Bg).padding(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
        when (title) {
            "Assistive Tools" -> {
                SettingsSwitch("Focus Peaking", "Highlight high-contrast focus edges", focusAssist) { focusAssist = it }
                SettingsSwitch("Horizontal Level", "Show the horizon guide", hLevel) { hLevel = it }
                Text("Grid và Histogram có thể bật nhanh từ Toolbox.", color = CamColors.Sub,
                    fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
            "Focus & Lens" -> {
                SettingsSwitch("Human / Eye AF priority", "Use continuous autofocus when supported", faceAf) { faceAf = it }
                SettingsSwitch("Lens distortion correction", "Camera pipeline dependent", distortion) { distortion = it }
                Text("Chạm vào khung ngắm để lấy nét và đo sáng tại điểm chạm. Điều khiển AF thủ công có trong Pro.",
                    color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            }
            "Capture & Storage" -> {
                SettingsSwitch("Auto save to gallery", "Store captured photos in Pictures/FujifilmMode", autoSave) { autoSave = it }
                Text("Aspect ratio hiện được chọn trong Quick Settings và được áp dụng cho ảnh chụp.",
                    color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            }
            "Photo Mode Settings" -> {
                SettingsSwitch("Highest photo quality", "Use CameraX maximum quality capture mode", highQuality) { highQuality = it }
                SettingsSwitch("Noise reduction preference", "Camera hardware support varies by device", denoise) { denoise = it }
                Text("Sharpening preference", color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                Slider(value = sharpening, onValueChange = { sharpening = it })
                Text("\${(sharpening * 100).toInt()}%", color = CamColors.Sub, fontSize = 12.sp)
            }
            else -> {
                Text("Fujifilm Mode (app-debug)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("CameraX experimental build", color = CamColors.Sub, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Một số tùy chọn chỉ lưu trong phiên hiện tại; chức năng phụ thuộc khả năng Camera2 của thiết bị.",
            color = CamColors.Sub, fontSize = 10.sp, modifier = Modifier.padding(bottom = 8.dp))
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

package com.example.camui

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.graphics.ImageFormat
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class SettingItem(val title: String, val subtitle: String)
private data class CameraLensRow(
    val id: String,
    val facing: String,
    val focal: String,
    val aperture: String,
    val sensor: String,
    val maxOutput: String,
    val ois: String,
    val eis: String,
    val physicalIds: String
)

private val settingItems = listOf(
    SettingItem("Assistive Tools", "Grid Lines · Focus Peaking · Histogram"),
    SettingItem("Focus & Lens", "Camera ID · Stabilization · Sensor information"),
    SettingItem("Capture & Quality", "Aspect ratio · Image quality · Save location"),
    SettingItem("Photo Mode Setting", "HDR · UHD · Noise reduction"),
    SettingItem("Appearance", "Custom shutter button"),
    SettingItem("Language", "System · English · Vietnamese · Japanese · Korean · Chinese"),
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
    AnimatedContent(
        targetState = selectedPage,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            (fadeIn(tween(240)) + scaleIn(
                initialScale = 0.88f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 430f)
            )) togetherWith (fadeOut(tween(140)) + scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(160)
            ))
        },
        label = "Settings page pop transition"
    ) { page ->
        if (page != null) {
            SettingsDetailPage(
                title = page,
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
        } else {
            Column(Modifier.fillMaxSize().background(CamColors.Bg)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("Settings", color = CamColors.TextColor, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
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
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE) }
    var imageQuality by remember { mutableStateOf(prefs.getString("image_quality", "Best") ?: "Best") }
    var noiseReduction by remember { mutableStateOf(prefs.getString("noise_reduction", "Medium") ?: "Medium") }
    var hdr by remember { mutableStateOf(prefs.getBoolean("hdr_coming_soon", false)) }
    var uhd by remember {
        mutableStateOf(prefs.getBoolean("uhd_capture_enabled", prefs.getBoolean("uhd_coming_soon", false)))
    }
    var shutterButtonColor by remember { mutableStateOf(prefs.getString("shutter_button_color", "#FFFFFFFF") ?: "#FFFFFFFF") }
    var shutterButtonImageUri by remember { mutableStateOf(prefs.getString("shutter_button_image_uri", null)) }
    val shutterImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            shutterButtonImageUri = uri.toString()
            prefs.edit().putString("shutter_button_image_uri", uri.toString()).apply()
        }
    }

    Column(
        Modifier.fillMaxSize().background(CamColors.Bg)
            .verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(title, color = CamColors.TextColor, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
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
                Text("Thông tin camera do Camera2 đọc trực tiếp từ thiết bị.",
                    color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                val cameraRows = remember {
                    runCatching {
                        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        manager.cameraIdList.map { id ->
                            val c = manager.getCameraCharacteristics(id)
                            val facing = when (c.get(CameraCharacteristics.LENS_FACING)) {
                                CameraCharacteristics.LENS_FACING_BACK -> "Sau"
                                CameraCharacteristics.LENS_FACING_FRONT -> "Trước"
                                CameraCharacteristics.LENS_FACING_EXTERNAL -> "Ngoài"
                                else -> "Không rõ"
                            }
                            val focal = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                                ?.joinToString(" / ") { "%.1f mm".format(it) } ?: "Không công bố"
                            val aperture = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                                ?.joinToString(" / ") { "f/%.1f".format(it) } ?: "Không công bố"
                            val physical = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                                ?.let { "%.2f × %.2f mm".format(it.width, it.height) } ?: "Không công bố"
                            val size = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                                ?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width.toLong() * it.height }
                                ?.let { "${it.width} × ${it.height}" } ?: "Không công bố"
                            val oisSupported = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                                ?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true
                            val eisSupported = c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                                ?.contains(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON) == true
                            // CameraManager.cameraIdList is rendered one row per available camera above.
                            // The physical-ID characteristics key is not present in this project's compile SDK.
                            val physicalIds = "Xem danh sách Camera ID ở trên"
                            CameraLensRow(
                                id, facing, focal, aperture, physical, size,
                                if (oisSupported) "Có" else "Không công bố",
                                if (eisSupported) "Có" else "Không công bố",
                                physicalIds
                            )
                        }
                    }.getOrDefault(emptyList())
                }
                if (cameraRows.isEmpty()) {
                    Text("Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng.",
                        color = CamColors.Sub, fontSize = 12.sp)
                } else {
                    cameraRows.forEach { row ->
                        InfoCard("Camera ID ${row.id}", "Camera ${row.facing}") {
                            InfoLine("Focal length", row.focal)
                            InfoLine("Khẩu độ", row.aperture)
                            InfoLine("Kích thước cảm biến", row.sensor)
                            InfoLine("JPEG tối đa", row.maxOutput)
                            InfoLine("OIS", row.ois)
                            InfoLine("EIS", row.eis)
                            InfoLine("Physical camera IDs", row.physicalIds)
                        }
                    }
                }
                Text("Thông tin tên mã cảm biến không được Android Camera2 công bố thống nhất trên mọi thiết bị; ứng dụng hiển thị kích thước cảm biến và độ phân giải tối đa mà hệ thống cung cấp.",
                    color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.padding(6.dp))
                InfoCard("Additional lens tools", "Coming soon") {
                    Text("Lens calibration · EIS+ · sensor diagnostics",
                        color = CamColors.Sub, fontSize = 11.sp)
                }
            }
            "Capture & Quality" -> {
                Text("Aspect Ratio", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    aspectRatios.forEachIndexed { index, pair ->
                        Chip(pair.first, index == ratioIdx, { onRatio(index) }, Modifier.width(68.dp))
                    }
                }
                Spacer(Modifier.padding(7.dp))
                Text("Image Quality", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Text("Best sử dụng mức nén JPEG cao nhất; độ phân giải thực tế phụ thuộc độ phân giải mà Camera2/CameraX cung cấp cho camera đang chọn.",
                    color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 9.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Low", "Medium", "High", "Best").forEach { option ->
                        Chip(option, imageQuality == option, {
                            imageQuality = option
                            prefs.edit().putString("image_quality", option).apply()
                        }, Modifier.width(75.dp))
                    }
                }
                Text("Ảnh được lưu vào Pictures/FujifilmMode.",
                    color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp))
            }
            "Appearance" -> {
                Text("Shutter button", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Text("Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút.",
                    color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 9.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(
                        "White" to "#FFFFFFFF",
                        "Yellow" to "#FFF5C842",
                        "Blue" to "#FF58A6FF",
                        "Black" to "#FF202020"
                    ).forEach { (label, hex) ->
                        Chip(label, shutterButtonColor == hex && shutterButtonImageUri == null, {
                            shutterButtonColor = hex
                            shutterButtonImageUri = null
                            prefs.edit().putString("shutter_button_color", hex).remove("shutter_button_image_uri").apply()
                            CamColors.Shutter = Color(android.graphics.Color.parseColor(hex))
                        }, Modifier.width(78.dp))
                    }
                }
                Button(onClick = { shutterImagePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text(if (shutterButtonImageUri == null) "Import ảnh cho nút chụp" else "Đổi ảnh nút chụp")
                }
                if (shutterButtonImageUri != null) {
                    Text("Đã chọn ảnh cho nút chụp.",
                        color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                    Text("Dùng nút màu", color = CamColors.Yellow, fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            shutterButtonImageUri = null
                            prefs.edit().remove("shutter_button_image_uri").apply()
                        }.padding(vertical = 7.dp))
                }
            }
            "Language" -> {
                var selectedLanguage by remember {
                    mutableStateOf(prefs.getString("app_language", "system") ?: "system")
                }
                Text("Language", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Text("Chọn ngôn ngữ hiển thị. Ứng dụng sẽ tải lại giao diện sau khi đổi.",
                    color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 10.dp))
                val languages = listOf(
                    Triple("system", "Phát hiện ngôn ngữ hệ thống", ""),
                    Triple("en", "English", "en"),
                    Triple("vi", "Vietnamese", "vi"),
                    Triple("ja", "Japan", "ja"),
                    Triple("ko", "Korea", "ko"),
                    Triple("zh-CN", "China", "zh-CN")
                )
                languages.forEach { (id, label, tag) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (selectedLanguage == id) Color(0x553F3822) else CamColors.Card)
                            .border(1.dp, if (selectedLanguage == id) CamColors.Yellow else CamColors.Stroke,
                                RoundedCornerShape(13.dp))
                            .clickable {
                                selectedLanguage = id
                                prefs.edit().putString("app_language", id).apply()
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = if (selectedLanguage == id) CamColors.Yellow else CamColors.TextColor,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (selectedLanguage == id) {
                            Text("✓", color = CamColors.Yellow, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("Japan = 日本語 · Korea = 한국어 · China = 简体中文",
                    color = CamColors.Sub, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
            }
            "Photo Mode Setting" -> {
                Text("Capture Quality", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                SettingsSwitch("HDR", "Coming soon · backend chưa được tích hợp", hdr) {
                    hdr = it
                    prefs.edit().putBoolean("hdr_coming_soon", it).apply()
                }
                SettingsSwitch(
                    "UHD · Độ phân giải tối đa",
                    "Yêu cầu CameraX chọn kích thước JPEG lớn nhất mà camera đang dùng hỗ trợ; không nội suy vượt cảm biến.",
                    uhd
                ) {
                    uhd = it
                    prefs.edit()
                        .putBoolean("uhd_capture_enabled", it)
                        .putBoolean("uhd_coming_soon", false)
                        .apply()
                }
                Spacer(Modifier.padding(5.dp))
                Text("Noise Reduction", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Text("Tắt sẽ yêu cầu camera không khử nhiễu; mức hỗ trợ thực tế tùy Camera2 HAL.",
                    color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 9.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Off", "Low", "Medium", "High").forEach { option ->
                        Chip(option, noiseReduction == option, {
                            noiseReduction = option
                            prefs.edit().putString("noise_reduction", option).apply()
                        }, Modifier.width(75.dp))
                    }
                }
            }
            else -> {
                Text("Fujifilm Mode (app-debug)", color = CamColors.TextColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Người làm App: Baoaka06", color = CamColors.TextColor, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 18.dp))
                Text("TikTok: @baoaka06", color = CamColors.Yellow, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp).clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@baoaka06")))
                        }
                    })
                Text("Version 1.1 experimental · Debug build", color = CamColors.Sub, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 18.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(RoundedCornerShape(15.dp))
            .background(CamColors.Card).padding(14.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
        content()
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SettingsSwitch(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(16.dp))
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

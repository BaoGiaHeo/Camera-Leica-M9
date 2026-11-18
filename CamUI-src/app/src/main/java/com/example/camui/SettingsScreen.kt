package com.example.camui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SettingItem(val icon: String, val title: String, val subtitle: String)
private val settingItems = listOf(
    SettingItem("⊞", "Assistive Tools", "Grid Lines · Focus Peaking · Histogram"),
    SettingItem("◎", "Focus & Lens", "Camera ID · Lens · Sensor information"),
    SettingItem("▣", "Capture & Quality", "Aspect ratio · Image quality · Save location"),
    SettingItem("◐", "Photo Mode Setting", "HDR · Noise reduction · RAW"),
    SettingItem("☾", "NightShotMode", "NightShotMode · Long Exposure / Star Trails"),
    SettingItem("◩", "Display / Appearance", "Shutter button · LCD texture · Simple mode · Left-hand layout"),
    SettingItem("♫", "Sound & Haptic", "Shutter sound · Dial clicks · Haptic feedback"),
    SettingItem("文", "Language", "System · English · Vietnamese · Japanese · Korean · Chinese"),
    SettingItem("ⓘ", "About", "LunaDream 2.5.0dream")
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
    var selectedCategory by remember { mutableStateOf("Assistive Tools") }
    val selectedItem = settingItems.firstOrNull { it.title == selectedCategory } ?: settingItems.first()

    AnimatedContent(
        targetState = selectedPage,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { AnimaT.enter() togetherWith AnimaT.exit() },
        label = "LunaDream settings menu transition"
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
            FujiPanel(
                title = "SETTINGS · MENU",
                modifier = Modifier.fillMaxSize(),
                onBack = onBack
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        Modifier.width(142.dp).fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "MENU TABS",
                            color = FujiSkin.lcdGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        settingItems.forEachIndexed { index, item ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (selectedCategory == item.title) Color(0xFF20292B) else Color(0xFF0A0D0E))
                                    .border(
                                        1.dp,
                                        if (selectedCategory == item.title) FujiSkin.orange else Color(0xFF283538),
                                        RoundedCornerShape(2.dp)
                                    )
                                    .animaTClickable { selectedCategory = item.title }
                                    .padding(horizontal = 6.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    item.icon,
                                    color = if (selectedCategory == item.title) FujiSkin.orange else FujiSkin.lcdGreen,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(23.dp)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "%02d  %s".format(index + 1, item.title.uppercase()),
                                        color = if (selectedCategory == item.title) Color.White else Color(0xFFC7D0D1),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Box(
                                        Modifier.padding(top = 3.dp)
                                            .width(if (selectedCategory == item.title) 24.dp else 8.dp)
                                            .height(2.dp)
                                            .background(if (selectedCategory == item.title) FujiSkin.orange else Color(0xFF526064))
                                    )
                                }
                            }
                        }
                    }
                    Column(
                        Modifier.weight(1f).fillMaxHeight()
                            .background(Color(0xFF080B0C))
                            .border(1.dp, Color(0xFF2C3A3D), RoundedCornerShape(2.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            selectedItem.title.uppercase(),
                            color = FujiSkin.lcdGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            selectedItem.subtitle,
                            color = FujiSkin.muted,
                            fontSize = 10.sp
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF354247)))
                        FujiListRow(
                            title = selectedItem.title,
                            value = "OPEN  >",
                            onClick = { selectedPage = selectedItem.title },
                            description = "MENU/OK · Mở nhóm cài đặt đang chọn"
                        )
                        Spacer(Modifier.weight(1f))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("LUNADREAM 2.5.0DREAM", color = FujiSkin.muted, fontSize = 8.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("DISP/BACK", color = FujiSkin.lcdGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val prefs = remember { context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE) }
    var imageQuality by remember { mutableStateOf(prefs.getString("image_quality", "Best") ?: "Best") }
    var noiseReduction by remember { mutableStateOf(prefs.getString("noise_reduction", "Medium") ?: "Medium") }
    var rawCaptureMode by remember {
        mutableStateOf(
            when (prefs.getString("raw_capture_mode", "jpeg") ?: "jpeg") {
                "raw_plus", "raw_dream" -> "raw_dream"
                "raw" -> "raw"
                else -> "jpeg"
            }
        )
    }
    var hdr by remember { mutableStateOf(prefs.getBoolean("hdr_enabled", prefs.getBoolean("hdr_coming_soon", false))) }
var nightShotMode by remember {
        mutableStateOf(prefs.getString("nightshot_mode", "off") ?: "off")
    }
    var nightShotDurationSeconds by remember {
        mutableStateOf(prefs.getInt("nightshot_duration_seconds", 5).let { if (it in listOf(5, 10, 3)) it else 5 })
    }
    var starTrailExposureSeconds by remember {
        mutableStateOf(prefs.getInt("star_trails_exposure_seconds", 4).coerceIn(1, 8))
    }
    var milkyWayNightShotEnabled by remember {
        mutableStateOf(prefs.getBoolean("milky_way_stack_enabled", false))
    }
    var lcdTextureEnabled by remember { mutableStateOf(prefs.getBoolean("lcd_texture_overlay_enabled", false)) }
    var simpleUiMode by remember { mutableStateOf(prefs.getBoolean("simple_ui_mode_enabled", false)) }
    var leftHandedLayout by remember { mutableStateOf(prefs.getBoolean("left_handed_layout_enabled", false)) }
    var shutterButtonColor by remember { mutableStateOf(prefs.getString("shutter_button_color", "#FFD9DBDD") ?: "#FFD9DBDD") }
    var shutterButtonImageUri by remember { mutableStateOf(prefs.getString("shutter_button_image_uri", null)) }
    var shutterSoundEnabled by remember { mutableStateOf(prefs.getBoolean("shutter_sound_enabled", true)) }
    var hapticFeedbackEnabled by remember { mutableStateOf(prefs.getBoolean("haptic_feedback_enabled", true)) }
    var dialClickEnabled by remember { mutableStateOf(prefs.getBoolean("dial_click_enabled", true)) }
    var interfaceSoundEnabled by remember { mutableStateOf(prefs.getBoolean("ui_sound_enabled", true)) }
    val shutterImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            shutterButtonImageUri = uri.toString()
            prefs.edit().putString("shutter_button_image_uri", uri.toString()).apply()
        }
    }

    FujiPanel(
        title = title,
        modifier = Modifier.fillMaxSize(),
        onBack = onBack
    ) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isLandscape) 22.dp else 12.dp, vertical = 10.dp)
        ) {
        when (title) {
            "Assistive Tools" -> {
                Text("Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm.",
                    color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                SettingsSwitch("Grid Lines", "Thirds composition guide", showGrid, onShowGrid)
                SettingsSwitch("Histogram", "Live luminance histogram", showHist, onShowHist)
                SettingsSwitch("Focus Peaking", "Highlight high-contrast focus edges", focusPeaking, onFocusPeaking)
            }
            "Focus & Lens" -> {
                CameraHardwareScannerPanel()
            }
            "Capture & Quality" -> {
                Text("Aspect Ratio", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    aspectRatios.forEachIndexed { index, pair ->
                        FujiChip(aspectRatioDisplayLabel(index, isLandscape), index == ratioIdx, { onRatio(index) }, Modifier.width(68.dp))
                    }
                }
                Spacer(Modifier.padding(7.dp))
                Text("Image Quality", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Text("Best sử dụng mức nén JPEG cao nhất; độ phân giải thực tế phụ thuộc độ phân giải mà Camera2/CameraX cung cấp cho camera đang chọn.",
                    color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 9.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Low", "Medium", "High", "Best").forEach { option ->
                        FujiChip(option, imageQuality == option, {
                            imageQuality = option
                            prefs.edit().putString("image_quality", option).apply()
                        }, Modifier.width(75.dp))
                    }
                }
                Text("Ảnh được lưu vào Pictures/FujifilmMode.",
                    color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp))
            }
            "Display / Appearance", "Appearance" -> {
                SettingsSwitch(
                    "LCD texture",
                    "Scanline overlay chỉ hiển thị trên preview, không lưu vào ảnh.",
                    lcdTextureEnabled
                ) {
                    lcdTextureEnabled = it
                    prefs.edit().putBoolean("lcd_texture_overlay_enabled", it).apply()
                }
                SettingsSwitch(
                    "Simple mode",
                    "Giảm texture và hiệu ứng để tiết kiệm GPU trên máy yếu.",
                    simpleUiMode
                ) {
                    simpleUiMode = it
                    prefs.edit().putBoolean("simple_ui_mode_enabled", it).apply()
                }
                SettingsSwitch(
                    "Left-hand layout",
                    "Đảo vị trí cụm chế độ và cụm nút/dial.",
                    leftHandedLayout
                ) {
                    leftHandedLayout = it
                    prefs.edit().putBoolean("left_handed_layout_enabled", it).apply()
                }
                Text("Custom shutter button", color = FujiSkin.lcdGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút.",
                    color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 9.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(
                        "Silver Classic" to "#FFD9DBDD",
                        "Black" to "#FF17191A",
                        "Chrome · Orange" to "#FFE8442B",
                        "Soft Release" to "#FFAEB7BA"
                    ).forEach { (label, hex) ->
                        FujiChip(
                            label,
                            shutterButtonColor == hex && shutterButtonImageUri == null,
                            {
                                shutterButtonColor = hex
                                shutterButtonImageUri = null
                                prefs.edit().putString("shutter_button_color", hex).remove("shutter_button_image_uri").apply()
                                CamColors.Shutter = Color(android.graphics.Color.parseColor(hex))
                            },
                            Modifier
                        )
                    }
                }
                FujiListRow(
                    title = if (shutterButtonImageUri == null) "Import ảnh cho nút chụp" else "Đổi ảnh nút chụp",
                    value = "PNG / JPG",
                    onClick = { shutterImagePicker.launch(arrayOf("image/*")) }
                )
                if (shutterButtonImageUri != null) {
                    Text("Đã chọn ảnh cho nút chụp.",
                        color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                    Text("Dùng nút màu", color = FujiSkin.orange, fontSize = 12.sp,
                        modifier = Modifier.animaTClickable {
                            shutterButtonImageUri = null
                            prefs.edit().remove("shutter_button_image_uri").apply()
                        }.padding(vertical = 7.dp))
                }
            }
            "Sound & Haptic" -> {
                Text(
                    "MECHANICAL FEEDBACK",
                    color = FujiSkin.lcdGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                SettingsSwitch(
                    "Shutter sound",
                    "Âm thanh màn trập của ảnh chụp.",
                    shutterSoundEnabled
                ) {
                    shutterSoundEnabled = it
                    prefs.edit().putBoolean("shutter_sound_enabled", it).apply()
                }
                SettingsSwitch(
                    "Dial detent clicks",
                    "Click nhẹ theo từng nấc của vòng zoom và command dial.",
                    dialClickEnabled
                ) {
                    dialClickEnabled = it
                    prefs.edit().putBoolean("dial_click_enabled", it).apply()
                }
                SettingsSwitch(
                    "UI mechanical sounds",
                    "Tick phím, clack công tắc và tiếng xác nhận panel.",
                    interfaceSoundEnabled
                ) {
                    interfaceSoundEnabled = it
                    prefs.edit().putBoolean("ui_sound_enabled", it).apply()
                }
                SettingsSwitch(
                    "Haptic feedback",
                    "Rung nhẹ khi xoay núm, gạt công tắc và nhấn nửa cò.",
                    hapticFeedbackEnabled
                ) {
                    hapticFeedbackEnabled = it
                    prefs.edit().putBoolean("haptic_feedback_enabled", it).apply()
                }
                Text(
                    "Âm thanh tùy chỉnh sẽ chỉ xuất hiện khi asset tương ứng đã được đóng gói trong ứng dụng.",
                    color = FujiSkin.muted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
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
                            .border(1.dp, if (selectedLanguage == id) FujiSkin.orange else CamColors.Stroke,
                                RoundedCornerShape(13.dp))
                            .animaTClickable {
                                selectedLanguage = id
                                prefs.edit().putString("app_language", id).apply()
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = if (selectedLanguage == id) FujiSkin.orange else CamColors.TextColor,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (selectedLanguage == id) {
                            Text("✓", color = FujiSkin.orange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("Japan = 日本語 · Korea = 한국어 · China = 简体中文",
                    color = CamColors.Sub, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
            }
            "Sao lưu & Khôi phục" -> {
                BackupRestorePanel(onRestoreComplete = {
                    onRatio(prefs.getInt("capture_ratio_index", 1).coerceIn(0, 4))
                    onShowGrid(prefs.getBoolean("viewfinder_grid_enabled", true))
                    onShowHist(prefs.getBoolean("viewfinder_histogram_enabled", true))
                    onFocusPeaking(prefs.getBoolean("viewfinder_focus_peaking_enabled", false))
                    val restoredLanguage = prefs.getString("app_language", "system") ?: "system"
                    val languageTag = if (restoredLanguage == "system") "" else restoredLanguage
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
                })
            }
            "NightShotMode" -> {
                Text(
                    "Chế độ chụp đêm",
                    color = CamColors.TextColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "NightShotMode thu nhiều khung trong thời lượng đã chọn, căn chỉnh và ghép để giảm nhiễu, tăng sáng có kiểm soát. Số khung thực tế phụ thuộc tốc độ camera; giữ máy ổn định đến khi ghép xong.",
                    color = CamColors.Sub,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp, bottom = 12.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Tắt" to "off",
                        "NightShotMode · Multi-frame" to "night",
                        "Phơi sáng lâu · Star Trails" to "star_trails"
                    ).forEach { (label, value) ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(CamColors.Card).animaTClickable {
                                    nightShotMode = value
                                    prefs.edit().putString("nightshot_mode", value).apply()
                                }.padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    label,
                                    color = if (nightShotMode == value) FujiSkin.orange else Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    when (value) {
                                        "night" -> "AE bình thường · EV 0 · căn khung và gộp nhiều ảnh"
                                        "star_trails" -> "Phơi sáng 1–8 giây mỗi khung trên máy hỗ trợ; nhấn chụp lần nữa để dừng rồi ghép vệt sao"
                                        else -> "Chụp ảnh bình thường"
                                    },
                                    color = CamColors.Sub,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                            Text(
                                if (nightShotMode == value) "✓" else "○",
                                color = if (nightShotMode == value) FujiSkin.orange else CamColors.Sub,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (nightShotMode == "night") {
                    Spacer(Modifier.padding(5.dp))
                    Text("Thời lượng thu khung", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Camera liên tục chụp trong khoảng thời gian đã chọn; số khung thực tế tùy tốc độ cảm biến và giới hạn CameraX.",
                        color = CamColors.Sub,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf(5, 10, 3).forEach { seconds ->
                            FujiChip(
                                "$seconds giây",
                                nightShotDurationSeconds == seconds,
                                {
                                    nightShotDurationSeconds = seconds
                                    prefs.edit().putInt("nightshot_duration_seconds", seconds).apply()
                                },
                                Modifier.width(82.dp)
                            )
                        }
                    }
                }
                if (nightShotMode == "night") {
                    Spacer(Modifier.padding(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(FujiSkin.lcd).border(1.dp, Color(0xFF344247), RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Milky Way", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Bật để dùng nhánh căn chỉnh và ghép khung bầu trời trong NightShotMode. Tắt mặc định; không hoạt động ở Photo thường.",
                                color = CamColors.Sub, fontSize = 10.sp,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        FujiSwitch(
                            checked = milkyWayNightShotEnabled,
                            onCheckedChange = {
                                milkyWayNightShotEnabled = it
                                prefs.edit().putBoolean("milky_way_stack_enabled", it).apply()
                            }
                        )
                    }
                }
                if (nightShotMode == "star_trails") {
                    Spacer(Modifier.padding(5.dp))
                    Text("Thời gian mỗi khung", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Mỗi khung dùng màn trập 1, 2, 4 hoặc 8 giây, ISO 800 và lấy nét vô cực khi camera hỗ trợ MANUAL_SENSOR. Máy không hỗ trợ sẽ báo rõ và dùng AE tự động; không giả lập thời gian phơi sáng.",
                        color = CamColors.Sub,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf(1, 2, 4, 8).forEach { seconds ->
                            FujiChip(
                                "$seconds giây",
                                starTrailExposureSeconds == seconds,
                                {
                                    starTrailExposureSeconds = seconds
                                    prefs.edit().putInt("star_trails_exposure_seconds", seconds).apply()
                                },
                                Modifier.width(82.dp)
                            )
                        }
                    }
                    Text(
                        "Dùng tripod, tắt flash và hướng máy vào bầu trời. Nhấn nút chụp lần nữa để dừng sau khung hiện tại; ứng dụng sẽ ghép các vị trí sao sáng thành vệt dài. Chỉ tự dừng sớm khi bộ nhớ tạm gần đầy.",
                        color = CamColors.Sub,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
            "Photo Mode Setting" -> {
                Text("Capture Quality", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                SettingsSwitch(
                    "HDR Tone Mapping",
                    "Engine HDR tone mapping độc lập, tự điều chỉnh mức xử lý theo dữ liệu sáng/tối và tài nguyên thiết bị. HDR không thay đổi độ phân giải đầu ra của LunaDream.",
                    hdr
                ) {
                    hdr = it
                    prefs.edit()
                        .putBoolean("hdr_enabled", it)
                        .putBoolean("hdr_coming_soon", false)
                        .apply()
                }
                Spacer(Modifier.padding(5.dp))
                Text("RAW Capture", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Text(
                    "RAW lưu DNG gốc theo dữ liệu cảm biến. RawDream 2.0 chụp đồng thời DNG và JPEG; JPEG được căn chỉnh, ghép đa khung, khử nhiễu và phục hồi chi tiết bằng LunaDream 2.0, đầu ra mục tiêu 108MP. RawDream 2.0 không kết hợp NightShotMode, Star Trails, Burst hoặc Bracketing. Nếu camera không hỗ trợ đồng thời RAW + JPEG, ứng dụng báo lỗi và không tự chuyển sang JPEG thường.",
                    color = CamColors.Sub, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp, bottom = 9.dp)
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf("JPEG" to "jpeg", "RAW" to "raw", "RawDream 2.0" to "raw_dream").forEach { (label, value) ->
                        FujiChip(label, rawCaptureMode == value, {
                            rawCaptureMode = value
                            prefs.edit().putString("raw_capture_mode", value).apply()
                        }, Modifier.width(92.dp))
                    }
                }
                Spacer(Modifier.padding(5.dp))
                Text("Noise Reduction", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Text(
                    "Off tắt khử nhiễu. Low / Medium / High chỉ dùng thuật toán khử nhiễu có sẵn của camera (Camera2); không có xử lý AI sau chụp. Ultra HDR giữ nguyên dữ liệu JPEG_R.",
                    color = CamColors.Sub, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp, bottom = 9.dp)
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Off", "Low", "Medium", "High").forEach { option ->
                        FujiChip(option, noiseReduction == option, {
                            noiseReduction = option
                            prefs.edit().putString("noise_reduction", option).apply()
                        }, Modifier.width(75.dp))
                    }
                }
            }
            else -> {
                Text("LunaDream Camera", color = CamColors.TextColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Người làm App: Baoaka06", color = CamColors.TextColor, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 18.dp))
                Text("TikTok: @baoaka06", color = FujiSkin.orange, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp).animaTClickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@baoaka06")))
                        }
                    })
                Text("Version 2.5.0dream · Build 15", color = CamColors.Sub, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 18.dp))
            }
        }
    }
}
}


@Composable
private fun CameraHardwareScannerPanel() {
    val context = LocalContext.current
    var refreshToken by remember { mutableStateOf(0) }
    var report by remember { mutableStateOf<CameraHardwareReport?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(refreshToken) {
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching { CameraHardwareScanner.scan(context) }
        }
        report = result.getOrNull()
        errorMessage = result.exceptionOrNull()?.message
        loading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Quét Camera2 công khai, Physical Camera và vendor metadata được HAL công bố.",
            color = CamColors.Sub,
            fontSize = 12.sp
        )
        FujiChip(
            if (loading) "ĐANG QUÉT CAMERA…" else "QUÉT LẠI PHẦN CỨNG CAMERA",
            false,
            { if (!loading) refreshToken++ },
            Modifier.fillMaxWidth()
        )

        if (loading && report == null) {
            Text("Đang đọc CameraCharacteristics…", color = CamColors.Sub, fontSize = 12.sp)
        }

        errorMessage?.let {
            InfoCard("Không thể hoàn tất quét", "Camera2 access") {
                Text(it, color = CamColors.Sub, fontSize = 11.sp)
            }
        }

        report?.let { scan ->
            InfoCard(
                "Camera Hardware Scanner",
                "${scan.manufacturer} ${scan.model} · Android API ${scan.androidSdk}"
            ) {
                InfoLine("Camera ID công khai", scan.publicIdCount.toString())
                InfoLine("Physical ID được công bố", scan.physicalIdCount.toString())
                InfoLine("Tổng mục đã đọc", scan.entries.size.toString())
                InfoLine("Vendor metadata keys", scan.vendorMetadataKeys.size.toString())
                InfoLine("Nhóm camera chạy đồng thời", scan.concurrentCameraSetCount.toString())
                InfoLine("ID không đọc được", scan.unreadableIds.size.toString())
            }

            if (scan.entries.isEmpty()) {
                InfoCard("Chưa tìm thấy camera", "Camera2") {
                    Text(
                        "Không có Camera ID nào được hệ thống công khai cho ứng dụng. Hãy kiểm tra quyền Camera và thử quét lại.",
                        color = CamColors.Sub,
                        fontSize = 11.sp
                    )
                }
            } else {
                scan.entries.forEach { camera ->
                    InfoCard(
                        "Camera ID ${camera.id}",
                        "${camera.lensRole} · ${camera.source}"
                    ) {
                        InfoLine("Hướng", camera.facing)
                        InfoLine("Tiêu cự", camera.focalLengths)
                        InfoLine(
                            "Tương đương 35mm",
                            camera.equivalent35mm?.let {
                                String.format(java.util.Locale.US, "%.1f mm", it)
                            } ?: "Không công bố"
                        )
                        InfoLine(
                            "Lấy nét gần nhất",
                            camera.minimumFocusDistanceDiopter?.takeIf { it > 0f }?.let {
                                String.format(java.util.Locale.US, "%.1f D · ~%.1f cm", it, 100f / it)
                            } ?: "Không công bố / không manual focus"
                        )
                        InfoLine("Sensor pixel array", camera.sensorPixels)
                        InfoLine("Kích thước cảm biến", camera.sensorSizeMm)
                        InfoLine("JPEG lớn nhất", camera.jpegMax)
                        InfoLine("RAW_SENSOR lớn nhất", camera.rawMax)
                        InfoLine("Hardware level", camera.hardwareLevel)
                        InfoLine(
                            "Zoom số tối đa",
                            camera.maxDigitalZoom?.let {
                                String.format(java.util.Locale.US, "%.1fx", it)
                            } ?: "Không công bố"
                        )
                        if (camera.parentId != null) {
                            InfoLine("Logical parent", camera.parentId)
                        }
                        if (camera.physicalIds.isNotEmpty()) {
                            InfoLine("Physical children", camera.physicalIds.joinToString())
                        }
                        if (camera.vendorMetadataKeys.isNotEmpty()) {
                            Text(
                                "Vendor keys: " + camera.vendorMetadataKeys.take(8).joinToString(),
                                color = CamColors.Sub,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                    }
                }
            }

            InfoCard(
                "Vendor metadata mở rộng",
                "Key riêng của OEM được HAL công bố trên camera truy cập được"
            ) {
                if (scan.vendorMetadataKeys.isEmpty()) {
                    Text(
                        "Không có vendor key ngoài namespace Android được công bố trong metadata đọc được.",
                        color = CamColors.Sub,
                        fontSize = 11.sp
                    )
                } else {
                    scan.vendorMetadataKeys.take(30).forEach {
                        Text(it, color = CamColors.TextColor, fontSize = 10.sp)
                    }
                    if (scan.vendorMetadataKeys.size > 30) {
                        Text(
                            "Còn ${scan.vendorMetadataKeys.size - 30} key khác.",
                            color = CamColors.Sub,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            InfoCard(
                "Camera ID OEM không công khai",
                "Giới hạn truy cập của Android"
            ) {
                Text(
                    "Scanner đã kiểm tra Logical Camera, các Physical ID mà OEM đưa vào metadata và vendor metadata có thể đọc được. Camera ID bị camera service/firmware ẩn hoàn toàn không thể được liệt kê bằng API Android công khai; muốn truy cập cần OEM/system API hoặc quyền hệ thống. App không tạo ID giả và không dùng root/hidden-API reflection để vượt giới hạn.",
                    color = CamColors.Sub,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(RoundedCornerShape(4.dp))
            .background(FujiSkin.lcd).border(1.dp, Color(0xFF344247), RoundedCornerShape(4.dp)).padding(12.dp)
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
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(4.dp))
            .background(FujiSkin.lcd).border(1.dp, Color(0xFF344247), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = CamColors.Sub, fontSize = 11.sp)
        }
        FujiSwitch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 3.dp))
}

@Composable
private fun SettingCard(item: SettingItem, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            .background(FujiSkin.lcd).border(1.dp, Color(0xFF344247), RoundedCornerShape(4.dp))
            .animaTClickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(item.title.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            Text(">", color = FujiSkin.lcdGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(item.subtitle, color = FujiSkin.muted, fontSize = 8.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

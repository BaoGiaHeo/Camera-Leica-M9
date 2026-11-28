package com.example.camui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SettingItem(
    val icon: String,
    val title: String,
    val subtitle: String,
    val titleResource: Int
)
private val settingItems = listOf(
    SettingItem("tab_01", "Công cụ hỗ trợ", "Lưới · Nét đỉnh · Biểu đồ", R.string.settings_tab_assistive_tools),
    SettingItem("tab_02", "Lấy nét & Ống kính", "Camera · Ống kính · Cảm biến", R.string.settings_tab_focus_lens),
    SettingItem("tab_03", "Chụp & Chất lượng", "Tỉ lệ · Chất lượng ảnh · Watermark", R.string.settings_tab_capture_quality),
    SettingItem("tab_04", "Setup", "Flash · Hẹn giờ · HDR · RAW · Preview FPS", R.string.settings_tab_photo_mode),
    SettingItem("tab_05", "Chụp đêm", "Night Shot · Phơi sáng dài · Star Trails", R.string.settings_tab_nightshot),
    SettingItem("tab_06", "Hiển thị / Giao diện", "Zoom · LCD · Nút chụp · Bố cục", R.string.settings_tab_display),
    SettingItem("tab_07", "Âm thanh & Rung", "Âm màn trập · Âm giao diện · Phản hồi rung", R.string.settings_tab_sound),
    SettingItem("tab_09", "Giới thiệu", "FujiFilm Mode · thông tin phiên bản", R.string.settings_tab_about),
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit, ratioIdx: Int, onRatio: (Int) -> Unit,
    showGrid: Boolean, onShowGrid: (Boolean) -> Unit,
    showHist: Boolean, onShowHist: (Boolean) -> Unit,
    focusPeaking: Boolean, onFocusPeaking: (Boolean) -> Unit,
    onOpenToolbox: () -> Unit = {},
    onOpenContentsManagement: () -> Unit = {},
    onReplayActivation: () -> Unit = {},
    onOpenWatermarkPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCaptureDiagnostics by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Chụp & Chất lượng") }
    var previousCategory by remember { mutableStateOf("Chụp & Chất lượng") }
    var categoryBeforeBackup by remember { mutableStateOf("Chụp & Chất lượng") }
    val displayVersionName = BuildConfig.VERSION_NAME
        .replace(Regex("(?i)dream.*$"), "")
        .trim()
    val localizedSettingItems = settingItems.map { item ->
        if (item.icon == "tab_09") {
            item.copy(
                subtitle = stringResource(
                    R.string.fujifilm_mode_version,
                    displayVersionName
                )
            )
        } else {
            item
        }
    }
    val visibleSettingItems = localizedSettingItems
    val activeIndex = visibleSettingItems.indexOfFirst { it.title == selectedCategory }.coerceAtLeast(0) + 1
    fun displayTabLabel(item: SettingItem): String = when (item.icon) {
        "tab_01" -> "CÔNG CỤ"
        "tab_02" -> "AF/MF"
        "tab_03" -> "I.Q."
        "tab_04" -> "FLASH"
        "tab_05" -> "ĐÊM"
        "tab_06" -> "HUD"
        "tab_07" -> "ÂM"
        "tab_09" -> "MY"
        else -> item.title.uppercase()
    }
    FujiScreenScaffold(
        title = stringResource(R.string.settings_screen_title),
        modifier = modifier.fillMaxSize(),
        onBack = {
            if (selectedCategory == "Sao lưu & Khôi phục") selectedCategory = categoryBeforeBackup
            else onBack()
        },
        showVersion = false,
        showHeader = false,
        footerActions = true,
        onFooterBack = {
            selectedCategory = if (selectedCategory == "Sao lưu & Khôi phục") {
                categoryBeforeBackup
            } else {
                previousCategory
            }
        }
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().height(30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FujiIconGlyph("camera", Modifier.size(22.dp), Color.White)
                Text(
                    "CÀI ĐẶT CHỤP",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                    modifier = Modifier.padding(start = 7.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    activeIndex.toString(),
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "/${visibleSettingItems.size}",
                    color = Color(0xFFB8BEC0),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(start = 1.dp, end = 2.dp)
                )
            }
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Column(
                    Modifier.width(82.dp).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF101416))
                        .border(1.dp, Color(0xFF3E484B), RoundedCornerShape(6.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp, horizontal = 3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        "MENU",
                        color = FujiSkin.lcdGreen,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 3.dp)
                    )
                    visibleSettingItems.forEach { item ->
                        val selected = selectedCategory == item.title
                        Row(
                            Modifier.fillMaxWidth().height(34.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (selected) Color(0xFFD0D0D0) else Color.Transparent)
                                .border(
                                    if (selected) 1.dp else 0.dp,
                                    if (selected) Color(0xFFF2F2F2) else Color.Transparent,
                                    RoundedCornerShape(5.dp)
                                )
                                .clickable {
                                    if (selectedCategory != item.title) {
                                        previousCategory = selectedCategory
                                        selectedCategory = item.title
                                    }
                                }
                                .padding(horizontal = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val label = displayTabLabel(item)
                            val glyph = when (item.icon) {
                                "tab_01" -> "wrench"
                                "tab_04" -> "flash"
                                "tab_05" -> "moon"
                                "tab_06" -> "display"
                                else -> item.icon
                            }
                            val textOnlyTab = item.icon == "tab_02" || item.icon == "tab_03" || item.icon == "tab_09"
                            if (textOnlyTab) {
                                Text(
                                    label,
                                    color = if (selected) Color(0xFF171A1B) else Color.White,
                                    fontSize = if (item.icon == "tab_02") 8.sp else 10.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                FujiIconGlyph(
                                    glyph,
                                    Modifier.size(22.dp),
                                    if (selected) Color(0xFF171A1B) else Color.White
                                )
                                Text(
                                    label,
                                    color = if (selected) Color(0xFF171A1B) else Color(0xFFE8EAEB),
                                    fontSize = 6.8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (selected) {
                                Box(Modifier.size(5.dp).background(Color(0xFFD8283C), RoundedCornerShape(1.dp)))
                                Text("◂", color = Color(0xFF171A1B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                        .background(FujiSkin.lcd)
                        .border(1.dp, Color(0xFF596265), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 6.dp)
                ) {
                    AnimatedContent(
                        targetState = selectedCategory,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        transitionSpec = {
                            (
                                fadeIn(tween(if (AnimaT.isSystemReducedMotion(context)) 0 else Tuning.SETTINGS_TAB_MS)) +
                                    slideInHorizontally(
                                        initialOffsetX = { it / 24 },
                                        animationSpec = tween(if (AnimaT.isSystemReducedMotion(context)) 0 else Tuning.SETTINGS_TAB_MS)
                                    )
                            ) togetherWith (
                                fadeOut(tween(if (AnimaT.isSystemReducedMotion(context)) 0 else 100)) +
                                    slideOutHorizontally(
                                        targetOffsetX = { -it / 36 },
                                        animationSpec = tween(if (AnimaT.isSystemReducedMotion(context)) 0 else 100)
                                    )
                            )
                        },
                        label = "Settings detail tab"
                    ) { detailTitle ->
                        SettingsDetailPage(
                            title = detailTitle,
                            onBack = {},
                            ratioIdx = ratioIdx,
                            onRatio = onRatio,
                            showGrid = showGrid,
                            onShowGrid = onShowGrid,
                            showHist = showHist,
                            onShowHist = onShowHist,
                            focusPeaking = focusPeaking,
                            onFocusPeaking = onFocusPeaking,
                            embedded = true,
                            modifier = Modifier.fillMaxSize(),
                            onOpenBackup = {
                                categoryBeforeBackup = selectedCategory
                                selectedCategory = "Sao lưu & Khôi phục"
                            },
                            onOpenToolbox = onOpenToolbox,
                            onOpenContentsManagement = onOpenContentsManagement,
                            onReplayActivation = onReplayActivation,
                            onOpenWatermarkPanel = onOpenWatermarkPanel,
                            onOpenDiagnostics = { showCaptureDiagnostics = true }
                        )
                    }
                }
            }
        }
    }
    if (showCaptureDiagnostics) {
        CaptureDiagnosticsDialog(onDismiss = { showCaptureDiagnostics = false })
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
    onFocusPeaking: (Boolean) -> Unit,
    embedded: Boolean = false,
    modifier: Modifier = Modifier,
    onOpenBackup: () -> Unit = {},
    onOpenToolbox: () -> Unit = {},
    onOpenContentsManagement: () -> Unit = {},
    onReplayActivation: () -> Unit = {},
    onOpenWatermarkPanel: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val prefs = remember { context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE) }
    var afAreaSize by remember {
        mutableStateOf(CameraAfAreaSize.fromPreference(prefs.getString("af_area_size", "m")).preferenceValue)
    }
    var faceTrackingMode by remember {
        mutableStateOf(
            prefs.getString(
                "face_tracking_mode",
                if (prefs.contains("face_tracking_enabled") && !prefs.getBoolean("face_tracking_enabled", true)) "off" else "face_eye"
            )?.takeIf { it in setOf("off", "face", "face_eye") } ?: "face_eye"
        )
    }
    var imageQuality by remember { mutableStateOf(prefs.getString("image_quality", "Best") ?: "Best") }
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
    var torchEnabled by remember {
        mutableStateOf(prefs.getInt("camera_flash_mode", ImageCapture.FLASH_MODE_OFF) == ImageCapture.FLASH_MODE_ON)
    }
    var captureTimerSeconds by remember {
        mutableIntStateOf(prefs.getInt("camera_timer_seconds", 0).coerceIn(0, 10))
    }
    val supportedFpsModes = remember(context) { discoverSupportedPreviewFpsModes(context) }
    val defaultFpsMode = remember(supportedFpsModes) { highestSupportedPreviewFpsMode(supportedFpsModes) }
    var previewFpsMode by remember {
        mutableStateOf(
            prefs.getString("preview_fps_mode", defaultFpsMode)
                ?.takeIf { it in supportedFpsModes } ?: defaultFpsMode
        )
    }
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
    var zoomButtonsMode by remember {
        mutableStateOf(prefs.getString("zoom_buttons_mode", "standard")?.takeIf { it == "standard" || it == "swapped" } ?: "standard")
    }
    var zoomBarPosition by remember {
        mutableStateOf(prefs.getString("zoom_bar_position", "top_center")?.let { if (it == "top_left") "middle_left" else it }?.takeIf { it == "top_center" || it == "middle_left" } ?: "top_center")
    }
    var pixelScaleMode by remember {
        mutableStateOf(prefs.getString("osd_pixel_mode", "fine")?.takeIf { it in setOf("fine", "medium", "chunky") } ?: "fine")
    }
    var shutterButtonColor by remember { mutableStateOf(prefs.getString("shutter_button_color", "#FFD9DBDD") ?: "#FFD9DBDD") }
    var shutterButtonImageUri by remember { mutableStateOf(prefs.getString("shutter_button_image_uri", null)) }
    var shutterSoundEnabled by remember { mutableStateOf(prefs.getBoolean("shutter_sound_enabled", true)) }
    var hapticFeedbackEnabled by remember { mutableStateOf(prefs.getBoolean("haptic_feedback_enabled", true)) }
    var interfaceSoundEnabled by remember { mutableStateOf(prefs.getBoolean("ui_sound_enabled", true)) }
    var soundMasterVolume by remember { mutableIntStateOf(prefs.getInt("sound_master_volume", 85).coerceIn(0, 100)) }
    val shutterImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            shutterButtonImageUri = uri.toString()
            prefs.edit().putString("shutter_button_image_uri", uri.toString()).apply()
        }
    }

    FujiPanel(
        title = title,
        modifier = modifier.fillMaxSize(),
        onBack = onBack,
        embedded = embedded
    ) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isLandscape) 22.dp else 12.dp, vertical = 10.dp)
        ) {
        when (title) {
            "Công cụ hỗ trợ" -> {
                Text("Các lớp phủ màn ngắm được thiết lập trong Hộp công cụ.",
                    color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                FujiListRow("Hộp công cụ", "MỞ", onClick = onOpenToolbox, description = "Lưới · Cân bằng · Biểu đồ · Nét đỉnh · HUD")
                FujiListRow("Quản lý LUT", "MỞ", onClick = onOpenContentsManagement, description = "Nhập và quản lý LUT")
                FujiListRow("Nhật ký chẩn đoán", "MỞ", onClick = onOpenDiagnostics, description = "30 lần chụp gần nhất · xuất tệp TXT")
            }
            "Lấy nét & Ống kính" -> {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        stringResource(R.string.af_area_size_title),
                        color = FujiSkin.lcdGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.af_area_size_description),
                        color = CamColors.Sub,
                        fontSize = 10.sp
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "s" to R.string.af_area_size_small,
                            "m" to R.string.af_area_size_medium,
                            "l" to R.string.af_area_size_large
                        ).forEach { (value, labelRes) ->
                            FujiChip(
                                stringResource(labelRes),
                                afAreaSize == value,
                                {
                                    afAreaSize = value
                                    prefs.edit().putString("af_area_size", value).apply()
                                },
                                Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.face_tracking_section_title),
                        color = FujiSkin.lcdGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.face_tracking_section_description),
                        color = CamColors.Sub,
                        fontSize = 10.sp
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "off" to R.string.face_tracking_off,
                            "face" to R.string.face_tracking_face,
                            "face_eye" to R.string.face_tracking_face_eye
                        ).forEach { (value, labelRes) ->
                            FujiChip(
                                stringResource(labelRes),
                                faceTrackingMode == value,
                                {
                                    faceTrackingMode = value
                                    prefs.edit()
                                        .putString("face_tracking_mode", value)
                                        .putBoolean("face_tracking_enabled", value != "off")
                                        .apply()
                                },
                                Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.face_tracking_hardware_note),
                        color = CamColors.Sub,
                        fontSize = 9.sp
                    )
                    CameraHardwareScannerPanel()
                }
            }
            "Chụp & Chất lượng" -> {
                Text("Tỉ lệ khung hình", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    aspectRatios.forEachIndexed { index, pair ->
                        FujiChip(aspectRatioDisplayLabel(index, isLandscape), index == ratioIdx, { onRatio(index) }, Modifier.width(68.dp))
                    }
                }
                Spacer(Modifier.padding(7.dp))
                Text("Chất lượng ảnh", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Text("Tốt nhất sử dụng mức nén JPEG cao nhất; độ phân giải thực tế phụ thuộc độ phân giải mà Camera2/CameraX cung cấp cho camera đang chọn.",
                    color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 9.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(
                        "Thấp" to "Low",
                        "Vừa" to "Medium",
                        "Cao" to "High",
                        "Tốt nhất" to "Best"
                    ).forEach { (label, storedValue) ->
                        FujiChip(label, imageQuality == storedValue, {
                            imageQuality = storedValue
                            prefs.edit().putString("image_quality", storedValue).apply()
                        }, Modifier.width(75.dp))
                    }
                }
                Text("Ảnh được lưu vào Pictures/FujifilmMode.",
                    color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp))
                FujiListRow("Watermark", "MỞ", onClick = onOpenWatermarkPanel,
                    description = "Bật/tắt, kiểu, nội dung, ngày giờ và vị trí")
                FujiListRow("Sao lưu & Khôi phục", "SAO LƯU", onClick = onOpenBackup,
                    description = "Lưu hoặc nạp lại các thiết lập camera")
            }
            "Hiển thị / Giao diện", "Appearance" -> {
                Text(
                    stringResource(R.string.zoom_controls_section),
                    color = FujiSkin.lcdGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Text(stringResource(R.string.zoom_buttons_label), color = CamColors.TextColor, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 7.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    FujiChip(stringResource(R.string.zoom_buttons_standard), zoomButtonsMode == "standard", {
                        zoomButtonsMode = "standard"
                        prefs.edit().putString("zoom_buttons_mode", "standard").apply()
                    })
                    FujiChip(stringResource(R.string.zoom_buttons_swapped), zoomButtonsMode == "swapped", {
                        zoomButtonsMode = "swapped"
                        prefs.edit().putString("zoom_buttons_mode", "swapped").apply()
                    })
                }
                Text(stringResource(R.string.zoom_bar_position_label), color = CamColors.TextColor, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 10.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    FujiChip(stringResource(R.string.zoom_bar_position_top_center), zoomBarPosition == "top_center", {
                        zoomBarPosition = "top_center"
                        prefs.edit().putString("zoom_bar_position", "top_center").apply()
                    })
                    FujiChip(stringResource(R.string.zoom_bar_position_middle_left), zoomBarPosition == "middle_left", {
                        zoomBarPosition = "middle_left"
                        prefs.edit().putString("zoom_bar_position", "middle_left").apply()
                    })
                }
                Text(stringResource(R.string.zoom_bar_description), color = CamColors.Sub, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                Text(stringResource(R.string.osd_pixel_scale_title), color = FujiSkin.lcdGreen,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf(
                        "fine" to R.string.osd_pixel_fine,
                        "medium" to R.string.osd_pixel_medium,
                        "chunky" to R.string.osd_pixel_chunky
                    ).forEach { (value, label) ->
                        FujiChip(stringResource(label), pixelScaleMode == value, {
                            pixelScaleMode = value
                            prefs.edit().putString("osd_pixel_mode", value).apply()
                        })
                    }
                }
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
                    stringResource(R.string.sound_master_volume, soundMasterVolume),
                    color = CamColors.TextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
                Slider(
                    value = soundMasterVolume.toFloat(),
                    onValueChange = {
                        soundMasterVolume = it.roundToInt().coerceIn(0, 100)
                        prefs.edit().putInt("sound_master_volume", soundMasterVolume).apply()
                        CameraSoundEngine.configure(context, soundMasterVolume)
                    },
                    valueRange = 0f..100f,
                    steps = 19
                )
                Text(
                    "CAMERA FEEDBACK",
                    color = FujiSkin.lcdGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                SettingsSwitch(
                    stringResource(R.string.sound_shutter_title),
                    stringResource(R.string.sound_shutter_description),
                    shutterSoundEnabled
                ) {
                    shutterSoundEnabled = it
                    prefs.edit().putBoolean("shutter_sound_enabled", it).apply()
                }
                SettingsSwitch(
                    stringResource(R.string.sound_ui_title),
                    stringResource(R.string.sound_ui_description),
                    interfaceSoundEnabled
                ) {
                    interfaceSoundEnabled = it
                    prefs.edit().putBoolean("ui_sound_enabled", it).apply()
                }
                SettingsSwitch(
                    stringResource(R.string.sound_haptic_title),
                    stringResource(R.string.sound_haptic_description),
                    hapticFeedbackEnabled
                ) {
                    hapticFeedbackEnabled = it
                    prefs.edit().putBoolean("haptic_feedback_enabled", it).apply()
                }
                Text(
                    stringResource(R.string.sound_pcm_note),
                    color = FujiSkin.muted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            "Sao lưu & Khôi phục" -> {
                BackupRestorePanel(onRestoreComplete = {
                    onRatio(prefs.getInt("capture_ratio_index", 1).coerceIn(0, 4))
                    onShowGrid(prefs.getBoolean("viewfinder_grid_enabled", true))
                    onShowHist(prefs.getBoolean("viewfinder_histogram_enabled", true))
                    onFocusPeaking(prefs.getBoolean("viewfinder_focus_peaking_enabled", false))
                    AppLanguageManager.initialize(context)
                })
            }
            "Chụp đêm" -> {
                Text(
                    "Chế độ chụp đêm",
                    color = CamColors.TextColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Night Shot thu nhiều khung trong thời lượng đã chọn, căn chỉnh và ghép để giảm nhiễu, tăng sáng có kiểm soát. Số khung thực tế phụ thuộc tốc độ camera; giữ máy ổn định đến khi ghép xong.",
                    color = CamColors.Sub,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp, bottom = 12.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Tắt" to "off",
                        "Night Shot · Nhiều khung" to "night",
                        "Phơi sáng lâu · Star Trails" to "star_trails"
                    ).forEach { (label, value) ->
                        Row(
                            Modifier.fillMaxWidth().background(FujiSkin.lcd).border(1.dp, Color(0xFF344247)).animaTClickable {
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
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                            .background(FujiSkin.lcd).border(1.dp, Color(0xFF344247), RoundedCornerShape(3.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Dải Ngân Hà", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Bật để dùng nhánh căn chỉnh và ghép khung bầu trời trong Night Shot. Tắt mặc định; không hoạt động ở Pro thường.",
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
            "Setup" -> {
                Text("ĐÈN LED / FLASH", color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                SettingsSwitch(
                    "Đèn trợ sáng liên tục",
                    "Dùng đèn LED để chiếu sáng khi ngắm; đèn không nháy tại thời điểm màn trập chụp.",
                    torchEnabled
                ) { enabled ->
                    torchEnabled = enabled
                    prefs.edit().putInt(
                        "camera_flash_mode",
                        if (enabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                    ).apply()
                }
                Spacer(Modifier.padding(5.dp))
                Text("HẸN GIỜ CHỤP", color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Hẹn giờ áp dụng cho Pro/JPEG thường; Night Shot và Star Trails giữ quy trình chụp riêng.",
                    color = CamColors.Sub, fontSize = 10.sp)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 3, 5, 10).forEach { seconds ->
                        FujiChip(
                            if (seconds == 0) "Tắt" else "$seconds giây",
                            captureTimerSeconds == seconds,
                            {
                                captureTimerSeconds = seconds
                                prefs.edit().putInt("camera_timer_seconds", seconds).apply()
                            },
                            Modifier.width(72.dp)
                        )
                    }
                }
                Spacer(Modifier.padding(5.dp))
                Text(
                    stringResource(R.string.preview_fps_title),
                    color = CamColors.TextColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.preview_fps_description),
                    color = CamColors.Sub,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp, bottom = 9.dp)
                )
                if (supportedFpsModes.isEmpty()) {
                    Text(stringResource(R.string.preview_fps_none), color = CamColors.Sub, fontSize = 11.sp)
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    supportedFpsModes.forEach { option ->
                        FujiChip(
                            if (option == "AUTO") option else option + "FPS",
                            previewFpsMode == option,
                            {
                                previewFpsMode = option
                                prefs.edit().putString("preview_fps_mode", option).apply()
                            },
                            Modifier.width(76.dp)
                        )
                    }
                }
                Spacer(Modifier.padding(5.dp))
                Text("Chất lượng chụp", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                SettingsSwitch(
                    "Ánh xạ tông HDR",
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
                Text("Chụp RAW", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
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
            }
            else -> {
                Text("FujiFilm Mode", color = CamColors.TextColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Người làm App: Baoaka06", color = CamColors.TextColor, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 18.dp))
                Text("TikTok: @baoaka06", color = FujiSkin.orange, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp).animaTClickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@baoaka06")))
                        }
                    })
                Text("Phiên bản ${BuildConfig.VERSION_NAME} · Bản dựng ${BuildConfig.VERSION_CODE}", color = CamColors.Sub, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 18.dp))
                val activeRefreshRate = runCatching {
                    @Suppress("DEPRECATION")
                    (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                        .defaultDisplay.refreshRate
                }.getOrDefault(60f)
                Text("${localizedUiText(context, "Tần số làm tươi")} ~ ${activeRefreshRate.toInt()}Hz", color = CamColors.Sub, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 5.dp))
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

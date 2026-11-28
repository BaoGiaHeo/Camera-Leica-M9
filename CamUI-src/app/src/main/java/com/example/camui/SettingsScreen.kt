package com.example.camui

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
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
        "tab_09" -> "MY"
        else -> item.title.uppercase()
    }
    val screenConfig = LocalConfiguration.current
    val sx = screenConfig.screenWidthDp.toFloat() / 1846f
    val sy = screenConfig.screenHeightDp.toFloat() / 852f
    val tabLabelColor = Color(0xFFF1F1F1)
    val outerSurface = Color(0xFF12252B)
    val innerSurface = Color(0xFF27353A)
    val tabIcon: (SettingItem) -> String = { item ->
        when (item.icon) {
            "tab_01" -> "wrench"
            "tab_02" -> "af"
            "tab_03" -> "camera"
            "tab_04" -> "flash"
            "tab_05" -> "moon"
            "tab_06" -> "display"
            else -> "settings"
        }
    }
    val tabWidth: (SettingItem) -> androidx.compose.ui.unit.Dp = { item ->
        when (item.icon) {
            "tab_01" -> 150.dp
            "tab_02" -> 132.dp
            "tab_03" -> 104.dp
            "tab_04" -> 112.dp
            "tab_05" -> 108.dp
            "tab_06" -> 158.dp
            else -> 96.dp
        }
    }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF080D10), Color(0xFF111C21), Color(0xFF070A0C))))
            .padding(10.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().height(54.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FujiCameraKey(
                    "Quay Lại",
                    modifier = Modifier.width(142.dp).height(54.dp),
                    icon = "back",
                    horizontalContent = true,
                    scale = 0.84f
                ) { onBack() }
                Row(
                    Modifier.weight(1f).fillMaxHeight().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visibleSettingItems.forEach { item ->
                        val selected = selectedCategory == item.title
                        val label = displayTabLabel(item)
                        val tabShape = RoundedCornerShape(9.dp)
                        Column(
                            Modifier.width(tabWidth(item)).height(54.dp)
                                .shadow(if (selected) 9.dp else 1.dp, tabShape)
                                .clip(tabShape)
                                .background(
                                    if (selected) Brush.verticalGradient(listOf(Color(0xFF1D414A), Color(0xFF111E24)))
                                    else Brush.verticalGradient(listOf(Color(0xFF292D30), Color(0xFF171A1C)))
                                )
                                .border(
                                    if (selected) 1.7.dp else 1.dp,
                                    if (selected) Color(0xFF65EAF0) else Color(0xFF596367),
                                    tabShape
                                )
                                .clickable {
                                    if (selectedCategory != item.title) {
                                        previousCategory = selectedCategory
                                        selectedCategory = item.title
                                    }
                                },
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                FujiIconGlyph(
                                    tabIcon(item),
                                    Modifier.size(17.dp),
                                    if (selected) Color(0xFF8EF5F1) else Color(0xFFD0D6D8)
                                )
                                Text(
                                    label,
                                    color = if (selected) Color(0xFF9CF4F1) else Color(0xFFE4E7E8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                Modifier.fillMaxWidth().height(2.5.dp)
                                    .background(
                                        if (selected) Brush.horizontalGradient(
                                            listOf(Color(0xFF39E0E9), Color(0xFF756BFF), Color(0xFF37E0B2))
                                        ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                            )
                        }
                    }
                }
            }

            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF122329), Color(0xFF0C151A), Color(0xFF111D22))))
                    .border(1.8.dp, Color(0xFF4BD4E1), RoundedCornerShape(25.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val gap = 19.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            Color.White.copy(alpha = 0.018f),
                            Offset(size.width * 0.03f, y),
                            Offset(size.width * 0.97f, y),
                            0.4.dp.toPx()
                        )
                        y += gap
                    }
                    drawCircle(
                        Color(0xFF54DCE4).copy(alpha = 0.035f),
                        size.minDimension * 0.22f,
                        Offset(size.width * 0.80f, size.height * 0.22f)
                    )
                }
                AnimatedContent(
                    targetState = selectedCategory,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        (
                            fadeIn(tween(if (AnimaT.isSystemReducedMotion(context)) 0 else Tuning.SETTINGS_TAB_MS)) +
                                slideInHorizontally(
                                    initialOffsetX = { it / 32 },
                                    animationSpec = tween(if (AnimaT.isSystemReducedMotion(context)) 0 else Tuning.SETTINGS_TAB_MS)
                                )
                        ) togetherWith (
                            fadeOut(tween(if (AnimaT.isSystemReducedMotion(context)) 0 else 110)) +
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 42 },
                                    animationSpec = tween(if (AnimaT.isSystemReducedMotion(context)) 0 else 110)
                                )
                        )
                    },
                    label = "Fuji 4.0 Settings detail"
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
                Canvas(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(3.dp)) {
                    drawRoundRect(
                        Color(0xFF26373B),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2f)
                    )
                    if (detailScrollState.maxValue > 0) {
                        val fraction = (size.height / (size.height + detailScrollState.maxValue.toFloat()))
                            .coerceIn(0.08f, 1f)
                        val thumbHeight = (size.height * fraction).coerceAtLeast(18.dp.toPx())
                        val top = (size.height - thumbHeight) *
                            (detailScrollState.value.toFloat() / detailScrollState.maxValue).coerceIn(0f, 1f)
                        drawRoundRect(
                            Color(0xFF63E5E8),
                            topLeft = Offset(0f, top),
                            size = androidx.compose.ui.geometry.Size(size.width, thumbHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2f)
                        )
                    } else {
                        drawRoundRect(
                            Color(0xFF63E5E8),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2f)
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
        mutableStateOf(CameraAfAreaSize.fromPreference(prefs.getString("af_area_size", "l")).preferenceValue)
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
        mutableIntStateOf(prefs.getInt("nightshot_duration_seconds", 5).let { if (it in listOf(3, 5, 10)) it else 5 })
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
        val detailScrollState = rememberScrollState()
        val groupIcon = when (title) {
            "Công cụ hỗ trợ" -> "wrench"
            "Lấy nét & Ống kính" -> "af"
            "Chụp & Chất lượng" -> "camera"
            "Setup" -> "flash"
            "Chụp đêm" -> "moon"
            "Hiển thị / Giao diện", "Appearance" -> "display"
            else -> "settings"
        }
        Column(
            Modifier.fillMaxSize().padding(3.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC0B151A))
                .border(1.dp, Color(0xFF334C54), RoundedCornerShape(12.dp))
                .padding(horizontal = 11.dp, vertical = 8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FujiIconGlyph(groupIcon, Modifier.size(18.dp), FujiSkin.lcdGreen)
                Text(
                    title.uppercase(Locale.ROOT),
                    color = Color(0xFFECF4F2),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(26.dp).height(2.dp).background(
                    Brush.horizontalGradient(listOf(Color(0xFF47D9DF), Color.Transparent))
                ))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.fillMaxSize()
                        .verticalScroll(detailScrollState)
                        .padding(end = 9.dp)
                        .padding(horizontal = if (isLandscape) 12.dp else 7.dp, vertical = 4.dp)
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
                Text("Khung hình cố định · 4:3", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                Text("Tỉ lệ ảnh và preview được cố định ở 4:3 trong phiên bản 3.1.0.", color = CamColors.Sub, fontSize = 10.sp)
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
                    "Night Shot chụp liên tiếp theo thời lượng đã chọn ở mục tiêu 8 FPS khi camera hỗ trợ; sau đó căn chỉnh và ghép để giảm nhiễu, tăng sáng có kiểm soát. Tốc độ thực tế phụ thuộc cảm biến và thời gian phơi sáng.",
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
                    Text("CHẾ ĐỘ CHỤP NHANH", color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Night Shot tự chụp liên tiếp trong ${nightShotDurationSeconds} giây, mục tiêu 8 FPS khi camera hỗ trợ. Số khung thực tế phụ thuộc cảm biến, thời gian phơi sáng và giới hạn HAL.",
                        color = CamColors.Sub,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
                if (nightShotMode == "night") {
                    Spacer(Modifier.padding(5.dp))
                    Text("Thời lượng chụp Night Shot", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf(3, 5, 10).forEach { seconds ->
                            FujiChip(
                                "${seconds}s",
                                nightShotDurationSeconds == seconds,
                                {
                                    nightShotDurationSeconds = seconds
                                    prefs.edit().putInt("nightshot_duration_seconds", seconds).apply()
                                },
                                Modifier.width(72.dp)
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
                    prefs.edit()
                        .putInt(
                            "camera_flash_mode",
                            if (enabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                        )
                        .putString("camera_illumination_mode", if (enabled) "continuous" else "off")
                        .apply()
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

package com.example.camui

import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.provider.OpenableColumns
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.BitmapShader
import android.graphics.Shader
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import android.media.MediaPlayer
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.Uri
import android.provider.MediaStore
import android.content.ContentUris
import android.util.Size
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.abs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.math.roundToInt
import java.util.zip.ZipInputStream
import java.net.URL

@Composable
private fun GlassParameterSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = CamColors.TextColor, fontSize = 11.sp)
        Text(
            if (range.endInclusive > 2f) "%.1f".format(value) else "%.0f%%".format(value * 100f),
            color = CamColors.Sub, fontSize = 10.sp
        )
    }
    Slider(value = value, onValueChange = onValueChange, valueRange = range)
}

@Composable
private fun LiquidGlassWatermarkPreview(
    template: String,
    refraction: Float,
    thickness: Float,
    dispersion: Float,
    blur: Float,
    roughness: Float,
    rim: Float,
    gloss: Float,
    corner: Float,
    gooey: Float,
    tint: String,
    brand: String = "fujifilm",
    modifier: Modifier = Modifier
) {
    val tintColor = when (tint) {
        "Milky" -> Color.White
        "Smoke" -> Color(0xFF171C25)
        else -> Color(0xFFD8E9F3)
    }
    Box(modifier.height(142.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF263C43))) {
        ComposeCanvas(Modifier.fillMaxSize()) {
            drawRect(brush = Brush.linearGradient(listOf(
                Color(0xFF6F8790), Color(0xFF536D64), Color(0xFF263C43)
            )))
            drawCircle(Color(0xFFFFD79B), size.minDimension * 0.11f,
                Offset(size.width * 0.76f, size.height * 0.26f))
            drawOval(Color(0xFF667F62),
                topLeft = Offset(-size.width * 0.12f, size.height * 0.56f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.55f))
            drawOval(Color(0xFF324C42),
                topLeft = Offset(size.width * 0.40f, size.height * 0.59f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.82f, size.height * 0.58f))
        }
        val panelShape = RoundedCornerShape((12f + corner * 14f + gooey * 5f).dp)
        val accent = Color(cameraWatermarkBrandAccent(brand))
        val exampleModel = cameraWatermarkExampleModel(brand)
        Row(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth(0.93f)
                .padding(bottom = 9.dp)
                .clip(panelShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f + gloss * 0.18f),
                            tintColor.copy(alpha = 0.12f + thickness * 0.12f),
                            Color(0xFFBBD9EA).copy(alpha = 0.08f + blur / 100f)
                        )
                    )
                )
                .border(
                    (0.7f + rim * 1.4f).dp,
                    Brush.linearGradient(listOf(
                        Color.White.copy(alpha = 0.45f + rim * 0.45f),
                        Color(0xFF72D8F5).copy(alpha = dispersion * 0.50f),
                        Color(0xFFFFA5C7).copy(alpha = dispersion * 0.38f),
                        Color.White.copy(alpha = 0.16f)
                    )),
                    panelShape
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.fillMaxWidth(0.315f).height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                WatermarkBrandMark(
                    brand = brand,
                    modifier = Modifier.fillMaxWidth(0.88f).height(17.dp)
                        .background(Color.White, RoundedCornerShape(3.dp)).padding(2.dp),
                    fallbackColor = accent
                )
            }
            Box(
                Modifier.width(1.dp).height(38.dp).background(accent.copy(alpha = 0.88f))
            )
            Column(
                Modifier.weight(1f).padding(start = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "MODEL · $exampleModel",
                    color = Color.White.copy(alpha = 0.96f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    "ISO 100 · 1/250s · F2.8",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
        Text(
            "REFRACTION " + (refraction * 100f).toInt() +
                "  ·  BLUR " + blur.toInt() +
                "  ·  GRAIN " + (roughness * 100f).toInt(),
            color = Color.White.copy(alpha = 0.8f), fontSize = 7.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        )
    }
}

@Composable
fun ToolboxScreen(
    onBack: () -> Unit,
    showGrid: Boolean,
    onShowGrid: (Boolean) -> Unit,
    showHist: Boolean,
    onShowHist: (Boolean) -> Unit,
    focusPeaking: Boolean,
    onFocusPeaking: (Boolean) -> Unit,
    animationPerformanceMode: Boolean = false,
    onAnimationPerformanceModeChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE) }
    val presetStore = remember { context.getSharedPreferences("fujifilm_camera_presets", Context.MODE_PRIVATE) }
    val filmRecipeStore = remember { context.getSharedPreferences("fujifilm_film_recipes", Context.MODE_PRIVATE) }
    var presetUiRevision by remember { mutableStateOf(0) }
    val presetKeys = remember {
        listOf(
            "capture_ratio_index", "camera_zoom_ratio", "camera_flash_mode", "camera_timer_seconds",
            "camera_metering_index", "camera_mode",
            "selected_filter", "image_quality", "noise_reduction", "hdr_enabled",
            "nightshot_mode", "nightshot_duration_seconds", "star_trails_exposure_seconds",
            "milky_way_stack_enabled", "raw_capture_mode",
            "uhd_capture_enabled", "fhd_capture_enabled", "uhd_plus_capture_enabled",
            "burst_capture_mode_enabled", "hdr_bracketing_enabled", "focus_bracketing_enabled",
            "viewfinder_grid_enabled", "viewfinder_histogram_enabled",
            "viewfinder_focus_peaking_enabled", "viewfinder_rgb_histogram_enabled",
            "viewfinder_zebra_enabled", "viewfinder_level_enabled",
            "face_tracking_enabled", "hud_auto_hide_enabled", "lens_correction_enabled",
            "pro_manual_exposure_enabled", "pro_manual_iso_enabled",
            "pro_manual_shutter_enabled", "pro_manual_wb_enabled",
            "pro_manual_iso", "pro_manual_shutter_ns", "pro_manual_wb_kelvin",
            "pro_manual_focus", "pro_hud_show_ev", "pro_hud_show_shutter",
            "pro_hud_show_iso", "pro_hud_show_focus", "pro_hud_show_white_balance"
        )
    }
    fun saveCameraPreset(slot: String) {
        val values = prefs.all
        val editor = presetStore.edit()
        presetKeys.forEach { key ->
            val storedKey = "$slot.$key"
            when (val value = values[key]) {
                is Boolean -> editor.putBoolean(storedKey, value)
                is Int -> editor.putInt(storedKey, value)
                is Long -> editor.putLong(storedKey, value)
                is Float -> editor.putFloat(storedKey, value)
                is String -> editor.putString(storedKey, value)
                is Set<*> -> editor.putStringSet(storedKey, value.filterIsInstance<String>().toSet())
                null -> editor.remove(storedKey)
            }
        }
        editor.putBoolean("$slot.exists", true)
            .putString("$slot.name", "Preset $slot")
            .putLong("$slot.saved_at", System.currentTimeMillis())
            .apply()
        presetUiRevision++
        Toast.makeText(context, "Đã lưu Camera Preset $slot", Toast.LENGTH_SHORT).show()
    }
    fun loadCameraPreset(slot: String) {
        if (!presetStore.getBoolean("$slot.exists", false)) {
            Toast.makeText(context, "Preset $slot chưa được lưu", Toast.LENGTH_SHORT).show()
            return
        }
        val saved = presetStore.all
        val editor = prefs.edit()
        presetKeys.forEach { key ->
            when (val value = saved["$slot.$key"]) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                null -> editor.remove(key)
            }
        }
        editor.apply()
        Toast.makeText(context, "Đã nạp Camera Preset $slot · cấu hình chụp đã khôi phục", Toast.LENGTH_LONG).show()
        onBack()
    }
    var watermarkEnabled by remember { mutableStateOf(prefs.getBoolean("watermark_enabled", true)) }
    var zebraEnabled by remember { mutableStateOf(prefs.getBoolean("viewfinder_zebra_enabled", false)) }
    var rgbHistogramEnabled by remember { mutableStateOf(prefs.getBoolean("viewfinder_rgb_histogram_enabled", false)) }
    var levelEnabled by remember { mutableStateOf(prefs.getBoolean("viewfinder_level_enabled", false)) }
    var proHudShowEv by remember { mutableStateOf(prefs.getBoolean("pro_hud_show_ev", true)) }
    var proHudShowShutter by remember { mutableStateOf(prefs.getBoolean("pro_hud_show_shutter", true)) }
    var proHudShowIso by remember { mutableStateOf(prefs.getBoolean("pro_hud_show_iso", true)) }
    var proHudShowFocus by remember { mutableStateOf(prefs.getBoolean("pro_hud_show_focus", true)) }
    var proHudShowWhiteBalance by remember { mutableStateOf(prefs.getBoolean("pro_hud_show_white_balance", true)) }
    var hdrBracketMode by remember { mutableStateOf(prefs.getBoolean("hdr_bracketing_enabled", false)) }
    var focusBracketMode by remember { mutableStateOf(prefs.getBoolean("focus_bracketing_enabled", false)) }
    var faceTrackingEnabled by remember { mutableStateOf(prefs.getBoolean("face_tracking_enabled", false)) }
    var filmHighlight by remember { mutableStateOf(prefs.getFloat("film_custom_highlight", 0f).coerceIn(-1f, 1f)) }
    var filmShadow by remember { mutableStateOf(prefs.getFloat("film_custom_shadow", 0f).coerceIn(-1f, 1f)) }
    var filmColorChrome by remember { mutableStateOf(prefs.getFloat("film_custom_color_chrome", 0f).coerceIn(0f, 1f)) }
    var filmGrain by remember { mutableStateOf(prefs.getFloat("film_custom_grain", 0f).coerceIn(0f, 1f)) }
    var filmTemperature by remember { mutableStateOf(prefs.getFloat("film_custom_temperature", 0f).coerceIn(-1f, 1f)) }
    var filmTone by remember { mutableStateOf(prefs.getFloat("film_custom_tone", 0f).coerceIn(-1f, 1f)) }
    var filmSaturation by remember { mutableStateOf(prefs.getFloat("film_custom_saturation", 1f).coerceIn(0.5f, 1.5f)) }

    val filmRecipeKeys = remember {
        listOf(
            "film_custom_highlight", "film_custom_shadow", "film_custom_color_chrome",
            "film_custom_grain", "film_custom_temperature", "film_custom_tone", "film_custom_saturation",
            "selected_filter"
        )
    }
    fun saveFilmRecipe(slot: String) {
        val editor = filmRecipeStore.edit()
        filmRecipeKeys.forEach { key ->
            when (key) {
                "film_custom_highlight" -> editor.putFloat("$slot.$key", filmHighlight)
                "film_custom_shadow" -> editor.putFloat("$slot.$key", filmShadow)
                "film_custom_color_chrome" -> editor.putFloat("$slot.$key", filmColorChrome)
                "film_custom_grain" -> editor.putFloat("$slot.$key", filmGrain)
                "film_custom_temperature" -> editor.putFloat("$slot.$key", filmTemperature)
                "film_custom_tone" -> editor.putFloat("$slot.$key", filmTone)
                "film_custom_saturation" -> editor.putFloat("$slot.$key", filmSaturation)
                "selected_filter" -> editor.putString("$slot.$key", prefs.getString(key, "None") ?: "None")
            }
        }
        editor.putBoolean("$slot.exists", true)
            .putLong("$slot.saved_at", System.currentTimeMillis())
            .apply()
        Toast.makeText(context, "Đã lưu Film Recipe $slot", Toast.LENGTH_SHORT).show()
    }
    fun loadFilmRecipe(slot: String) {
        if (!filmRecipeStore.getBoolean("$slot.exists", false)) {
            Toast.makeText(context, "Film Recipe $slot chưa được lưu", Toast.LENGTH_SHORT).show()
            return
        }
        val values = filmRecipeStore
        filmHighlight = values.getFloat("$slot.film_custom_highlight", 0f).coerceIn(-1f, 1f)
        filmShadow = values.getFloat("$slot.film_custom_shadow", 0f).coerceIn(-1f, 1f)
        filmColorChrome = values.getFloat("$slot.film_custom_color_chrome", 0f).coerceIn(0f, 1f)
        filmGrain = values.getFloat("$slot.film_custom_grain", 0f).coerceIn(0f, 1f)
        filmTemperature = values.getFloat("$slot.film_custom_temperature", 0f).coerceIn(-1f, 1f)
        filmTone = values.getFloat("$slot.film_custom_tone", 0f).coerceIn(-1f, 1f)
        filmSaturation = values.getFloat("$slot.film_custom_saturation", 1f).coerceIn(0.5f, 1.5f)
        val selected = values.getString("$slot.selected_filter", "None") ?: "None"
        prefs.edit()
            .putFloat("film_custom_highlight", filmHighlight)
            .putFloat("film_custom_shadow", filmShadow)
            .putFloat("film_custom_color_chrome", filmColorChrome)
            .putFloat("film_custom_grain", filmGrain)
            .putFloat("film_custom_temperature", filmTemperature)
            .putFloat("film_custom_tone", filmTone)
            .putFloat("film_custom_saturation", filmSaturation)
            .putString("selected_filter", selected)
            .apply()
        Toast.makeText(context, "Đã nạp Film Recipe $slot", Toast.LENGTH_SHORT).show()
    }
    var hudAutoHideEnabled by remember { mutableStateOf(prefs.getBoolean("hud_auto_hide_enabled", true)) }
    var lensCorrectionEnabled by remember { mutableStateOf(prefs.getBoolean("lens_correction_enabled", true)) }
    var watermarkStyle by remember { mutableStateOf((prefs.getString("watermark_style", "White") ?: "White").takeIf { it == "White" || it == "Black" } ?: "White") }
    var watermarkTemplate by remember { mutableStateOf((prefs.getString("watermark_template", "fuji_auto") ?: "fuji_auto").takeIf { it in setOf("fuji_auto", "fuji_classic", "fuji_signature", "classic", "dark", "minimal", "simple", "micro", "vintage", "polaroid", "gallery", "frosted", "film", "datestamp") } ?: "fuji_auto") }
    var watermarkBrand by remember { mutableStateOf((prefs.getString("watermark_brand", "fujifilm") ?: "fujifilm").lowercase().takeIf { id -> cameraWatermarkBrands.any { it.first == id } } ?: "fujifilm") }
    var watermarkShowTime by remember { mutableStateOf(prefs.getBoolean("watermark_show_time", true)) }
    var watermarkShowModel by remember { mutableStateOf(prefs.getBoolean("watermark_show_model", true)) }
    var watermarkShowSpecs by remember { mutableStateOf(prefs.getBoolean("watermark_show_specs", true)) }
    LaunchedEffect(watermarkStyle) { prefs.edit().putString("watermark_style", watermarkStyle).apply() }
    LaunchedEffect(watermarkBrand) { prefs.edit().putString("watermark_brand", watermarkBrand).apply() }
    var customModel by remember { mutableStateOf(prefs.getString("watermark_custom_model", "") ?: "") }
    var customLocation by remember { mutableStateOf(prefs.getString("watermark_custom_location", "") ?: "") }
    var shutterEffect by remember { mutableStateOf(prefs.getString("shutter_effect", "Film Wash") ?: "Film Wash") }
    var shutterSound by remember { mutableStateOf(prefs.getBoolean("shutter_sound_enabled", true)) }
    var shutterSoundUri by remember { mutableStateOf(prefs.getString("shutter_sound_uri", null)) }
    var shutterSoundAsset by remember { mutableStateOf(prefs.getString("shutter_sound_asset", "classic") ?: "classic") }
    val shutterSoundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val persisted = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.isSuccess
            if (persisted) {
                shutterSoundUri = uri.toString()
                prefs.edit().putString("shutter_sound_uri", uri.toString()).apply()
                Toast.makeText(context, "Đã chọn âm thanh màn trập tùy chỉnh.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Không thể lưu quyền đọc tệp âm thanh này.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(CamColors.Bg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        PageHeader("Toolbox", onBack)
        Text("Viewfinder overlays", color = CamColors.Sub, fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        Text("Pro HUD elements", color = CamColors.Sub, fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, top = 15.dp, bottom = 8.dp))
        ToolToggle("EV display", "Hiện hoặc ẩn thông số bù phơi sáng trong hàng Pro HUD.",
            Icons.Default.Tune, proHudShowEv) {
            proHudShowEv = it
            prefs.edit().putBoolean("pro_hud_show_ev", it).apply()
        }
        ToolToggle("Shutter display", "Hiện hoặc ẩn tốc độ màn trập trong hàng Pro HUD.",
            Icons.Default.Tune, proHudShowShutter) {
            proHudShowShutter = it
            prefs.edit().putBoolean("pro_hud_show_shutter", it).apply()
        }
        ToolToggle("ISO display", "Hiện hoặc ẩn độ nhạy ISO trong hàng Pro HUD.",
            Icons.Default.Tune, proHudShowIso) {
            proHudShowIso = it
            prefs.edit().putBoolean("pro_hud_show_iso", it).apply()
        }
        ToolToggle("Focus display", "Hiện hoặc ẩn trạng thái lấy nét trong hàng Pro HUD.",
            Icons.Default.Tune, proHudShowFocus) {
            proHudShowFocus = it
            prefs.edit().putBoolean("pro_hud_show_focus", it).apply()
        }
        ToolToggle("White balance display", "Hiện hoặc ẩn nhiệt độ màu / cân bằng trắng trong hàng Pro HUD.",
            Icons.Default.Tune, proHudShowWhiteBalance) {
            proHudShowWhiteBalance = it
            prefs.edit().putBoolean("pro_hud_show_white_balance", it).apply()
        }
        ToolToggle("Grid Lines", "Thirds composition guide", Icons.Default.GridOn, showGrid, onShowGrid)
        ToolToggle("Histogram", "Live luminance histogram", Icons.Default.ShowChart, showHist, onShowHist)
        ToolToggle("RGB Histogram", "Biểu đồ phân bố kênh đỏ, lục và lam từ khung preview.", Icons.Default.ShowChart, rgbHistogramEnabled) {
            rgbHistogramEnabled = it
            prefs.edit().putBoolean("viewfinder_rgb_histogram_enabled", it).apply()
        }
        ToolToggle("Focus Peaking", "Highlight high-contrast focus edges", Icons.Default.Tune,
            focusPeaking, onFocusPeaking)
        ToolToggle("Smart Focus Tracking", "Ưu tiên khuôn mặt khi Camera2/HAL công bố face detection; tự đưa AF về vùng mặt.",
            Icons.Default.Tune, faceTrackingEnabled) {
            faceTrackingEnabled = it
            prefs.edit().putBoolean("face_tracking_enabled", it).apply()
        }
        ToolToggle("Auto-hide Camera HUD", "Ẩn thanh điều khiển sau 5 giây không thao tác; chạm màn hình để hiện lại.",
            Icons.Default.Tune, hudAutoHideEnabled) {
            hudAutoHideEnabled = it
            prefs.edit().putBoolean("hud_auto_hide_enabled", it).apply()
        }
        ToolToggle(
            "Animation Performance Mode",
            "Giảm trượt/scale khi máy nóng hoặc khi bật thủ công; Android tự kích hoạt khi nhiệt độ đạt mức trung bình.",
            Icons.Default.Tune,
            animationPerformanceMode,
            onAnimationPerformanceModeChanged
        )
        ToolToggle("Lens Correction", "Yêu cầu Camera2/HAL hiệu chỉnh méo hình, viền màu và tối góc bằng các chế độ công khai nếu ống kính hỗ trợ; không tự bịa dữ liệu lens profile.",
            Icons.Default.Tune, lensCorrectionEnabled) {
            lensCorrectionEnabled = it
            prefs.edit().putBoolean("lens_correction_enabled", it).apply()
        }
        ToolToggle("Zebra Warning", "Đánh dấu vùng sáng gần cháy sáng trên kính ngắm (không ảnh hưởng ảnh lưu).",
            Icons.Default.Warning, zebraEnabled) {
            zebraEnabled = it
            prefs.edit().putBoolean("viewfinder_zebra_enabled", it).apply()
        }
        ToolToggle("Camera Level & Horizon", "Đo độ nghiêng bằng cảm biến thiết bị và hiển thị đường chân trời trong kính ngắm.",
            Icons.Default.Tune, levelEnabled) {
            levelEnabled = it
            prefs.edit().putBoolean("viewfinder_level_enabled", it).apply()
        }
        ToolToggle("HDR Bracketing", "Chụp 3 khung JPEG với EV thấp / chuẩn / cao nếu camera công bố dải phơi sáng.",
            Icons.Default.ShowChart, hdrBracketMode) {
            hdrBracketMode = it
            if (it) focusBracketMode = false
            prefs.edit()
                .putBoolean("hdr_bracketing_enabled", it)
                .putBoolean("focus_bracketing_enabled", if (it) false else focusBracketMode)
                .apply()
        }
        ToolToggle("Focus Bracketing", "Chụp 5 khoảng nét từ gần đến vô cực khi ống kính hỗ trợ lấy nét thủ công.",
            Icons.Default.Tune, focusBracketMode) {
            focusBracketMode = it
            if (it) hdrBracketMode = false
            prefs.edit()
                .putBoolean("focus_bracketing_enabled", it)
                .putBoolean("hdr_bracketing_enabled", if (it) false else hdrBracketMode)
                .apply()
        }
        Text("Camera Presets", color = CamColors.TextColor,
            fontSize = 17.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 7.dp))
        Text("Lưu và nạp nhanh tỉ lệ, zoom, EV, bộ lọc, HDR, NightShotMode, RAW và cấu hình LunaDream.",
            color = CamColors.Sub, fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp))
        listOf("A", "B", "C").forEach { slot ->
            val savedName = presetStore.getString("$slot.name", "Trống") ?: "Trống"
            val savedAt = presetStore.getLong("$slot.saved_at", 0L)
            val timeLabel = if (savedAt > 0L) {
                java.text.SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(java.util.Date(savedAt))
            } else "Chưa lưu"
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(11.dp)).background(CamColors.Card)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("$savedName · $timeLabel", color = CamColors.TextColor,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { saveCameraPreset(slot) }) { Text("Lưu") }
                TextButton(
                    onClick = { loadCameraPreset(slot) },
                    enabled = presetStore.getBoolean("$slot.exists", false)
                ) { Text("Nạp") }
            }
        }
        // Read the revision so the slot labels refresh immediately after saving.
        if (presetUiRevision < 0) Text(" ", fontSize = 1.sp)
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(CamColors.Card).padding(12.dp)
        ) {
            Text("Film Simulation Customizer", color = CamColors.TextColor,
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Điều chỉnh này được áp dụng giống nhau trong preview bộ lọc và ảnh JPEG sau chụp.",
                color = CamColors.Sub, fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            Text("Highlight · ${String.format(Locale.US, "%+.2f", filmHighlight)}",
                color = CamColors.Sub, fontSize = 12.sp)
            Slider(filmHighlight, {
                filmHighlight = it
                prefs.edit().putFloat("film_custom_highlight", it).apply()
            }, valueRange = -1f..1f)
            Text("Shadow · ${String.format(Locale.US, "%+.2f", filmShadow)}",
                color = CamColors.Sub, fontSize = 12.sp)
            Slider(filmShadow, {
                filmShadow = it
                prefs.edit().putFloat("film_custom_shadow", it).apply()
            }, valueRange = -1f..1f)
            Text("Color Chrome · ${String.format(Locale.US, "%.2f", filmColorChrome)}",
                color = CamColors.Sub, fontSize = 12.sp)
            Slider(filmColorChrome, {
                filmColorChrome = it
                prefs.edit().putFloat("film_custom_color_chrome", it).apply()
            }, valueRange = 0f..1f)
            Text("Grain · ${String.format(Locale.US, "%.2f", filmGrain)}",
                color = CamColors.Sub, fontSize = 12.sp)
            Slider(filmGrain, {
                filmGrain = it
                prefs.edit().putFloat("film_custom_grain", it).apply()
            }, valueRange = 0f..1f)
            Text("Color Temperature · ${String.format(Locale.US, "%+.2f", filmTemperature)}",
                color = CamColors.Sub, fontSize = 12.sp)
            Slider(filmTemperature, {
                filmTemperature = it
                prefs.edit().putFloat("film_custom_temperature", it).apply()
            }, valueRange = -1f..1f)
            Text("Color Tone · ${String.format(Locale.US, "%+.2f", filmTone)}  (Green ↔ Magenta)", color = CamColors.Sub, fontSize = 12.sp)
            Slider(filmTone, {
                filmTone = it
                prefs.edit().putFloat("film_custom_tone", it).apply()
            }, valueRange = -1f..1f)
            Text("Saturation · ${String.format(Locale.US, "%.2f", filmSaturation)}",
                color = CamColors.Sub, fontSize = 12.sp)
            Slider(filmSaturation, {
                filmSaturation = it
                prefs.edit().putFloat("film_custom_saturation", it).apply()
            }, valueRange = 0.5f..1.5f)
            TextButton(onClick = {
                filmHighlight = 0f
                filmShadow = 0f
                filmColorChrome = 0f
                filmGrain = 0f
                filmTemperature = 0f
                filmTone = 0f
                filmSaturation = 1f
                prefs.edit()
                    .putFloat("film_custom_highlight", 0f)
                    .putFloat("film_custom_shadow", 0f)
                    .putFloat("film_custom_color_chrome", 0f)
                    .putFloat("film_custom_grain", 0f)
                    .putFloat("film_custom_temperature", 0f)
                    .putFloat("film_custom_tone", 0f)
                    .putFloat("film_custom_saturation", 1f)
                    .apply()
            }) { Text("Reset Recipe") }
            Text("Saved Film Recipes", color = CamColors.TextColor,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            listOf("A", "B", "C").forEach { slot ->
                val savedAt = filmRecipeStore.getLong("$slot.saved_at", 0L)
                val savedLabel = if (filmRecipeStore.getBoolean("$slot.exists", false)) {
                    java.text.SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        .format(java.util.Date(savedAt))
                } else "Trống"
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recipe $slot · $savedLabel", color = CamColors.Sub,
                        fontSize = 11.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { saveFilmRecipe(slot) }) { Text("Lưu") }
                    TextButton(
                        onClick = { loadFilmRecipe(slot) },
                        enabled = filmRecipeStore.getBoolean("$slot.exists", false)
                    ) { Text("Nạp") }
                }
            }
        }
        CameraFrameSettingsPanel()
        Spacer(Modifier.height(16.dp))
        Text("Watermark", color = CamColors.TextColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        ToolToggle("Enable Watermark", "Logo thương hiệu, thông số chụp và bố cục watermark đã chọn",
            Icons.Default.Image, watermarkEnabled) {
            watermarkEnabled = it
            prefs.edit().putBoolean("watermark_enabled", it).apply()
        }
        Text("Thương hiệu watermark", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 7.dp, top = 9.dp))
        cameraWatermarkBrands.chunked(3).forEach { brandRow ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                brandRow.forEach { (id, label) ->
                    Chip(label, watermarkBrand == id, {
                        watermarkBrand = id
                        prefs.edit().putString("watermark_brand", id).apply()
                    }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        Text("Mẫu watermark · chọn riêng từng kiểu", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp, top = 9.dp))
        val watermarkBrandLabel = cameraWatermarkBrands.firstOrNull { it.first == watermarkBrand }?.second
            ?: cameraWatermarkBrandTitle(watermarkBrand)
        val watermarkTemplates = listOf(
            Triple("fuji_auto", "$watermarkBrandLabel Auto", "Tự chọn bố cục ngang hoặc dọc"),
            Triple("fuji_classic", "$watermarkBrandLabel Classic", "Logo + model trái · EXIF phải"),
            Triple("fuji_signature", "$watermarkBrandLabel Signature", "Chữ ký máy · vị trí · ngày chụp"),
            Triple("classic", "Classic Bar", "Thanh trắng · máy trái · thông số phải"),
            Triple("dark", "Dark Bar", "Bố cục Classic trên nền đen"),
            Triple("minimal", "Minimal", "Một dòng thông tin gọn, không logo"),
            Triple("simple", "Simple", "Logo giữa · model bên dưới"),
            Triple("micro", "Micro", "Dải thông số siêu mảnh một dòng"),
            Triple("vintage", "Vintage", "Giấy ngà · chữ máy ảnh kiểu film"),
            Triple("polaroid", "Polaroid", "Viền ảnh trắng · model nổi bật"),
            Triple("gallery", "Gallery", "Passe-partout kiểu ảnh triển lãm"),
            Triple("frosted", "Frosted", "Panel kính mờ trên ảnh"),
            Triple("film", "Film Strip", "Viền phim 35mm · lỗ kéo film"),
            Triple("datestamp", "Date Stamp", "Ngày chụp kiểu máy compact 90s")
        )
        watermarkTemplates.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                rowItems.forEach { (id, title, subtitle) ->
                    WatermarkTemplateCard(
                        id = id, title = title, subtitle = subtitle,
                        brand = watermarkBrand,
                        frameStyle = watermarkStyle,
                        selected = watermarkTemplate == id,
                        onClick = {
                            watermarkTemplate = id
                            prefs.edit().putString("watermark_template", id).apply()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
        }
        Text("Màu khung", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 7.dp, top = 5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("White", "Black").forEach { style ->
                Chip(style, watermarkStyle == style, {
                    watermarkStyle = style
                    prefs.edit().putString("watermark_style", style).apply()
                }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        ToolToggle("Hiện thời gian", "Bật/tắt ngày và giờ trên watermark",
            Icons.Default.Tune, watermarkShowTime) {
            watermarkShowTime = it
            prefs.edit().putBoolean("watermark_show_time", it).apply()
        }
        ToolToggle("Hiện model máy", "Bật/tắt tên model điện thoại",
            Icons.Default.Tune, watermarkShowModel) {
            watermarkShowModel = it
            prefs.edit().putBoolean("watermark_show_model", it).apply()
        }
        ToolToggle("Hiện thông số", "Bật/tắt tiêu cự, khẩu độ, tốc độ màn trập và ISO",
            Icons.Default.Tune, watermarkShowSpecs) {
            watermarkShowSpecs = it
            prefs.edit().putBoolean("watermark_show_specs", it).apply()
        }
        Text("Model tùy chỉnh", color = Color.White, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 15.dp))
        Text("Auto sẽ lấy tên model của điện thoại: ${android.os.Build.MODEL}",
            color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        OutlinedTextField(
            value = customModel,
            onValueChange = { customModel = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("Custom Model (để trống = Auto)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Button(onClick = {
            prefs.edit().putString("watermark_custom_model", customModel.trim()).apply()
        }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text("Save Model")
        }
        Text("Tên model tùy chỉnh được lưu trong dữ liệu ứng dụng và giữ nguyên cho đến khi xóa dữ liệu hoặc gỡ ứng dụng.",
            color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
        Text("Địa điểm watermark (dùng cho mẫu Fuji Signature)", color = Color.White,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
        OutlinedTextField(
            value = customLocation,
            onValueChange = { customLocation = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("Ví dụ: Winnipeg · để trống = ẩn") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Button(onClick = {
            prefs.edit().putString("watermark_custom_location", customLocation.trim()).apply()
        }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text("Save Location")
        }
        Text("Watermark được đóng trực tiếp lên ảnh sau khi chụp. Thông số mm, khẩu độ, shutter và ISO lấy từ camera đang hoạt động khi HAL có cung cấp.",
            color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))

        Spacer(Modifier.height(20.dp))
        Text("Shutter", color = CamColors.TextColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("Hiệu ứng khi bấm chụp", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(top = 5.dp, bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Rửa film", shutterEffect == "Film Wash", {
                shutterEffect = "Film Wash"
                prefs.edit().putString("shutter_effect", "Film Wash").apply()
            }, Modifier.weight(1f))
            Chip("Chớp nhanh", shutterEffect == "Mechanical", {
                shutterEffect = "Mechanical"
                prefs.edit().putString("shutter_effect", "Mechanical").apply()
            }, Modifier.weight(1f))
        }
        ToolToggle("Âm thanh màn trập", "Bật/tắt âm thanh khi chụp; có thể dùng MP3 hoặc tệp âm thanh riêng.",
            Icons.Default.Tune, shutterSound) {
            shutterSound = it
            prefs.edit().putBoolean("shutter_sound_enabled", it).apply()
        }
        Text("Âm thanh có sẵn", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(top = 9.dp, bottom = 6.dp))
        val shutterSoundChoices = listOf(
            "digital" to "Digital", "lens" to "Lens", "click" to "Click",
            "mechanical" to "Mechanical", "classic" to "Classic",
            "nikon" to "Nikon", "vintage" to "Vintage"
        )
        shutterSoundChoices.chunked(2).forEach { soundRow ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                soundRow.forEach { (id, label) ->
                    Chip(label, shutterSoundUri.isNullOrBlank() && shutterSoundAsset == id, {
                        shutterSoundAsset = id
                        shutterSoundUri = null
                        prefs.edit().putString("shutter_sound_asset", id).remove("shutter_sound_uri").apply()
                    }, Modifier.weight(1f))
                }
                if (soundRow.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { shutterSoundPicker.launch(arrayOf("audio/mpeg", "audio/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (shutterSoundUri.isNullOrBlank()) "Nhập âm thanh / MP3" else "Đổi âm thanh")
            }
            if (!shutterSoundUri.isNullOrBlank()) {
                TextButton(onClick = {
                    shutterSoundUri = null
                    prefs.edit().remove("shutter_sound_uri").putString("shutter_sound_asset", shutterSoundAsset).apply()
                    Toast.makeText(context, "Đã quay về âm thanh mặc định.", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Mặc định")
                }
            }
        }
        Text(
            if (shutterSoundUri.isNullOrBlank()) {
                "Đang dùng âm thanh màn trập đã chọn trong danh sách."
            } else {
                "Đã chọn tệp âm thanh tùy chỉnh. Quyền đọc được lưu để dùng sau khi mở lại ứng dụng."
            },
            color = CamColors.Sub,
            fontSize = 10.sp
        )

    }
}


@Composable
private fun CameraFrameSettingsPanel() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE) }
    val contentPrefs = remember { context.getSharedPreferences("fujifilm_contents", Context.MODE_PRIVATE) }
    var selectedTemplate by remember {
        mutableStateOf(prefs.getString("selected_frame_template", "none") ?: "none")
    }
    var selectedStyle by remember {
        mutableStateOf(prefs.getString("frame_style", "White") ?: "White")
    }
    val customFrames = remember {
        mutableStateListOf<String>().apply {
            addAll(contentPrefs.getStringSet("frames", emptySet())?.toList()?.sorted() ?: emptyList())
        }
    }
    val builtIn = listOf(
        "none" to "Tắt frame",
        "classic" to "Classic Card",
        "clean" to "Clean Corners",
        "film" to "35mm Film",
        "polaroid" to "Instant / Polaroid",
        "logo" to "Center Logo",
        "expo_around" to "Expo Around",
        "expo_left" to "Expo Left",
        "expo_right" to "Expo Right",
        "double" to "Double Line"
    )
    val allFrames = builtIn + customFrames.map { uri ->
        "custom|$uri" to documentDisplayName(context, Uri.parse(uri))
    }

    Spacer(Modifier.height(20.dp))
    Text("Photo Frame", color = CamColors.TextColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    Text(
        "Chọn frame tại đây. Frame được đóng lên ảnh sau khi chụp; không còn nút Frame ở màn hình chụp.",
        color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
    )
    allFrames.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            row.forEach { (id, title) ->
                CameraFrameTemplateCard(
                    id = id,
                    title = title,
                    frameStyle = selectedStyle,
                    selected = selectedTemplate == id,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedTemplate = id
                        prefs.edit().putString("selected_frame_template", id).apply()
                    }
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
    Text("Màu frame", color = CamColors.Sub, fontSize = 12.sp,
        modifier = Modifier.padding(top = 5.dp, bottom = 7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("White", "Black", "Transparent").forEach { style ->
            Chip(style, selectedStyle == style, {
                selectedStyle = style
                prefs.edit().putString("frame_style", style).apply()
            }, Modifier.weight(1f))
        }
    }
    Text(
        "Frame có viền trắng hoặc đen; ảnh PNG nhập vào vẫn giữ độ trong suốt gốc.",
        color = CamColors.Sub, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp)
    )
    if (customFrames.isEmpty()) {
        Text("Muốn thêm frame PNG/JPG, vào Contents Management → Frame Manage → Import Frame.",
            color = CamColors.Sub, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CameraFrameTemplateCard(
    id: String,
    title: String,
    frameStyle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val lineColor = when (frameStyle) {
        "Black" -> Color.Black
        "Transparent" -> Color.Transparent
        else -> Color.White
    }
    val fillColor = when (frameStyle) {
        "Black" -> Color.Black
        "White" -> Color.White
        else -> Color.Transparent
    }
    val textColor = if (frameStyle == "White") Color.Black else Color.White
    Column(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0x553F3822) else CamColors.Card)
            .border(1.dp, if (selected) CamColors.Yellow else CamColors.Stroke, RoundedCornerShape(12.dp))
            .animaTClickable(onClick = onClick)
            .padding(7.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF536C60))
        ) {
            ComposeCanvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFF748678))
                drawCircle(Color(0xFFFFD79A), size.minDimension * 0.10f,
                    Offset(size.width * 0.78f, size.height * 0.24f))
                drawLine(Color(0xFF344D41), Offset(0f, size.height * 0.64f),
                    Offset(size.width, size.height * 0.57f), size.height * 0.22f)
                drawLine(Color(0xFF293B32), Offset(0f, size.height * 0.86f),
                    Offset(size.width, size.height * 0.76f), size.height * 0.23f)
                if (!id.startsWith("custom|") && id != "none" && frameStyle != "Transparent") {
                    val inset = size.minDimension * 0.075f
                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = (size.minDimension * 0.025f).coerceAtLeast(1.2f)
                    )
                    when (id) {
                        "clean" -> {
                            val arm = size.minDimension * 0.16f
                            for (sx in listOf(1f, -1f)) for (sy in listOf(1f, -1f)) {
                                val x = if (sx > 0f) inset else size.width - inset
                                val y = if (sy > 0f) inset else size.height - inset
                                drawLine(lineColor, Offset(x, y), Offset(x + arm * sx, y), strokeWidth = stroke.width)
                                drawLine(lineColor, Offset(x, y), Offset(x, y + arm * sy), strokeWidth = stroke.width)
                            }
                        }
                        "film" -> {
                            drawRect(lineColor, topLeft = Offset(inset, inset),
                                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                                style = stroke)
                            val holeW = size.width * 0.045f
                            for (n in 0..8) {
                                val x = size.width * 0.08f + n * size.width * 0.105f
                                drawRoundRect(lineColor, Offset(x, 2f), androidx.compose.ui.geometry.Size(holeW, 4f))
                                drawRoundRect(lineColor, Offset(x, size.height - 6f), androidx.compose.ui.geometry.Size(holeW, 4f))
                            }
                        }
                        "logo" -> {
                            drawRect(lineColor, topLeft = Offset(inset, inset),
                                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                                style = stroke)
                            drawLine(lineColor, Offset(size.width * 0.34f, size.height * 0.50f),
                                Offset(size.width * 0.66f, size.height * 0.50f), strokeWidth = stroke.width)
                        }
                        "expo_left", "expo_right" -> {
                            val band = size.width * 0.10f
                            drawRect(fillColor, topLeft = Offset(if (id == "expo_left") 0f else size.width - band, 0f),
                                size = androidx.compose.ui.geometry.Size(band, size.height))
                            drawRect(lineColor, topLeft = Offset(inset, inset),
                                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                                style = stroke)
                        }
                        "polaroid" -> {
                            drawRect(fillColor, topLeft = Offset(inset, inset),
                                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2))
                            drawRect(Color(0xFF748678), topLeft = Offset(inset * 1.6f, inset * 1.6f),
                                size = androidx.compose.ui.geometry.Size(size.width - inset * 3.2f, size.height - inset * 4f))
                        }
                        else -> {
                            drawRect(lineColor, topLeft = Offset(inset, inset),
                                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                                style = stroke)
                            if (id == "double" || id == "expo_around") {
                                drawRect(lineColor, topLeft = Offset(inset * 1.8f, inset * 1.8f),
                                    size = androidx.compose.ui.geometry.Size(size.width - inset * 3.6f, size.height - inset * 3.6f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke.width * 0.65f))
                            }
                        }
                    }
                }
            }
            if (id.startsWith("custom|")) {
                Text("PNG / JPG", color = Color.White, fontSize = 8.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).background(Color(0xAA000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp))
            }
            if (id == "none") {
                Text("ORIGINAL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center).background(Color(0xAA000000)).padding(5.dp))
            }
        }
        Text(title, color = if (selected) CamColors.Yellow else CamColors.TextColor,
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            modifier = Modifier.padding(top = 6.dp))
        Text(if (id.startsWith("custom|")) "Imported frame" else "Preview · $frameStyle",
            color = CamColors.Sub, fontSize = 9.sp, maxLines = 1)
    }
}




private val cameraWatermarkBrands = listOf(
    "fujifilm" to "Fujifilm",
    "sony" to "Sony",
    "nikon" to "Nikon",
    "canon" to "Canon",
    "leica" to "Leica",
    "panasonic" to "LUMIX",
    "ricoh" to "RICOH",
    "hasselblad" to "Hasselblad",
    "kodak" to "Kodak",
    "zeiss" to "ZEISS",
    "gopro" to "GoPro",
    "dji" to "DJI",
    "insta360" to "Insta360"
)

private fun cameraWatermarkBrandTitle(brand: String): String = when (brand.lowercase()) {
    "sony" -> "SONY"
    "nikon" -> "NIKON"
    "canon" -> "Canon"
    "leica" -> "LEICA"
    "panasonic" -> "LUMIX"
    "ricoh" -> "RICOH"
    "hasselblad" -> "HASSELBLAD"
    "kodak" -> "KODAK"
    "zeiss" -> "ZEISS"
    "gopro" -> "GoPro"
    "dji" -> "DJI"
    "insta360" -> "Insta360"
    else -> "FUJIFILM"
}

private fun cameraWatermarkBrandAccent(brand: String): Int = when (brand.lowercase()) {
    "sony" -> AndroidColor.rgb(65, 78, 91)
    "nikon" -> AndroidColor.rgb(255, 210, 0)
    "canon" -> AndroidColor.rgb(205, 0, 34)
    "leica" -> AndroidColor.rgb(220, 20, 35)
    "panasonic" -> AndroidColor.rgb(0, 66, 125)
    "ricoh" -> AndroidColor.rgb(218, 25, 35)
    "hasselblad" -> AndroidColor.rgb(137, 105, 45)
    "kodak" -> AndroidColor.rgb(205, 26, 34)
    "zeiss" -> AndroidColor.rgb(0, 128, 184)
    "gopro" -> AndroidColor.rgb(0, 174, 239)
    "dji" -> AndroidColor.rgb(46, 54, 62)
    "insta360" -> AndroidColor.rgb(35, 174, 145)
    else -> AndroidColor.rgb(0, 91, 145)
}

private fun cameraWatermarkBrandAssetPath(brand: String): String? = when (brand.lowercase()) {
    "gopro" -> "watermark-brands/gopro.webp"
    "hasselblad" -> "watermark-brands/hasselblad.webp"
    "insta360" -> "watermark-brands/insta360.webp"
    "kodak" -> "watermark-brands/kodak.webp"
    "panasonic", "lumix" -> "watermark-brands/lumix.webp"
    "zeiss" -> "watermark-brands/zeiss.webp"
    "dji" -> "watermark-brands/dji.webp"
    else -> null
}

private fun cameraWatermarkBrandLogoResource(brand: String): Int? = when (brand.lowercase()) {
    "sony" -> R.drawable.sony_wordmark
    "nikon" -> R.drawable.nikon_wordmark
    "canon" -> R.drawable.canon_wordmark
    "fujifilm" -> R.drawable.fujifilm_wordmark
    else -> null
}

private fun cameraWatermarkExampleModel(brand: String): String = when (brand.lowercase()) {
    "sony" -> "ILCE-7M4"
    "nikon" -> "Z8"
    "canon" -> "EOS R5"
    "leica" -> "M11"
    "panasonic" -> "LUMIX S5II"
    "ricoh" -> "GR III"
    "hasselblad" -> "X2D 100C"
    "kodak" -> "PIXPRO"
    "zeiss" -> "ZX1"
    "gopro" -> "HERO"
    "dji" -> "Osmo"
    "insta360" -> "X4"
    else -> "X-T5"
}

@Composable
private fun WatermarkBrandMark(
    brand: String,
    modifier: Modifier = Modifier,
    fallbackColor: Color = Color.Unspecified
) {
    val logoResource = cameraWatermarkBrandLogoResource(brand)
    val assetPath = cameraWatermarkBrandAssetPath(brand)
    val context = LocalContext.current
    val assetBitmap = remember(context, assetPath) {
        assetPath?.let { path -> CameraAssetStore.bitmap(context, path)?.asImageBitmap() }
    }
    if (logoResource != null) {
        Image(
            painter = painterResource(id = logoResource),
            contentDescription = "${cameraWatermarkBrandTitle(brand)} watermark wordmark",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else if (assetBitmap != null) {
        Image(
            bitmap = assetBitmap,
            contentDescription = "${cameraWatermarkBrandTitle(brand)} watermark wordmark",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text = cameraWatermarkBrandTitle(brand),
                color = if (fallbackColor == Color.Unspecified) Color(cameraWatermarkBrandAccent(brand)) else fallbackColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WatermarkTemplateCard(
    id: String,
    title: String,
    subtitle: String,
    brand: String,
    frameStyle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewModel = cameraWatermarkExampleModel(brand)
    val brandAccent = Color(cameraWatermarkBrandAccent(brand))
    Column(
        modifier.clip(RoundedCornerShape(13.dp))
            .background(if (selected) Color(0xFF333126) else CamColors.Card)
            .border(
                width = if (selected) 1.5.dp else 0.7.dp,
                color = if (selected) CamColors.Yellow else Color(0x334C535B),
                shape = RoundedCornerShape(13.dp)
            )
            .animaTClickable(onClick = onClick)
            .padding(7.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(88.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF475550))
        ) {
            val panelColor = when (id) {
                "dark", "film" -> Color(0xFF121416)
                "vintage" -> Color(0xFFECE3CF)
                "polaroid" -> Color(0xFFFAF9F5)
                "gallery" -> Color(0xFFF3F1EC)
                "frosted" -> Color.White.copy(alpha = 0.24f)
                else -> when (frameStyle) {
                    "White" -> Color.White
                    "Black" -> Color.Black
                    "LiquidGlass" -> Color.White.copy(alpha = 0.20f)
                    else -> Color.Transparent
                }
            }
            val textColor = when (id) {
                "dark", "film" -> Color.White
                "vintage" -> Color(0xFF4B3A29)
                "polaroid", "gallery" -> Color(0xFF222222)
                else -> if (frameStyle == "White") Color.Black else Color.White
            }
            val frameColor = when (frameStyle) {
                "Black" -> Color.Black
                "Transparent" -> Color.Transparent
                else -> Color.White
            }
            ComposeCanvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFF697A70))
                drawCircle(Color(0xFFFFD8A0), size.minDimension * 0.13f,
                    Offset(size.width * 0.76f, size.height * 0.28f))
                drawLine(Color(0xFF526A59), Offset(0f, size.height * 0.64f),
                    Offset(size.width, size.height * 0.52f), strokeWidth = size.height * 0.25f)
                drawLine(Color(0xFF31483E), Offset(0f, size.height * 0.82f),
                    Offset(size.width, size.height * 0.72f), strokeWidth = size.height * 0.32f)
            }
            if (id == "minimal") {
                Text(
                    "$previewModel · 35mm · F2.8 · 1/250s · ISO 100",
                    color = textColor, fontSize = 6.sp, maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(5.dp).background(panelColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 7.dp)
                )
            } else if (id == "simple") {
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(5.dp)
                        .background(panelColor, RoundedCornerShape(5.dp)).padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    WatermarkBrandMark(
                        brand = brand,
                        modifier = Modifier.width(58.dp).height(12.dp),
                        fallbackColor = brandAccent
                    )
                    Text("$previewModel", color = textColor, fontSize = 6.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            } else if (id == "datestamp") {
                Text(
                    "24 11 17",
                    color = Color(0xFFFF9637), fontSize = 8.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                )
            } else if (id == "micro") {
                Text(
                    "$previewModel · 35mm F2.8 · 1/250s · ISO 100",
                    color = textColor, fontSize = 5.3.sp, maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(5.dp)
                        .background(panelColor, RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 4.dp)
                )
            } else {
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(5.dp)
                        .background(panelColor, RoundedCornerShape(5.dp))
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(Modifier.weight(0.56f), verticalAlignment = Alignment.CenterVertically) {
                        WatermarkBrandMark(
                            brand = brand,
                            modifier = Modifier.weight(0.38f).height(15.dp)
                                .background(Color.White, RoundedCornerShape(2.dp)).padding(2.dp),
                            fallbackColor = brandAccent
                        )
                        Column(
                            Modifier.weight(0.62f).padding(start = 3.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("MODEL · $previewModel", color = textColor,
                                fontSize = 5.9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("FE 24–70mm", color = textColor.copy(alpha = 0.82f),
                                fontSize = 5.4.sp, maxLines = 1)
                        }
                    }
                    Box(
                        Modifier.padding(horizontal = 4.dp).width(1.dp).height(28.dp)
                            .background(brandAccent.copy(alpha = if (frameStyle == "Transparent") 0.95f else 0.72f))
                    )
                    Column(Modifier.weight(0.44f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("35mm · F2.8 · 1/250s · ISO 100", color = textColor,
                            fontSize = 5.7.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text("2026.11.17", color = textColor.copy(alpha = 0.82f),
                            fontSize = 5.5.sp, maxLines = 1)
                    }
                }
            }
        }
        Text(title, color = if (selected) CamColors.Yellow else Color.White,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp))
        Text(subtitle, color = CamColors.Sub, fontSize = 9.sp,
            modifier = Modifier.padding(top = 1.dp))
    }
}

/**
 * Reads the legacy MediaStore rotation only as a fallback when JPEG EXIF is neutral.
 * CameraX writes the authoritative EXIF orientation; never apply both rotations.
 */
private fun readMediaStoreRotation(context: Context, uri: Uri): Int {
    return runCatching {
        context.contentResolver.query(
            uri, arrayOf(MediaStore.Images.Media.ORIENTATION), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        } ?: 0
    }.getOrDefault(0).let { ((it % 360) + 360) % 360 }
}

private fun readExifOrientation(context: Context, uri: Uri): Int {
    return runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
}

/**
 * Returns the authoritative pixel-orientation transform. Some camera HALs leave JPEG EXIF
 * neutral and publish the rotation through MediaStore instead; treat both sources identically.
 */
private fun readEffectiveImageOrientation(context: Context, uri: Uri): Int {
    val exif = readExifOrientation(context, uri)
    if (exif != ExifInterface.ORIENTATION_NORMAL && exif != ExifInterface.ORIENTATION_UNDEFINED) {
        return exif
    }
    return when (readMediaStoreRotation(context, uri)) {
        90 -> ExifInterface.ORIENTATION_ROTATE_90
        180 -> ExifInterface.ORIENTATION_ROTATE_180
        270 -> ExifInterface.ORIENTATION_ROTATE_270
        else -> ExifInterface.ORIENTATION_NORMAL
    }
}

/**
 * Decodes a JPEG with orientation applied to pixels. The output dimensions and pixels
 * therefore agree, independent of the viewer or MediaStore thumbnail implementation.
 */
fun decodeUprightBitmap(context: Context, uri: Uri, maxDimension: Int = 0): Bitmap? {
    val orientation = readEffectiveImageOrientation(context, uri)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    if (maxDimension > 0) {
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension) {
            sampleSize *= 2
        }
    }
    val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.Options().let { options ->
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inMutable = true
            options.inSampleSize = sampleSize
            BitmapFactory.decodeStream(input, null, options)
        }
    } ?: return null

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
    }
    return if (orientation == ExifInterface.ORIENTATION_NORMAL ||
        orientation == ExifInterface.ORIENTATION_UNDEFINED) decoded
    else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}

private fun decodeRawPixelBitmap(context: Context, uri: Uri): Bitmap? {
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inMutable = true
    }
    return context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
}

/** RGB_565 watermark surface halves the working-set size on 50–108MP LunaDream images. */
private fun decodeWatermarkPixelBitmap(context: Context, uri: Uri): Bitmap? {
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.RGB_565
        inMutable = true
        inDither = true
    }
    return context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
}

private fun setMediaStoreOrientationUpright(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Images.Media.ORIENTATION, 0) },
            null,
            null
        )
    }
}

/**
 * Physically writes the JPEG in its final upright pixel orientation before any look or watermark.
 *
 * Prefer the orientation actually stored by the JPEG/MediaStore. Only when both are neutral do
 * we use CameraX's sensor-relative rotation, which includes the active camera's SENSOR_ORIENTATION
 * and the frozen display target rotation. No width-vs-height 90-degree guess is made here.
 */
private data class CaptureOrientationCandidate(
    val source: String,
    val degrees: Int,
    val flipHorizontal: Boolean,
    val aspectError: Float
)

private fun aspectErrorAfterRotation(
    width: Int,
    height: Int,
    degrees: Int,
    expectedAspectRatio: Float
): Float {
    val swapsAxes = ((degrees % 180) + 180) % 180 != 0
    val outputWidth = if (swapsAxes) height else width
    val outputHeight = if (swapsAxes) width else height
    val actual = outputWidth.toFloat() / outputHeight.coerceAtLeast(1)
    return kotlin.math.abs(actual / expectedAspectRatio - 1f)
}

private fun writeUprightExifMetadata(
    context: Context,
    uri: Uri,
    width: Int,
    height: Int
): Boolean {
    return runCatching {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "rw")
            ?: return@runCatching false
        descriptor.use {
            ExifInterface(it.fileDescriptor).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                setAttribute(ExifInterface.TAG_THUMBNAIL_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, width.toString())
                setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, height.toString())
                setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
                setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
                saveAttributes()
            }
        }
        true
    }.onFailure {
        android.util.Log.e("FUJI_ORIENTATION", "Could not write upright EXIF dimensions", it)
    }.getOrDefault(false)
}

/**
 * Finalize the JPEG after pixels, filters and watermark are complete.
 * Synchronize EXIF pixel dimensions and MediaStore's cached dimensions/orientation,
 * then publish the MediaStore row so gallery apps index the final file, not the
 * temporary camera JPEG metadata.
 */
/**
 * Publish a CameraX JPEG_R file without decoding or recompressing it.
 * Re-encoding through BitmapFactory/Bitmap.compress would discard its Ultra HDR gain map.
 * Keep the EXIF orientation supplied by CameraX and only publish MediaStore metadata.
 */
fun publishUltraHdrCapture(context: Context, uri: Uri): Boolean {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching false

        val exifOrientation = context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        val rotationDegrees = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSPOSE -> 90
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSVERSE -> 270
            else -> 0
        }
        val published = context.contentResolver.update(
            uri,
            ContentValues().apply {
                put(MediaStore.Images.Media.ORIENTATION, rotationDegrees)
                put(MediaStore.Images.Media.WIDTH, bounds.outWidth)
                put(MediaStore.Images.Media.HEIGHT, bounds.outHeight)
                put(MediaStore.Images.Media.IS_PENDING, 0)
            },
            null,
            null
        ) > 0
        android.util.Log.d(
            "FUJI_ULTRA_HDR",
            "published=$published, exifOrientation=$exifOrientation, rotation=$rotationDegrees, pixels=${bounds.outWidth}x${bounds.outHeight}; gain map bytes preserved"
        )
        published
    }.onFailure {
        android.util.Log.e("FUJI_ULTRA_HDR", "Could not publish original JPEG_R capture", it)
    }.getOrDefault(false)
}

fun finalizeCapturedImageMetadata(context: Context, uri: Uri): Boolean {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

    val exifOk = writeUprightExifMetadata(context, uri, bounds.outWidth, bounds.outHeight)
    val orientationAndPublishOk = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.ORIENTATION, 0)
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        context.contentResolver.update(uri, values, null, null) > 0
    }.getOrDefault(false)

    // WIDTH/HEIGHT are maintained by MediaStore on some Android builds. Update them
    // explicitly where supported; EXIF dimensions above remain the portable fallback.
    runCatching {
        context.contentResolver.update(
            uri,
            ContentValues().apply {
                put(MediaStore.Images.Media.WIDTH, bounds.outWidth)
                put(MediaStore.Images.Media.HEIGHT, bounds.outHeight)
            },
            null,
            null
        )
    }

    android.util.Log.d(
        "FUJI_ORIENTATION",
        "published=" + orientationAndPublishOk + ", exif=" + exifOk +
            ", finalPixels=" + bounds.outWidth + "x" + bounds.outHeight
    )
    return exifOk && orientationAndPublishOk
}

/**
 * CameraX can return the sensor's default still ratio even when the preview ViewPort uses
 * a narrower/wider crop. Enforce the selected ratio on the upright pixels before LunaDream runs,
 * otherwise later upscaling simply magnifies the wrong 4:3/16:9 frame.
 */
private fun cropBitmapToAspect(source: Bitmap, targetAspect: Float): Bitmap {
    if (!targetAspect.isFinite() || targetAspect <= 0f) return source
    val sourceAspect = source.width.toFloat() / source.height.coerceAtLeast(1)
    if (kotlin.math.abs(sourceAspect / targetAspect - 1f) <= 0.0005f) return source

    var cropWidth = source.width
    var cropHeight = source.height
    if (sourceAspect > targetAspect) {
        cropWidth = (source.height * targetAspect).roundToInt().coerceIn(2, source.width)
    } else {
        cropHeight = (source.width / targetAspect).roundToInt().coerceIn(2, source.height)
    }
    // Keep the crop centered, matching CameraX FILL_CENTER composition rather than stretching.
    val left = ((source.width - cropWidth) / 2).coerceAtLeast(0)
    val top = ((source.height - cropHeight) / 2).coerceAtLeast(0)
    return Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
}

/**
 * Normalize the pixels to the selected capture aspect ratio.
 *
 * The JPEG may already contain correctly oriented landscape pixels while its EXIF tag
 * still requests a 90/270-degree turn. In that case, the pixel aspect ratio is the
 * decisive evidence: keep the pixels as-is and clear the stale orientation metadata.
 * EXIF and CameraX rotations are alternatives; they are never added together.
 */
fun normalizeCapturedImage(
    context: Context,
    uri: Uri,
    fallbackRotationDegrees: Int = 0,
    expectedAspectRatio: Float = 1f
): Boolean {
    return runCatching {
        val rawBitmap = decodeRawPixelBitmap(context, uri) ?: return@runCatching false
        val safeExpectedAspect = expectedAspectRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
        val cameraXDegrees = ((fallbackRotationDegrees % 360) + 360) % 360

        val exifOrientation = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        // EXIF 5/7 are mirrored transpose variants. Standard mapping: transpose=90,
        // transverse=270, matching the rotation-plus-mirror mapping used by the decoder.
        val exifDegrees = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSPOSE -> 90
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSVERSE -> 270
            else -> 0
        }
        val exifFlipped = exifOrientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL ||
            exifOrientation == ExifInterface.ORIENTATION_FLIP_VERTICAL ||
            exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE
        val hasExifTransform = exifOrientation != ExifInterface.ORIENTATION_NORMAL &&
            exifOrientation != ExifInterface.ORIENTATION_UNDEFINED

        val mediaStoreDegrees = if (hasExifTransform) 0 else readMediaStoreRotation(context, uri)
        val metadataCandidate = when {
            hasExifTransform -> CaptureOrientationCandidate(
                "EXIF", exifDegrees, exifFlipped,
                aspectErrorAfterRotation(rawBitmap.width, rawBitmap.height, exifDegrees, safeExpectedAspect)
            )
            mediaStoreDegrees != 0 -> CaptureOrientationCandidate(
                "MediaStore", mediaStoreDegrees, false,
                aspectErrorAfterRotation(rawBitmap.width, rawBitmap.height, mediaStoreDegrees, safeExpectedAspect)
            )
            else -> null
        }
        val rawCandidate = CaptureOrientationCandidate(
            "Raw pixels", 0, false,
            aspectErrorAfterRotation(rawBitmap.width, rawBitmap.height, 0, safeExpectedAspect)
        )
        val cameraCandidate = CaptureOrientationCandidate(
            "CameraX", cameraXDegrees, false,
            aspectErrorAfterRotation(rawBitmap.width, rawBitmap.height, cameraXDegrees, safeExpectedAspect)
        )

        // 6% tolerance covers small even-pixel crop rounding from JPEG encoders.
        val aspectTolerance = 0.06f
        val selected = when {
            // Preserve explicit 180-degree and mirrored EXIF transforms when geometry agrees.
            metadataCandidate?.let {
                it.aspectError <= aspectTolerance &&
                    (it.degrees == 180 || it.flipHorizontal)
            } == true -> metadataCandidate
            // Preserve CameraX's 180-degree correction when EXIF and MediaStore are neutral.
            !hasExifTransform && mediaStoreDegrees == 0 &&
                cameraXDegrees == 180 && cameraCandidate.aspectError <= aspectTolerance -> cameraCandidate
            // If the JPEG pixels already match the requested frame, clear stale rotation tags.
            rawCandidate.aspectError <= aspectTolerance -> rawCandidate
            // When axes must swap, prefer the frozen CameraX sensor-to-display rotation.
            // EXIF 90 and 270 have identical aspect errors, so geometry alone cannot choose
            // the clockwise direction. CameraX's target rotation identifies that direction.
            cameraCandidate.aspectError <= aspectTolerance -> cameraCandidate
            // Fall back to EXIF/MediaStore only when CameraX rotation does not fit the frame.
            metadataCandidate?.aspectError?.let { it <= aspectTolerance } == true -> metadataCandidate
            else -> listOfNotNull(cameraCandidate, metadataCandidate, rawCandidate)
                .minByOrNull { it.aspectError } ?: rawCandidate
        }

        val matrix = Matrix().apply {
            if (selected.flipHorizontal) preScale(-1f, 1f)
            if (selected.degrees != 0) postRotate(selected.degrees.toFloat())
        }
        val orientedBitmap = if (selected.degrees == 0 && !selected.flipHorizontal) {
            rawBitmap
        } else {
            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        }
        val finalBitmap = cropBitmapToAspect(orientedBitmap, safeExpectedAspect)
        try {
            val outputWritten = context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 98, output)
            } ?: false
            if (!outputWritten) return@runCatching false

            val exifWritten = writeUprightExifMetadata(
                context, uri, finalBitmap.width, finalBitmap.height
            )
            setMediaStoreOrientationUpright(context, uri)

            val finalAspect = finalBitmap.width.toFloat() / finalBitmap.height.coerceAtLeast(1)
            android.util.Log.d(
                "FUJI_ORIENTATION",
                "source=" + selected.source + ", degrees=" + selected.degrees +
                    ", raw=" + rawBitmap.width + "x" + rawBitmap.height +
                    ", oriented=" + orientedBitmap.width + "x" + orientedBitmap.height +
                    ", final=" + finalBitmap.width + "x" + finalBitmap.height +
                    ", expectedAspect=" + safeExpectedAspect +
                    ", finalAspect=" + finalAspect +
                    ", orientationError=" + selected.aspectError + ", exifWritten=" + exifWritten
            )
            exifWritten
        } finally {
            if (finalBitmap !== orientedBitmap && !finalBitmap.isRecycled) finalBitmap.recycle()
            if (orientedBitmap !== rawBitmap && !orientedBitmap.isRecycled) orientedBitmap.recycle()
            if (!rawBitmap.isRecycled) rawBitmap.recycle()
        }
    }.onFailure {
        android.util.Log.e("FUJI_ORIENTATION", "normalizeCapturedImage failed", it)
    }.getOrDefault(false)
}

private data class CubeLut(
    val size: Int,
    val domainMin: FloatArray,
    val domainMax: FloatArray,
    val values: FloatArray
) {
    private fun value(x: Int, y: Int, z: Int, channel: Int): Float =
        values[((z * size + y) * size + x) * 3 + channel]

    private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t

    fun sample(red: Float, green: Float, blue: Float): Triple<Float, Float, Float> {
        fun coordinate(input: Float, channel: Int): Pair<Int, Float> {
            val span = (domainMax[channel] - domainMin[channel]).coerceAtLeast(0.00001f)
            val normalized = ((input - domainMin[channel]) / span).coerceIn(0f, 1f) * (size - 1)
            val low = normalized.toInt().coerceIn(0, size - 1)
            return low to (normalized - low).coerceIn(0f, 1f)
        }
        val (r0, rt) = coordinate(red, 0)
        val (g0, gt) = coordinate(green, 1)
        val (b0, bt) = coordinate(blue, 2)
        val r1 = (r0 + 1).coerceAtMost(size - 1)
        val g1 = (g0 + 1).coerceAtMost(size - 1)
        val b1 = (b0 + 1).coerceAtMost(size - 1)
        fun channel(c: Int): Float {
            val c00 = mix(value(r0, g0, b0, c), value(r1, g0, b0, c), rt)
            val c10 = mix(value(r0, g1, b0, c), value(r1, g1, b0, c), rt)
            val c01 = mix(value(r0, g0, b1, c), value(r1, g0, b1, c), rt)
            val c11 = mix(value(r0, g1, b1, c), value(r1, g1, b1, c), rt)
            return mix(mix(c00, c10, gt), mix(c01, c11, gt), bt).coerceIn(0f, 1f)
        }
        return Triple(channel(0), channel(1), channel(2))
    }
}

private fun parseCubeLutText(text: String): CubeLut? {
    return runCatching {
        var size = 0
        var oneDSize = 0
        val min = floatArrayOf(0f, 0f, 0f)
        val max = floatArrayOf(1f, 1f, 1f)
        val rows = ArrayList<FloatArray>()
        text.lineSequence().forEach { raw ->
            val line = raw.substringBefore("#").trim()
            if (line.isEmpty()) return@forEach
            val parts = line.split(Regex("\\s+"))
            when (parts.firstOrNull()) {
                "TITLE" -> Unit
                "LUT_1D_SIZE" -> oneDSize = parts.getOrNull(1)?.toIntOrNull() ?: 0
                "LUT_3D_SIZE" -> size = parts.getOrNull(1)?.toIntOrNull() ?: 0
                "DOMAIN_MIN" -> if (parts.size >= 4) for (i in 0..2) min[i] = parts[i + 1].toFloat()
                "DOMAIN_MAX" -> if (parts.size >= 4) for (i in 0..2) max[i] = parts[i + 1].toFloat()
                else -> if (parts.size >= 3) {
                    val rgb = parts.take(3).mapNotNull { it.toFloatOrNull() }
                    if (rgb.size == 3) rows.add(floatArrayOf(rgb[0], rgb[1], rgb[2]))
                }
            }
        }
        if (size !in 2..65) return@runCatching null
        val expected3dRows = size * size * size
        val cubeRows = when {
            rows.size == expected3dRows -> rows
            oneDSize in 2..65536 && rows.size == expected3dRows + oneDSize ->
                rows.drop(oneDSize)
            else -> return@runCatching null
        }
        if (cubeRows.any { rgb -> rgb.any { !it.isFinite() } }) return@runCatching null
        if ((0..2).any { max[it] <= min[it] || !min[it].isFinite() || !max[it].isFinite() }) {
            return@runCatching null
        }
        CubeLut(size, min, max, FloatArray(cubeRows.size * 3).also { out ->
            cubeRows.forEachIndexed { index, rgb ->
                out[index * 3] = rgb[0]
                out[index * 3 + 1] = rgb[1]
                out[index * 3 + 2] = rgb[2]
            }
        })
    }.getOrNull()
}

/**
 * Canon catalog LUTs are raw RGB8 33x33x33 volumes (the same byte layout used by
 * WebGL RGB8 3D textures). Red is the fastest-varying lattice axis, matching CubeLut.value().
 */
private const val CANON_LUT_SIZE = 33
private const val CANON_LUT_BYTE_COUNT = CANON_LUT_SIZE * CANON_LUT_SIZE * CANON_LUT_SIZE * 3

private fun parseCanonBinaryLut(bytes: ByteArray): CubeLut? {
    if (bytes.size != CANON_LUT_BYTE_COUNT) return null
    val values = FloatArray(bytes.size) { index ->
        (bytes[index].toInt() and 0xFF) / 255f
    }
    return CubeLut(
        CANON_LUT_SIZE,
        floatArrayOf(0f, 0f, 0f),
        floatArrayOf(1f, 1f, 1f),
        values
    )
}

private data class LutImportEntry(
    val name: String,
    val category: String,
    val bytes: ByteArray,
    val binaryRgb33: Boolean
)

/** Custom LUTs remain in app-private storage; source document permissions are not needed after import. */
data class CustomCameraLut(
    val id: String,
    val name: String,
    val category: String = "LUT tùy chỉnh"
)

private const val CUSTOM_CAMERA_LUT_PREFIX = "camera_lut:"
private const val CUSTOM_CAMERA_LUT_IDS = "camera_lut_ids"
private const val ACTIVE_CAMERA_LUT_IDS = "active_camera_lut_ids"
private const val MAX_LUT_INPUT_BYTES = 256L * 1024L * 1024L
private const val MAX_LUT_ENTRY_BYTES = 8 * 1024 * 1024
private const val MAX_LUT_ARCHIVE_ENTRIES = 1500
private const val MAX_LUT_EXPANDED_BYTES = 320L * 1024L * 1024L
private const val MAX_LUTS_PER_IMPORT = 700
private val customCameraLutCache = LinkedHashMap<String, CubeLut>()

private data class LutImportBudget(var entries: Int = 0, var expandedBytes: Long = 0L)

private fun readBoundedLutBytes(input: InputStream, maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (count == 0) continue
        total += count
        require(total <= maxBytes) { "Tệp LUT vượt quá giới hạn dung lượng cho phép." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun catalogLutCategory(path: String): String {
    val parts = path.replace('\\', '/').split('/').filter { it.isNotBlank() }
    val folder = parts.firstOrNull {
        it.matches(Regex("\\d{2}_[A-Za-z0-9_-]+"))
    }?.substringAfter('_')?.replace('_', ' ')
    if (folder != null) return "Canon · $folder"
    if (path.contains("Panasonic-Standard", true)) return "RAW/Log · Panasonic adapter"
    val brand = parts.firstOrNull {
        it.equals("Fujifilm", true) || it.equals("Kodak", true) ||
            it.equals("Leica", true) || it.equals("Hasselblad", true) ||
            it.equals("Nikon", true) || it.equals("Arri", true) ||
            it.equals("RED", true) || it.equals("Cineon", true)
    }
    if (brand != null) {
        val logInput = path.contains("V-Log", true) || path.contains("VLog", true) ||
            path.contains("N-Log", true) || path.contains("FLog", true) ||
            path.contains("LogC", true) || path.contains("Cineon", true)
        return if (logInput) "RAW/Log · $brand" else brand
    }
    return "LUT nhập"
}

private fun scanLutZip(
    archiveBytes: ByteArray,
    depth: Int,
    budget: LutImportBudget
): List<LutImportEntry> {
    require(depth <= 3) { "ZIP LUT lồng quá nhiều cấp." }
    val found = ArrayList<LutImportEntry>()
    ZipInputStream(ByteArrayInputStream(archiveBytes)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            budget.entries += 1
            require(budget.entries <= MAX_LUT_ARCHIVE_ENTRIES) {
                "ZIP chứa quá nhiều tệp."
            }
            if (!entry.isDirectory) {
                val bytes = readBoundedLutBytes(zip, MAX_LUT_ENTRY_BYTES)
                budget.expandedBytes += bytes.size.toLong()
                require(budget.expandedBytes <= MAX_LUT_EXPANDED_BYTES) {
                    "Dung lượng giải nén ZIP vượt giới hạn an toàn."
                }
                val extension = entry.name.substringAfterLast('.', "").lowercase()
                val innerName = entry.name.substringAfterLast('/').substringBeforeLast('.')
                    .ifBlank { "LUT nhập" }
                val category = catalogLutCategory(entry.name)
                when (extension) {
                    "zip" -> found.addAll(scanLutZip(bytes, depth + 1, budget))
                    "cube" -> {
                        require(parseCubeLutText(String(bytes, StandardCharsets.UTF_8)) != null) {
                            "Tệp .cube bên trong ZIP không đúng định dạng LUT 3D."
                        }
                        found.add(LutImportEntry(innerName, category, bytes, false))
                    }
                    "bin" -> {
                        require(parseCanonBinaryLut(bytes) != null) {
                            "Tệp .bin không phải LUT RGB 33x33x33 được hỗ trợ."
                        }
                        found.add(LutImportEntry(innerName, category, bytes, true))
                    }
                }
            }
            zip.closeEntry()
            require(found.size <= MAX_LUTS_PER_IMPORT) {
                "Gói LUT vượt quá $MAX_LUTS_PER_IMPORT bộ lọc."
            }
        }
    }
    return found
}

private fun customCameraLutFile(context: Context, id: String): File? {
    if (!id.matches(Regex("[a-f0-9-]{36}"))) return null
    return File(File(context.filesDir, "camera_luts"), id + ".cube")
}

fun loadCustomCameraLuts(context: Context, activeOnly: Boolean = false): List<CustomCameraLut> {
    val prefs = context.getSharedPreferences("fujifilm_contents", Context.MODE_PRIVATE)
    val allIds = prefs.getStringSet(CUSTOM_CAMERA_LUT_IDS, emptySet()).orEmpty()
    val activeIds = prefs.getStringSet(ACTIVE_CAMERA_LUT_IDS, emptySet()).orEmpty()
    val ids = if (activeOnly) allIds.intersect(activeIds) else allIds
    return ids.mapNotNull { id ->
        val file = customCameraLutFile(context, id) ?: return@mapNotNull null
        if (!file.isFile) return@mapNotNull null
        val name = prefs.getString("camera_lut_name_" + id, null)
            ?: file.nameWithoutExtension
        val category = prefs.getString("camera_lut_category_" + id, "LUT tùy chỉnh")
            ?: "LUT tùy chỉnh"
        CustomCameraLut(id, name, category)
    }.sortedBy { it.name.lowercase() }
}

fun activeCustomCameraLutIds(context: Context): Set<String> {
    return context.getSharedPreferences("fujifilm_contents", Context.MODE_PRIVATE)
        .getStringSet(ACTIVE_CAMERA_LUT_IDS, emptySet()).orEmpty().toSet()
}

fun customCameraLutPreset(id: String): String = CUSTOM_CAMERA_LUT_PREFIX + id

private fun customCameraLutId(preset: String): String? {
    if (!preset.startsWith(CUSTOM_CAMERA_LUT_PREFIX)) return null
    val id = preset.removePrefix(CUSTOM_CAMERA_LUT_PREFIX)
    return id.takeIf { it.matches(Regex("[a-f0-9-]{36}")) }
}

fun cameraLookDisplayName(context: Context, preset: String): String {
    val id = customCameraLutId(preset) ?: return preset
    return loadCustomCameraLuts(context).firstOrNull { it.id == id }?.name ?: "LUT đã nhập"
}

fun setCustomCameraLutActive(context: Context, id: String, active: Boolean) {
    if (customCameraLutFile(context, id) == null) return
    val prefs = context.getSharedPreferences("fujifilm_contents", Context.MODE_PRIVATE)
    val ids = prefs.getStringSet(ACTIVE_CAMERA_LUT_IDS, emptySet()).orEmpty().toMutableSet()
    if (active) ids.add(id) else ids.remove(id)
    prefs.edit().putStringSet(ACTIVE_CAMERA_LUT_IDS, ids).apply()
}

fun deleteCustomCameraLut(context: Context, id: String) {
    val file = customCameraLutFile(context, id) ?: return
    val prefs = context.getSharedPreferences("fujifilm_contents", Context.MODE_PRIVATE)
    val ids = prefs.getStringSet(CUSTOM_CAMERA_LUT_IDS, emptySet()).orEmpty().toMutableSet()
    val activeIds = prefs.getStringSet(ACTIVE_CAMERA_LUT_IDS, emptySet()).orEmpty().toMutableSet()
    ids.remove(id)
    activeIds.remove(id)
    prefs.edit()
        .putStringSet(CUSTOM_CAMERA_LUT_IDS, ids)
        .putStringSet(ACTIVE_CAMERA_LUT_IDS, activeIds)
        .remove("camera_lut_name_" + id)
        .remove("camera_lut_category_" + id)
        .apply()
    file.delete()
    synchronized(customCameraLutCache) { customCameraLutCache.remove(id) }
    val cameraPrefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    if (cameraPrefs.getString("selected_filter", "None") == customCameraLutPreset(id)) {
        cameraPrefs.edit().putString("selected_filter", "None").apply()
    }
}

fun importCustomCameraLuts(context: Context, uri: Uri): List<CustomCameraLut> {
    val displayName = documentDisplayName(context, uri)
    val extension = displayName.substringAfterLast('.', "").lowercase()
    val input = context.contentResolver.openInputStream(uri)
        ?: error("Không thể mở tệp LUT đã chọn.")
    val inputBytes = input.use { readBoundedLutBytes(it, MAX_LUT_INPUT_BYTES.toInt()) }
    val entries = when (extension) {
        "cube" -> {
            require(parseCubeLutText(String(inputBytes, StandardCharsets.UTF_8)) != null) {
                "Tệp .cube không hợp lệ hoặc không phải LUT 3D."
            }
            listOf(
                LutImportEntry(
                    displayName.substringBeforeLast('.'),
                    "LUT tùy chỉnh",
                    inputBytes,
                    false
                )
            )
        }
        "bin" -> {
            require(parseCanonBinaryLut(inputBytes) != null) {
                "Tệp .bin không phải LUT RGB 33x33x33 được hỗ trợ."
            }
            listOf(
                LutImportEntry(
                    displayName.substringBeforeLast('.'),
                    "Canon Picture Styles",
                    inputBytes,
                    true
                )
            )
        }
        "zip" -> scanLutZip(inputBytes, 0, LutImportBudget())
        else -> error("Hỗ trợ .cube, LUT .bin RGB 33x33x33 và ZIP chứa các định dạng này.")
    }
    require(entries.isNotEmpty()) {
        "Không tìm thấy LUT .cube hoặc .bin hợp lệ bên trong ZIP."
    }
    val directory = File(context.filesDir, "camera_luts").apply { mkdirs() }
    val prefs = context.getSharedPreferences("fujifilm_contents", Context.MODE_PRIVATE)
    val ids = prefs.getStringSet(CUSTOM_CAMERA_LUT_IDS, emptySet()).orEmpty().toMutableSet()
    val imported = ArrayList<CustomCameraLut>()
    entries.forEach { entry ->
        val id = UUID.randomUUID().toString()
        val safeName = entry.name.substringAfterLast('/').substringAfterLast('\\')
            .ifBlank { "Imported LUT" }.take(100)
        val target = File(directory, id + ".cube")
        val temporary = File(directory, id + ".tmp")
        temporary.writeBytes(entry.bytes)
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
        ids.add(id)
        imported.add(CustomCameraLut(id, safeName, entry.category))
    }
    val editor = prefs.edit().putStringSet(CUSTOM_CAMERA_LUT_IDS, ids)
    imported.forEach { lut ->
        editor.putString("camera_lut_name_" + lut.id, lut.name)
        editor.putString("camera_lut_category_" + lut.id, lut.category)
    }
    editor.apply()
    return imported
}

// Built-in looks use the app-authored sRGB transforms in transformPreset.
// Non-commercial LUT files are not bundled into this commercial application.
private val filmSimulationAssets = emptyMap<String, String>()

private val filmSimulationCache = mutableMapOf<String, CubeLut>()

private fun readFilmSimulationLut(context: Context, preset: String): CubeLut? {
    val assetPath = filmSimulationAssets[preset] ?: return null
    synchronized(filmSimulationCache) {
        filmSimulationCache[preset]?.let { return it }
    }
    val parsed = runCatching {
        context.assets.open(assetPath).bufferedReader().use { parseCubeLutText(it.readText()) }
    }.getOrNull() ?: return null
    synchronized(filmSimulationCache) {
        filmSimulationCache[preset] = parsed
    }
    return parsed
}

private fun readCameraLookLut(context: Context, preset: String): CubeLut? {
    val customId = customCameraLutId(preset) ?: return readFilmSimulationLut(context, preset)
    synchronized(customCameraLutCache) {
        customCameraLutCache[customId]?.let { return it }
    }
    val file = customCameraLutFile(context, customId) ?: return null
    val parsed = runCatching {
        val bytes = file.readBytes()
        parseCanonBinaryLut(bytes)
            ?: parseCubeLutText(String(bytes, StandardCharsets.UTF_8))
    }.getOrNull() ?: return null
    synchronized(customCameraLutCache) {
        // A 33^3 RGB float LUT is ~430 KB; retain only a few so importing hundreds
        // of profiles does not turn the picker into a large permanent heap allocation.
        if (customCameraLutCache.size >= 4) {
            val oldest = customCameraLutCache.keys.firstOrNull()
            if (oldest != null) customCameraLutCache.remove(oldest)
        }
        customCameraLutCache[customId] = parsed
    }
    return parsed
}

private fun clamp8(value: Float): Int = value.toInt().coerceIn(0, 255)

private fun transformPreset(name: String, red: Float, green: Float, blue: Float): Triple<Float, Float, Float> {
    val luma = red * 0.2126f + green * 0.7152f + blue * 0.0722f
    var r = red
    var g = green
    var b = blue
    fun saturation(amount: Float) {
        r = luma + (r - luma) * amount
        g = luma + (g - luma) * amount
        b = luma + (b - luma) * amount
    }
    fun contrast(amount: Float) {
        r = (r - 128f) * amount + 128f
        g = (g - 128f) * amount + 128f
        b = (b - 128f) * amount + 128f
    }
    fun warmHighlights(amount: Float) {
        val weight = ((luma - 76f) / 179f).coerceIn(0f, 1f)
        r += amount * weight
        g += amount * 0.34f * weight
        b -= amount * 0.62f * weight
    }
    fun coolShadows(amount: Float) {
        val weight = ((150f - luma) / 150f).coerceIn(0f, 1f)
        r -= amount * 0.28f * weight
        g += amount * 0.08f * weight
        b += amount * weight
    }
    when (name) {
        "Provia" -> { saturation(1.04f); contrast(1.03f) }
        "Velvia" -> { saturation(1.30f); contrast(1.13f); b += 3f; g += 1f }
        "Astia" -> { saturation(0.96f); contrast(0.97f); warmHighlights(3f) }
        "Classic Chrome" -> { saturation(0.82f); contrast(1.07f); coolShadows(3f); r += 2f }
        "Classic Neg" -> { saturation(0.98f); contrast(1.11f); warmHighlights(4f); g += 2f }
        "Eterna" -> { saturation(0.78f); contrast(0.91f); r += 3f; g += 3f; b += 4f }
        "Pro Neg Std" -> { saturation(0.90f); contrast(0.93f); warmHighlights(2f) }
        "Pro Neg Hi" -> { saturation(0.96f); contrast(1.10f); warmHighlights(2f) }
        "Nostalgic Neg" -> { saturation(0.94f); contrast(0.97f); warmHighlights(7f); coolShadows(2f) }
        "Reala Ace" -> { saturation(1.04f); contrast(1.06f) }
        "Bleach Bypass" -> { saturation(0.48f); contrast(1.34f) }
        "Acros" -> { r = luma; g = luma; b = luma; contrast(1.22f) }
        "Portra 160" -> { saturation(0.90f); contrast(0.93f); warmHighlights(7f); coolShadows(2f) }
        "Portra 400" -> { saturation(0.88f); contrast(0.94f); warmHighlights(5f); coolShadows(3f) }
        "Portra 800" -> { saturation(0.91f); contrast(0.96f); warmHighlights(6f); coolShadows(3f) }
        "Gold 200" -> { saturation(1.13f); contrast(1.06f); warmHighlights(11f); r += 2f }
        "Ektar 100" -> { saturation(1.22f); contrast(1.10f); warmHighlights(3f); b += 2f }
        "Kodachrome 64" -> { saturation(1.06f); contrast(1.13f); r += 3f; g += 1f; b -= 2f }
        "ColorPlus 200" -> { saturation(1.08f); contrast(1.02f); warmHighlights(8f) }
        "CineStill 800T" -> { saturation(1.04f); contrast(0.96f); coolShadows(10f); warmHighlights(6f) }
        "Vision3 250D" -> { saturation(0.94f); contrast(0.91f); warmHighlights(4f); coolShadows(2f) }
        "Vision3 500T" -> { saturation(0.92f); contrast(0.94f); coolShadows(8f); warmHighlights(3f) }
        "400H" -> { saturation(0.88f); contrast(0.91f); r += 3f; g += 4f; b += 2f }
        "Tri-X 400" -> { r = luma; g = luma; b = luma; contrast(1.20f) }
        "T-Max 400" -> { r = luma; g = luma; b = luma; contrast(1.15f) }
        "Ektachrome 100" -> { saturation(1.10f); contrast(1.10f); coolShadows(3f); warmHighlights(3f) }
        "Vivid" -> { saturation(1.24f); contrast(1.14f) }
        "Natural" -> { saturation(0.96f); contrast(1.02f) }
        "Monochrome" -> { r = luma; g = luma; b = luma; contrast(1.16f) }
        "Cool" -> { saturation(1.08f); contrast(1.09f); r -= 5f; b += 9f }
        "Warm" -> { saturation(1.05f); contrast(1.07f); r += 8f; b -= 5f }
        "Cine Tone" -> { saturation(0.88f); contrast(0.92f); r += 5f; b -= 2f }
        "Soft Log" -> { saturation(0.78f); r = 42f + r * 0.78f; g = 42f + g * 0.78f; b = 42f + b * 0.78f }
        "Kodak Portra 400" -> { saturation(0.88f); contrast(0.94f); r += 8f; g += 3f; b -= 5f }
        "Agfa" -> { saturation(1.2f); r += 7f; g += 2f; b -= 5f }
        "Kodak Gold" -> { saturation(1.12f); r += 10f; g += 4f; b -= 8f }
        "Fuji 400" -> { saturation(0.96f); r += 2f; g += 4f; b -= 2f; contrast(0.97f) }
        "Leica Vivid" -> { saturation(1.24f); contrast(1.14f) }
        "Leica Natural" -> { saturation(0.96f); contrast(1.02f) }
        "Leica Mono" -> { r = luma; g = luma; b = luma; contrast(1.16f) }
        "Hasselblad HNCS" -> { saturation(0.98f); contrast(1.06f); r += 2f; g += 2f }
        "Zeiss Cool" -> { saturation(1.08f); contrast(1.09f); r -= 5f; b += 9f }
        "Zeiss Warm" -> { saturation(1.05f); contrast(1.07f); r += 8f; b -= 5f }
        "Sony S-Cinetone" -> { saturation(0.88f); contrast(0.92f); r += 5f; b -= 2f }
        "Sony S-Log3" -> { r = 64f + r * 0.72f; g = 64f + g * 0.72f; b = 64f + b * 0.72f }
        "Cinematic Log" -> { saturation(0.78f); r = 42f + r * 0.78f; g = 42f + g * 0.78f; b = 42f + b * 0.78f }
    }
    return Triple(r.coerceIn(0f, 255f), g.coerceIn(0f, 255f), b.coerceIn(0f, 255f))
}

/**
 * Applies a built-in Fujifilm profile or a retained non-Fujifilm filter to the saved photo.
 * The image is processed row-by-row to avoid allocating a second full-resolution pixel array.
 */
private fun applyCameraLookPixels(
    context: Context,
    bitmap: Bitmap,
    preset: String,
    cube: CubeLut?
): Boolean {
    if ((preset in filmSimulationAssets || customCameraLutId(preset) != null) && cube == null) return false
    val recipe = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    val highlight = recipe.getFloat("film_custom_highlight", 0f).coerceIn(-1f, 1f)
    val shadow = recipe.getFloat("film_custom_shadow", 0f).coerceIn(-1f, 1f)
    val colorChrome = recipe.getFloat("film_custom_color_chrome", 0f).coerceIn(0f, 1f)
    val grain = recipe.getFloat("film_custom_grain", 0f).coerceIn(0f, 1f)
    val temperature = recipe.getFloat("film_custom_temperature", 0f).coerceIn(-1f, 1f)
    val tone = recipe.getFloat("film_custom_tone", 0f).coerceIn(-1f, 1f)
    val saturation = recipe.getFloat("film_custom_saturation", 1f).coerceIn(0.5f, 1.5f)
    val row = IntArray(bitmap.width)
    for (y in 0 until bitmap.height) {
        bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
        for (x in row.indices) {
            val pixel = row[x]
            val alpha = AndroidColor.alpha(pixel)
            val red = AndroidColor.red(pixel).toFloat()
            val green = AndroidColor.green(pixel).toFloat()
            val blue = AndroidColor.blue(pixel).toFloat()
            val mapped = cube?.sample(red / 255f, green / 255f, blue / 255f)
                ?: transformPreset(preset, red, green, blue).let {
                    Triple(it.first / 255f, it.second / 255f, it.third / 255f)
                }

            var r = mapped.first.coerceIn(0f, 1f)
            var g = mapped.second.coerceIn(0f, 1f)
            var b = mapped.third.coerceIn(0f, 1f)
            val luma = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
            val highlightWeight = ((luma - 0.55f) / 0.45f).coerceIn(0f, 1f)
            val shadowWeight = ((0.45f - luma) / 0.45f).coerceIn(0f, 1f)
            val tonalShift = highlight * highlightWeight * 0.12f +
                shadow * shadowWeight * 0.12f
            r += tonalShift
            g += tonalShift
            b += tonalShift

            val chromeChroma = maxOf(abs(r - luma), abs(g - luma), abs(b - luma))
            val chromeFactor = 1f + colorChrome * (0.05f + chromeChroma * 0.18f)
            val saturationFactor = saturation * chromeFactor
            r = luma + (r - luma) * saturationFactor
            g = luma + (g - luma) * saturationFactor
            b = luma + (b - luma) * saturationFactor

            r += temperature * 0.045f
            g += temperature * 0.006f
            b -= temperature * 0.045f
            // Color Tone shifts green/magenta independently from global color temperature.
            r += tone * 0.018f
            g -= tone * 0.035f
            b += tone * 0.018f

            if (grain > 0.001f) {
                // Deterministic per-pixel grain keeps a given image stable across preview/export passes.
                val seed = (x.toLong() * 73_856_093L) xor
                    (y.toLong() * 19_349_663L) xor preset.hashCode().toLong()
                val noise = ((((seed ushr 13) and 0xFFFFL).toFloat() / 32_767.5f) - 1f) *
                    grain * 0.035f
                r += noise
                g += noise
                b += noise
            }
            row[x] = AndroidColor.argb(
                alpha,
                clamp8(r.coerceIn(0f, 1f) * 255f),
                clamp8(g.coerceIn(0f, 1f) * 255f),
                clamp8(b.coerceIn(0f, 1f) * 255f)
            )
        }
        bitmap.setPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
    }
    return true
}

/**
 * Render the exact same bundled profile/pixel transform used by saved-photo processing.
 * The filter picker uses a shared source bitmap, so switching presets never compounds filters.
 */
fun renderCameraLookPreview(
    context: Context,
    source: Bitmap,
    preset: String,
    maxDimension: Int = 180
): Bitmap? {
    return runCatching {
        val maxSide = maxOf(source.width, source.height).coerceAtLeast(1)
        val scale = minOf(1f, maxDimension.toFloat() / maxSide)
        val scaled = if (scale < 0.999f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else source
        val output = scaled.copy(Bitmap.Config.ARGB_8888, true) ?: return@runCatching null
        if (scaled !== source) scaled.recycle()
        val cube = readCameraLookLut(context, preset)
        if (!applyCameraLookPixels(context, output, preset, cube)) {
            output.recycle()
            return@runCatching null
        }
        output
    }.getOrNull()
}

/** A built-in color-rich reference scene is shown until the first unfiltered capture is available. */
fun createFilmLookPreviewSample(): Bitmap {
    val width = 480
    val height = 300
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = LinearGradient(
        0f, 0f, 0f, height.toFloat(),
        intArrayOf(AndroidColor.rgb(111, 168, 205), AndroidColor.rgb(233, 202, 161), AndroidColor.rgb(245, 220, 180)),
        null, Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null

    paint.color = AndroidColor.rgb(255, 226, 163)
    canvas.drawCircle(385f, 64f, 29f, paint)

    val distant = Path().apply {
        moveTo(0f, 190f); lineTo(74f, 92f); lineTo(130f, 173f)
        lineTo(213f, 80f); lineTo(305f, 185f); lineTo(390f, 112f)
        lineTo(480f, 186f); lineTo(480f, 300f); lineTo(0f, 300f); close()
    }
    paint.color = AndroidColor.rgb(113, 125, 139)
    canvas.drawPath(distant, paint)
    val near = Path().apply {
        moveTo(0f, 222f); lineTo(115f, 148f); lineTo(208f, 224f)
        lineTo(324f, 143f); lineTo(480f, 229f); lineTo(480f, 300f); lineTo(0f, 300f); close()
    }
    paint.color = AndroidColor.rgb(82, 119, 91)
    canvas.drawPath(near, paint)

    paint.shader = LinearGradient(0f, 218f, 0f, 300f,
        AndroidColor.rgb(83, 125, 83), AndroidColor.rgb(38, 75, 58), Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 218f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null

    // Colorful still-life subject: skin tones, fabric, flowers, foliage and warm highlights.
    paint.color = AndroidColor.rgb(203, 145, 100)
    canvas.drawCircle(218f, 177f, 28f, paint)
    paint.color = AndroidColor.rgb(58, 89, 116)
    canvas.drawRoundRect(RectF(178f, 204f, 260f, 300f), 15f, 15f, paint)
    paint.color = AndroidColor.rgb(231, 190, 145)
    canvas.drawOval(RectF(203f, 165f, 230f, 190f), paint)
    paint.color = AndroidColor.rgb(59, 45, 42)
    canvas.drawOval(RectF(197f, 149f, 240f, 173f), paint)

    paint.color = AndroidColor.rgb(168, 78, 58)
    canvas.drawRoundRect(RectF(55f, 208f, 115f, 270f), 8f, 8f, paint)
    paint.color = AndroidColor.rgb(231, 178, 100)
    canvas.drawRect(62f, 216f, 108f, 228f, paint)
    paint.color = AndroidColor.rgb(78, 112, 73)
    canvas.drawRect(330f, 205f, 338f, 270f, paint)
    paint.color = AndroidColor.rgb(55, 98, 65)
    canvas.drawCircle(334f, 183f, 35f, paint)
    paint.color = AndroidColor.rgb(231, 142, 105)
    canvas.drawCircle(315f, 178f, 10f, paint)
    paint.color = AndroidColor.rgb(247, 210, 120)
    canvas.drawCircle(350f, 158f, 11f, paint)
    paint.color = AndroidColor.rgb(217, 226, 205)
    canvas.drawRect(12f, 18f, 104f, 42f, paint)
    paint.color = AndroidColor.rgb(40, 47, 50)
    paint.textSize = 14f
    paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    canvas.drawText("FUJI FILM LOOK", 19f, 35f, paint)
    return bitmap
}

private val watermarkExifTags = listOf(
    "ImageDescription", "Make", "Model", "Software", "DateTime", "Artist", "Copyright",
    "ExposureTime", "FNumber", "ExposureProgram", "SpectralSensitivity", "ISOSpeedRatings",
    "SensitivityType", "StandardOutputSensitivity", "RecommendedExposureIndex",
    "PhotographicSensitivity", "ExifVersion", "DateTimeOriginal", "DateTimeDigitized",
    "ComponentsConfiguration", "CompressedBitsPerPixel", "ShutterSpeedValue", "ApertureValue",
    "BrightnessValue", "ExposureBiasValue", "MaxApertureValue", "SubjectDistance", "MeteringMode",
    "LightSource", "Flash", "FocalLength", "SubjectArea", "UserComment", "SubSecTime",
    "SubSecTimeOriginal", "SubSecTimeDigitized", "FlashpixVersion", "ColorSpace",
    "FocalLengthIn35mmFilm", "SceneType", "CustomRendered", "ExposureMode", "WhiteBalance",
    "DigitalZoomRatio", "SceneCaptureType", "GainControl", "Contrast", "Saturation", "Sharpness",
    "SubjectDistanceRange", "ImageUniqueID", "CameraOwnerName", "BodySerialNumber",
    "LensSpecification", "LensMake", "LensModel", "LensSerialNumber",
    "GPSVersionID", "GPSLatitudeRef", "GPSLatitude", "GPSLongitudeRef", "GPSLongitude",
    "GPSAltitudeRef", "GPSAltitude", "GPSTimeStamp", "GPSSatellites", "GPSStatus",
    "GPSMeasureMode", "GPSDOP", "GPSSpeedRef", "GPSSpeed", "GPSTrackRef", "GPSTrack",
    "GPSImgDirectionRef", "GPSImgDirection", "GPSMapDatum", "GPSDestLatitudeRef",
    "GPSDestLatitude", "GPSDestLongitudeRef", "GPSDestLongitude", "GPSDestBearingRef",
    "GPSDestBearing", "GPSDestDistanceRef", "GPSDestDistance", "GPSProcessingMethod",
    "GPSAreaInformation", "GPSDateStamp", "GPSDifferential", "GPSHPositioningError"
)

private fun readImageExifAttributes(context: Context, uri: Uri): Map<String, String> {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            watermarkExifTags.mapNotNull { tag ->
                exif.getAttribute(tag)?.takeIf { it.isNotBlank() }?.let { tag to it }
            }.toMap()
        }.orEmpty()
    }.onFailure {
        android.util.Log.w("FUJI_EXIF", "Could not read source EXIF attributes", it)
    }.getOrDefault(emptyMap())
}

/** Reapply source EXIF after JPEG recompression; pixel dimensions and orientation follow new pixels. */
private fun restoreImageExifAttributes(
    context: Context,
    uri: Uri,
    attributes: Map<String, String>,
    width: Int,
    height: Int
): Boolean {
    return runCatching {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "rw")
            ?: return@runCatching false
        descriptor.use {
            ExifInterface(it.fileDescriptor).apply {
                attributes.forEach { (tag, value) ->
                    runCatching { setAttribute(tag, value) }
                        .onFailure { failure ->
                            android.util.Log.d("FUJI_EXIF", "Skipped unsupported EXIF tag $tag", failure)
                        }
                }
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                setAttribute(ExifInterface.TAG_THUMBNAIL_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, width.toString())
                setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, height.toString())
                setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
                setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
                saveAttributes()
            }
        }
        true
    }.onFailure {
        android.util.Log.e("FUJI_EXIF", "Could not restore source EXIF attributes", it)
    }.getOrDefault(false)
}

fun applyCameraLook(context: Context, uri: Uri): Boolean {
    val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    val preset = prefs.getString("selected_filter", "None") ?: "None"
    val hasCustomAdjustments =
        kotlin.math.abs(prefs.getFloat("film_custom_highlight", 0f)) > 0.001f ||
        kotlin.math.abs(prefs.getFloat("film_custom_shadow", 0f)) > 0.001f ||
        prefs.getFloat("film_custom_color_chrome", 0f) > 0.001f ||
        prefs.getFloat("film_custom_grain", 0f) > 0.001f ||
        kotlin.math.abs(prefs.getFloat("film_custom_temperature", 0f)) > 0.001f ||
        kotlin.math.abs(prefs.getFloat("film_custom_tone", 0f)) > 0.001f ||
        kotlin.math.abs(prefs.getFloat("film_custom_saturation", 1f) - 1f) > 0.001f
    if (preset == "None" && !hasCustomAdjustments) return false

    return runCatching {
        // Film customizer controls are active even when the selected LUT/profile is None.
        val cube = if (preset == "None") null else readCameraLookLut(context, preset)
        if ((preset in filmSimulationAssets || customCameraLutId(preset) != null) && cube == null) {
            android.util.Log.e("FUJI_FILTER", "Missing film simulation asset for $preset")
            return@runCatching false
        }

        val sourceExif = readImageExifAttributes(context, uri)
        val bitmap = decodeRawPixelBitmap(context, uri) ?: return@runCatching false
        try {
            if (!applyCameraLookPixels(context, bitmap, preset, cube)) return@runCatching false
            val written = context.contentResolver.openOutputStream(uri, "wt")?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            } ?: false
            if (!written) return@runCatching false

            val exifWritten = restoreImageExifAttributes(context, uri, sourceExif, bitmap.width, bitmap.height)
            setMediaStoreOrientationUpright(context, uri)
            exifWritten
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }.onFailure {
        android.util.Log.e("FUJI_FILTER", "Could not apply selected filter", it)
    }.getOrDefault(false)
}

/**
 * Draws a camera-spec frame directly onto the upright pixel matrix. No portrait/landscape
 * heuristic is applied here: the image pixels, metadata and watermark all share one orientation.
 */
private val watermarkTypefaceCache = mutableMapOf<String, android.graphics.Typeface>()

private fun resolveWatermarkTypeface(context: Context, brand: String): android.graphics.Typeface {
    val key = brand.lowercase()
    synchronized(watermarkTypefaceCache) {
        watermarkTypefaceCache[key]?.let { return it }
        val (assetName, variation, fallbackFamily, fallbackStyle) = when (key) {
            "sony" -> arrayOf("Inter-Variable.ttf", "'wght' 300, 'opsz' 14", "sans-serif-light", "0")
            "nikon" -> arrayOf("CreatoDisplay-Light.otf", "", "sans-serif-light", "0")
            "canon" -> arrayOf("LiberationSans-Regular.ttf", "", "sans-serif", "0")
            else -> arrayOf("Jost-Variable.ttf", "'wght' 300", "sans-serif-light", "0")
        }
        val typeface = runCatching {
            val directory = File(context.cacheDir, "watermark-fonts").apply { mkdirs() }
            val fontFile = File(directory, assetName)
            if (!fontFile.isFile || fontFile.length() == 0L) {
                context.assets.open("fonts/" + assetName).use { input ->
                    fontFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            android.graphics.Typeface.Builder(fontFile)
                .apply { if (variation.isNotBlank()) setFontVariationSettings(variation) }
                .build()
        }.getOrElse {
            android.graphics.Typeface.create(
                fallbackFamily,
                fallbackStyle.toIntOrNull() ?: android.graphics.Typeface.NORMAL
            )
        }
        watermarkTypefaceCache[key] = typeface
        return typeface
    }
}

fun applyCameraWatermark(
    context: Context,
    uri: Uri,
    focal: String,
    aperture: String,
    shutter: String,
    iso: String,
    preservePhotoAspectRatio: Boolean = false
): Boolean {
    val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    if (!prefs.getBoolean("watermark_enabled", true)) {
        android.util.Log.d("FUJI_WATERMARK", "Watermark disabled in settings")
        return false
    }

    return runCatching {
        val sourceExif = readImageExifAttributes(context, uri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Watermark source JPEG has invalid dimensions" }
        val estimatedBitmapBytes = bounds.outWidth.toLong() * bounds.outHeight.toLong() * 2L
        val watermarkRuntime = Runtime.getRuntime()
        val watermarkUsedHeap = (watermarkRuntime.totalMemory() - watermarkRuntime.freeMemory()).coerceAtLeast(0L)
        val watermarkAvailableHeap = (watermarkRuntime.maxMemory() - watermarkUsedHeap).coerceAtLeast(0L)
        require(estimatedBitmapBytes + 48L * 1024L * 1024L <= watermarkAvailableHeap) {
            "Not enough heap to safely stamp this high-resolution photo; source image is preserved"
        }
        val sourceBitmap = decodeWatermarkPixelBitmap(context, uri) ?: return@runCatching false
        var outputBitmap: Bitmap? = null
        var watermarkCropBitmap: Bitmap? = null
        var watermarkBrandBitmap: Bitmap? = null
        try {
        val selectedTemplate = (prefs.getString("watermark_template", "fuji_auto") ?: "fuji_auto").takeIf { it in setOf("fuji_auto", "fuji_classic", "fuji_signature", "classic", "dark", "minimal", "simple", "micro", "vintage", "polaroid", "gallery", "frosted", "film", "datestamp") } ?: "fuji_auto"
        val frameStyle = (prefs.getString("watermark_style", "White") ?: "White").takeIf { it == "White" || it == "Black" } ?: "White"
        val template = if (selectedTemplate == "fuji_auto") {
            if (sourceBitmap.width > sourceBitmap.height) "fuji_classic" else "fuji_signature"
        } else {
            selectedTemplate
        }
        val isFujiFooterTemplate = template != "minimal" && template != "simple" && template != "micro" && template != "datestamp"
        val sourceWidthPx = sourceBitmap.width
        val sourceHeightPx = sourceBitmap.height
        val sourceMinSide = minOf(sourceWidthPx, sourceHeightPx).toFloat()
        val footerMarginPx = (sourceMinSide * 0.025f).coerceIn(14f, 72f).toInt()
        // A restrained ~12% footer keeps the photo area dominant while remaining readable.
        val footerHeightPx = (sourceMinSide * 0.14f).coerceIn(88f, 360f).toInt()
        val compactFooterHeightPx = (sourceMinSide * 0.12f).coerceIn(72f, 300f).toInt()
        val expandedWidth = sourceBitmap.width + footerMarginPx * 2
        val expandedHeight = sourceBitmap.height + footerMarginPx * 2 + footerHeightPx
        val expandedPixelCount = expandedWidth.toLong() * expandedHeight.toLong()
        // The source and expanded watermark canvas both use RGB_565. Budget the full
        // appended canvas while the decoded source is still live.
        val extraBytes = expandedPixelCount * 2L
        val runtime = Runtime.getRuntime()
        val freeHeapBytes = (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()))
            .coerceAtLeast(0L)
        // The reference layouts append a real white footer without downscaling the captured photo.
        // If the additional canvas would threaten the current heap, keep the original dimensions
        // and render the footer as an in-image plate instead of risking an OOM.
        val expandedFujiFooter = !preservePhotoAspectRatio &&
            isFujiFooterTemplate &&
            frameStyle in setOf("White", "Black") &&
            expandedWidth <= 32768 && expandedHeight <= 32768 &&
            expandedPixelCount <= 100_000_000L && extraBytes <= (freeHeapBytes * 0.42f).toLong()
        val output = if (expandedFujiFooter) {
            Bitmap.createBitmap(expandedWidth, expandedHeight, Bitmap.Config.RGB_565).also { expanded ->
                outputBitmap = expanded
                val expandedCanvas = Canvas(expanded)
                expandedCanvas.drawColor(if (frameStyle == "Black") AndroidColor.BLACK else AndroidColor.WHITE)
                val photoRect = RectF(
                    footerMarginPx.toFloat(),
                    footerMarginPx.toFloat(),
                    footerMarginPx.toFloat() + sourceBitmap.width,
                    footerMarginPx.toFloat() + sourceBitmap.height
                )
                expandedCanvas.drawBitmap(
                    sourceBitmap, null, photoRect,
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                )
                // Do not add an automatic outline around the photograph.
                // Optional camera frames are rendered only when the user selects one.
                sourceBitmap.recycle()
            }
        } else if (sourceBitmap.isMutable) {
            sourceBitmap.also { outputBitmap = it }
        } else {
            sourceBitmap.copy(Bitmap.Config.RGB_565, true)?.also { outputBitmap = it }
                ?: return@runCatching false
        }
        outputBitmap = output
        val canvas = Canvas(output)
        val showTime = prefs.getBoolean("watermark_show_time", true)
        val showModel = prefs.getBoolean("watermark_show_model", true)
        val showSpecs = prefs.getBoolean("watermark_show_specs", true)

        // Frame, backing plate and type all follow the selected watermark style.
        val glassEnabled = frameStyle == "LiquidGlass"
        val glassRefraction = prefs.getFloat("watermark_glass_refraction", 0.55f).coerceIn(0f, 1f)
        val glassThickness = prefs.getFloat("watermark_glass_thickness", 0.48f).coerceIn(0f, 1f)
        val glassDispersion = prefs.getFloat("watermark_glass_dispersion", 0.28f).coerceIn(0f, 1f)
        val glassBlur = prefs.getFloat("watermark_glass_blur", 8f).coerceIn(0f, 20f)
        val glassRoughness = prefs.getFloat("watermark_glass_roughness", 0.16f).coerceIn(0f, 1f)
        val glassRim = prefs.getFloat("watermark_glass_rim", 0.72f).coerceIn(0f, 1f)
        val glassGloss = prefs.getFloat("watermark_glass_gloss", 0.72f).coerceIn(0f, 1f)
        val glassCorner = prefs.getFloat("watermark_glass_corner", 0.58f).coerceIn(0f, 1f)
        val glassGooey = prefs.getFloat("watermark_glass_gooey", 0.34f).coerceIn(0f, 1f)
        val glassTintMode = prefs.getString("watermark_glass_tint", "Auto") ?: "Auto"
        val frameColor = if (frameStyle == "Black") AndroidColor.BLACK else AndroidColor.WHITE
        val textColor = when {
            frameStyle == "White" -> AndroidColor.BLACK
            glassEnabled && sampleBitmapLuminance(output) > 0.55f -> AndroidColor.rgb(24, 29, 35)
            else -> AndroidColor.WHITE
        }
        val panelColor = when (frameStyle) {
            "White" -> AndroidColor.argb(238, 255, 255, 255)
            "Black" -> AndroidColor.argb(220, 8, 8, 8)
            "LiquidGlass" -> AndroidColor.argb(38, 228, 241, 250)
            else -> AndroidColor.TRANSPARENT
        }
        val minSide = minOf(output.width, output.height).toFloat()
        // Keep logo and metadata in one restrained visual scale across output resolutions.
        val watermarkTextSize = (minSide * 0.0125f).coerceIn(12f, 54f)
        val watermarkLogoHeight = minSide * 0.032f
        val margin = (minSide * 0.035f).coerceAtLeast(18f)
        fun exifNumber(tag: String): Double? = sourceExif[tag]?.let { raw ->
            val value = raw.trim()
            runCatching {
                if ('/' in value) {
                    val parts = value.split('/', limit = 2)
                    parts[0].toDouble() / parts[1].toDouble()
                } else value.toDouble()
            }.getOrNull()?.takeIf { it.isFinite() }
        }
        fun formatDecimal(value: Double, pattern: String): String =
            String.format(java.util.Locale.US, pattern, value)
        val exifFocal = exifNumber("FocalLength")?.let { formatDecimal(it, "%.1f mm") }
            ?: focal.takeIf { it.isNotBlank() }
        val equivalent35mm = sourceExif["FocalLengthIn35mmFilm"]?.toIntOrNull()
            ?.takeIf { it > 0 }?.let { "$it mm equiv" }
        val exifAperture = exifNumber("FNumber")?.let {
            "F" + formatDecimal(it, "%.1f").trimEnd('0').trimEnd('.')
        } ?: aperture.takeIf { it.isNotBlank() }?.replace("f/", "F", ignoreCase = true)
        val exposureSeconds = exifNumber("ExposureTime")
        val exifShutter = exposureSeconds?.let { seconds ->
            when {
                seconds >= 1.0 -> formatDecimal(seconds, "%.2f s").trimEnd('0').trimEnd('.')
                seconds > 0.0 -> "1/${(1.0 / seconds).roundToInt().coerceAtLeast(1)} s"
                else -> null
            }
        } ?: shutter.takeIf { it.isNotBlank() }
        val exifIso = sourceExif["PhotographicSensitivity"]
            ?: sourceExif["ISOSpeedRatings"]
            ?: iso.takeIf { it.isNotBlank() }
        val exposureBias = exifNumber("ExposureBiasValue")?.let { ev ->
            (if (ev > 0.0) "+" else "") + formatDecimal(ev, "%.1f EV")
        }
        val lensModel = sourceExif["LensModel"]?.trim()?.takeIf { it.isNotBlank() }
        val exifMake = sourceExif["Make"]?.trim()?.takeIf { it.isNotBlank() }
        val exifModel = sourceExif["Model"]?.trim()?.takeIf { it.isNotBlank() }
        val cameraLabel = listOf(exifMake, exifModel).distinct().filterNotNull()
            .joinToString(" ").ifBlank { "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}" }
        val customModelLabel = prefs.getString("watermark_custom_model", "")?.trim().orEmpty()
        // Keep the body model distinct from the selected brand wordmark. The watermark
        // should read "SONY | MODEL ILCE-7M4", not repeat the manufacturer in the model line.
        val rawModelLabel = exifModel ?: android.os.Build.MODEL
        val makePrefix = (exifMake ?: android.os.Build.MANUFACTURER).trim()
        val cleanModelLabel = if (makePrefix.isNotBlank() &&
            rawModelLabel.startsWith(makePrefix, ignoreCase = true)
        ) rawModelLabel.drop(makePrefix.length).trim() else rawModelLabel.trim()
        val model = customModelLabel.ifBlank { cleanModelLabel }
        // Prioritize the three exposure values the user needs to read at a glance.
        // Focal length and exposure compensation follow only when space permits.
        val specs = listOf(
            exifFocal,
            exifAperture,
            exifShutter?.replace(" s", "s"),
            exifIso?.let { "ISO $it" },
            exposureBias
        ).filterNotNull().filter { it.isNotBlank() }
            .joinToString("  ·  ")
            .takeIf { showSpecs }.orEmpty()
        val modelLine = model.takeIf { showModel }.orEmpty()
        val capturedDateRaw = sourceExif["DateTimeOriginal"] ?: sourceExif["DateTime"]
        val capturedDate = capturedDateRaw?.let { raw ->
            runCatching {
                java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US).apply {
                    isLenient = false
                }.parse(raw)
            }.getOrNull()
        }
        val timeLine = capturedDate?.let {
            java.text.SimpleDateFormat("yyyy.MM.dd  HH:mm", java.util.Locale.US).format(it)
        }.orEmpty().takeIf { showTime }.orEmpty()
        val dateOnlyLine = capturedDate?.let {
            java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.US).format(it)
        }.orEmpty().takeIf { showTime }.orEmpty()
        val locationLine = prefs.getString("watermark_custom_location", "")?.trim().orEmpty()
        val watermarkBrand = (prefs.getString("watermark_brand", "fujifilm") ?: "fujifilm").lowercase().takeIf { id -> cameraWatermarkBrands.any { it.first == id } } ?: "fujifilm"
        val brandTitle = cameraWatermarkBrandTitle(watermarkBrand)
        // Brand-matched typefaces are bundled with their OFL notices in assets/fonts.
        val brandTypeface = resolveWatermarkTypeface(context, watermarkBrand)
        val detailTypeface = when (watermarkBrand) {
            "sony" -> brandTypeface
            "nikon" -> brandTypeface
            "canon" -> brandTypeface
            else -> brandTypeface
        }
        val brandInk = cameraWatermarkBrandAccent(watermarkBrand)
        val brandLogo = cameraWatermarkBrandLogoResource(watermarkBrand)?.let {
            ContextCompat.getDrawable(context, it)
        } ?: cameraWatermarkBrandAssetPath(watermarkBrand)?.let { assetPath ->
            CameraAssetStore.bitmap(context, assetPath)?.also { watermarkBrandBitmap = it }
                ?.let { android.graphics.drawable.BitmapDrawable(context.resources, it) }
        }
        val footerRect = if (expandedFujiFooter) {
            RectF(
                footerMarginPx.toFloat(),
                (sourceHeightPx + footerMarginPx + footerMarginPx * 0.35f).toFloat(),
                output.width - footerMarginPx.toFloat(),
                output.height - footerMarginPx.toFloat()
            )
        } else {
            RectF(
                0f,
                output.height - compactFooterHeightPx.coerceAtMost(output.height / 4).toFloat(),
                output.width.toFloat(),
                output.height.toFloat()
            )
        }
        fun drawBrandLogo(rect: RectF) {
            val logo = brandLogo
            if (logo != null) {
                val intrinsicWidth = logo.intrinsicWidth.coerceAtLeast(1).toFloat()
                val intrinsicHeight = logo.intrinsicHeight.coerceAtLeast(1).toFloat()
                val fitScale = minOf(rect.width() / intrinsicWidth, rect.height() / intrinsicHeight)
                val drawWidth = intrinsicWidth * fitScale
                val drawHeight = intrinsicHeight * fitScale
                val drawLeft = rect.centerX() - drawWidth * 0.5f
                val drawTop = rect.centerY() - drawHeight * 0.5f
                canvas.save()
                logo.setBounds(
                    drawLeft.toInt(), drawTop.toInt(),
                    (drawLeft + drawWidth).toInt(), (drawTop + drawHeight).toInt()
                )
                logo.draw(canvas)
                canvas.restore()
                return
            }
            if (watermarkBrand == "nikon" || watermarkBrand == "kodak") {
                val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.rgb(255, 221, 0)
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rect, rect.height() * 0.08f, rect.height() * 0.08f, plate)
            }
            val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = brandInk
                typeface = brandTypeface
                textAlign = Paint.Align.CENTER
                textSize = minOf(rect.height() * 0.76f, rect.width() * 0.34f)
                if (watermarkBrand == "sony") textScaleX = 1.06f
            }
            val baseline = rect.centerY() - (markPaint.ascent() + markPaint.descent()) * 0.5f
            canvas.drawText(brandTitle, rect.centerX(), baseline, markPaint)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = brandTypeface
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            alpha = 235
            typeface = detailTypeface
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = brandTypeface
        }
        if (frameStyle == "Transparent") {
            titlePaint.setShadowLayer((minSide * 0.0022f).coerceAtLeast(2f), 0f, 1f, AndroidColor.BLACK)
            detailPaint.setShadowLayer((minSide * 0.0018f).coerceAtLeast(1.5f), 0f, 1f, AndroidColor.BLACK)
            accentPaint.setShadowLayer((minSide * 0.0018f).coerceAtLeast(1.5f), 0f, 1f, AndroidColor.BLACK)
        }
        fun fitText(value: String, paint: Paint, x: Float, baseline: Float, maxWidth: Float, baseSize: Float) {
            if (value.isBlank()) return
            paint.textSize = baseSize
            while (paint.textSize > 9f && paint.measureText(value) > maxWidth) {
                paint.textSize *= 0.92f
            }
            canvas.drawText(value, x, baseline, paint)
        }
        fun drawLiquidGlassPanel(rect: RectF, baseRadius: Float) {
            val left = rect.left.toInt().coerceIn(0, output.width - 1)
            val top = rect.top.toInt().coerceIn(0, output.height - 1)
            val right = rect.right.toInt().coerceIn(left + 1, output.width)
            val bottom = rect.bottom.toInt().coerceIn(top + 1, output.height)
            val cropWidth = (right - left).coerceAtLeast(1)
            val cropHeight = (bottom - top).coerceAtLeast(1)
            val crop = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
            watermarkCropBitmap = crop
            Canvas(crop).drawBitmap(output, -left.toFloat(), -top.toFloat(), null)

            val blurRadius = (minSide * glassBlur / 1200f).toInt().coerceIn(0, 30)
            if (blurRadius > 0) applyWatermarkBoxBlur(crop, blurRadius, 2)

            val safeRadius = (baseRadius * (1f + glassGooey * 0.75f) +
                minSide * (0.004f + glassCorner * 0.020f))
                .coerceIn(8f, minOf(rect.width(), rect.height()) * 0.48f)
            val path = Path().apply {
                addRoundRect(rect, safeRadius, safeRadius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(path)

            // Convex mesh magnifies the center of the sampled photo through the glass plate.
            val mesh = 12
            val vertices = FloatArray((mesh + 1) * (mesh + 1) * 2)
            for (row in 0..mesh) {
                for (column in 0..mesh) {
                    val nx = column.toFloat() / mesh
                    val ny = row.toFloat() / mesh
                    val dx = nx - 0.5f
                    val dy = ny - 0.5f
                    val edgeDistance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) * 2f
                    val bulge = glassRefraction * 0.18f * (1f - edgeDistance).coerceAtLeast(0f)
                    val index = (row * (mesh + 1) + column) * 2
                    vertices[index] = rect.left + (nx + dx * bulge) * rect.width()
                    vertices[index + 1] = rect.top + (ny + dy * bulge) * rect.height()
                }
            }
            val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmapMesh(crop, mesh, mesh, vertices, 0, null, 0, meshPaint)

            val averageTint = sampleBitmapColor(output, rect)
            val tintColor = when (glassTintMode) {
                "Milky" -> AndroidColor.WHITE
                "Smoke" -> AndroidColor.rgb(20, 26, 36)
                else -> averageTint
            }
            val tintAlpha = (18 + glassThickness * 22 + glassGloss * 8).toInt().coerceIn(12, 52)
            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.argb(tintAlpha, AndroidColor.red(tintColor),
                    AndroidColor.green(tintColor), AndroidColor.blue(tintColor))
            }
            canvas.drawRoundRect(rect, safeRadius, safeRadius, tintPaint)

            if (glassGloss > 0.01f) {
                val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        rect.left, rect.top, rect.right, rect.bottom,
                        intArrayOf(
                            AndroidColor.argb((100 * glassGloss).toInt(), 255, 255, 255),
                            AndroidColor.argb((26 * glassGloss).toInt(), 255, 255, 255),
                            AndroidColor.TRANSPARENT,
                            AndroidColor.argb((16 * glassGloss).toInt(), 255, 255, 255)
                        ),
                        floatArrayOf(0f, 0.28f, 0.68f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(rect, safeRadius, safeRadius, glossPaint)
            }

            if (glassRoughness > 0.01f) {
                val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb((8 + glassRoughness * 18).toInt(), 255, 255, 255)
                    strokeWidth = (minSide * 0.0007f).coerceAtLeast(1f)
                }
                val random = java.util.Random(left * 31L + top * 97L + cropWidth * 13L)
                repeat((30 + glassRoughness * 240).toInt()) {
                    val x = rect.left + random.nextFloat() * rect.width()
                    val y = rect.top + random.nextFloat() * rect.height()
                    canvas.drawPoint(x, y, grainPaint)
                }
            }

            if (glassDispersion > 0.01f) {
                val shift = 1f + glassDispersion * 5f
                val cyan = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb((28 + glassDispersion * 95).toInt(), 92, 224, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f + glassDispersion * 2f
                }
                val magenta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb((24 + glassDispersion * 85).toInt(), 255, 116, 164)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f + glassDispersion * 1.6f
                }
                canvas.drawRoundRect(RectF(rect.left - shift, rect.top, rect.right - shift, rect.bottom),
                    safeRadius, safeRadius, cyan)
                canvas.drawRoundRect(RectF(rect.left + shift, rect.top, rect.right + shift, rect.bottom),
                    safeRadius, safeRadius, magenta)
            }

            val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.argb((48 + glassRim * 185).toInt(), 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = (minSide * (0.001f + glassRim * 0.0017f)).coerceAtLeast(1.5f)
            }
            canvas.drawRoundRect(rect, safeRadius, safeRadius, rimPaint)

            val innerHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.argb((24 + glassGloss * 88).toInt(), 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = (minSide * 0.0008f).coerceAtLeast(1f)
            }
            val inset = (minSide * (0.002f + glassThickness * 0.003f)).coerceAtLeast(2f)
            val innerRect = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
            canvas.drawRoundRect(innerRect, (safeRadius - inset).coerceAtLeast(2f),
                (safeRadius - inset).coerceAtLeast(2f), innerHighlight)
            canvas.restore()
            if (!crop.isRecycled) crop.recycle()
            watermarkCropBitmap = null
        }

        fun drawFrame(rect: RectF, radius: Float) {
            if (frameStyle == "Transparent") return
            if (glassEnabled) {
                val rr = radius * (1f + glassGooey * 0.8f)
                val outerGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb((34 + glassRim * 115).toInt(), 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = (minSide * (0.0015f + glassThickness * 0.0015f)).coerceAtLeast(2f)
                    setShadowLayer((minSide * glassRim * 0.003f).coerceAtLeast(1f), 0f, 0f, AndroidColor.WHITE)
                }
                canvas.drawRoundRect(rect, rr, rr, outerGlow)
                if (glassDispersion > 0.01f) {
                    val cyan = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.argb((24 + glassDispersion * 90).toInt(), 90, 220, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = (minSide * 0.001f).coerceAtLeast(1f)
                    }
                    val pink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.argb((18 + glassDispersion * 78).toInt(), 255, 124, 176)
                        style = Paint.Style.STROKE
                        strokeWidth = (minSide * 0.0008f).coerceAtLeast(1f)
                    }
                    canvas.drawRoundRect(RectF(rect.left - 1.5f, rect.top, rect.right - 1.5f, rect.bottom), rr, rr, cyan)
                    canvas.drawRoundRect(RectF(rect.left + 1.5f, rect.top, rect.right + 1.5f, rect.bottom), rr, rr, pink)
                }
                return
            }
            val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = frameColor
                alpha = 245
                style = Paint.Style.STROKE
                strokeWidth = (minSide * 0.0014f).coerceAtLeast(1.5f)
            }
            canvas.drawRoundRect(rect, radius, radius, framePaint)
        }
        fun drawBottomPlate(heightFactor: Float = 0.12f, inset: Float = margin): RectF {
            val height = (minSide * heightFactor).coerceAtLeast(76f)
            val plate = RectF(inset, output.height - inset - height, output.width - inset, output.height - inset)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = panelColor
                style = Paint.Style.FILL
                setShadowLayer((minSide * 0.006f).coerceAtLeast(3f), 0f, 3f, AndroidColor.BLACK)
            }
            val plateRadius = (minSide * 0.012f).coerceAtLeast(7f)
            if (glassEnabled) {
                drawLiquidGlassPanel(plate, plateRadius)
            } else if (frameStyle != "Transparent") {
                canvas.drawRoundRect(plate, plateRadius, plateRadius, paint)
            }
            return plate
        }
        fun drawWatermarkFooter(rect: RectF, radius: Float = 0f) {
            when (frameStyle) {
                "White", "Black" -> {
                    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (frameStyle == "Black") AndroidColor.BLACK else AndroidColor.WHITE
                        style = Paint.Style.FILL
                    }
                    if (radius > 0f) canvas.drawRoundRect(rect, radius, radius, fill)
                    else canvas.drawRect(rect, fill)
                }
                "LiquidGlass" -> drawLiquidGlassPanel(rect, radius.coerceAtLeast(minSide * 0.012f))
                "Transparent" -> Unit
            }
        }
        fun brandAccentColor(): Int = cameraWatermarkBrandAccent(watermarkBrand)
        fun drawBrandAccent(rect: RectF, thickness: Float) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = brandAccentColor()
                style = Paint.Style.FILL
            }
            canvas.drawRect(rect.left, rect.top, rect.right, rect.top + thickness, paint)
        }

        val sharedFooterRadius = (minSide * 0.012f).coerceAtLeast(7f)
        val sharedLogoRatio = brandLogo?.let {
            if (it.intrinsicHeight > 0) it.intrinsicWidth.toFloat() / it.intrinsicHeight.toFloat() else 3f
        } ?: when {
            watermarkBrand == "hasselblad" -> 4.9f
            watermarkBrand == "panasonic" || watermarkBrand == "om_system" -> 4.6f
            watermarkBrand == "insta360" -> 3.9f
            else -> 4.4f
        }
        fun drawTemplateText(
            value: String,
            source: Paint,
            x: Float,
            centerY: Float,
            maxWidth: Float,
            size: Float,
            centered: Boolean = false
        ) {
            if (value.isBlank()) return
            val paint = Paint(source).apply {
                textAlign = if (centered) Paint.Align.CENTER else Paint.Align.LEFT
                textSize = size
            }
            var rendered = value
            while (paint.measureText(rendered) > maxWidth && paint.textSize > 9f) {
                paint.textSize = (paint.textSize * 0.93f).coerceAtLeast(9f)
            }
            if (paint.measureText(rendered) > maxWidth) {
                while (rendered.length > 1 && paint.measureText(rendered + "…") > maxWidth) {
                    rendered = rendered.dropLast(1)
                }
                rendered = rendered.trimEnd() + "…"
            }
            val metrics = paint.fontMetrics
            val baseline = centerY - (metrics.ascent + metrics.descent) * 0.5f
            canvas.drawText(rendered, x, baseline, paint)
        }

        fun drawTemplateFooter(color: Int) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
            canvas.drawRect(footerRect, paint)
        }

        val footerWidth = footerRect.width().coerceAtLeast(1f)
        val footerHeight = footerRect.height().coerceAtLeast(1f)
        val centerX = footerRect.centerX()
        val centerY = footerRect.centerY()
        val pad = footerWidth * 0.045f
        val primarySize = (minSide * 0.023f).coerceIn(19f, 80f)
        val secondarySize = (minSide * 0.014f).coerceIn(12f, 44f)
        val infoSize = (minSide * 0.019f).coerceIn(17f, 68f)
        val dateSize = (minSide * 0.013f).coerceIn(12f, 40f)
        val cameraTitle = modelLine.ifBlank { brandTitle }
        val cameraLine = listOfNotNull(
            modelLine.takeIf { it.isNotBlank() }?.let { "MODEL $it" },
            lensModel?.takeIf { it.isNotBlank() }
        ).joinToString("  ·  ").ifBlank { cameraLabel }
        val exposureLine = specs.ifBlank { "ISO N/A" }
        val dateLabel = dateOnlyLine.ifBlank { timeLine }

        when (template) {
            "dark" -> {
                drawTemplateFooter(AndroidColor.rgb(18, 20, 23))
                val white = Paint(titlePaint).apply { color = AndroidColor.WHITE }
                val muted = Paint(detailPaint).apply { color = AndroidColor.rgb(190, 196, 202) }
                val accent = Paint(accentPaint).apply { color = brandAccentColor() }
                drawTemplateText(cameraLine, white, footerRect.left + pad, centerY - footerHeight * 0.16f,
                    footerWidth * 0.53f, primarySize)
                drawTemplateText(exposureLine, accent, footerRect.left + pad, centerY + footerHeight * 0.20f,
                    footerWidth * 0.53f, secondarySize)
                drawTemplateText(dateLabel, muted, footerRect.right - pad - footerWidth * 0.34f,
                    centerY + footerHeight * 0.20f, footerWidth * 0.34f, dateSize)
                val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = brandAccentColor(); strokeWidth = (minSide * 0.0012f).coerceAtLeast(1.5f)
                }
                canvas.drawLine(footerRect.left + footerWidth * 0.61f, footerRect.top + footerHeight * 0.2f,
                    footerRect.left + footerWidth * 0.61f, footerRect.bottom - footerHeight * 0.2f, rule)
                val logoH = footerHeight * 0.35f
                val logoW = minOf(footerWidth * 0.26f, logoH * sharedLogoRatio)
                drawBrandLogo(RectF(footerRect.right - pad - logoW, centerY - logoH / 2f,
                    footerRect.right - pad, centerY + logoH / 2f))
            }
            "minimal" -> {
                val darkText = frameStyle == "White" || frameStyle == "LiquidGlass"
                val linePaint = Paint(titlePaint).apply {
                    color = if (darkText) AndroidColor.BLACK else AndroidColor.WHITE
                    textSize = secondarySize
                }
                if (frameStyle == "White" || frameStyle == "Black") drawTemplateFooter(frameColor)
                val oneLine = listOfNotNull(cameraTitle.takeIf { it.isNotBlank() }, exposureLine.takeIf { showSpecs }, dateLabel.takeIf { showTime })
                    .joinToString("   ·   ")
                linePaint.setShadowLayer(if (frameStyle == "Transparent") 3f else 0f, 0f, 1f, AndroidColor.BLACK)
                drawTemplateText(oneLine, linePaint, centerX, footerRect.top + footerHeight * 0.55f,
                    footerWidth * 0.92f, secondarySize, centered = true)
            }
            "simple" -> {
                if (frameStyle == "White" || frameStyle == "Black") drawTemplateFooter(frameColor)
                else if (frameStyle == "LiquidGlass") drawLiquidGlassPanel(footerRect, sharedFooterRadius)
                val logoH = footerHeight * 0.38f
                val logoW = minOf(footerWidth * 0.48f, logoH * sharedLogoRatio)
                drawBrandLogo(RectF(centerX - logoW / 2f, footerRect.top + footerHeight * 0.08f,
                    centerX + logoW / 2f, footerRect.top + footerHeight * 0.08f + logoH))
                drawTemplateText(cameraTitle, titlePaint, centerX, footerRect.top + footerHeight * 0.70f,
                    footerWidth * 0.86f, primarySize, centered = true)
            }
            "micro" -> {
                val bg = if (frameStyle == "Black") AndroidColor.BLACK else AndroidColor.WHITE
                if (frameStyle != "Transparent" && frameStyle != "LiquidGlass") drawTemplateFooter(bg)
                else if (frameStyle == "LiquidGlass") drawLiquidGlassPanel(footerRect, sharedFooterRadius)
                val text = listOfNotNull(cameraTitle, specs.takeIf { showSpecs }, dateLabel.takeIf { showTime })
                    .filter { it.isNotBlank() }.joinToString("  ·  ")
                val ink = Paint(detailPaint).apply {
                    color = if (frameStyle == "White" || frameStyle == "LiquidGlass") AndroidColor.BLACK else AndroidColor.WHITE
                    textSize = dateSize
                }
                drawTemplateText(text, ink, centerX, centerY, footerWidth * 0.94f,
                    dateSize, centered = true)
            }
            "vintage" -> {
                drawTemplateFooter(AndroidColor.rgb(236, 227, 207))
                val ink = Paint(titlePaint).apply {
                    color = AndroidColor.rgb(75, 58, 41)
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                val faded = Paint(detailPaint).apply {
                    color = AndroidColor.rgb(134, 112, 79)
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                val lineOne = cameraTitle.uppercase()
                val lineTwo = listOfNotNull(exifFocal, exifAperture, exifShutter, exifIso?.let { "ISO $it" })
                    .joinToString("  ")
                drawTemplateText(lineOne, ink, centerX, footerRect.top + footerHeight * 0.39f,
                    footerWidth * 0.88f, primarySize * 0.8f, centered = true)
                drawTemplateText(lineTwo, faded, centerX, footerRect.top + footerHeight * 0.70f,
                    footerWidth * 0.90f, secondarySize, centered = true)
                val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.rgb(75, 58, 41); strokeWidth = (minSide * 0.001f).coerceAtLeast(1f)
                }
                canvas.drawLine(footerRect.left + pad, footerRect.top + footerHeight * 0.12f,
                    footerRect.right - pad, footerRect.top + footerHeight * 0.12f, rule)
            }
            "polaroid" -> {
                drawTemplateFooter(AndroidColor.rgb(250, 249, 245))
                val captionPaint = Paint(titlePaint).apply {
                    color = AndroidColor.rgb(35, 35, 35)
                    typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
                }
                val small = Paint(detailPaint).apply { color = AndroidColor.rgb(115, 115, 115) }
                drawTemplateText(cameraTitle, captionPaint, footerRect.left + pad,
                    footerRect.top + footerHeight * 0.56f, footerWidth * 0.62f,
                    primarySize * 1.05f)
                drawTemplateText(listOfNotNull(exposureLine.takeIf { showSpecs }, dateLabel.takeIf { showTime })
                    .joinToString("  ·  "), small, footerRect.right - pad - footerWidth * 0.32f,
                    footerRect.top + footerHeight * 0.56f, footerWidth * 0.32f,
                    secondarySize)
            }
            "gallery" -> {
                drawTemplateFooter(AndroidColor.rgb(243, 241, 236))
                val ink = Paint(titlePaint).apply { color = AndroidColor.rgb(29, 29, 29) }
                val quiet = Paint(detailPaint).apply { color = AndroidColor.rgb(112, 108, 100) }
                val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.rgb(218, 215, 207); style = Paint.Style.STROKE
                    strokeWidth = (minSide * 0.001f).coerceAtLeast(1f)
                }
                canvas.drawRect(footerRect.left + pad * 0.45f, footerRect.top + footerHeight * 0.12f,
                    footerRect.right - pad * 0.45f, footerRect.bottom - footerHeight * 0.12f, border)
                drawTemplateText(cameraTitle, ink, centerX, footerRect.top + footerHeight * 0.43f,
                    footerWidth * 0.86f, primarySize, centered = true)
                drawTemplateText(listOfNotNull(exposureLine.takeIf { showSpecs }, dateLabel.takeIf { showTime })
                    .joinToString("  ·  "), quiet, centerX, footerRect.top + footerHeight * 0.72f,
                    footerWidth * 0.86f, secondarySize, centered = true)
            }
            "frosted" -> {
                drawLiquidGlassPanel(footerRect, (sharedFooterRadius * (1f + glassGooey * 0.6f)).coerceAtLeast(8f))
                val ink = Paint(titlePaint).apply { color = if (sampleBitmapLuminance(output) > 0.52f) AndroidColor.BLACK else AndroidColor.WHITE }
                val quiet = Paint(detailPaint).apply { color = ink.color; alpha = 210 }
                val logoH = footerHeight * 0.38f
                val logoW = minOf(footerWidth * 0.30f, logoH * sharedLogoRatio)
                drawBrandLogo(RectF(footerRect.left + pad, centerY - logoH / 2f,
                    footerRect.left + pad + logoW, centerY + logoH / 2f))
                drawTemplateText(cameraLine, ink, footerRect.left + pad + logoW + pad * 0.45f,
                    centerY - footerHeight * 0.12f, footerWidth * 0.48f, primarySize * 0.8f)
                drawTemplateText(exposureLine, quiet, footerRect.left + pad + logoW + pad * 0.45f,
                    centerY + footerHeight * 0.20f, footerWidth * 0.48f, secondarySize)
            }
            "film" -> {
                drawTemplateFooter(AndroidColor.rgb(18, 17, 16))
                val orange = Paint(titlePaint).apply { color = AndroidColor.rgb(240, 162, 74); typeface = android.graphics.Typeface.MONOSPACE }
                val line = Paint(detailPaint).apply { color = AndroidColor.rgb(240, 162, 74); typeface = android.graphics.Typeface.MONOSPACE }
                val holeW = footerWidth * 0.024f
                val holeH = footerHeight * 0.16f
                val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(230, 224, 211) }
                for (i in 0..((footerWidth / (holeW * 2.6f)).toInt().coerceAtMost(60))) {
                    val x = footerRect.left + i * holeW * 2.6f + holeW
                    canvas.drawRoundRect(RectF(x, footerRect.top + footerHeight * 0.08f,
                        x + holeW, footerRect.top + footerHeight * 0.08f + holeH),
                        holeW * 0.18f, holeW * 0.18f, holePaint)
                    canvas.drawRoundRect(RectF(x, footerRect.bottom - footerHeight * 0.08f - holeH,
                        x + holeW, footerRect.bottom - footerHeight * 0.08f),
                        holeW * 0.18f, holeW * 0.18f, holePaint)
                }
                drawTemplateText(cameraTitle.uppercase(), orange, footerRect.left + pad,
                    footerRect.top + footerHeight * 0.44f, footerWidth * 0.55f,
                    secondarySize, centered = false)
                drawTemplateText(listOfNotNull(exposureLine.takeIf { showSpecs }, dateLabel.takeIf { showTime })
                    .joinToString("  ·  "), line, footerRect.right - pad - footerWidth * 0.34f,
                    footerRect.top + footerHeight * 0.44f, footerWidth * 0.34f,
                    dateSize, centered = false)
            }
            "datestamp" -> {
                val stamp = timeLine.ifBlank { dateLabel }
                val ink = Paint(titlePaint).apply {
                    color = AndroidColor.rgb(255, 150, 55)
                    typeface = android.graphics.Typeface.MONOSPACE
                    setShadowLayer((minSide * 0.0022f).coerceAtLeast(2f), 0f, 1f, AndroidColor.BLACK)
                    textAlign = Paint.Align.RIGHT
                }
                val metrics = ink.fontMetrics
                canvas.drawText(stamp, footerRect.right - pad,
                    footerRect.bottom - pad - metrics.descent, ink)
            }
            else -> {
                    // The existing Fuji/Sony/Nikon/Canon lockup is retained for Auto and Signature.
    
            // brand logo + camera model on the left; shooting settings + capture date on the right.
            // The logo is vertically centred against the left text stack, not against the footer top.
            val footerRadius = (minSide * 0.012f).coerceAtLeast(7f)
            drawWatermarkFooter(footerRect, footerRadius)
    
            val footerWidth = footerRect.width().coerceAtLeast(1f)
            val footerHeight = footerRect.height().coerceAtLeast(1f)
            val horizontalInset = footerWidth * 0.045f
            val dividerX = footerRect.left + footerWidth * 0.565f
            val leftContentWidth = (dividerX - footerRect.left - horizontalInset * 1.5f).coerceAtLeast(1f)
            val rightLeft = dividerX + footerWidth * 0.045f
            val rightRight = footerRect.right - horizontalInset
            val rightContentWidth = (rightRight - rightLeft).coerceAtLeast(1f)
    
            val logoColumnLeft = footerRect.left + horizontalInset
            val logoColumnWidth = leftContentWidth * 0.34f
            val modelLeft = logoColumnLeft + logoColumnWidth + footerWidth * 0.025f
            val modelWidth = (dividerX - modelLeft - footerWidth * 0.035f).coerceAtLeast(1f)
            val modelText = modelLine.takeIf { it.isNotBlank() }?.let { "MODEL  $it" }.orEmpty()
            val leftSecondary = when {
                lensModel.isNullOrBlank() && locationLine.isBlank() -> ""
                template == "fuji_signature" && locationLine.isNotBlank() -> locationLine
                else -> lensModel.orEmpty()
            }
            val leftHasTitle = modelText.isNotBlank() || brandTitle.isNotBlank()
            val leftHasSecondary = leftSecondary.isNotBlank()
            val rowTopY = footerRect.top + footerHeight * 0.34f
            val rowBottomY = footerRect.top + footerHeight * 0.68f
            val leftTitleCenterY = if (leftHasTitle && leftHasSecondary) rowTopY else footerRect.top + footerHeight * 0.51f
            val leftSecondaryCenterY = if (leftHasTitle && leftHasSecondary) rowBottomY else leftTitleCenterY
            val logoCenterY = if (leftHasTitle && leftHasSecondary) {
                (leftTitleCenterY + leftSecondaryCenterY) * 0.5f
            } else {
                leftTitleCenterY
            }
    
            val logoRatio = brandLogo?.let {
                if (it.intrinsicHeight > 0) it.intrinsicWidth.toFloat() / it.intrinsicHeight.toFloat() else 3f
            } ?: when (watermarkBrand) {
                "sony" -> 2.8f
                "nikon" -> 3.1f
                "canon" -> 3.0f
                else -> 5.95f
            }
            val logoHeight = minOf(footerHeight * 0.46f, logoColumnWidth / logoRatio)
            val logoWidth = logoHeight * logoRatio
            val logoCenterX = logoColumnLeft + logoColumnWidth * 0.5f
            drawBrandLogo(
                RectF(
                    logoCenterX - logoWidth * 0.5f,
                    logoCenterY - logoHeight * 0.5f,
                    logoCenterX + logoWidth * 0.5f,
                    logoCenterY + logoHeight * 0.5f
                )
            )
    
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = brandAccentColor()
                alpha = if (frameStyle == "Transparent") 215 else 155
                style = Paint.Style.STROKE
                strokeWidth = (minSide * 0.0012f).coerceIn(1.5f, 4.5f)
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(
                dividerX,
                footerRect.top + footerHeight * 0.20f,
                dividerX,
                footerRect.bottom - footerHeight * 0.20f,
                dividerPaint
            )
    
            fun drawMeasuredLine(
                value: String,
                sourcePaint: Paint,
                centerY: Float,
                x: Float,
                maxWidth: Float,
                baseSize: Float,
                minimumSize: Float
            ) {
                if (value.isBlank()) return
                val paint = Paint(sourcePaint).apply {
                    textAlign = Paint.Align.LEFT
                    textSize = baseSize
                }
                var fittedText = value
                while (paint.measureText(fittedText) > maxWidth && paint.textSize > minimumSize) {
                    paint.textSize = (paint.textSize * 0.94f).coerceAtLeast(minimumSize)
                }
                if (paint.measureText(fittedText) > maxWidth) {
                    val ellipsis = "…"
                    var endIndex = fittedText.length
                    while (endIndex > 1 &&
                        paint.measureText(fittedText.substring(0, endIndex).trimEnd() + ellipsis) > maxWidth
                    ) {
                        endIndex--
                    }
                    fittedText = fittedText.substring(0, endIndex).trimEnd() + ellipsis
                }
                val metrics = paint.fontMetrics
                val baseline = centerY - (metrics.ascent + metrics.descent) * 0.5f
                canvas.drawText(fittedText, x, baseline, paint)
            }
    
            // Explicit text anchors keep the left model/lens and right exposure/date columns balanced.
            val titleSize = (minSide * 0.023f).coerceIn(20f, 82f)
            val secondarySize = (minSide * 0.014f).coerceIn(13f, 46f)
            val specsSize = (minSide * 0.020f).coerceIn(18f, 72f)
            val dateSize = (minSide * 0.014f).coerceIn(13f, 46f)
            val leftTitle = modelText.ifBlank { brandTitle }
            val rightDate = dateOnlyLine.ifBlank { timeLine }
            drawMeasuredLine(leftTitle, titlePaint, leftTitleCenterY, modelLeft, modelWidth, titleSize, (titleSize * 0.72f).coerceAtLeast(16f))
            drawMeasuredLine(leftSecondary, detailPaint, leftSecondaryCenterY, modelLeft, modelWidth, secondarySize, (secondarySize * 0.72f).coerceAtLeast(12f))
            drawMeasuredLine(specs, accentPaint, footerRect.top + footerHeight * 0.39f, rightLeft, rightContentWidth, specsSize, (specsSize * 0.72f).coerceAtLeast(15f))
            drawMeasuredLine(rightDate, detailPaint, footerRect.top + footerHeight * 0.70f, rightLeft, rightContentWidth, dateSize, (dateSize * 0.72f).coerceAtLeast(12f))
            }
        }

        val outputWritten = context.contentResolver.openOutputStream(uri, "wt")?.use {
            output.compress(Bitmap.CompressFormat.JPEG, 98, it)
        } ?: false
        if (!outputWritten) return@runCatching false
        val exifWritten = restoreImageExifAttributes(context, uri, sourceExif, output.width, output.height)
        if (!exifWritten) return@runCatching false
        setMediaStoreOrientationUpright(context, uri)
        true
        } finally {
            watermarkCropBitmap?.let { if (!it.isRecycled) it.recycle() }
            watermarkBrandBitmap?.let { if (!it.isRecycled) it.recycle() }
            outputBitmap?.let { if (it !== sourceBitmap && !it.isRecycled) it.recycle() }
            if (!sourceBitmap.isRecycled) sourceBitmap.recycle()
        }
    }.onFailure {
        android.util.Log.e("FUJI_WATERMARK", "applyCameraWatermark failed", it)
    }.getOrDefault(false)
}

private fun sampleBitmapColor(bitmap: Bitmap, rect: RectF): Int {
    val left = rect.left.toInt().coerceIn(0, bitmap.width - 1)
    val top = rect.top.toInt().coerceIn(0, bitmap.height - 1)
    val right = rect.right.toInt().coerceIn(left + 1, bitmap.width)
    val bottom = rect.bottom.toInt().coerceIn(top + 1, bitmap.height)
    var count = 0
    var red = 0L
    var green = 0L
    var blue = 0L
    for (row in 0 until 8) {
        val y = top + (bottom - top - 1) * row / 7
        for (column in 0 until 8) {
            val x = left + (right - left - 1) * column / 7
            val color = bitmap.getPixel(x, y)
            red += AndroidColor.red(color)
            green += AndroidColor.green(color)
            blue += AndroidColor.blue(color)
            count++
        }
    }
    return AndroidColor.rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
}

private fun sampleBitmapLuminance(bitmap: Bitmap): Float {
    val width = bitmap.width
    val height = bitmap.height
    var total = 0f
    var count = 0
    for (row in 1..7) {
        for (column in 1..7) {
            val x = (width * column / 8).coerceIn(0, width - 1)
            val y = (height * row / 8).coerceIn(0, height - 1)
            val pixel = bitmap.getPixel(x, y)
            total += (AndroidColor.red(pixel) * 0.2126f +
                AndroidColor.green(pixel) * 0.7152f +
                AndroidColor.blue(pixel) * 0.0722f) / 255f
            count++
        }
    }
    return if (count == 0) 0.5f else total / count
}

/** Fast separable box blur on a small watermark panel crop, never on the full photo. */
private fun applyWatermarkBoxBlur(bitmap: Bitmap, radius: Int, passes: Int) {
    if (radius <= 0 || bitmap.width < 2 || bitmap.height < 2) return
    val width = bitmap.width
    val height = bitmap.height
    var source = IntArray(width * height)
    var temporary = IntArray(width * height)
    bitmap.getPixels(source, 0, width, 0, 0, width, height)
    repeat(passes.coerceIn(1, 3)) {
        for (y in 0 until height) {
            var a = 0
            var r = 0
            var g = 0
            var b = 0
            val diameter = radius * 2 + 1
            for (offset in -radius..radius) {
                val pixel = source[y * width + offset.coerceIn(0, width - 1)]
                a += AndroidColor.alpha(pixel)
                r += AndroidColor.red(pixel)
                g += AndroidColor.green(pixel)
                b += AndroidColor.blue(pixel)
            }
            for (x in 0 until width) {
                temporary[y * width + x] = AndroidColor.argb(
                    a / diameter, r / diameter, g / diameter, b / diameter
                )
                val remove = source[y * width + (x - radius).coerceIn(0, width - 1)]
                val add = source[y * width + (x + radius + 1).coerceIn(0, width - 1)]
                a += AndroidColor.alpha(add) - AndroidColor.alpha(remove)
                r += AndroidColor.red(add) - AndroidColor.red(remove)
                g += AndroidColor.green(add) - AndroidColor.green(remove)
                b += AndroidColor.blue(add) - AndroidColor.blue(remove)
            }
        }
        for (x in 0 until width) {
            var a = 0
            var r = 0
            var g = 0
            var b = 0
            val diameter = radius * 2 + 1
            for (offset in -radius..radius) {
                val pixel = temporary[offset.coerceIn(0, height - 1) * width + x]
                a += AndroidColor.alpha(pixel)
                r += AndroidColor.red(pixel)
                g += AndroidColor.green(pixel)
                b += AndroidColor.blue(pixel)
            }
            for (y in 0 until height) {
                source[y * width + x] = AndroidColor.argb(
                    a / diameter, r / diameter, g / diameter, b / diameter
                )
                val remove = temporary[(y - radius).coerceIn(0, height - 1) * width + x]
                val add = temporary[(y + radius + 1).coerceIn(0, height - 1) * width + x]
                a += AndroidColor.alpha(add) - AndroidColor.alpha(remove)
                r += AndroidColor.red(add) - AndroidColor.red(remove)
                g += AndroidColor.green(add) - AndroidColor.green(remove)
                b += AndroidColor.blue(add) - AndroidColor.blue(remove)
            }
        }
        val swap = source
        source = temporary
        temporary = swap
    }
    bitmap.setPixels(source, 0, width, 0, 0, width, height)
}

@Composable
fun ContentManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fujifilm_contents", 0) }
    val scope = rememberCoroutineScope()
    var frames by remember {
        mutableStateOf(prefs.getStringSet("frames", emptySet()).orEmpty().toList())
    }
    var luts by remember { mutableStateOf(loadCustomCameraLuts(context)) }
    var lutSearch by remember { mutableStateOf("") }
    var activeLutIds by remember { mutableStateOf(activeCustomCameraLutIds(context)) }
    var pendingDeleteLut by remember { mutableStateOf<CustomCameraLut?>(null) }
    var isImportingLut by remember { mutableStateOf(false) }

    val lutPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                isImportingLut = true
                try {
                    val imported = withContext(Dispatchers.IO) { importCustomCameraLuts(context, uri) }
                    luts = loadCustomCameraLuts(context)
                    activeLutIds = activeCustomCameraLutIds(context)
                    Toast.makeText(
                        context,
                        "Đã nhập " + imported.size + " LUT. Chọn Add để bật từng bộ lọc.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (failure: Throwable) {
                    Toast.makeText(
                        context,
                        "Import LUT thất bại: " + (failure.message ?: "không rõ lỗi"),
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    isImportingLut = false
                }
            }
        }
    }
    val framePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val value = uri.toString()
            if (!frames.contains(value)) frames = frames + value
            prefs.edit().putStringSet("frames", frames.toSet()).apply()
        }
    }

    Column(
        Modifier.fillMaxSize().background(CamColors.Bg)
            .verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        PageHeader("Contents Management", onBack)

        Text("LUT Manage", color = CamColors.TextColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(
            "Nhập LUT .cube, LUT Canon RGB 33x33x33 (.bin), hoặc ZIP chứa tối đa 700 LUT .cube/.bin. Hỗ trợ ZIP lồng. LUT Log được giữ riêng, không áp nhầm lên JPEG sRGB.",
            color = CamColors.Sub,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = lutSearch,
            onValueChange = { lutSearch = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
            label = { Text("Tìm LUT theo tên hoặc nhóm") }
        )
        Button(
            onClick = { lutPicker.launch(arrayOf("*/*")) },
            enabled = !isImportingLut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(if (isImportingLut) "Đang nhập LUT…" else "Import LUT")
        }
        if (luts.isEmpty()) {
            EmptyNote("Chưa có LUT nào. Hỗ trợ .cube, Canon RGB .bin và ZIP chứa nhiều LUT.")
        } else {
            val matchingLuts = luts.filter {
                lutSearch.isBlank() ||
                    it.name.contains(lutSearch.trim(), ignoreCase = true) ||
                    it.category.contains(lutSearch.trim(), ignoreCase = true)
            }
            Text(
                "Hiển thị ${matchingLuts.size.coerceAtMost(100)} / ${matchingLuts.size} LUT khớp · Tổng ${luts.size}",
                color = CamColors.Sub,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 5.dp)
            )
            matchingLuts.take(100).forEach { entry ->
                val isActive = activeLutIds.contains(entry.id)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp)).background(CamColors.Card)
                        .padding(start = 12.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = CamColors.Yellow)
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(entry.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            (if (isActive) "Đã thêm · " else "Chưa thêm · ") + entry.category,
                            color = CamColors.Sub,
                            fontSize = 9.sp
                        )
                    }
                    TextButton(onClick = {
                        setCustomCameraLutActive(context, entry.id, !isActive)
                        activeLutIds = activeCustomCameraLutIds(context)
                    }) {
                        Text(if (isActive) "Bỏ" else "Add")
                    }
                    IconButton(onClick = { pendingDeleteLut = entry }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete LUT", tint = CamColors.Sub)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Frame Manage", color = CamColors.TextColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(
            "Nhập khung phủ PNG/JPG để quản lý.",
            color = CamColors.Sub,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Button(
            onClick = { framePicker.launch(arrayOf("image/png", "image/jpeg")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Import Frame")
        }
        if (frames.isEmpty()) {
            EmptyNote("Chưa có frame nào được nhập.")
        } else {
            frames.forEach { value ->
                val uri = Uri.parse(value)
                val name = documentDisplayName(context, uri)
                ManagedItem(name, onDelete = {
                    frames = frames.filterNot { it == value }
                    prefs.edit().putStringSet("frames", frames.toSet()).apply()
                })
            }
        }
    }

    pendingDeleteLut?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDeleteLut = null },
            title = { Text("Xóa LUT?") },
            text = { Text("Bạn có chắc chắn muốn xóa LUT \"" + entry.name + "\"? LUT này cũng sẽ bị gỡ khỏi mục Bộ lọc.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteCustomCameraLut(context, entry.id)
                    luts = loadCustomCameraLuts(context)
                    activeLutIds = activeCustomCameraLutIds(context)
                    pendingDeleteLut = null
                }) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteLut = null }) { Text("Hủy") }
            }
        )
    }
}

private fun documentDisplayName(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(title, color = CamColors.TextColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ToolToggle(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(16.dp))
            .background(CamColors.Card).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CamColors.Yellow)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(description, color = CamColors.Sub, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ManagedItem(name: String, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(10.dp))
            .background(CamColors.Card).padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Image, contentDescription = null, tint = CamColors.Sub)
        Text(name, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = CamColors.Sub)
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
}


private data class AlbumPhotoEntry(val uri: Uri, val dateAdded: Long)

@Composable
private fun AlbumLoadingSkeleton() {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Đang tải thư viện ảnh…", color = CamColors.Sub, fontSize = 11.sp,
            modifier = Modifier.padding(start = 3.dp, bottom = 8.dp))
        repeat(3) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(3) {
                    Box(
                        Modifier.weight(1f).aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CamColors.Card)
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<AlbumPhotoEntry>>(emptyList()) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var inspectorVisible by remember { mutableStateOf(false) }
    var inspectorRows by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        photos = withContext(Dispatchers.IO) {
            runCatching {
                val result = ArrayList<AlbumPhotoEntry>()
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
                    "${MediaStore.Images.Media.RELATIVE_PATH} = ?",
                    arrayOf("Pictures/FujifilmMode/"),
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        result.add(
                            AlbumPhotoEntry(
                                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                                cursor.getLong(dateColumn)
                            )
                        )
                    }
                }
                result
            }.getOrDefault(emptyList())
        }
        isLoading = false
    }

    LaunchedEffect(selectedPhoto, inspectorVisible) {
        if (!inspectorVisible) {
            inspectorRows = null
        } else {
            val uri = selectedPhoto ?: return@LaunchedEffect
            inspectorRows = withContext(Dispatchers.IO) {
                runCatching { readPhotoInspectorInfo(context, uri) }
                    .getOrElse { listOf("Thông tin ảnh" to "Không thể đọc metadata: ${it.message ?: "lỗi không xác định"}") }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(CamColors.Bg).padding(12.dp)) {
        PageHeader("Album · Fujifilm Mode", onBack)
        when {
            isLoading -> AlbumLoadingSkeleton()
            photos.isEmpty() -> EmptyNote("Chưa có ảnh. Ảnh chụp trong ứng dụng sẽ xuất hiện ở đây.")
            else -> Text("${photos.size} ảnh", color = CamColors.Sub, fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(photos, key = { it.uri.toString() }) { entry ->
                var bitmap by remember(entry.uri) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(entry.uri) {
                    bitmap = withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.loadThumbnail(entry.uri, Size(360, 360), null).asImageBitmap()
                        }.getOrNull()
                    }
                }
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                        .background(CamColors.Card).animaTClickable { selectedPhoto = entry.uri }
                ) {
                    if (bitmap != null) {
                        Image(bitmap!!, "Photo thumbnail", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }
        }
    }

    if (selectedPhoto != null) {
        Dialog(
            onDismissRequest = { selectedPhoto = null; selectedBitmap = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            LaunchedEffect(selectedPhoto) {
                selectedBitmap = withContext(Dispatchers.IO) {
                    runCatching {
                        decodeUprightBitmap(context, selectedPhoto!!)?.asImageBitmap()
                    }.getOrNull()
                }
            }
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (selectedBitmap != null) {
                    Image(selectedBitmap!!, "Ảnh trong album", Modifier.fillMaxSize().padding(12.dp),
                        contentScale = ContentScale.Fit)
                } else {
                    Text("Đang tải ảnh…", color = Color.White, modifier = Modifier.align(Alignment.Center))
                }
                Row(
                    Modifier.align(Alignment.TopEnd).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Thông tin", color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(CamColors.Glass)
                            .animaTClickable { inspectorVisible = true }.padding(horizontal = 12.dp, vertical = 10.dp))
                    Text("Đóng", color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(CamColors.Glass)
                            .animaTClickable {
                                selectedPhoto = null
                                selectedBitmap = null
                                inspectorVisible = false
                            }.padding(horizontal = 12.dp, vertical = 10.dp))
                }
            }
        }
    }

    if (inspectorVisible && selectedPhoto != null) {
        AlertDialog(
            onDismissRequest = { inspectorVisible = false },
            title = { Text("Photo Inspector", color = CamColors.TextColor) },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    val rows = inspectorRows
                    if (rows == null) {
                        Text("Đang đọc metadata ảnh…", color = CamColors.Sub)
                    } else {
                        rows.forEach { (label, value) ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = CamColors.Sub, fontSize = 12.sp,
                                    modifier = Modifier.weight(0.42f))
                                Text(value, color = CamColors.TextColor, fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    modifier = Modifier.weight(0.58f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectorVisible = false }) { Text("Đóng") }
            }
        )
    }
}


private fun readPhotoInspectorInfo(context: Context, uri: Uri): List<Pair<String, String>> {
    var fileName = "Không có"
    var fileSize = -1L
    var mimeType = "Không có"
    var width = -1
    var height = -1
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                fun stringAt(column: String): String? {
                    val index = cursor.getColumnIndex(column)
                    return if (index >= 0 && !cursor.isNull(index)) cursor.getString(index) else null
                }
                fun longAt(column: String): Long? {
                    val index = cursor.getColumnIndex(column)
                    return if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null
                }
                fileName = stringAt(OpenableColumns.DISPLAY_NAME) ?: fileName
                fileSize = longAt(OpenableColumns.SIZE) ?: fileSize
                mimeType = stringAt(MediaStore.MediaColumns.MIME_TYPE) ?: mimeType
                width = longAt(MediaStore.Images.Media.WIDTH)?.toInt() ?: width
                height = longAt(MediaStore.Images.Media.HEIGHT)?.toInt() ?: height
            }
        }
    }
    val exif = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input -> ExifInterface(input) }
    }.getOrNull()
    // MediaStore WIDTH/HEIGHT may lag behind a replaced JPEG after LunaDream output.
    // Prefer the actual encoded pixel bounds, then apply EXIF axis rotation so the
    // Inspector reports the dimensions as the user sees the upright photo.
    val exifOrientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0) ?: 0
    val actualDimensions = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        PhotoInspectorGeometry.fromEncoded(bounds.outWidth, bounds.outHeight, exifOrientation)
    }.getOrNull()
    actualDimensions?.let {
        width = it.width
        height = it.height
    }
    val photoPrefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    val lunaDreamStatus = photoPrefs.getString("photo_lunadream_status:$uri", "Không có dữ liệu phiên LunaDream") ?: "Không có dữ liệu phiên LunaDream"
    val logicalCameraId = photoPrefs.getString("photo_camera_id:$uri", "Không công bố") ?: "Không công bố"
    val physicalCameraId = photoPrefs.getString("photo_physical_camera_id:$uri", "HAL không công bố") ?: "HAL không công bố"
    val processedOutputSize = photoPrefs.getString("photo_output_size:$uri", "Không ghi nhận") ?: "Không ghi nhận"
    fun tag(name: String): String? = exif?.getAttribute(name)?.trim()?.takeIf { it.isNotEmpty() }
    if (width <= 0) width = tag(ExifInterface.TAG_IMAGE_WIDTH)?.toIntOrNull() ?: -1
    if (height <= 0) height = tag(ExifInterface.TAG_IMAGE_LENGTH)?.toIntOrNull() ?: -1
    val makeModel = listOfNotNull(tag(ExifInterface.TAG_MAKE), tag(ExifInterface.TAG_MODEL))
        .distinct().joinToString(" ").ifBlank { "Không có EXIF" }
    val resolution = if (width > 0 && height > 0) {
        String.format(Locale.US, "%,d × %,d px", width, height)
    } else "Không có"
    val sizeLabel = when {
        fileSize < 0L -> "Không có"
        fileSize >= 1024L * 1024L -> String.format(Locale.US, "%.2f MB", fileSize / (1024.0 * 1024.0))
        fileSize >= 1024L -> String.format(Locale.US, "%.1f KB", fileSize / 1024.0)
        else -> "$fileSize bytes"
    }
    val exposure = tag(ExifInterface.TAG_EXPOSURE_TIME)?.let { raw ->
        raw.toDoubleOrNull()?.let { seconds ->
            if (seconds > 0.0 && seconds < 1.0) "1/${(1.0 / seconds).toInt()}s"
            else if (seconds >= 1.0) String.format(Locale.US, "%.2fs", seconds)
            else raw
        } ?: raw
    } ?: "Không có"
    val focal = tag(ExifInterface.TAG_FOCAL_LENGTH)?.let { "$it mm" } ?: "Không có"
    val aperture = tag(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" } ?: "Không có"
    val qualityBitmap = runCatching {
        decodeUprightBitmap(context, uri, maxDimension = 640)
    }.getOrNull()
    val qualityReport: PhotoQualityReport? = try {
        qualityBitmap?.let { bitmap -> PhotoQualityAnalyzer.analyze(bitmap) }
    } catch (failure: Throwable) {
        android.util.Log.w("FUJI_PHOTO_QUALITY", "Quick quality analysis failed", failure)
        null
    } finally {
        qualityBitmap?.let { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
    }
    return listOf(
        "Tên tệp" to fileName,
        "Độ phân giải" to resolution,
        "Dung lượng" to sizeLabel,
        "Định dạng" to mimeType,
        "Thiết bị" to makeModel,
        "Camera ID logic" to logicalCameraId,
        "Physical Camera ID thực tế" to physicalCameraId,
        "Ống kính" to (tag("LensModel") ?: "Không có EXIF"),
        "ISO" to (tag(ExifInterface.TAG_ISO_SPEED_RATINGS) ?: "Không có"),
        "Tốc độ màn trập" to exposure,
        "Khẩu độ" to aperture,
        "Tiêu cự" to focal,
        "Ngày chụp" to (tag(ExifInterface.TAG_DATETIME_ORIGINAL) ?: "Không có EXIF"),
        "Độ nét (ước tính)" to (qualityReport?.sharpnessLabel ?: "Không phân tích được"),
        "Vùng cháy sáng" to (qualityReport?.highlightClippingLabel ?: "Không phân tích được"),
        "Vùng tối bệt" to (qualityReport?.shadowClippingLabel ?: "Không phân tích được"),
        "Nhiễu nền (ước tính)" to (qualityReport?.noiseLabel ?: "Không phân tích được"),
        "Gợi ý chất lượng" to (qualityReport?.recommendation ?: "Không phân tích được"),
        "Kích thước sau xử lý" to processedOutputSize,
        "Trạng thái LunaDream" to lunaDreamStatus
    )
}


/**
 * Applies a selected border/layout frame to the already-upright capture.
 * Built-in layouts are original Android Canvas implementations based on the flexible
 * photo-card / clean / film / expo layout ideas demonstrated by phew.
 */
fun applyCameraFrameOverlay(context: Context, uri: Uri): Boolean {
    val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    val brandId = prefs.getString("watermark_brand", "fujifilm") ?: "fujifilm"
    val brandTitle = cameraWatermarkBrandTitle(brandId)
    val template = prefs.getString("selected_frame_template", "none") ?: "none"
    if (template == "none") return false

    return runCatching {
        val sourceExif = readImageExifAttributes(context, uri)
        val bitmap = decodeRawPixelBitmap(context, uri) ?: return@runCatching false
        var outputBitmap: Bitmap? = null
        try {
            val output = if (bitmap.isMutable) {
                bitmap.also { outputBitmap = it }
            } else {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)?.also { outputBitmap = it }
                    ?: return@runCatching false
            }
            val canvas = Canvas(output)
        val frameStyle = (prefs.getString("frame_style", "White") ?: "White").takeIf { it == "Black" || it == "White" } ?: "White"
        val transparent = false
        val frameColor = if (frameStyle == "Black") AndroidColor.BLACK else AndroidColor.WHITE
        val textColor = if (frameStyle == "Black" || transparent) AndroidColor.WHITE else AndroidColor.BLACK
        val minSide = minOf(output.width, output.height).toFloat()
        val margin = (minSide * 0.035f).coerceAtLeast(16f)
        val stroke = (minSide * 0.0032f).coerceAtLeast(2f)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = frameColor
            this.style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = frameColor
            this.style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (transparent) AndroidColor.WHITE else textColor
            typeface = android.graphics.Typeface.create(
                "sans-serif-medium", android.graphics.Typeface.NORMAL
            )
            textSize = (minSide * 0.022f).coerceIn(14f, 48f)
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (transparent) AndroidColor.WHITE else textColor
            alpha = 220
            typeface = android.graphics.Typeface.create(
                "sans-serif", android.graphics.Typeface.NORMAL
            )
            textSize = (minSide * 0.012f).coerceIn(10f, 26f)
        }
        fun border(rect: RectF, width: Float = stroke) {
            if (transparent) return
            linePaint.strokeWidth = width
            canvas.drawRect(rect, linePaint)
        }
        fun plate(rect: RectF, radius: Float = margin * 0.16f) {
            if (!transparent) canvas.drawRoundRect(rect, radius, radius, fillPaint)
        }
        fun caption(title: String, detail: String = "") {
            val bandHeight = (minSide * 0.075f).coerceAtLeast(42f)
            val band = RectF(margin, output.height - margin - bandHeight, output.width - margin, output.height - margin)
            plate(band)
            val x = band.left + margin * 0.42f
            val titleBaseline = band.top + band.height() * 0.43f
            canvas.drawText(title, x, titleBaseline, textPaint)
            if (detail.isNotBlank()) {
                canvas.drawText(detail.take(90), x, band.top + band.height() * 0.77f, detailPaint)
            }
        }

        if (template.startsWith("custom|")) {
            val frameUri = Uri.parse(template.removePrefix("custom|"))
            val imported = context.contentResolver.openInputStream(frameUri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@runCatching false
            try {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                if (!transparent) {
                    paint.colorFilter = android.graphics.PorterDuffColorFilter(
                        frameColor, android.graphics.PorterDuff.Mode.SRC_IN
                    )
                }
                canvas.drawBitmap(imported, null, RectF(0f, 0f, output.width.toFloat(), output.height.toFloat()), paint)
            } finally {
                if (!imported.isRecycled) imported.recycle()
            }
        } else {
            val outer = RectF(margin, margin, output.width - margin, output.height - margin)
            val inner = RectF(margin * 1.55f, margin * 1.55f,
                output.width - margin * 1.55f, output.height - margin * 1.55f)
            when (template) {
                "classic" -> {
                    border(outer, stroke * 1.8f)
                    border(inner, stroke * 0.55f)
                    caption("${brandTitle}  /  CAMERA", "PHOTO  •  ${java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.US).format(java.util.Date())}")
                }
                "clean" -> {
                    val corner = minSide * 0.085f
                    val p = Paint(linePaint).apply { strokeWidth = stroke * 1.5f }
                    if (!transparent) {
                        listOf(
                            Triple(margin, margin, 1), Triple(output.width - margin, margin, 2),
                            Triple(margin, output.height - margin, 3), Triple(output.width - margin, output.height - margin, 4)
                        ).forEach { (x, y, index) ->
                            val sx = if (index == 1 || index == 3) -1f else 1f
                            val sy = if (index >= 3) -1f else 1f
                            canvas.drawLine(x, y, x + corner * sx, y, p)
                            canvas.drawLine(x, y, x, y + corner * sy, p)
                        }
                    }
                    textPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("FUJIFILM MODE", output.width - margin, output.height - margin * 1.65f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                }
                "film" -> {
                    border(outer, stroke * 2f)
                    if (!transparent) {
                        val holeW = minSide * 0.028f
                        val holeH = minSide * 0.014f
                        val gap = holeW * 0.75f
                        val count = (output.width / (holeW + gap)).toInt().coerceIn(4, 28)
                        val start = (output.width - count * (holeW + gap) + gap) / 2f
                        for (i in 0 until count) {
                            val x = start + i * (holeW + gap)
                            canvas.drawRoundRect(RectF(x, margin * 0.28f, x + holeW, margin * 0.28f + holeH), 3f, 3f, fillPaint)
                            canvas.drawRoundRect(RectF(x, output.height - margin * 0.28f - holeH,
                                x + holeW, output.height - margin * 0.28f), 3f, 3f, fillPaint)
                        }
                    }
                    caption("35mm FILM", "FRAME  ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())}")
                }
                "polaroid" -> {
                    val band = RectF(margin, margin, output.width - margin, output.height - margin)
                    val imageInset = margin * 0.55f
                    val imageArea = RectF(band.left + imageInset, band.top + imageInset,
                        band.right - imageInset, band.bottom - margin * 2.35f)
                    // Paint only the outside strips. Never fill the whole photo rectangle.
                    if (!transparent) {
                        fillPaint.alpha = 255
                        canvas.drawRect(RectF(band.left, band.top, band.right, imageArea.top), fillPaint)
                        canvas.drawRect(RectF(band.left, imageArea.top, imageArea.left, imageArea.bottom), fillPaint)
                        canvas.drawRect(RectF(imageArea.right, imageArea.top, band.right, imageArea.bottom), fillPaint)
                        canvas.drawRect(RectF(band.left, imageArea.bottom, band.right, band.bottom), fillPaint)
                    }
                    border(imageArea, stroke * 0.55f)
                    textPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText("INSTANT  /  FUJIFILM", output.width / 2f,
                        output.height - margin * 1.25f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                }
                "logo" -> {
                    border(outer, stroke)
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.textSize = (minSide * 0.045f).coerceIn(24f, 76f)
                    canvas.drawText("FUJIFILM", output.width / 2f, output.height * 0.52f, textPaint)
                    textPaint.textSize = (minSide * 0.014f).coerceIn(11f, 28f)
                    detailPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText("X SERIES", output.width / 2f, output.height * 0.55f, detailPaint)
                    detailPaint.textAlign = Paint.Align.LEFT
                    textPaint.textAlign = Paint.Align.LEFT
                }
                "expo_left", "expo_right" -> {
                    val bandWidth = minSide * 0.105f
                    val leftSide = template == "expo_left"
                    val band = if (leftSide) RectF(0f, 0f, bandWidth, output.height.toFloat())
                        else RectF(output.width - bandWidth, 0f, output.width.toFloat(), output.height.toFloat())
                    plate(band, 0f)
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.textSize = (minSide * 0.018f).coerceIn(12f, 34f)
                    canvas.save()
                    canvas.rotate(if (leftSide) -90f else 90f, band.centerX(), band.centerY())
                    canvas.drawText("FUJIFILM  •  ${java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.US).format(java.util.Date())}",
                        band.centerX(), band.centerY(), textPaint)
                    canvas.restore()
                    textPaint.textAlign = Paint.Align.LEFT
                    border(outer, stroke * 0.7f)
                }
                "expo_around" -> {
                    border(outer, stroke * 1.2f)
                    border(inner, stroke * 0.45f)
                    caption("EXPO", "EXPO  •  ${java.text.SimpleDateFormat("yyyy.MM.dd  HH:mm", java.util.Locale.US).format(java.util.Date())}")
                }
                "double" -> {
                    border(outer, stroke * 1.8f)
                    border(RectF(margin * 2.1f, margin * 2.1f,
                        output.width - margin * 2.1f, output.height - margin * 2.1f), stroke * 0.6f)
                }
                else -> return@runCatching false
            }
        }

        val written = context.contentResolver.openOutputStream(uri, "wt")?.use {
            output.compress(Bitmap.CompressFormat.JPEG, 98, it)
        } ?: false
        if (!written) return@runCatching false
        val exifWritten = restoreImageExifAttributes(context, uri, sourceExif, output.width, output.height)
        setMediaStoreOrientationUpright(context, uri)
        exifWritten
        } finally {
            outputBitmap?.let { if (it !== bitmap && !it.isRecycled) it.recycle() }
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }.onFailure {
        android.util.Log.e("FUJI_FRAME", "Could not apply selected frame", it)
    }.getOrDefault(false)
}

@Composable
fun CameraFrameDialog(
    selectedTemplate: String,
    selectedStyle: String,
    customFrames: List<String>,
    onSelectTemplate: (String) -> Unit,
    onSelectStyle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val builtInFrames = listOf(
        "none" to "Tắt frame",
        "classic" to "Classic Card",
        "clean" to "Clean Corners",
        "film" to "35mm Film",
        "polaroid" to "Instant / Polaroid",
        "logo" to "Center Logo",
        "expo_around" to "Expo Around",
        "expo_left" to "Expo Left",
        "expo_right" to "Expo Right",
        "double" to "Double Line"
    )
    val options = builtInFrames + customFrames.map { uri ->
        "custom|$uri" to documentDisplayName(context, Uri.parse(uri))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.94f).heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp)).background(CamColors.Panel)
                .verticalScroll(rememberScrollState()).padding(15.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Photo Frames", color = CamColors.TextColor, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Đóng", color = CamColors.Yellow,
                    modifier = Modifier.animaTClickable(onClick = onDismiss).padding(6.dp), fontSize = 12.sp)
            }
            Text("Chọn frame để xem trước và đóng lên ảnh sau khi chụp.",
                color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp, bottom = 10.dp))
            options.chunked(2).forEach { rowOptions ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { (id, label) ->
                        Column(
                            Modifier.weight(1f).padding(bottom = 7.dp).clip(RoundedCornerShape(11.dp))
                                .background(if (selectedTemplate == id) Color(0x553F3822) else CamColors.Card)
                                .border(1.dp, if (selectedTemplate == id) CamColors.Yellow else CamColors.Stroke,
                                    RoundedCornerShape(11.dp))
                                .animaTClickable { onSelectTemplate(id) }.padding(10.dp)
                        ) {
                            Text(label, color = if (selectedTemplate == id) CamColors.Yellow else CamColors.TextColor,
                                fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text(if (id.startsWith("custom|")) "PNG trong suốt khuyên dùng · JPG có thể che ảnh" else "Frame dựng sẵn",
                                color = CamColors.Sub, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                    if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Text("Frame color", color = CamColors.TextColor, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp, bottom = 7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("White", "Black").forEach { style ->
                    Chip(style, selectedStyle == style, { onSelectStyle(style) }, Modifier.weight(1f))
                }
            }
            Text("Transparent: bỏ viền và panel nền; các chi tiết chữ/ảnh PNG vẫn được giữ.",
                color = CamColors.Sub, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("Áp dụng frame")
            }
        }
    }
}

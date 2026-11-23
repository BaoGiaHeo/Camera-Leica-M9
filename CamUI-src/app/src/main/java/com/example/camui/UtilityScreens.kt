package com.example.camui

import android.content.Context
import android.location.Geocoder
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
import android.graphics.Typeface
import android.graphics.RectF
import android.media.ExifInterface
import android.media.MediaPlayer
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.content.ContentUris
import android.util.Size
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.abs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
private fun FujiSliderCompat(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    FujiSlider(value = value, valueRange = valueRange, onValueChange = onValueChange)
}
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
    FujiSlider(value = value, valueRange = range, onValueChange = onValueChange)
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
                    modifier = Modifier.fillMaxWidth(0.88f).height(17.dp).padding(2.dp),
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
                    exampleModel,
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
    var retroLcdEnabled by remember { mutableStateOf(prefs.getBoolean("lcd_texture_overlay_enabled", false)) }
    var zebraEnabled by remember { mutableStateOf(prefs.getBoolean("viewfinder_zebra_enabled", false)) }
    var rgbHistogramEnabled by remember { mutableStateOf(prefs.getBoolean("viewfinder_rgb_histogram_enabled", false)) }
    var levelEnabled by remember { mutableStateOf(prefs.getBoolean("viewfinder_level_enabled", true)) }
    var gridOverlayMode by remember { mutableStateOf(prefs.getString("grid_overlay_mode", if (showGrid) "3x3" else "off") ?: "off") }
    var gridOverlayColor by remember { mutableStateOf(prefs.getString("grid_overlay_color", "auto") ?: "auto") }
    var gridOverlayOpacity by remember { mutableIntStateOf(prefs.getInt("grid_overlay_opacity", 50).coerceIn(30, 70)) }
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
    var filmSharpness by remember { mutableStateOf(prefs.getFloat("film_custom_sharpness", 0f).coerceIn(0f, 1f)) }
    var filmNoiseReduction by remember { mutableStateOf(prefs.getFloat("film_custom_noise_reduction", 0f).coerceIn(0f, 1f)) }
    var filmGrain by remember { mutableStateOf(prefs.getFloat("film_custom_grain", 0f).coerceIn(0f, 1f)) }
    var filmTemperature by remember { mutableStateOf(prefs.getFloat("film_custom_temperature", 0f).coerceIn(-1f, 1f)) }
    var filmTone by remember { mutableStateOf(prefs.getFloat("film_custom_tone", 0f).coerceIn(-1f, 1f)) }
    var filmSaturation by remember { mutableStateOf(prefs.getFloat("film_custom_saturation", 1f).coerceIn(0.5f, 1.5f)) }

    val filmRecipeKeys = remember {
        listOf(
            "film_custom_highlight", "film_custom_shadow", "film_custom_color_chrome",
            "film_custom_sharpness", "film_custom_noise_reduction",
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
                "film_custom_sharpness" -> editor.putFloat("$slot.$key", filmSharpness)
                "film_custom_noise_reduction" -> editor.putFloat("$slot.$key", filmNoiseReduction)
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
        filmSharpness = values.getFloat("$slot.film_custom_sharpness", 0f).coerceIn(0f, 1f)
        filmNoiseReduction = values.getFloat("$slot.film_custom_noise_reduction", 0f).coerceIn(0f, 1f)
        filmGrain = values.getFloat("$slot.film_custom_grain", 0f).coerceIn(0f, 1f)
        filmTemperature = values.getFloat("$slot.film_custom_temperature", 0f).coerceIn(-1f, 1f)
        filmTone = values.getFloat("$slot.film_custom_tone", 0f).coerceIn(-1f, 1f)
        filmSaturation = values.getFloat("$slot.film_custom_saturation", 1f).coerceIn(0.5f, 1.5f)
        val selected = values.getString("$slot.selected_filter", "None") ?: "None"
        prefs.edit()
            .putFloat("film_custom_highlight", filmHighlight)
            .putFloat("film_custom_shadow", filmShadow)
            .putFloat("film_custom_color_chrome", filmColorChrome)
            .putFloat("film_custom_sharpness", filmSharpness)
            .putFloat("film_custom_noise_reduction", filmNoiseReduction)
            .putFloat("film_custom_grain", filmGrain)
            .putFloat("film_custom_temperature", filmTemperature)
            .putFloat("film_custom_tone", filmTone)
            .putFloat("film_custom_saturation", filmSaturation)
            .putString("selected_filter", selected)
            .apply()
        Toast.makeText(context, "Đã nạp Film Recipe $slot", Toast.LENGTH_SHORT).show()
    }
    var lensCorrectionEnabled by remember { mutableStateOf(prefs.getBoolean("lens_correction_enabled", true)) }
    var shutterSound by remember { mutableStateOf(prefs.getBoolean("shutter_sound_enabled", true)) }
    // Sound selection was removed: use one fixed bundled default, with only the on/off switch.
    LaunchedEffect(Unit) {
        prefs.edit()
            .remove("watermark_show_model")
            .remove("shutter_effect")
            .remove("shutter_sound_uri")
            .putString("shutter_sound_asset", "classic")
            .apply()
    }

    FujiPanel(title = "SETTINGS · MENU", breadcrumb = "TOOLBOX", modifier = Modifier.fillMaxSize(), onBack = onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).padding(end = 4.dp)) {
        Text("Viewfinder overlays", color = CamColors.Sub, fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        ToolToggle(
            stringResource(R.string.retro_lcd_texture_title),
            stringResource(R.string.retro_lcd_texture_description),
            "film",
            retroLcdEnabled
        ) {
            retroLcdEnabled = it
            prefs.edit().putBoolean("lcd_texture_overlay_enabled", it).apply()
        }
        Text("Pro HUD elements", color = CamColors.Sub, fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, top = 15.dp, bottom = 8.dp))
        ToolToggle("EV display", "Hiện hoặc ẩn thông số bù phơi sáng trong hàng Pro HUD.",
            "sliders", proHudShowEv) {
            proHudShowEv = it
            prefs.edit().putBoolean("pro_hud_show_ev", it).apply()
        }
        ToolToggle("Shutter display", "Hiện hoặc ẩn tốc độ màn trập trong hàng Pro HUD.",
            "sliders", proHudShowShutter) {
            proHudShowShutter = it
            prefs.edit().putBoolean("pro_hud_show_shutter", it).apply()
        }
        ToolToggle("ISO display", "Hiện hoặc ẩn độ nhạy ISO trong hàng Pro HUD.",
            "sliders", proHudShowIso) {
            proHudShowIso = it
            prefs.edit().putBoolean("pro_hud_show_iso", it).apply()
        }
        ToolToggle("Focus display", "Hiện hoặc ẩn trạng thái lấy nét trong hàng Pro HUD.",
            "sliders", proHudShowFocus) {
            proHudShowFocus = it
            prefs.edit().putBoolean("pro_hud_show_focus", it).apply()
        }
        ToolToggle("White balance display", "Hiện hoặc ẩn nhiệt độ màu / cân bằng trắng trong hàng Pro HUD.",
            "sliders", proHudShowWhiteBalance) {
            proHudShowWhiteBalance = it
            prefs.edit().putBoolean("pro_hud_show_white_balance", it).apply()
        }
        Text(stringResource(R.string.grid_overlay_title), color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 5.dp))
        val gridModes = listOf(
            "off" to R.string.grid_mode_off,
            "3x3" to R.string.grid_mode_thirds,
            "4x4" to R.string.grid_mode_four_by_four,
            "diagonal" to R.string.grid_mode_diagonal,
            "golden" to R.string.grid_mode_golden,
            "crosshair" to R.string.grid_mode_crosshair,
            "square" to R.string.grid_mode_square
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            gridModes.forEach { (modeId, labelId) ->
                FujiChip(
                    stringResource(labelId),
                    gridOverlayMode == modeId,
                    {
                        gridOverlayMode = modeId
                        val enabled = modeId != "off"
                        prefs.edit().putString("grid_overlay_mode", modeId)
                            .putBoolean("viewfinder_grid_enabled", enabled).apply()
                        onShowGrid(enabled)
                    }
                )
            }
        }
        Text(stringResource(R.string.grid_color_title), color = CamColors.Sub, fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp, top = 7.dp, bottom = 4.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            listOf(
                "white" to R.string.grid_color_white,
                "black" to R.string.grid_color_black,
                "auto" to R.string.grid_color_auto
            ).forEach { (colorId, labelId) ->
                FujiChip(
                    stringResource(labelId),
                    gridOverlayColor == colorId,
                    {
                        gridOverlayColor = colorId
                        prefs.edit().putString("grid_overlay_color", colorId).apply()
                    }
                )
            }
        }
        Text(stringResource(R.string.grid_opacity_title), color = CamColors.Sub, fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp, top = 7.dp, bottom = 4.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            listOf(30, 50, 70).forEach { opacity ->
                FujiChip(
                    "$opacity%",
                    gridOverlayOpacity == opacity,
                    {
                        gridOverlayOpacity = opacity
                        prefs.edit().putInt("grid_overlay_opacity", opacity).apply()
                    },
                    Modifier.weight(1f)
                )
            }
        }
        ToolToggle("Histogram", "Live luminance histogram", "histogram", showHist, onShowHist)
        ToolToggle("RGB Histogram", "Biểu đồ phân bố kênh đỏ, lục và lam từ khung preview.", "histogram", rgbHistogramEnabled) {
            rgbHistogramEnabled = it
            prefs.edit().putBoolean("viewfinder_rgb_histogram_enabled", it).apply()
        }
        ToolToggle("Focus Peaking", "Highlight high-contrast focus edges", "sliders",
            focusPeaking, onFocusPeaking)
        ToolToggle("Smart Focus Tracking", "Ưu tiên khuôn mặt khi Camera2/HAL công bố face detection; tự đưa AF về vùng mặt.",
            "sliders", faceTrackingEnabled) {
            faceTrackingEnabled = it
            prefs.edit()
                .putBoolean("face_tracking_enabled", it)
                .putString("face_tracking_mode", if (it) "face_eye" else "off")
                .apply()
        }
        ToolToggle(
            "Animation Performance Mode",
            "Giảm trượt/scale khi máy nóng hoặc khi bật thủ công; Android tự kích hoạt khi nhiệt độ đạt mức trung bình.",
            "sliders",
            animationPerformanceMode,
            onAnimationPerformanceModeChanged
        )
        ToolToggle("Lens Correction", "Yêu cầu Camera2/HAL hiệu chỉnh méo hình, viền màu và tối góc bằng các chế độ công khai nếu ống kính hỗ trợ; không tự bịa dữ liệu lens profile.",
            "sliders", lensCorrectionEnabled) {
            lensCorrectionEnabled = it
            prefs.edit().putBoolean("lens_correction_enabled", it).apply()
        }
        ToolToggle("Zebra Warning", "Đánh dấu vùng sáng gần cháy sáng trên kính ngắm (không ảnh hưởng ảnh lưu).",
            "warning", zebraEnabled) {
            zebraEnabled = it
            prefs.edit().putBoolean("viewfinder_zebra_enabled", it).apply()
        }
        ToolToggle("Camera Level & Horizon", "Đo độ nghiêng bằng cảm biến thiết bị và hiển thị đường chân trời trong kính ngắm.",
            "sliders", levelEnabled) {
            levelEnabled = it
            prefs.edit().putBoolean("viewfinder_level_enabled", it).apply()
        }
        ToolToggle("HDR Bracketing", "Chụp 3 khung JPEG với EV thấp / chuẩn / cao nếu camera công bố dải phơi sáng.",
            "histogram", hdrBracketMode) {
            hdrBracketMode = it
            if (it) focusBracketMode = false
            prefs.edit()
                .putBoolean("hdr_bracketing_enabled", it)
                .putBoolean("focus_bracketing_enabled", if (it) false else focusBracketMode)
                .apply()
        }
        ToolToggle("Focus Bracketing", "Chụp 5 khoảng nét từ gần đến vô cực khi ống kính hỗ trợ lấy nét thủ công.",
            "sliders", focusBracketMode) {
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
                    .clip(RoundedCornerShape(3.dp)).background(FujiSkin.lcd)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("$savedName · $timeLabel", color = CamColors.TextColor,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                FujiKey("LƯU", onClick = { saveCameraPreset(slot) }, modifier = Modifier.width(52.dp))
                FujiKey("NẠP", onClick = { loadCameraPreset(slot) }, enabled = presetStore.getBoolean("$slot.exists", false), modifier = Modifier.width(52.dp))
            }
        }
        // Read the revision so the slot labels refresh immediately after saving.
        if (presetUiRevision < 0) Text(" ", fontSize = 1.sp)
            }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
        Text(localizedUiText(context, "Shutter sound"), color = CamColors.TextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        ToolToggle("Âm thanh màn trập", "Bật/tắt âm thanh chụp mặc định.",
            "sliders", shutterSound) {
            shutterSound = it
            prefs.edit().putBoolean("shutter_sound_enabled", it).apply()
        }

            }
        }
        }
    }
}


internal val watermarkStudioLayouts = listOf(
    "bar" to "BAR",
    "caption" to "CAPTION",
    "gallery" to "GALLERY",
    "black" to "BLACK",
    "dot_light" to "DOT LIGHT",
    "dot_dark" to "DOT DARK",
    "split" to "SPLIT",
    "palette" to "PALETTE"
)

private data class WatermarkLayoutResource(val id: String, val title: Int, val description: Int)

private val watermarkStudioLayoutResources = listOf(
    WatermarkLayoutResource("bar", R.string.watermark_layout_bar, R.string.watermark_layout_bar_description),
    WatermarkLayoutResource("caption", R.string.watermark_layout_caption, R.string.watermark_layout_caption_description),
    WatermarkLayoutResource("gallery", R.string.watermark_layout_gallery, R.string.watermark_layout_gallery_description),
    WatermarkLayoutResource("black", R.string.watermark_layout_black, R.string.watermark_layout_black_description),
    WatermarkLayoutResource("dot_light", R.string.watermark_layout_dot_light, R.string.watermark_layout_dot_light_description),
    WatermarkLayoutResource("dot_dark", R.string.watermark_layout_dot_dark, R.string.watermark_layout_dot_dark_description),
    WatermarkLayoutResource("split", R.string.watermark_layout_split, R.string.watermark_layout_split_description),
    WatermarkLayoutResource("palette", R.string.watermark_layout_palette, R.string.watermark_layout_palette_description)
)

/**
 * The studio preview uses the production applyCameraWatermark() renderer on a
 * temporary MediaStore copy of the latest app photo. The source photo is never
 * modified; the temporary row is deleted after the preview bitmap has been decoded.
 */
private suspend fun renderWatermarkStudioPreview(context: Context): ImageBitmap? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val sourceUri = runCatching {
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Images.Media.MIME_TYPE} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} NOT LIKE ?",
            arrayOf(
                "Pictures/FujifilmMode/%",
                "image/jpeg",
                "Pictures/FujifilmMode/.WatermarkStudio/%"
            ),
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(0)
                )
            } else null
        }
    }.getOrNull() ?: return@withContext null

    val previewUri = runCatching {
        resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, ".wmstudio_preview_${UUID.randomUUID()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FujifilmMode/.WatermarkStudio/")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        )
    }.getOrNull() ?: return@withContext null

    try {
        val copied = runCatching {
            resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(previewUri, "wt")?.use { output ->
                    input.copyTo(output)
                    true
                } ?: false
            } ?: false
        }.getOrDefault(false)
        if (!copied) return@withContext null

        val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("watermark_enabled", true)) {
            applyCameraWatermark(context, previewUri, "", "", "", "", preservePhotoAspectRatio = true)
        }
        resolver.update(
            previewUri,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null
        )
        decodeUprightBitmap(context, previewUri, maxDimension = 1600)?.asImageBitmap()
    } catch (failure: Throwable) {
        android.util.Log.w("FUJI_WATERMARK_STUDIO", "Could not render temporary watermark preview", failure)
        null
    } finally {
        runCatching { resolver.delete(previewUri, null, null) }
    }
}

@Composable
fun WatermarkStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE) }
    var layout by remember {
        mutableStateOf(
            WatermarkLayoutType.preferenceValue(
                WatermarkLayoutType.fromPreference(prefs.getString("watermark_template", "bar"))
            )
        )
    }
    var appearance by remember {
        mutableStateOf(
            prefs.getString("watermark_style", "White")
                ?.takeIf { it in setOf("White", "Black", "Auto", "LiquidGlass") }
                ?: "White"
        )
    }
    var logoSize by remember {
        mutableStateOf(prefs.getString("watermark_logo_size", "M")?.takeIf { it in setOf("S", "M", "L") } ?: "M")
    }
    var showDate by remember { mutableStateOf(prefs.getBoolean("watermark_show_time", true)) }
    var showSpecs by remember { mutableStateOf(prefs.getBoolean("watermark_show_specs", true)) }
    var showLocation by remember { mutableStateOf(prefs.getBoolean("watermark_show_location", true)) }
    var showCameraName by remember { mutableStateOf(prefs.getBoolean("watermark_show_camera_name", true)) }
    var showLens by remember { mutableStateOf(prefs.getBoolean("watermark_show_lens", true)) }
    var showFocal by remember { mutableStateOf(prefs.getBoolean("watermark_show_focal", true)) }
    var showAperture by remember { mutableStateOf(prefs.getBoolean("watermark_show_aperture", true)) }
    var showShutter by remember { mutableStateOf(prefs.getBoolean("watermark_show_shutter", true)) }
    var showIso by remember { mutableStateOf(prefs.getBoolean("watermark_show_iso", true)) }
    var showEv by remember { mutableStateOf(prefs.getBoolean("watermark_show_ev", true)) }
    var showFilter by remember { mutableStateOf(prefs.getBoolean("watermark_show_filter", false)) }
    var showNightLabel by remember { mutableStateOf(prefs.getBoolean("watermark_show_night", true)) }
    var showCopyright by remember { mutableStateOf(prefs.getBoolean("watermark_show_copyright", false)) }
    var showMegapixels by remember { mutableStateOf(prefs.getBoolean("watermark_show_megapixels", true)) }
    var watermarkEnabled by remember { mutableStateOf(prefs.getBoolean("watermark_enabled", true)) }
    var customModel by remember { mutableStateOf(prefs.getString("watermark_custom_model", "") ?: "") }
    var customLocation by remember { mutableStateOf(prefs.getString("watermark_custom_location", "") ?: "") }
    var customCopyright by remember { mutableStateOf(prefs.getString("watermark_custom_copyright", "") ?: "") }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var previewLoading by remember { mutableStateOf(false) }
    var previewRevision by remember { mutableStateOf(0) }
    LaunchedEffect(
        layout, appearance, logoSize, showDate, showSpecs, showLocation, showCameraName,
        showLens, showFocal, showAperture, showShutter, showIso, showEv, showFilter,
        showNightLabel, showCopyright, showMegapixels, watermarkEnabled, customModel,
        customLocation, customCopyright, previewRevision
    ) {
        prefs.edit()
            .putString("watermark_template", layout)
            .putString("watermark_style", appearance)
            .putString("watermark_logo_size", logoSize)
            .putBoolean("watermark_show_time", showDate)
            .putBoolean("watermark_show_specs", showSpecs)
            .putBoolean("watermark_show_location", showLocation)
            .putBoolean("watermark_show_camera_name", showCameraName)
            .putBoolean("watermark_show_lens", showLens)
            .putBoolean("watermark_show_focal", showFocal)
            .putBoolean("watermark_show_aperture", showAperture)
            .putBoolean("watermark_show_shutter", showShutter)
            .putBoolean("watermark_show_iso", showIso)
            .putBoolean("watermark_show_ev", showEv)
            .putBoolean("watermark_show_filter", showFilter)
            .putBoolean("watermark_show_night", showNightLabel)
            .putBoolean("watermark_show_copyright", showCopyright)
            .putBoolean("watermark_show_megapixels", showMegapixels)
            .putBoolean("watermark_enabled", watermarkEnabled)
            .putString("watermark_custom_model", customModel.trim())
            .putString("watermark_custom_location", customLocation.trim())
            .putString("watermark_custom_copyright", customCopyright.trim())
            .apply()
        previewLoading = true
        try {
            delay(220L)
            preview = renderWatermarkStudioPreview(context)
        } finally {
            previewLoading = false
        }
    }

    FujiPanel(title = stringResource(R.string.watermark_studio_title), modifier = Modifier.fillMaxSize(), onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                stringResource(R.string.watermark_studio_preview),
                color = FujiSkin.lcdGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Box(
                Modifier.fillMaxWidth().heightIn(min = 170.dp, max = 300.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF101719))
                    .border(1.dp, Color(0xFF596568), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = preview,
                    animationSpec = tween(durationMillis = 250),
                    label = "Watermark Studio preview"
                ) { image ->
                    if (image != null) {
                        Image(
                            image,
                            stringResource(R.string.watermark_studio_preview_content_description),
                            Modifier.fillMaxWidth().heightIn(min = 170.dp, max = 300.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FujiIconGlyph("image", Modifier.size(34.dp), FujiSkin.lcdGreen)
                            Text(
                                stringResource(R.string.watermark_studio_no_photo),
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                if (previewLoading) {
                    Text(stringResource(R.string.watermark_studio_rendering), color = FujiSkin.orange, fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
            }
            Text(
                stringResource(R.string.watermark_studio_preview_note),
                color = CamColors.Sub, fontSize = 10.sp
            )
            AnimatedVisibility(
                visible = showLocation,
                enter = fadeIn(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(250))
            ) {
                Text(
                    stringResource(R.string.watermark_studio_location_auto_note),
                    color = CamColors.Sub,
                    fontSize = 9.sp
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FujiKey(stringResource(R.string.watermark_studio_refresh), onClick = { previewRevision++ }, modifier = Modifier.weight(1f))
                FujiKey(stringResource(R.string.watermark_studio_shuffle), onClick = {
                    val alternatives = watermarkStudioLayouts.map { it.first }.filter { it != layout }
                    layout = alternatives.random()
                }, modifier = Modifier.weight(1f))
                FujiKey(stringResource(R.string.watermark_studio_apply), onClick = {
                    watermarkEnabled = true
                    prefs.edit()
                        .putString("watermark_template", layout)
                        .putString("watermark_style", appearance)
                        .putString("watermark_logo_size", logoSize)
                        .putBoolean("watermark_show_time", showDate)
                        .putBoolean("watermark_show_specs", showSpecs)
                        .putBoolean("watermark_show_location", showLocation)
                        .putBoolean("watermark_show_camera_name", showCameraName)
                        .putBoolean("watermark_show_lens", showLens)
                        .putBoolean("watermark_show_focal", showFocal)
                        .putBoolean("watermark_show_aperture", showAperture)
                        .putBoolean("watermark_show_shutter", showShutter)
                        .putBoolean("watermark_show_iso", showIso)
                        .putBoolean("watermark_show_ev", showEv)
                        .putBoolean("watermark_show_filter", showFilter)
                        .putBoolean("watermark_show_night", showNightLabel)
                        .putBoolean("watermark_show_copyright", showCopyright)
                        .putBoolean("watermark_show_megapixels", showMegapixels)
                        .putBoolean("watermark_enabled", true)
                        .putString("watermark_custom_model", customModel.trim())
                        .putString("watermark_custom_location", customLocation.trim())
                        .putString("watermark_custom_copyright", customCopyright.trim())
                        .apply()
                    onBack()
                }, modifier = Modifier.weight(1f))
            }
            Text(stringResource(R.string.watermark_studio_layouts), color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                watermarkStudioLayoutResources.forEach { item ->
                    FujiChip(
                        stringResource(item.title),
                        layout == item.id,
                        { layout = item.id }
                    )
                }
            }
            Text(stringResource(R.string.watermark_studio_appearance), color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "White" to stringResource(R.string.watermark_studio_light),
                    "Black" to stringResource(R.string.watermark_studio_dark),
                    "Auto" to stringResource(R.string.watermark_studio_auto),
                    "LiquidGlass" to stringResource(R.string.watermark_studio_glass)
                ).forEach { (value, label) ->
                    FujiChip(label, appearance == value, { appearance = value })
                }
            }
            Text(stringResource(R.string.watermark_studio_logo_size), color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("S", "M", "L").forEach { value ->
                    FujiChip(value, logoSize == value, { logoSize = value })
                }
            }
            Text(stringResource(R.string.watermark_studio_fields), color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FujiChip(stringResource(R.string.watermark_field_camera_name), showCameraName, { showCameraName = !showCameraName })
                FujiChip(stringResource(R.string.watermark_field_lens), showLens, { showLens = !showLens })
                FujiChip(stringResource(R.string.watermark_field_focal), showFocal, { showFocal = !showFocal })
                FujiChip(stringResource(R.string.watermark_field_aperture), showAperture, { showAperture = !showAperture })
                FujiChip(stringResource(R.string.watermark_field_shutter), showShutter, { showShutter = !showShutter })
                FujiChip(stringResource(R.string.watermark_field_iso), showIso, { showIso = !showIso })
                FujiChip(stringResource(R.string.watermark_field_ev), showEv, { showEv = !showEv })
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FujiChip(stringResource(if (showDate) R.string.watermark_studio_date_on else R.string.watermark_studio_date_off), showDate, { showDate = !showDate })
                FujiChip(stringResource(if (showLocation) R.string.watermark_studio_location_on else R.string.watermark_studio_location_off), showLocation, { showLocation = !showLocation })
                FujiChip(stringResource(R.string.watermark_field_filter), showFilter, { showFilter = !showFilter })
                FujiChip(stringResource(R.string.watermark_field_night), showNightLabel, { showNightLabel = !showNightLabel })
                FujiChip(stringResource(R.string.watermark_field_copyright), showCopyright, { showCopyright = !showCopyright })
                FujiChip(stringResource(R.string.watermark_field_megapixels), showMegapixels, { showMegapixels = !showMegapixels })
                FujiChip(stringResource(if (showSpecs) R.string.watermark_studio_exif_on else R.string.watermark_studio_exif_off), showSpecs, { showSpecs = !showSpecs })
            }
            AnimatedVisibility(
                visible = showCameraName,
                enter = fadeIn(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(250))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.watermark_studio_device_name), color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    FujiSearchField(
                        value = customModel,
                        onValueChange = { customModel = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = stringResource(R.string.watermark_studio_model_placeholder)
                    )
                }
            }
            AnimatedVisibility(
                visible = showCopyright,
                enter = fadeIn(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(250))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.watermark_field_copyright), color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    FujiSearchField(
                        value = customCopyright,
                        onValueChange = { customCopyright = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = stringResource(R.string.watermark_studio_copyright_placeholder)
                    )
                }
            }
            Text(stringResource(R.string.watermark_studio_location), color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            AnimatedVisibility(
                visible = showLocation,
                enter = fadeIn(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(250))
            ) {
                FujiSearchField(
                    value = customLocation,
                    onValueChange = { customLocation = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.watermark_studio_location_placeholder)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FujiChip(stringResource(R.string.watermark_studio_enabled), watermarkEnabled, { watermarkEnabled = true })
                FujiChip(stringResource(R.string.watermark_studio_disabled), !watermarkEnabled, { watermarkEnabled = false })
            }
            Text(
                stringResource(R.string.watermark_studio_output_note),
                color = CamColors.Sub, fontSize = 9.sp
            )
        }
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

private fun cameraWatermarkBrandLogoResource(brand: String, light: Boolean = false): Int? = when (brand.lowercase()) {
    "sony" -> R.drawable.sony_wordmark
    "nikon" -> R.drawable.nikon_wordmark
    "canon" -> R.drawable.canon_wordmark
    "fujifilm" -> if (light) R.drawable.fujifilm_logo_white else R.drawable.fujifilm_logo
    "gopro" -> if (light) R.drawable.gopro_wordmark_white else R.drawable.gopro_wordmark
    "hasselblad" -> R.drawable.hasselblad_wordmark
    "insta360" -> R.drawable.insta360_wordmark
    "kodak" -> R.drawable.kodak_wordmark
    "panasonic", "lumix" -> R.drawable.lumix_wordmark
    "zeiss" -> R.drawable.zeiss_wordmark
    "dji" -> R.drawable.dji_wordmark
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
    frameStyle: String = "White",
    modifier: Modifier = Modifier,
    fallbackColor: Color = Color.Unspecified
) {
    val logoResource = cameraWatermarkBrandLogoResource(brand, frameStyle == "Black")
    if (logoResource != null) {
        Image(
            painter = painterResource(id = logoResource),
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
        modifier.background(if (selected) Color(0xFF25312F) else FujiSkin.lcd)
            .border(width = if (selected) 1.5.dp else 0.7.dp, color = if (selected) FujiSkin.orange else Color(0xFF344247))
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
                        frameStyle = frameStyle,
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
                            frameStyle = frameStyle,
                            modifier = Modifier.weight(0.38f).height(15.dp).padding(2.dp),
                            fallbackColor = brandAccent
                        )
                        Column(
                            Modifier.weight(0.62f).padding(start = 3.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(previewModel, color = textColor,
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
        Text(title, color = if (selected) FujiSkin.orange else Color.White,
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
 * Normalize capture orientation only. Portrait framing is applied later by
 * PhotoOutputResolution with a direct center crop; Landscape preserves the complete frame.
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
        val finalBitmap = orientedBitmap
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

internal data class CubeLut(
    val size: Int,
    val domainMin: FloatArray,
    val domainMax: FloatArray,
    val values: FloatArray,
    val oneDSize: Int = 0,
    val oneDValues: FloatArray = FloatArray(0),
    /** Canon's raw binary files use R-fastest ordering; standard .cube files use B-fastest. */
    val redFastest: Boolean = false
) {
    private fun value(red: Int, green: Int, blue: Int, channel: Int): Float {
        val voxel = if (redFastest) {
            (blue * size + green) * size + red
        } else {
            (red * size + green) * size + blue
        }
        return values[voxel * 3 + channel]
    }

    /**
     * Allocation-free sampler for row/tile image processing. The optional 1D shaper
     * is applied before the 3D stage. The 3D volume uses six-case tetrahedral
     * interpolation; callers reuse the supplied output buffer across pixels.
     */
    fun sampleInto(red: Float, green: Float, blue: Float, output: FloatArray) {
        require(output.size >= 3) { "LUT output buffer must have at least three channels." }
        var inputRed = red.coerceIn(0f, 1f)
        var inputGreen = green.coerceIn(0f, 1f)
        var inputBlue = blue.coerceIn(0f, 1f)

        if (oneDSize >= 2) {
            val redPosition = inputRed * (oneDSize - 1)
            val redLow = redPosition.toInt().coerceIn(0, oneDSize - 1)
            val redHigh = (redLow + 1).coerceAtMost(oneDSize - 1)
            val redT = redPosition - redLow
            inputRed = (oneDValues[redLow * 3] +
                (oneDValues[redHigh * 3] - oneDValues[redLow * 3]) * redT).coerceIn(0f, 1f)

            val greenPosition = inputGreen * (oneDSize - 1)
            val greenLow = greenPosition.toInt().coerceIn(0, oneDSize - 1)
            val greenHigh = (greenLow + 1).coerceAtMost(oneDSize - 1)
            val greenT = greenPosition - greenLow
            inputGreen = (oneDValues[greenLow * 3 + 1] +
                (oneDValues[greenHigh * 3 + 1] - oneDValues[greenLow * 3 + 1]) * greenT).coerceIn(0f, 1f)

            val bluePosition = inputBlue * (oneDSize - 1)
            val blueLow = bluePosition.toInt().coerceIn(0, oneDSize - 1)
            val blueHigh = (blueLow + 1).coerceAtMost(oneDSize - 1)
            val blueT = bluePosition - blueLow
            inputBlue = (oneDValues[blueLow * 3 + 2] +
                (oneDValues[blueHigh * 3 + 2] - oneDValues[blueLow * 3 + 2]) * blueT).coerceIn(0f, 1f)
        }

        if (size < 2) {
            output[0] = inputRed
            output[1] = inputGreen
            output[2] = inputBlue
            return
        }

        val redSpan = (domainMax[0] - domainMin[0]).coerceAtLeast(0.00001f)
        val greenSpan = (domainMax[1] - domainMin[1]).coerceAtLeast(0.00001f)
        val blueSpan = (domainMax[2] - domainMin[2]).coerceAtLeast(0.00001f)
        val redPosition = ((inputRed - domainMin[0]) / redSpan).coerceIn(0f, 1f) * (size - 1)
        val greenPosition = ((inputGreen - domainMin[1]) / greenSpan).coerceIn(0f, 1f) * (size - 1)
        val bluePosition = ((inputBlue - domainMin[2]) / blueSpan).coerceIn(0f, 1f) * (size - 1)

        val r0 = redPosition.toInt().coerceIn(0, size - 1)
        val g0 = greenPosition.toInt().coerceIn(0, size - 1)
        val b0 = bluePosition.toInt().coerceIn(0, size - 1)
        val rt = (redPosition - r0).coerceIn(0f, 1f)
        val gt = (greenPosition - g0).coerceIn(0f, 1f)
        val bt = (bluePosition - b0).coerceIn(0f, 1f)
        val r1 = (r0 + 1).coerceAtMost(size - 1)
        val g1 = (g0 + 1).coerceAtMost(size - 1)
        val b1 = (b0 + 1).coerceAtMost(size - 1)

        for (channel in 0..2) {
            val c000 = value(r0, g0, b0, channel)
            val c100 = value(r1, g0, b0, channel)
            val c010 = value(r0, g1, b0, channel)
            val c001 = value(r0, g0, b1, channel)
            val c110 = value(r1, g1, b0, channel)
            val c101 = value(r1, g0, b1, channel)
            val c011 = value(r0, g1, b1, channel)
            val c111 = value(r1, g1, b1, channel)

            val result = when {
                rt >= gt && gt >= bt ->
                    c000 + rt * (c100 - c000) + gt * (c110 - c100) + bt * (c111 - c110)
                rt >= bt && bt > gt ->
                    c000 + rt * (c100 - c000) + bt * (c101 - c100) + gt * (c111 - c101)
                bt > rt && rt >= gt ->
                    c000 + bt * (c001 - c000) + rt * (c101 - c001) + gt * (c111 - c101)
                gt > rt && rt >= bt ->
                    c000 + gt * (c010 - c000) + rt * (c110 - c010) + bt * (c111 - c110)
                gt >= bt && bt > rt ->
                    c000 + gt * (c010 - c000) + bt * (c011 - c010) + rt * (c111 - c011)
                else ->
                    c000 + bt * (c001 - c000) + gt * (c011 - c001) + rt * (c111 - c011)
            }
            output[channel] = result.coerceIn(0f, 1f)
        }
    }

    fun sample(red: Float, green: Float, blue: Float): Triple<Float, Float, Float> {
        val output = FloatArray(3)
        sampleInto(red, green, blue, output)
        return Triple(output[0], output[1], output[2])
    }
}

/**
 * Reads standard .cube 1D/3D data in two passes so a 129^3 file does not allocate
 * millions of small FloatArray objects. Standard .cube ordering has blue varying fastest.
 */
internal fun parseCubeLutText(text: String): CubeLut? {
    return runCatching {
        if (text.length > 96 * 1024 * 1024) return@runCatching null
        var size3d = 0
        var size1d = 0
        val min = floatArrayOf(0f, 0f, 0f)
        val max = floatArrayOf(1f, 1f, 1f)
        var declared = false

        text.lineSequence().forEach { raw ->
            val line = raw.substringBefore("#").trim()
            if (line.isEmpty()) return@forEach
            val parts = line.split(Regex("\\s+"))
            when (parts.firstOrNull()) {
                "TITLE" -> Unit
                "LUT_1D_SIZE" -> size1d = parts.getOrNull(1)?.toIntOrNull() ?: 0
                "LUT_3D_SIZE" -> {
                    size3d = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    declared = true
                }
                "DOMAIN_MIN" -> {
                    require(parts.size >= 4) { "DOMAIN_MIN requires three components." }
                    for (i in 0..2) min[i] = parts[i + 1].toFloat()
                }
                "DOMAIN_MAX" -> {
                    require(parts.size >= 4) { "DOMAIN_MAX requires three components." }
                    for (i in 0..2) max[i] = parts[i + 1].toFloat()
                }
            }
        }

        require(size3d == 0 || size3d in 2..129) { "LUT_3D_SIZE must be 2..129." }
        require(size1d == 0 || size1d in 2..65536) { "LUT_1D_SIZE must be 2..65536." }
        require(size3d > 0 || size1d > 0) { "No LUT_1D_SIZE or LUT_3D_SIZE header found." }
        require((0..2).all { min[it].isFinite() && max[it].isFinite() && max[it] > min[it] }) {
            "LUT domain must contain finite increasing ranges."
        }
        if (size3d > 0) require(declared) { "Missing LUT_3D_SIZE." }

        val expected1d = size1d
        val expected3d = if (size3d > 0) size3d * size3d * size3d else 0
        val totalRows = expected1d + expected3d
        val values = FloatArray(totalRows * 3)
        var rowIndex = 0

        text.lineSequence().forEach { raw ->
            val line = raw.substringBefore("#").trim()
            if (line.isEmpty()) return@forEach
            val first = line.firstOrNull()
            if (first == null || first.isLetter() || first == '_' ) return@forEach
            val parts = line.split(Regex("\\s+"))
            require(parts.size == 3) { "Each LUT data row must have exactly three components." }
            val r = parts[0].toFloat()
            val g = parts[1].toFloat()
            val b = parts[2].toFloat()
            require(r.isFinite() && g.isFinite() && b.isFinite()) { "LUT contains NaN or infinity." }
            require(rowIndex < totalRows) { "LUT contains more rows than declared." }
            val offset = rowIndex * 3
            values[offset] = r
            values[offset + 1] = g
            values[offset + 2] = b
            rowIndex++
        }
        require(rowIndex == totalRows) {
            "LUT row count mismatch: expected $totalRows, received $rowIndex."
        }

        val oneDValueCount = expected1d * 3
        val oneDValues = if (oneDValueCount > 0) values.copyOfRange(0, oneDValueCount) else FloatArray(0)
        val threeDValues = if (expected3d > 0) values.copyOfRange(oneDValueCount, values.size) else FloatArray(0)
        CubeLut(
            size = size3d,
            domainMin = min,
            domainMax = max,
            values = threeDValues,
            oneDSize = size1d,
            oneDValues = oneDValues,
            redFastest = false
        )
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
        size = CANON_LUT_SIZE,
        domainMin = floatArrayOf(0f, 0f, 0f),
        domainMax = floatArrayOf(1f, 1f, 1f),
        values = values,
        redFastest = true
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

internal fun catalogLutCategory(path: String): String {
    val normalized = path.replace('\\', '/')
    val parts = normalized.split('/').filter { it.isNotBlank() }
    val folder = parts.firstOrNull {
        it.matches(Regex("\\d{2}_[A-Za-z0-9_-]+"))
    }?.substringAfter('_')?.replace('_', ' ')
    if (folder != null) return "Canon · $folder"

    val lower = normalized.lowercase(Locale.ROOT)
    val logInput = listOf(
        "v-log", "vlog", "s-log", "slog", "n-log", "nlog",
        "f-log", "flog", "logc", "cineon", "c-log", "clog",
        "bmd film", "blackmagic film", "cinealta", "venice"
    ).any { lower.contains(it) }
    if (logInput || lower.contains("cinema / log") || lower.contains("cinema_log")) {
        return "Cinema / Log"
    }

    fun hasFolder(vararg names: String): Boolean =
        parts.any { part -> names.any { it.equals(part.trim(), ignoreCase = true) } }

    return when {
        hasFolder("Fujifilm", "FujiFilm") -> "Fujifilm"
        hasFolder("Leica") -> "Leica"
        hasFolder("Panasonic", "Panasonic Lumix", "Lumix") -> "Panasonic Lumix"
        hasFolder("Sony") -> "Sony"
        hasFolder("Hald CLUT", "HaldCLUT", "Hald_CLUT") || lower.contains("hald-clut") -> "Hald CLUT"
        hasFolder("Kodak") -> "Kodak"
        hasFolder("Hasselblad") -> "Hasselblad"
        hasFolder("Nikon") -> "Nikon"
        hasFolder("Arri", "ARRI") -> "Cinema / Log"
        hasFolder("RED") -> "Cinema / Log"
        hasFolder("Other", "Khác") -> "Khác"
        lower.contains("panasonic-standard") -> "Panasonic Lumix"
        else -> "LUT nhập"
    }
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
    val sharpness = recipe.getFloat("film_custom_sharpness", 0f).coerceIn(0f, 1f)
    val noiseReduction = recipe.getFloat("film_custom_noise_reduction", 0f).coerceIn(0f, 1f)
    val grain = recipe.getFloat("film_custom_grain", 0f).coerceIn(0f, 1f)
    val temperature = recipe.getFloat("film_custom_temperature", 0f).coerceIn(-1f, 1f)
    val tone = recipe.getFloat("film_custom_tone", 0f).coerceIn(-1f, 1f)
    val saturation = recipe.getFloat("film_custom_saturation", 1f).coerceIn(0.5f, 1.5f)
    val tonePlaneX = recipe.getFloat("film_tone_plane_x", 0.5f).coerceIn(0f, 1f)
    val tonePlaneY = recipe.getFloat("film_tone_plane_y", 0.5f).coerceIn(0f, 1f)
    val palette = recipe.getInt("film_tone_palette", 100).coerceIn(0, 100) / 100f
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return false

    // Keep three original rows in memory. This lets the same spatial noise/detail controls
    // drive both the live filter preview and the final JPEG without another full-size bitmap.
    var previousRow = IntArray(width)
    var currentRow = IntArray(width)
    var nextRow = IntArray(width)
    val outputRow = IntArray(width)
    val lutOutput = FloatArray(3)
    bitmap.getPixels(currentRow, 0, width, 0, 0, width, 1)
    currentRow.copyInto(previousRow)
    if (height > 1) bitmap.getPixels(nextRow, 0, width, 0, 1, width, 1)
    else currentRow.copyInto(nextRow)

    fun pixelLuma(pixel: Int): Float =
        (AndroidColor.red(pixel) * 0.2126f +
            AndroidColor.green(pixel) * 0.7152f +
            AndroidColor.blue(pixel) * 0.0722f) / 255f

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = currentRow[x]
            val alpha = AndroidColor.alpha(pixel)
            val leftPixel = currentRow[(x - 1).coerceAtLeast(0)]
            val rightPixel = currentRow[(x + 1).coerceAtMost(width - 1)]
            val upperPixel = previousRow[x]
            val lowerPixel = nextRow[x]
            val centerLuma = pixelLuma(pixel)
            val leftLuma = pixelLuma(leftPixel)
            val rightLuma = pixelLuma(rightPixel)
            val upperLuma = pixelLuma(upperPixel)
            val lowerLuma = pixelLuma(lowerPixel)

            val leftWeight = (1f - kotlin.math.abs(centerLuma - leftLuma) * 5f).coerceIn(0f, 1f)
            val rightWeight = (1f - kotlin.math.abs(centerLuma - rightLuma) * 5f).coerceIn(0f, 1f)
            val upperWeight = (1f - kotlin.math.abs(centerLuma - upperLuma) * 5f).coerceIn(0f, 1f)
            val lowerWeight = (1f - kotlin.math.abs(centerLuma - lowerLuma) * 5f).coerceIn(0f, 1f)
            val neighbourWeight = leftWeight + rightWeight + upperWeight + lowerWeight
            val denoiseMix = noiseReduction * 0.28f
            fun denoiseChannel(channel: (Int) -> Int): Float {
                if (neighbourWeight <= 0.001f || denoiseMix <= 0f) {
                    return channel(pixel).toFloat()
                }
                val neighbours = (
                    channel(leftPixel) * leftWeight +
                        channel(rightPixel) * rightWeight +
                        channel(upperPixel) * upperWeight +
                        channel(lowerPixel) * lowerWeight
                    ) / neighbourWeight
                return channel(pixel) * (1f - denoiseMix) + neighbours * denoiseMix
            }

            val red = denoiseChannel(AndroidColor::red)
            val green = denoiseChannel(AndroidColor::green)
            val blue = denoiseChannel(AndroidColor::blue)
            val mapped = if (cube != null) {
                cube.sampleInto(red / 255f, green / 255f, blue / 255f, lutOutput)
                null
            } else {
                transformPreset(preset, red, green, blue)
            }

            var r = if (cube != null) lutOutput[0] else (mapped!!.first / 255f).coerceIn(0f, 1f)
            var g = if (cube != null) lutOutput[1] else (mapped!!.second / 255f).coerceIn(0f, 1f)
            var b = if (cube != null) lutOutput[2] else (mapped!!.third / 255f).coerceIn(0f, 1f)
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
            val planeSaturation = (0.72f + tonePlaneX * 0.56f) * palette
            val saturationFactor = saturation * chromeFactor * planeSaturation
            r = luma + (r - luma) * saturationFactor
            g = luma + (g - luma) * saturationFactor
            b = luma + (b - luma) * saturationFactor

            val planeExposure = (0.5f - tonePlaneY) * 0.16f * palette
            r += temperature * 0.045f + planeExposure
            g += temperature * 0.006f + planeExposure
            b -= temperature * 0.045f
            // Color Tone shifts green/magenta independently from global color temperature.
            r += tone * 0.018f
            g -= tone * 0.035f
            b += tone * 0.018f

            // Restrained local-contrast recovery avoids hard edge halos.
            val localSourceLuma = (leftLuma + rightLuma + upperLuma + lowerLuma) * 0.25f
            val detail = centerLuma - localSourceLuma
            val outputLuma = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
            val sharpenedLuma = (outputLuma + detail * sharpness * 0.32f).coerceIn(0f, 1f)
            val sharpnessScale = if (outputLuma > 0.015f) {
                (sharpenedLuma / outputLuma).coerceIn(0.82f, 1.18f)
            } else 1f
            r *= sharpnessScale
            g *= sharpnessScale
            b *= sharpnessScale

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
            outputRow[x] = AndroidColor.argb(
                alpha,
                clamp8(r.coerceIn(0f, 1f) * 255f),
                clamp8(g.coerceIn(0f, 1f) * 255f),
                clamp8(b.coerceIn(0f, 1f) * 255f)
            )
        }
        bitmap.setPixels(outputRow, 0, width, 0, y, width, 1)
        if (y + 1 < height) {
            val recycled = previousRow
            previousRow = currentRow
            currentRow = nextRow
            nextRow = recycled
            if (y + 2 < height) bitmap.getPixels(nextRow, 0, width, 0, y + 2, width, 1)
            else currentRow.copyInto(nextRow)
        }
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
fun createFilmLookPreviewSample(context: Context): Bitmap {
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
    paint.typeface = FujiTypography.androidTypeface(context)
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
/**
 * Centers a watermark text line by its actual embedded-font cap height.
 * The optical anchor is the midpoint between the top of capital H and baseline;
 * accents above capitals are intentionally excluded from this optical measurement.
 */
private fun readWatermarkMarketName(): String? = runCatching {
    val properties = Class.forName("android.os.SystemProperties")
    val get = properties.getMethod("get", String::class.java)
    (get.invoke(null, "ro.product.marketname") as? String)?.trim()?.takeIf { it.isNotBlank() }
}.getOrNull()

private fun watermarkOpticalBaseline(paint: Paint, centerY: Float): Float {
    val capBounds = android.graphics.Rect()
    paint.getTextBounds("H", 0, 1, capBounds)
    val capHeight = (-capBounds.top).toFloat().coerceAtLeast(paint.textSize * 0.5f)
    return watermarkBaselineFromCapHeight(centerY, capHeight)
}

private fun parseWatermarkExifNumber(value: String?): Double? {
    val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        if ('/' in raw) {
            val parts = raw.split('/', limit = 2)
            parts[0].toDouble() / parts[1].toDouble()
        } else raw.toDouble()
    }.getOrNull()?.takeIf { it.isFinite() }
}

private fun formatWatermarkDms(latitude: Double, longitude: Double): String {
    fun part(value: Double, positive: String, negative: String): String {
        val absolute = kotlin.math.abs(value)
        val degrees = absolute.toInt()
        val minutes = (((absolute - degrees) * 60.0).toInt()).coerceIn(0, 59)
        val hemisphere = if (value < 0.0) negative else positive
        return String.format(Locale.US, "%d°%02d'%s", degrees, minutes, hemisphere)
    }
    return part(latitude, "N", "S") + " " + part(longitude, "E", "W")
}

private fun watermarkLocationFromExif(
    context: Context,
    attributes: Map<String, String>,
    preferences: android.content.SharedPreferences
): Pair<String, String> {
    val decimal = formatWatermarkGpsCoordinates(attributes)
    val values = decimal?.split(',')?.map { it.trim().toDoubleOrNull() }
    val latitude = values?.getOrNull(0)
    val longitude = values?.getOrNull(1)
    val coordinates = if (latitude != null && longitude != null) {
        formatWatermarkDms(latitude, longitude)
    } else ""
    val place = if (latitude != null && longitude != null && Geocoder.isPresent()) {
        runCatching {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.let { address ->
                    address.locality ?: address.subAdminArea ?: address.adminArea ?: address.subLocality
                }
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.onFailure {
            android.util.Log.d("FUJI_WATERMARK_GPS", "Reverse geocoding unavailable; use EXIF coordinates", it)
        }.getOrNull().orEmpty()
    } else ""
    val customPlace = preferences.getString("watermark_custom_location", "")?.trim().orEmpty()
    return (place.ifBlank { customPlace }.ifBlank { coordinates }) to coordinates
}

private fun duplicateOriginalWatermarkSource(context: Context, uri: Uri): Uri? {
    val resolver = context.contentResolver
    var displayName = "FujiFilm_original_" + System.currentTimeMillis() + ".jpg"
    var relativePath: String? = null
    var mimeType = "image/jpeg"
    runCatching {
        resolver.query(
            uri,
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.MIME_TYPE),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                val mimeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                if (nameIndex >= 0) {
                    val originalName = cursor.getString(nameIndex)?.trim().orEmpty()
                    if (originalName.isNotBlank()) {
                        val extension = originalName.substringAfterLast('.', "jpg")
                        val stem = originalName.substringBeforeLast('.', originalName)
                        displayName = stem + "_original." + extension
                    }
                }
                if (pathIndex >= 0) relativePath = cursor.getString(pathIndex)
                if (mimeIndex >= 0) mimeType = cursor.getString(mimeIndex)?.takeIf { it.isNotBlank() } ?: mimeType
            }
        }
    }
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        if (android.os.Build.VERSION.SDK_INT >= 29 && !relativePath.isNullOrBlank()) {
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val copyUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    return runCatching {
        val input = resolver.openInputStream(uri) ?: error("Could not read original source photo")
        val output = resolver.openOutputStream(copyUri, "w") ?: error("Could not create original photo copy")
        input.use { source -> output.use { destination -> source.copyTo(destination) } }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            resolver.update(
                copyUri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null, null
            )
        }
        android.util.Log.i("FUJI_WATERMARK", "ORIGINAL_COPY_SAVED uri=" + copyUri + " name=" + displayName)
        copyUri
    }.getOrElse {
        resolver.delete(copyUri, null, null)
        android.util.Log.e("FUJI_WATERMARK", "Could not preserve original photo; watermark write cancelled", it)
        null
    }
}

fun applyCameraWatermark(
    context: Context,
    uri: Uri,
    focal: String,
    aperture: String,
    shutter: String,
    iso: String,
    preservePhotoAspectRatio: Boolean = true
): Boolean {
    val preferences = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    if (!preferences.getBoolean("watermark_enabled", true)) {
        android.util.Log.d("FUJI_WATERMARK", "Watermark disabled in settings")
        return false
    }
    val sourceExif = readImageExifAttributes(context, uri)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsRead = runCatching {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    }.isSuccess
    if (!boundsRead || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        android.util.Log.e("FUJI_WATERMARK", "SOURCE_DIMENSIONS_INVALID uri=" + uri)
        return false
    }

    val customName = preferences.getString("watermark_custom_model", "")?.trim().orEmpty()
    val exifMake = sourceExif[ExifInterface.TAG_MAKE]?.trim()?.takeIf { it.isNotBlank() }
        ?: android.os.Build.MANUFACTURER
    val exifModel = sourceExif[ExifInterface.TAG_MODEL]?.trim()?.takeIf { it.isNotBlank() }
        ?: android.os.Build.MODEL
    val cameraName = resolveWatermarkDeviceName(
        customName = customName,
        marketName = readWatermarkMarketName(),
        globalDeviceName = runCatching {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        }.getOrNull(),
        manufacturer = exifMake,
        model = exifModel
    )

    val trueFocal = parseWatermarkExifNumber(sourceExif[ExifInterface.TAG_FOCAL_LENGTH])
        ?.let { String.format(Locale.US, "%.1fmm", it) }
        ?: focal.trim().takeIf { it.isNotBlank() }?.let { if (it.endsWith("mm", true)) it else it + "mm" }.orEmpty()
    val focal35 = sourceExif["FocalLengthIn35mmFilm"]?.toIntOrNull()
        ?.takeIf { it > 0 }?.let { it.toString() + "mm" } ?: trueFocal
    val fNumber = parseWatermarkExifNumber(sourceExif[ExifInterface.TAG_F_NUMBER])
        ?.let { "f/" + String.format(Locale.US, "%.1f", it) }
        ?: aperture.trim().takeIf { it.isNotBlank() }?.let {
            if (it.startsWith("f/", true)) it else "f/" + it.removePrefix("F")
        }.orEmpty()
    val exposureSeconds = parseWatermarkExifNumber(sourceExif[ExifInterface.TAG_EXPOSURE_TIME])
    val shutterValue = exposureSeconds?.takeIf { it > 0.0 }?.let { seconds ->
        if (seconds >= 0.4) String.format(Locale.US, "%.1fs", seconds).replace(".0s", "s")
        else "1/" + (1.0 / seconds).roundToInt().coerceAtLeast(1) + "s"
    } ?: shutter.trim().takeIf { it.isNotBlank() }?.let { if (it.endsWith("s", true)) it else it + "s" }.orEmpty()
    val isoRaw = sourceExif["PhotographicSensitivity"]
        ?: sourceExif[ExifInterface.TAG_ISO_SPEED_RATINGS]
        ?: iso
    val isoNumber = isoRaw.substringBefore(',').trim().toIntOrNull()
    val isoLabel = isoNumber?.let { "ISO " + it }
        ?: iso.trim().takeIf { it.isNotBlank() }?.let { if (it.startsWith("ISO", true)) it else "ISO " + it }.orEmpty()

    val dateRaw = sourceExif[ExifInterface.TAG_DATETIME_ORIGINAL] ?: sourceExif[ExifInterface.TAG_DATETIME]
    val parsedCaptureDate = dateRaw?.let { raw ->
        runCatching {
            java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                .apply { isLenient = false }
                .parse(raw)
        }.getOrNull()
    }
    // CameraX/OEM may omit EXIF DateTimeOriginal. Use capture wall-clock time as a
    // fallback so enabled date/time watermark fields are never silently blank.
    val capturedDate = parsedCaptureDate ?: java.util.Date()
    val dateText = java.text.SimpleDateFormat("yyyy.MM.dd", Locale.US).format(capturedDate)
    val timeText = java.text.SimpleDateFormat("HH:mm", Locale.US).format(capturedDate)
    val location = watermarkLocationFromExif(context, sourceExif, preferences)

    val filterRaw = preferences.getString("selected_filter", "STD")?.trim().orEmpty()
    val standardFilter = filterRaw.isBlank() || filterRaw.equals("None", true) ||
        filterRaw.equals("STD", true) || filterRaw.equals("Standard", true)
    val filterName = if (standardFilter) "" else filterRaw
    val filterCategory = when {
        filterRaw.contains("classic", true) || filterRaw.contains("negative", true) -> "Classic"
        filterRaw.contains("mono", true) || filterRaw.contains("black", true) -> "Monochrome"
        else -> "Fujifilm"
    }
    val filterBadge = when {
        filterRaw.contains("classic negative", true) || filterRaw.contains("classic neg", true) -> "CLASSIC\nNEG."
        filterRaw.contains("classic chrome", true) -> "CLASSIC\nCHROME"
        filterRaw.contains("provia", true) -> "PROVIA"
        filterRaw.contains("velvia", true) -> "VELVIA"
        else -> filterRaw.uppercase(Locale.ROOT).take(16)
    }

    val legacyLocationEnabled = preferences.getBoolean("watermark_show_location", true)
    val fields = WatermarkFieldVisibility(
        cameraName = preferences.getBoolean("watermark_field_name", preferences.getBoolean("watermark_show_camera_name", true)),
        lens = preferences.getBoolean("watermark_field_lens", preferences.getBoolean("watermark_show_lens", true)),
        focalLength = preferences.getBoolean("watermark_field_focal", preferences.getBoolean("watermark_show_focal", true)),
        aperture = preferences.getBoolean("watermark_field_aperture", preferences.getBoolean("watermark_show_aperture", true)),
        shutter = preferences.getBoolean("watermark_field_shutter", preferences.getBoolean("watermark_show_shutter", true)),
        iso = preferences.getBoolean("watermark_field_iso", preferences.getBoolean("watermark_show_iso", true)),
        date = preferences.getBoolean("watermark_field_date", preferences.getBoolean("watermark_show_time", true)),
        time = preferences.getBoolean("watermark_field_clock", false),
        location = preferences.getBoolean("watermark_field_location", legacyLocationEnabled),
        filter = preferences.getBoolean("watermark_field_filter", true)
    )
    val settings = WatermarkLayoutSettings(
        layout = WatermarkLayoutType.fromPreference(preferences.getString("watermark_template", "bar")),
        sizeStep = preferences.getInt("watermark_size_step", 0).coerceIn(-2, 2),
        invert = preferences.getBoolean("watermark_invert", false),
        fields = fields,
        locationEnabled = preferences.getBoolean("watermark_location_enabled", legacyLocationEnabled),
        locationMode = preferences.getString("watermark_location_mode", "PLACE") ?: "PLACE",
        useTrueFocalLength = preferences.getBoolean("watermark_use_true_focal", false),
        customCameraName = customName,
        saveMode = preferences.getString("watermark_save_mode", "watermarked") ?: "watermarked",
    )
    val metadata = WatermarkPhotoMetadata(
        cameraName = cameraName,
        lensName = sourceExif["LensModel"]?.trim()?.takeIf { it.isNotBlank() }
            ?: when {
                (sourceExif["FocalLengthIn35mmFilm"]?.toIntOrNull() ?: 35) <= 20 -> "Ultra-wide"
                (sourceExif["FocalLengthIn35mmFilm"]?.toIntOrNull() ?: 35) >= 70 -> "Tele"
                else -> "Main"
            },
        focal35mm = focal35,
        trueFocalLength = trueFocal,
        aperture = fNumber,
        shutter = shutterValue,
        iso = isoLabel,
        date = dateText,
        time = timeText,
        location = location.first,
        coordinates = location.second,
        filterName = filterName,
        filterCategory = filterCategory,
        filterBadge = filterBadge
    )

    return runCatching {
        val estimatedPixels = bounds.outWidth.toLong() * bounds.outHeight.toLong()
        val estimatedBytes = estimatedPixels * 4L
        val runtime = Runtime.getRuntime()
        val usedHeap = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
        val availableHeap = (runtime.maxMemory() - usedHeap).coerceAtLeast(0L)
        require(estimatedBytes + 96L * 1024L * 1024L <= availableHeap) {
            "Not enough memory to render watermark without reducing source resolution"
        }

        val originalCopy = if (settings.saveMode.equals("both", true)) {
            duplicateOriginalWatermarkSource(context, uri)
                ?: error("Original backup could not be saved; source image has not been overwritten")
        } else null

        val sourceBitmap = decodeWatermarkPixelBitmap(context, uri)
            ?: error("Could not decode original photo for watermark")
        var outputBitmap: Bitmap? = null
        try {
            val hadGainMap = android.os.Build.VERSION.SDK_INT >= 34 &&
                runCatching { sourceBitmap.hasGainmap() }.getOrDefault(false)
            if (hadGainMap) {
                android.util.Log.w(
                    "FUJI_WATERMARK",
                    "ULTRA_HDR_GAINMAP_NOT_PRESERVED: output is standard JPEG SDR; original backup=" + (originalCopy != null)
                )
            }
            outputBitmap = WatermarkRenderer.render(context, sourceBitmap, settings, metadata)
            val outWidth = outputBitmap.width
            val outHeight = outputBitmap.height
            val written = context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                outputBitmap.compress(Bitmap.CompressFormat.JPEG, 96, output)
            } ?: false
            check(written) { "Could not write watermarked JPEG" }
            val exifRestored = restoreImageExifAttributes(context, uri, sourceExif, outWidth, outHeight)
            check(exifRestored) { "Could not restore source EXIF and GPS after watermark rendering" }
            setMediaStoreOrientationUpright(context, uri)
            android.util.Log.i(
                "FUJI_WATERMARK",
                "OUTPUT_ASPECT layout=" + settings.layout.id +
                    " original=" + bounds.outWidth + "x" + bounds.outHeight +
                    " output=" + outWidth + "x" + outHeight +
                    " ratio=" + outWidth + ":" + outHeight +
                    " saveMode=" + settings.saveMode +
                    " GPS_EXIF_PRESERVED=" + sourceExif.containsKey(ExifInterface.TAG_GPS_LATITUDE)
            )
            true
        } finally {
            outputBitmap?.takeIf { !it.isRecycled }?.recycle()
            sourceBitmap.takeIf { !it.isRecycled }?.recycle()
        }
    }.onFailure {
        android.util.Log.e("FUJI_WATERMARK", "Saira A-E watermark rendering failed", it)
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
    val scope = rememberCoroutineScope()
    var luts by remember { mutableStateOf(loadCustomCameraLuts(context)) }
    var lutSearch by remember { mutableStateOf("") }
    var activeLutIds by remember { mutableStateOf(activeCustomCameraLutIds(context)) }
    var pendingDeleteLut by remember { mutableStateOf<CustomCameraLut?>(null) }
    var isImportingLut by remember { mutableStateOf(false) }

    val lutPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
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

    FujiPanel(
        title = "SETTINGS · MENU",
        breadcrumb = "LUT",
        modifier = Modifier.fillMaxSize(),
        onBack = onBack
    ) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("LUT MANAGER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                "Nhập LUT .cube, LUT Canon RGB 33x33x33 (.bin), hoặc ZIP chứa tối đa 700 LUT .cube/.bin. Hỗ trợ ZIP lồng. LUT Log được giữ riêng, không áp nhầm lên JPEG sRGB.",
                color = FujiSkin.muted,
                fontSize = 10.sp
            )
            FujiSearchField(
                value = lutSearch,
                onValueChange = { lutSearch = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Tìm LUT theo tên hoặc nhóm"
            )
            FujiKey(
                if (isImportingLut) "ĐANG NHẬP LUT…" else "+ IMPORT LUT",
                onClick = { lutPicker.launch(arrayOf("*/*")) },
                enabled = !isImportingLut,
                modifier = Modifier.fillMaxWidth()
            )
            if (luts.isEmpty()) {
                EmptyNote("Chưa có LUT nào. Hỗ trợ .cube, Canon RGB .bin và ZIP chứa nhiều LUT.")
            } else {
                val matchingLuts = luts.filter {
                    lutSearch.isBlank() ||
                        it.name.contains(lutSearch.trim(), ignoreCase = true) ||
                        it.category.contains(lutSearch.trim(), ignoreCase = true)
                }
                Text(
                    "Hiển thị \${matchingLuts.size.coerceAtMost(100)} / \${matchingLuts.size} LUT khớp · Tổng \${luts.size}",
                    color = FujiSkin.muted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                matchingLuts.take(100).forEach { entry ->
                    val active = activeLutIds.contains(entry.id)
                    Column(
                        Modifier.fillMaxWidth()
                            .background(FujiSkin.lcd)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    (if (active) "Đã thêm · " else "Chưa thêm · ") + entry.category,
                                    color = FujiSkin.muted,
                                    fontSize = 8.sp
                                )
                            }
                            FujiSwitch(checked = active, onCheckedChange = {
                                setCustomCameraLutActive(context, entry.id, it)
                                activeLutIds = activeCustomCameraLutIds(context)
                            })
                            FujiKey(
                                "X",
                                onClick = { pendingDeleteLut = entry },
                                modifier = Modifier.width(34.dp).padding(start = 4.dp)
                            )
                        }
                        Box(
                            Modifier.fillMaxWidth().padding(top = 4.dp)
                                .height(0.6.dp).background(Color(0xFF2A383B))
                        )
                    }
                }
            }
        }
    }

    pendingDeleteLut?.let { entry ->
        Dialog(onDismissRequest = { pendingDeleteLut = null }) {
            FujiPanel(title = "DELETE LUT", modifier = Modifier.width(340.dp)) {
                Text("Xóa LUT?", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Bạn có chắc chắn muốn xóa LUT \"" + entry.name + "\"? LUT này cũng sẽ bị gỡ khỏi mục Bộ lọc.",
                    color = FujiSkin.muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FujiKey("HỦY", onClick = { pendingDeleteLut = null }, modifier = Modifier.weight(1f))
                    FujiKey("XÓA", onClick = {
                        deleteCustomCameraLut(context, entry.id)
                        luts = loadCustomCameraLuts(context)
                        activeLutIds = activeCustomCameraLutIds(context)
                        pendingDeleteLut = null
                    }, modifier = Modifier.weight(1f))
                }
            }
        }
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
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), color = FujiSkin.lcdGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        FujiKey("DISP/BACK", onClick = onBack, modifier = Modifier.width(82.dp))
    }
}

@Composable
private fun ToolToggle(
    title: String,
    description: String,
    icon: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).background(FujiSkin.lcd).border(1.dp, Color(0xFF344247)).padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FujiIconGlyph(icon, Modifier.size(18.dp), FujiSkin.lcdGreen)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(description, color = CamColors.Sub, fontSize = 11.sp)
        }
        FujiSwitch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, color = FujiSkin.muted, fontSize = 10.sp, modifier = Modifier.padding(vertical = 8.dp))
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
                            .background(FujiSkin.lcd)
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
                        .background(FujiSkin.lcd).animaTClickable { selectedPhoto = entry.uri }
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
        Dialog(onDismissRequest = { inspectorVisible = false }) {
            FujiPanel(title = "PHOTO INSPECTOR", modifier = Modifier.width(380.dp)) {
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    val rows = inspectorRows
                    if (rows == null) {
                        Text("Đang đọc metadata ảnh…", color = FujiSkin.lcdGreen)
                    } else {
                        rows.forEach { (label, value) ->
                            FujiListRow(title = label, value = value, onClick = {}, navigate = false)
                        }
                    }
                }
                FujiKey("ĐÓNG", onClick = { inspectorVisible = false }, modifier = Modifier.fillMaxWidth())
            }
        }
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



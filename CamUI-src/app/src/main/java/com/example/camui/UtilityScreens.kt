package com.example.camui

import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolboxScreen(
    onBack: () -> Unit,
    showGrid: Boolean,
    onShowGrid: (Boolean) -> Unit,
    showHist: Boolean,
    onShowHist: (Boolean) -> Unit,
    focusPeaking: Boolean,
    onFocusPeaking: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE) }
    var watermarkEnabled by remember { mutableStateOf(prefs.getBoolean("watermark_enabled", false)) }
    var watermarkStyle by remember { mutableStateOf(prefs.getString("watermark_style", "White") ?: "White") }
    var customModel by remember { mutableStateOf(prefs.getString("watermark_custom_model", "") ?: "") }
    var shutterEffect by remember { mutableStateOf(prefs.getString("shutter_effect", "Mechanical") ?: "Mechanical") }
    var shutterSound by remember { mutableStateOf(prefs.getBoolean("shutter_sound_enabled", true)) }

    Column(Modifier.fillMaxSize().background(CamColors.Bg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        PageHeader("Toolbox", onBack)
        Text("Viewfinder overlays", color = CamColors.Sub, fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        ToolToggle("Grid Lines", "Thirds composition guide", Icons.Default.GridOn, showGrid, onShowGrid)
        ToolToggle("Histogram", "Live luminance histogram", Icons.Default.ShowChart, showHist, onShowHist)
        ToolToggle("Focus Peaking", "Highlight high-contrast focus edges", Icons.Default.Tune,
            focusPeaking, onFocusPeaking)
        Spacer(Modifier.height(16.dp))
        Text("Watermark", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        ToolToggle("Enable Watermark", "Khung ảnh phong cách rangefinder, không có logo thương hiệu",
            Icons.Default.Image, watermarkEnabled) {
            watermarkEnabled = it
            prefs.edit().putBoolean("watermark_enabled", it).apply()
        }
        Text("Frame color", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 7.dp, top = 5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("White", "Black", "Transparent").forEach { style ->
                Chip(style, watermarkStyle == style, {
                    watermarkStyle = style
                    prefs.edit().putString("watermark_style", style).apply()
                }, Modifier.weight(1f))
            }
        }
        Text("Model", color = Color.White, fontWeight = FontWeight.SemiBold,
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
        Text("Watermark được đóng trực tiếp lên ảnh sau khi chụp. Thông số mm, khẩu độ, shutter và ISO lấy từ camera đang hoạt động khi HAL có cung cấp.",
            color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))

        Spacer(Modifier.height(20.dp))
        Text("Shutter", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("Hiệu ứng khi bấm chụp", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(top = 5.dp, bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Đảo màu", shutterEffect == "Invert", {
                shutterEffect = "Invert"
                prefs.edit().putString("shutter_effect", "Invert").apply()
            }, Modifier.weight(1f))
            Chip("Máy ảnh", shutterEffect == "Mechanical", {
                shutterEffect = "Mechanical"
                prefs.edit().putString("shutter_effect", "Mechanical").apply()
            }, Modifier.weight(1f))
        }
        ToolToggle("Âm thanh màn trập", "Dùng âm click chụp ảnh hệ thống của Android",
            Icons.Default.Tune, shutterSound) {
            shutterSound = it
            prefs.edit().putBoolean("shutter_sound_enabled", it).apply()
        }
    }
}


/**
 * Decodes a JPEG and applies its EXIF orientation to the pixels before post-processing.
 */
private fun decodeUprightBitmap(context: Context, uri: Uri): Bitmap? {
    val orientation = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.Options().let { options ->
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inMutable = true
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

private fun readCubeLut(context: Context, uri: Uri): CubeLut? = runCatching {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: return null
    var size = 0
    val min = floatArrayOf(0f, 0f, 0f)
    val max = floatArrayOf(1f, 1f, 1f)
    val rows = ArrayList<FloatArray>()
    text.lineSequence().forEach { raw ->
        val line = raw.substringBefore("#").trim()
        if (line.isEmpty()) return@forEach
        val parts = line.split(Regex("\s+"))
        when (parts.firstOrNull()) {
            "TITLE" -> Unit
            "LUT_3D_SIZE" -> size = parts.getOrNull(1)?.toIntOrNull() ?: 0
            "DOMAIN_MIN" -> if (parts.size >= 4) for (i in 0..2) min[i] = parts[i + 1].toFloat()
            "DOMAIN_MAX" -> if (parts.size >= 4) for (i in 0..2) max[i] = parts[i + 1].toFloat()
            else -> {
                if (parts.size >= 3) {
                    val rgb = parts.take(3).mapNotNull { it.toFloatOrNull() }
                    if (rgb.size == 3) rows.add(floatArrayOf(rgb[0], rgb[1], rgb[2]))
                }
            }
        }
    }
    if (size !in 2..65 || rows.size != size * size * size) return null
    CubeLut(size, min, max, FloatArray(rows.size * 3).also { out ->
        rows.forEachIndexed { index, rgb ->
            out[index * 3] = rgb[0]
            out[index * 3 + 1] = rgb[1]
            out[index * 3 + 2] = rgb[2]
        }
    })
}.getOrNull()

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
    when (name) {
        "Provia" -> { saturation(1.08f); contrast(1.04f) }
        "Velvia" -> { saturation(1.42f); contrast(1.12f) }
        "Astia" -> { saturation(0.92f); contrast(0.94f); r += 7f; b -= 3f }
        "Classic Neg" -> { saturation(1.16f); contrast(1.18f); r += 5f; b -= 6f }
        "Eterna" -> { saturation(0.72f); contrast(0.86f); r += 4f; b += 3f }
        "Acros" -> { r = luma; g = luma; b = luma; contrast(1.22f) }
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
 * Applies a built-in film-inspired look or an imported .cube LUT to the saved photo.
 * The image is processed row-by-row to avoid allocating a second full-resolution pixel array.
 */
fun applyCameraLook(context: Context, uri: Uri): Boolean {
    val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    val preset = prefs.getString("selected_filter", "None") ?: "None"
    val lutUriText = prefs.getString("selected_lut_uri", null)
    if (preset == "None" && lutUriText.isNullOrBlank()) return false

    return runCatching {
        val cube = lutUriText?.let { readCubeLut(context, Uri.parse(it)) }
        if (!lutUriText.isNullOrBlank() && cube == null) return@runCatching false
        val bitmap = decodeUprightBitmap(context, uri) ?: return@runCatching false
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (x in row.indices) {
                val pixel = row[x]
                val a = android.graphics.Color.alpha(pixel)
                val red = android.graphics.Color.red(pixel).toFloat()
                val green = android.graphics.Color.green(pixel).toFloat()
                val blue = android.graphics.Color.blue(pixel).toFloat()
                val mapped = cube?.sample(red / 255f, green / 255f, blue / 255f)
                    ?: transformPreset(preset, red, green, blue).let {
                        Triple(it.first / 255f, it.second / 255f, it.third / 255f)
                    }
                row[x] = android.graphics.Color.argb(
                    a, clamp8(mapped.first * 255f), clamp8(mapped.second * 255f), clamp8(mapped.third * 255f)
                )
            }
            bitmap.setPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
        }
        context.contentResolver.openOutputStream(uri, "wt")?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        } ?: return@runCatching false
        context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }
        }
        true
    }.getOrDefault(false)
}

/**
 * Draws a landscape-aware camera-spec frame with a compact, premium information plate.
 */
fun applyCameraWatermark(
    context: Context,
    uri: Uri,
    focal: String,
    aperture: String,
    shutter: String,
    iso: String,
    expectedLandscape: Boolean = false
): Boolean {
    val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    if (!prefs.getBoolean("watermark_enabled", false)) return false

    return runCatching {
        var bitmap = decodeUprightBitmap(context, uri) ?: return@runCatching false
        val isLandscapeBitmap = bitmap.width > bitmap.height
        if (expectedLandscape != isLandscapeBitmap && bitmap.width != bitmap.height) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height,
                Matrix().apply { postRotate(90f) }, true)
        }
        val output = if (bitmap.isMutable) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, true)
            ?: return@runCatching false
        val canvas = Canvas(output)
        val frameStyle = prefs.getString("watermark_style", "White") ?: "White"
        val foreground = if (frameStyle == "Black") AndroidColor.rgb(12, 12, 12) else AndroidColor.WHITE
        val panelColor = when (frameStyle) {
            "Black" -> AndroidColor.argb(210, 245, 245, 245)
            "Transparent" -> AndroidColor.argb(48, 0, 0, 0)
            else -> AndroidColor.argb(155, 0, 0, 0)
        }
        val minSide = minOf(output.width, output.height).toFloat()
        val inset = (minSide * 0.032f).coerceAtLeast(18f)
        val radius = (minSide * 0.012f).coerceAtLeast(8f)
        val stroke = (minSide * 0.0018f).coerceAtLeast(2f)
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = foreground
            alpha = if (frameStyle == "Transparent") 145 else 245
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        canvas.drawRoundRect(RectF(inset, inset, output.width - inset, output.height - inset),
            radius, radius, frame)
        frame.strokeWidth = (stroke * 0.45f).coerceAtLeast(1f)
        frame.alpha = if (frameStyle == "Transparent") 85 else 130
        val inner = inset + stroke * 3.2f
        canvas.drawRoundRect(RectF(inner, inner, output.width - inner, output.height - inner),
            radius * 0.72f, radius * 0.72f, frame)

        val plateHeight = (minSide * 0.105f).coerceAtLeast(74f)
        val plate = RectF(inset + stroke, output.height - inset - plateHeight,
            output.width - inset - stroke, output.height - inset - stroke)
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = panelColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(plate, radius * 0.75f, radius * 0.75f, platePaint)

        val model = prefs.getString("watermark_custom_model", "")?.trim()
            ?.takeIf { it.isNotEmpty() } ?: "\${android.os.Build.MANUFACTURER} \${android.os.Build.MODEL}"
        val detail = listOf(focal, aperture, shutter, "ISO $iso")
            .filter { it.isNotBlank() }.joinToString("    ")
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = foreground
            alpha = if (frameStyle == "Transparent") 180 else 250
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
        val left = plate.left + plateHeight * 0.25f
        text.textSize = (minSide * 0.022f).coerceIn(16f, 54f)
        canvas.drawText(model.take(48), left, plate.top + plateHeight * 0.43f, text)
        text.textSize = (minSide * 0.015f).coerceIn(12f, 38f)
        val detailWidth = text.measureText(detail)
        val availableWidth = plate.width() - plateHeight * 0.45f
        text.textSize = (text.textSize * (availableWidth / detailWidth.coerceAtLeast(1f)).coerceAtMost(1f))
            .coerceAtLeast(10f)
        canvas.drawText(detail, left, plate.top + plateHeight * 0.78f, text)

        val signature = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = foreground
            alpha = if (frameStyle == "Transparent") 135 else 195
            textSize = (minSide * 0.0105f).coerceIn(10f, 26f)
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        canvas.drawText("FUJIFILM MODE  •  CAMERA NOTES", inset + stroke * 2, inset + plateHeight * 0.25f, signature)

        context.contentResolver.openOutputStream(uri, "wt")?.use {
            output.compress(Bitmap.CompressFormat.JPEG, 98, it)
        } ?: return@runCatching false
        context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }
        }
        true
    }.getOrDefault(false)
}

@Composable
fun ContentManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fujifilm_contents", 0) }
    val luts = remember { mutableStateListOf<String>().apply {
        addAll(prefs.getStringSet("luts", emptySet()) ?: emptySet())
    } }
    val frames = remember { mutableStateListOf<String>().apply {
        addAll(prefs.getStringSet("frames", emptySet()) ?: emptySet())
    } }
    var pendingDeleteLut by remember { mutableStateOf<String?>(null) }

    val lutPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = documentDisplayName(context, uri)
            if (!name.endsWith(".cube", ignoreCase = true)) {
                Toast.makeText(context, "Chỉ hỗ trợ LUT định dạng .cube.", Toast.LENGTH_LONG).show()
            } else {
                val value = uri.toString()
                if (!luts.contains(value)) luts.add(value)
                prefs.edit().putStringSet("luts", luts.toSet()).apply()
                Toast.makeText(context, "Đã nhập LUT: $name", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val framePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val value = uri.toString()
            if (!frames.contains(value)) frames.add(value)
            prefs.edit().putStringSet("frames", frames.toSet()).apply()
        }
    }

    Column(Modifier.fillMaxSize().background(CamColors.Bg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        PageHeader("Contents Management", onBack)
        Text("LUT Manage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text("Nhập tệp .cube để quản lý hồ sơ màu.", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        Button(onClick = { lutPicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Import LUT")
        }
        if (luts.isEmpty()) EmptyNote("Chưa có LUT nào được nhập.")
        else {
            Column {
                luts.forEach { value ->
                    val name = documentDisplayName(context, Uri.parse(value))
                    ManagedItem(name, onDelete = { pendingDeleteLut = value })
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Frame Manage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text("Nhập khung phủ PNG/JPG để quản lý.", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        Button(onClick = { framePicker.launch(arrayOf("image/png", "image/jpeg")) },
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Import Frame")
        }
        if (frames.isEmpty()) EmptyNote("Chưa có frame nào được nhập.")
        else {
            Column {
                frames.forEach { value ->
                    val name = documentDisplayName(context, Uri.parse(value))
                    ManagedItem(name, onDelete = {
                        frames.remove(value)
                        prefs.edit().putStringSet("frames", frames.toSet()).apply()
                    })
                }
            }
        }
    }
    pendingDeleteLut?.let { value ->
        val name = documentDisplayName(context, Uri.parse(value))
        AlertDialog(
            onDismissRequest = { pendingDeleteLut = null },
            title = { Text("Xóa LUT?") },
            text = { Text("Bạn chắc chắn muốn xóa LUT \"$name\" khỏi danh sách đã nhập?") },
            confirmButton = {
                TextButton(onClick = {
                    luts.remove(value)
                    prefs.edit().putStringSet("luts", luts.toSet()).apply()
                    val appPrefs = context.getSharedPreferences("fujifilm_settings", 0)
                    if (appPrefs.getString("selected_lut_uri", null) == value) {
                        appPrefs.edit().remove("selected_lut_uri").apply()
                    }
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
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
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

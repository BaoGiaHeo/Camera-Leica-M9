package com.example.camui

import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
    }
}


/**
 * Burns an unobtrusive, logo-free rangefinder-style frame into the captured JPEG.
 * Large images are retried with a lower decode sample size to avoid exhausting device memory.
 */
fun applyCameraWatermark(
    context: Context,
    uri: Uri,
    focal: String,
    aperture: String,
    shutter: String,
    iso: String
): Boolean {
    val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    if (!prefs.getBoolean("watermark_enabled", false)) return false

    return runCatching {
        val orientation = context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            ExifInterface(pfd.fileDescriptor).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        var decoded: Bitmap? = null
        for (sample in listOf(1, 2, 4, 8)) {
            decoded = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.Options().let { options ->
                        options.inSampleSize = sample
                        BitmapFactory.decodeStream(input, null, options)
                    }
                }
            } catch (_: OutOfMemoryError) {
                null
            }
            if (decoded != null) break
        }
        val source = decoded ?: return@runCatching false
        val rotateDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90f
            ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180f
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270f
            else -> 0f
        }
        val upright = if (rotateDegrees != 0f) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height,
                Matrix().apply { postRotate(rotateDegrees) }, true)
        } else source
        val output = if (upright.isMutable) upright else (upright.copy(Bitmap.Config.ARGB_8888, true) ?: return@runCatching false)
        val canvas = Canvas(output)
        val frameStyle = prefs.getString("watermark_style", "White") ?: "White"
        val baseColor = when (frameStyle) {
            "Black" -> AndroidColor.BLACK
            "Transparent" -> AndroidColor.WHITE
            else -> AndroidColor.WHITE
        }
        val alpha = if (frameStyle == "Transparent") 105 else 235
        val minSide = minOf(output.width, output.height).toFloat()
        val inset = (minSide * 0.035f).coerceAtLeast(12f)
        val stroke = (minSide * 0.003f).coerceAtLeast(2f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = baseColor
            this.alpha = alpha
            this.style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        canvas.drawRect(RectF(inset, inset, output.width - inset, output.height - inset), paint)
        paint.style = Paint.Style.FILL
        paint.textSize = (minSide * 0.022f).coerceIn(12f, 42f)
        paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        val model = prefs.getString("watermark_custom_model", "")?.trim()
            ?.takeIf { it.isNotEmpty() } ?: "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val line1 = model
        val line2 = listOf(focal, aperture, shutter, "ISO $iso")
            .filter { it.isNotBlank() }.joinToString("   ")
        val bottomY = output.height - inset * 0.48f
        canvas.drawText(line1.take(48), inset * 1.25f, bottomY - paint.textSize * 1.25f, paint)
        val line2Width = paint.measureText(line2)
        canvas.drawText(line2, (output.width - inset * 1.25f - line2Width).coerceAtLeast(inset),
            bottomY, paint)

        val bytes = java.io.ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.JPEG, 96, bytes)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes.toByteArray()) }
            ?: return@runCatching false
        context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
            ExifInterface(pfd.fileDescriptor).apply {
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

    val lutPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val value = uri.toString()
            if (!luts.contains(value)) luts.add(value)
            prefs.edit().putStringSet("luts", luts.toSet()).apply()
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
                    val name = Uri.parse(value).lastPathSegment?.substringAfterLast('/') ?: value
                    ManagedItem(name, onDelete = {
                        luts.remove(value)
                        prefs.edit().putStringSet("luts", luts.toSet()).apply()
                    })
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
                    val name = Uri.parse(value).lastPathSegment?.substringAfterLast('/') ?: value
                    ManagedItem(name, onDelete = {
                        frames.remove(value)
                        prefs.edit().putStringSet("frames", frames.toSet()).apply()
                    })
                }
            }
        }
    }
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

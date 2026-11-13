package com.example.camui

import android.content.Context
import android.content.ContentValues
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
import android.provider.MediaStore
import android.content.ContentUris
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    var watermarkEnabled by remember { mutableStateOf(prefs.getBoolean("watermark_enabled", true)) }
    var watermarkStyle by remember { mutableStateOf(prefs.getString("watermark_style", "White") ?: "White") }
    var watermarkTemplate by remember { mutableStateOf(prefs.getString("watermark_template", "standard1") ?: "standard1") }
    var watermarkShowTime by remember { mutableStateOf(prefs.getBoolean("watermark_show_time", true)) }
    var watermarkShowModel by remember { mutableStateOf(prefs.getBoolean("watermark_show_model", true)) }
    var watermarkShowSpecs by remember { mutableStateOf(prefs.getBoolean("watermark_show_specs", true)) }
    var customModel by remember { mutableStateOf(prefs.getString("watermark_custom_model", "") ?: "") }
    var shutterEffect by remember { mutableStateOf(prefs.getString("shutter_effect", "Film Wash") ?: "Film Wash") }
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
        Text("Watermark", color = CamColors.TextColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        ToolToggle("Enable Watermark", "Khung ảnh phong cách rangefinder, không có logo thương hiệu",
            Icons.Default.Image, watermarkEnabled) {
            watermarkEnabled = it
            prefs.edit().putBoolean("watermark_enabled", it).apply()
        }
        Text("Mẫu watermark · chọn riêng từng kiểu", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp, top = 9.dp))
        val watermarkTemplates = listOf(
            Triple("standard1", "Standard 1", "EXIF cổ điển"),
            Triple("standard2", "Standard 2", "Viền bo + khoảng trống"),
            Triple("blur", "Blur", "Nền mờ, thông số nổi"),
            Triple("normal1", "Normal 1", "Thông số góc phải"),
            Triple("normal2", "Normal 2", "Ngày chụp tối giản"),
            Triple("center_logo", "Center Logo", "Logo ở chính giữa")
        )
        watermarkTemplates.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                rowItems.forEach { (id, title, subtitle) ->
                    WatermarkTemplateCard(
                        id = id, title = title, subtitle = subtitle,
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("White", "Black", "Transparent").forEach { style ->
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
        ToolToggle("Âm thanh màn trập", "Dùng âm click chụp ảnh hệ thống của Android",
            Icons.Default.Tune, shutterSound) {
            shutterSound = it
            prefs.edit().putBoolean("shutter_sound_enabled", it).apply()
        }
    }
}



@Composable
private fun WatermarkTemplateCard(
    id: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.clip(RoundedCornerShape(13.dp))
            .background(if (selected) Color(0xFF333126) else CamColors.Card)
            .border(
                width = if (selected) 1.5.dp else 0.7.dp,
                color = if (selected) CamColors.Yellow else Color(0x334C535B),
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onClick)
            .padding(7.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(88.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF475550))
        ) {
            ComposeCanvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFF697A70))
                drawCircle(Color(0xFFFFD8A0), size.minDimension * 0.13f,
                    Offset(size.width * 0.76f, size.height * 0.28f))
                drawLine(Color(0xFF526A59), Offset(0f, size.height * 0.64f),
                    Offset(size.width, size.height * 0.52f), strokeWidth = size.height * 0.25f)
                drawLine(Color(0xFF31483E), Offset(0f, size.height * 0.82f),
                    Offset(size.width, size.height * 0.72f), strokeWidth = size.height * 0.32f)
            }
            when (id) {
                "standard1" -> Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Color(0xD9000000)).padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("FUJIFILM X", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    Text("23mm  •  f/2  •  1/125  •  ISO 400",
                        color = Color.White.copy(alpha = 0.82f), fontSize = 5.5.sp, maxLines = 1)
                }
                "standard2" -> Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(5.dp)
                        .background(Color(0xEEFFFFFF), RoundedCornerShape(4.dp)).padding(5.dp)
                ) {
                    Text("FUJIFILM  /  X SERIES", color = Color(0xFF202020),
                        fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
                    Text("23mm  ·  f/2.0  ·  ISO 400", color = Color(0xFF353535), fontSize = 5.5.sp)
                }
                "blur" -> Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Color(0xAA17201D)).padding(6.dp)
                ) {
                    Text("X SERIES", color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                    Text("23mm   f/2.0   ISO 400", color = Color.White, fontSize = 5.5.sp)
                }
                "normal1" -> Column(
                    Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("23mm  f/2.0", color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                    Text("1/125  ISO 400", color = Color.White, fontSize = 5.5.sp)
                }
                "normal2" -> Column(Modifier.align(Alignment.BottomStart).padding(6.dp)) {
                    Text("FUJIFILM MODE", color = Color(0xFFFFB15F), fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold)
                    Text("2026.11.13  •  18:42", color = Color.White, fontSize = 5.5.sp)
                }
                else -> Column(
                    Modifier.align(Alignment.Center).padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("FUJIFILM", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.padding(vertical = 3.dp).fillMaxWidth(0.62f).height(1.dp)
                        .background(Color.White.copy(alpha = 0.8f)))
                    Text("X SERIES", color = Color.White, fontSize = 5.5.sp, letterSpacing = 1.sp)
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
fun normalizeCapturedImage(
    context: Context,
    uri: Uri,
    fallbackRotationDegrees: Int = 0,
    expectedLandscape: Boolean? = null
): Boolean {
    return runCatching {
        // Decode raw JPEG pixels; BitmapFactory does not apply EXIF orientation.
        // CameraX rotationDegrees is the clockwise transform needed for targetRotation.
        val rawBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return@runCatching false

        val cameraXDegrees = ((fallbackRotationDegrees % 360) + 360) % 360
        val exifInfo = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor)
            }
        }.getOrNull()
        val exifOrientation = exifInfo?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL
        // Match ExifInterface.getRotationDegrees()/isFlipped() semantics without relying
        // on newer convenience accessors that are absent in this project's dependency.
        val exifDegrees = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE -> 90
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE -> 270
            else -> 0
        }
        val exifFlipped = exifOrientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL ||
            exifOrientation == ExifInterface.ORIENTATION_FLIP_VERTICAL ||
            exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE

        fun transform(degrees: Int, flipHorizontal: Boolean): Bitmap {
            val normalized = ((degrees % 360) + 360) % 360
            if (normalized == 0 && !flipHorizontal) return rawBitmap
            val matrix = Matrix().apply {
                if (flipHorizontal) preScale(-1f, 1f)
                if (normalized != 0) postRotate(normalized.toFloat())
            }
            return Bitmap.createBitmap(
                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
            )
        }

        fun matchesExpectedOrientation(bitmap: Bitmap): Boolean? {
            return expectedLandscape?.let { landscape ->
                if (landscape) bitmap.width > bitmap.height
                else bitmap.height > bitmap.width
            }
        }

        // EXIF and CameraX are alternative descriptions of the same JPEG orientation.
        // Never add the two angles together.
        var appliedSource = "CameraX"
        var appliedDegrees = cameraXDegrees
        var finalBitmap = transform(cameraXDegrees, false)

        if (matchesExpectedOrientation(finalBitmap) == false) {
            // Use EXIF only as a mutually-exclusive fallback when CameraX rotation
            // contradicts the known portrait/landscape capture geometry.
            val exifCandidate = transform(exifDegrees, exifFlipped)
            if (matchesExpectedOrientation(exifCandidate) == true) {
                finalBitmap = exifCandidate
                appliedSource = "EXIF fallback"
                appliedDegrees = exifDegrees
            } else {
                android.util.Log.e(
                    "FUJI_ORIENTATION",
                    "No candidate matches expectedLandscape=" + expectedLandscape +
                        "; raw=" + rawBitmap.width + "x" + rawBitmap.height +
                        ", cameraX=" + cameraXDegrees + ", exif=" + exifDegrees
                )
                return@runCatching false
            }
        } else if (exifDegrees == cameraXDegrees && exifFlipped) {
            finalBitmap = transform(cameraXDegrees, true)
            appliedSource = "CameraX + EXIF mirror"
        }

        val outputWritten = context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 98, output)
        } ?: false
        if (!outputWritten) return@runCatching false

        // Rotation is now baked into pixels. Clear metadata so gallery/watermark passes
        // cannot apply another rotation.
        val descriptor = context.contentResolver.openFileDescriptor(uri, "rw")
            ?: return@runCatching false
        descriptor.use {
            ExifInterface(it.fileDescriptor).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }
        }
        setMediaStoreOrientationUpright(context, uri)

        android.util.Log.d(
            "FUJI_ORIENTATION",
            "source=" + appliedSource + ", rotation=" + appliedDegrees +
                ", result=" + finalBitmap.width + "x" + finalBitmap.height +
                ", expectedLandscape=" + expectedLandscape
        )
        true
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

private fun readCubeLut(context: Context, uri: Uri): CubeLut? {
    return runCatching {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return@runCatching null
        var size = 0
        val min = floatArrayOf(0f, 0f, 0f)
        val max = floatArrayOf(1f, 1f, 1f)
        val rows = ArrayList<FloatArray>()
        text.lineSequence().forEach { raw ->
            val line = raw.substringBefore("#").trim()
            if (line.isEmpty()) return@forEach
            val parts = line.split(Regex("\\s+"))
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
        if (size !in 2..65 || rows.size != size * size * size) return@runCatching null
        CubeLut(size, min, max, FloatArray(rows.size * 3).also { out ->
            rows.forEachIndexed { index, rgb ->
                out[index * 3] = rgb[0]
                out[index * 3 + 1] = rgb[1]
                out[index * 3 + 2] = rgb[2]
            }
        })
    }.getOrNull()
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
        setMediaStoreOrientationUpright(context, uri)
        true
    }.getOrDefault(false)
}

/**
 * Draws a camera-spec frame directly onto the upright pixel matrix. No portrait/landscape
 * heuristic is applied here: the image pixels, metadata and watermark all share one orientation.
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
    if (!prefs.getBoolean("watermark_enabled", true)) {
        android.util.Log.d("FUJI_WATERMARK", "Watermark disabled in settings")
        return false
    }

    return runCatching {
        val bitmap = decodeUprightBitmap(context, uri) ?: return@runCatching false
        val output = if (bitmap.isMutable) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, true)
            ?: return@runCatching false
        val canvas = Canvas(output)
        val template = prefs.getString("watermark_template", "standard1") ?: "standard1"
        val frameStyle = prefs.getString("watermark_style", "White") ?: "White"
        val showTime = prefs.getBoolean("watermark_show_time", true)
        val showModel = prefs.getBoolean("watermark_show_model", true)
        val showSpecs = prefs.getBoolean("watermark_show_specs", true)

        // Text and backing plate stay readable; the selected color controls only the frame.
        val frameColor = if (frameStyle == "Black") AndroidColor.BLACK else AndroidColor.WHITE
        val panelColor = if (frameStyle == "Transparent") AndroidColor.TRANSPARENT
            else AndroidColor.argb(166, 8, 12, 11)
        val minSide = minOf(output.width, output.height).toFloat()
        val margin = (minSide * 0.035f).coerceAtLeast(18f)
        val model = prefs.getString("watermark_custom_model", "")?.trim()
            ?.takeIf { it.isNotEmpty() } ?: "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val specs = listOf(focal, aperture, shutter, "ISO $iso")
            .filter { it.isNotBlank() }.joinToString("   •   ").takeIf { showSpecs }.orEmpty()
        val modelLine = model.takeIf { showModel }.orEmpty()
        val timeLine = java.text.SimpleDateFormat("yyyy.MM.dd  HH:mm", java.util.Locale.US)
            .format(java.util.Date()).takeIf { showTime }.orEmpty()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            alpha = 235
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(239, 164, 91)
            typeface = android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.NORMAL)
        }
        fun fitText(value: String, paint: Paint, x: Float, baseline: Float, maxWidth: Float, baseSize: Float) {
            if (value.isBlank()) return
            paint.textSize = baseSize
            while (paint.textSize > 9f && paint.measureText(value) > maxWidth) {
                paint.textSize *= 0.92f
            }
            canvas.drawText(value, x, baseline, paint)
        }
        fun drawFrame(rect: RectF, radius: Float) {
            if (frameStyle == "Transparent") return
            val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = frameColor
                alpha = 245
                style = Paint.Style.STROKE
                strokeWidth = (minSide * 0.0022f).coerceAtLeast(2f)
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
            if (frameStyle != "Transparent") {
                canvas.drawRoundRect(plate, (minSide * 0.012f).coerceAtLeast(7f),
                    (minSide * 0.012f).coerceAtLeast(7f), paint)
            }
            return plate
        }

        if (template != "center_logo") {
            drawFrame(RectF(margin, margin, output.width - margin, output.height - margin), margin * 0.30f)
        }

        when (template) {
            "standard2" -> {
                val plate = drawBottomPlate(0.145f, margin * 1.25f)
                val left = plate.left + plate.height() * 0.22f
                fitText(modelLine.uppercase(), titlePaint, left, plate.top + plate.height() * 0.38f,
                    plate.width() - plate.height() * 0.42f, minSide * 0.024f)
                fitText(specs, detailPaint, left, plate.top + plate.height() * 0.70f,
                    plate.width() - plate.height() * 0.42f, minSide * 0.014f)
                fitText(timeLine, detailPaint, plate.left + 5f, plate.top - margin * 0.25f,
                    plate.width(), minSide * 0.010f)
            }
            "blur" -> {
                val plate = drawBottomPlate(0.17f, margin)
                val left = plate.left + plate.height() * 0.20f
                fitText(modelLine, titlePaint, left, plate.top + plate.height() * 0.38f,
                    plate.width() - plate.height() * 0.35f, minSide * 0.026f)
                fitText(specs, detailPaint, left, plate.top + plate.height() * 0.70f,
                    plate.width() - plate.height() * 0.35f, minSide * 0.014f)
                fitText(timeLine, accentPaint, left, plate.top - margin * 0.25f,
                    plate.width() - plate.height() * 0.35f, minSide * 0.011f)
            }
            "normal1" -> {
                val right = output.width - margin
                titlePaint.textAlign = Paint.Align.RIGHT
                detailPaint.textAlign = Paint.Align.RIGHT
                var baseline = output.height - margin * 1.18f
                fitText(timeLine, detailPaint, right, baseline, output.width * 0.60f, minSide * 0.014f)
                baseline -= margin * 0.92f
                fitText(specs, detailPaint, right, baseline, output.width * 0.60f, minSide * 0.016f)
                baseline -= margin * 0.98f
                fitText(modelLine, titlePaint, right, baseline, output.width * 0.60f, minSide * 0.022f)
                titlePaint.textAlign = Paint.Align.LEFT
                detailPaint.textAlign = Paint.Align.LEFT
            }
            "normal2" -> {
                accentPaint.textSize = (minSide * 0.020f).coerceIn(16f, 42f)
                canvas.drawText("FUJIFILM MODE", margin, output.height - margin * 2.05f, accentPaint)
                val optionalLines = listOf(modelLine, specs, timeLine).filter { it.isNotBlank() }
                optionalLines.forEachIndexed { index, line ->
                    fitText(line, detailPaint, margin,
                        output.height - margin * (1.12f + index * 0.82f),
                        output.width - margin * 2f, minSide * 0.013f)
                }
            }
            "center_logo" -> {
                val centerX = output.width / 2f
                val centerY = output.height * 0.52f
                val panel = RectF(output.width * 0.12f, centerY - minSide * 0.085f,
                    output.width * 0.88f, centerY + minSide * 0.105f)
                val backing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = panelColor
                    style = Paint.Style.FILL
                }
                if (frameStyle != "Transparent") {
                    canvas.drawRoundRect(panel, minSide * 0.018f, minSide * 0.018f, backing)
                }
                drawFrame(panel, minSide * 0.018f)
                titlePaint.textAlign = Paint.Align.CENTER
                titlePaint.textSize = (minSide * 0.034f).coerceIn(22f, 66f)
                canvas.drawText("FUJIFILM", centerX, centerY - minSide * 0.006f, titlePaint)
                if (frameStyle != "Transparent") {
                    val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = frameColor
                        alpha = 245
                        strokeWidth = (minSide * 0.0015f).coerceAtLeast(2f)
                    }
                    canvas.drawLine(output.width * 0.30f, centerY + minSide * 0.025f,
                        output.width * 0.70f, centerY + minSide * 0.025f, line)
                }
                detailPaint.textAlign = Paint.Align.CENTER
                val optionalLines = listOf(modelLine, specs, timeLine).filter { it.isNotBlank() }
                optionalLines.forEachIndexed { index, value ->
                    fitText(value, detailPaint, centerX,
                        centerY + minSide * (0.060f + index * 0.032f),
                        panel.width() * 0.88f, minSide * 0.011f)
                }
                titlePaint.textAlign = Paint.Align.LEFT
                detailPaint.textAlign = Paint.Align.LEFT
            }
            else -> {
                val plate = drawBottomPlate(0.12f, margin)
                val left = plate.left + plate.height() * 0.22f
                fitText(modelLine, titlePaint, left, plate.top + plate.height() * 0.38f,
                    plate.width() - plate.height() * 0.42f, minSide * 0.023f)
                fitText(specs, detailPaint, left, plate.top + plate.height() * 0.70f,
                    plate.width() - plate.height() * 0.42f, minSide * 0.014f)
                fitText(timeLine, accentPaint, margin + 2f,
                    margin + minSide * 0.012f, output.width - margin * 2f, minSide * 0.0105f)
            }
        }

        val outputWritten = context.contentResolver.openOutputStream(uri, "wt")?.use {
            output.compress(Bitmap.CompressFormat.JPEG, 98, it)
        } ?: false
        if (!outputWritten) return@runCatching false
        context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }
        }
        setMediaStoreOrientationUpright(context, uri)
        true
    }.onFailure {
        android.util.Log.e("FUJI_WATERMARK", "applyCameraWatermark failed", it)
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
        Text("LUT Manage", color = CamColors.TextColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
        Text("Frame Manage", color = CamColors.TextColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
fun AlbumScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<AlbumPhotoEntry>>(emptyList()) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
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

    Column(Modifier.fillMaxSize().background(CamColors.Bg).padding(12.dp)) {
        PageHeader("Album · Fujifilm Mode", onBack)
        when {
            isLoading -> Text("Đang tải ảnh…", color = CamColors.Sub, modifier = Modifier.padding(12.dp))
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
                        .background(CamColors.Card).clickable { selectedPhoto = entry.uri }
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
                Text("Đóng", color = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)
                        .clip(RoundedCornerShape(14.dp)).background(CamColors.Glass)
                        .clickable { selectedPhoto = null; selectedBitmap = null }.padding(12.dp))
            }
        }
    }
}

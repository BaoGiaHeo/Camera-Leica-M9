package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.provider.MediaStore
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WatermarkPanelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("watermark_enabled", true)) }
    var layout by remember { mutableStateOf(WatermarkLayoutType.fromPreference(prefs.getString("watermark_template", "bar"))) }
    var invert by remember { mutableStateOf(prefs.getBoolean("watermark_invert", false)) }
    var sizeStep by remember { mutableStateOf(prefs.getInt("watermark_size_step", 0).coerceIn(-2, 2)) }
    var fields by remember {
        mutableStateOf(
            WatermarkFieldVisibility(
                cameraName = prefs.getBoolean("watermark_field_name", true),
                lens = prefs.getBoolean("watermark_field_lens", true),
                focalLength = prefs.getBoolean("watermark_field_focal", true),
                aperture = prefs.getBoolean("watermark_field_aperture", true),
                shutter = prefs.getBoolean("watermark_field_shutter", true),
                iso = prefs.getBoolean("watermark_field_iso", true),
                date = prefs.getBoolean("watermark_field_date", true),
                time = prefs.getBoolean("watermark_field_clock", false),
                location = prefs.getBoolean("watermark_field_location", true),
                filter = prefs.getBoolean("watermark_field_filter", true)
            )
        )
    }
    var locationEnabled by remember { mutableStateOf(prefs.getBoolean("watermark_location_enabled", true)) }
    var locationMode by remember { mutableStateOf(prefs.getString("watermark_location_mode", "PLACE") ?: "PLACE") }
    var trueFocal by remember { mutableStateOf(prefs.getBoolean("watermark_use_true_focal", false)) }
    var customModel by remember { mutableStateOf(prefs.getString("watermark_custom_model", "") ?: "") }
    var saveMode by remember { mutableStateOf(prefs.getString("watermark_save_mode", "watermarked") ?: "watermarked") }
    var story916 by remember { mutableStateOf(prefs.getBoolean("watermark_story_916", false)) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val settings = WatermarkLayoutSettings(
        layout = layout,
        sizeStep = sizeStep,
        invert = invert,
        fields = fields,
        locationEnabled = locationEnabled,
        locationMode = locationMode,
        useTrueFocalLength = trueFocal,
        customCameraName = customModel,
        saveMode = saveMode,
        storyNineSixteen = story916
    )
    val previewMetadata = WatermarkPhotoMetadata(
        cameraName = customModel.trim().ifBlank { "X-T5" },
        lensName = "Main",
        focal35mm = "35mm",
        trueFocalLength = "23mm",
        aperture = "f/2.0",
        shutter = "1/125s",
        iso = "ISO 400",
        date = "2024.12.16",
        time = "14:32",
        location = "Winnipeg",
        coordinates = "49°53'N 97°08'W",
        filterName = "CLASSIC NEG.",
        filterCategory = "Classic",
        filterBadge = "CLASSIC\nNEG."
    )

    LaunchedEffect(Unit) {
        sourceBitmap = loadWatermarkPreviewPhoto(context)
    }
    LaunchedEffect(sourceBitmap, settings, customModel) {
        val source = sourceBitmap ?: return@LaunchedEffect
        val metadata = previewMetadata.copy(
            cameraName = customModel.trim().ifBlank { previewMetadata.cameraName }
        )
        val rendered = withContext(Dispatchers.IO) {
            WatermarkRenderer.renderPreview(context, source, settings, metadata, 1600)
        }
        val previous = previewBitmap
        previewBitmap = rendered
        if (previous != null && previous !== rendered && !previous.isRecycled) previous.recycle()
    }
    SideEffect {
        prefs.edit()
            .putBoolean("watermark_enabled", enabled)
            .putString("watermark_template", WatermarkLayoutType.preferenceValue(layout))
            .putBoolean("watermark_invert", invert)
            .putInt("watermark_size_step", sizeStep)
            .putBoolean("watermark_field_name", fields.cameraName)
            .putBoolean("watermark_field_lens", fields.lens)
            .putBoolean("watermark_field_focal", fields.focalLength)
            .putBoolean("watermark_field_aperture", fields.aperture)
            .putBoolean("watermark_field_shutter", fields.shutter)
            .putBoolean("watermark_field_iso", fields.iso)
            .putBoolean("watermark_field_date", fields.date)
            .putBoolean("watermark_field_clock", fields.time)
            .putBoolean("watermark_field_location", fields.location)
            .putBoolean("watermark_field_filter", fields.filter)
            .putBoolean("watermark_location_enabled", locationEnabled)
            .putString("watermark_location_mode", locationMode)
            .putBoolean("watermark_use_true_focal", trueFocal)
            .putString("watermark_custom_model", customModel)
            .putString("watermark_save_mode", saveMode)
            .putBoolean("watermark_story_916", story916)
            .apply()
    }
    DisposableEffect(Unit) {
        onDispose {
            previewBitmap?.takeIf { !it.isRecycled }?.recycle()
            sourceBitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    FujiPanel(
        title = stringResource(R.string.watermark_panel_title),
        breadcrumb = stringResource(R.string.watermark_panel_breadcrumb),
        modifier = Modifier.fillMaxSize(),
        onBack = onBack
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                Modifier.weight(1.05f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    stringResource(R.string.watermark_preview),
                    color = FujiSkin.muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    Modifier.weight(1f).fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(ComposeColor(0xFF666666))
                        .border(1.dp, ComposeColor(0xFF596366), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    previewBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = stringResource(R.string.watermark_preview),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } ?: Text(
                        stringResource(R.string.watermark_preview_loading),
                        color = ComposeColor.White,
                        fontSize = 10.sp
                    )
                }
            }
            Column(
                Modifier.weight(0.95f).fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.watermark_enabled), color = ComposeColor.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Text(
                        if (enabled) stringResource(R.string.watermark_status_on) else stringResource(R.string.watermark_status_off),
                        color = if (enabled) ComposeColor(0xFF55D58A) else FujiSkin.muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(stringResource(R.string.watermark_layout), color = FujiSkin.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    WatermarkLayoutType.values().forEach { item ->
                        val selected = layout == item
                        Button(
                            onClick = { layout = item },
                            modifier = Modifier.animateContentSize(tween(250)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(item.watermarkLayoutString()),
                                fontSize = 9.sp,
                                color = if (selected) ComposeColor.White else ComposeColor(0xFFD0D0D0)
                            )
                        }
                    }
                }
                if (layout == WatermarkLayoutType.CARD) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.watermark_story_916), color = ComposeColor.White, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f))
                        Switch(checked = story916, onCheckedChange = { story916 = it })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.watermark_invert), color = ComposeColor.White, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = invert, onCheckedChange = { invert = it })
                }
                Text(
                    stringResource(R.string.watermark_size_value, sizeStep),
                    color = ComposeColor.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = sizeStep.toFloat(),
                    onValueChange = { sizeStep = it.toInt().coerceIn(-2, 2) },
                    valueRange = -2f..2f,
                    steps = 3
                )
                Text(stringResource(R.string.watermark_fields), color = FujiSkin.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    WatermarkFieldRow(stringResource(R.string.watermark_field_model), fields.cameraName) { fields = fields.copy(cameraName = it) }
                    WatermarkFieldRow(stringResource(R.string.watermark_field_lens), fields.lens) { fields = fields.copy(lens = it) }
                    WatermarkFieldRow(stringResource(R.string.watermark_field_focal), fields.focalLength) { fields = fields.copy(focalLength = it) }
                    WatermarkFieldRow(stringResource(R.string.watermark_field_aperture), fields.aperture) { fields = fields.copy(aperture = it) }
                    WatermarkFieldRow(stringResource(R.string.watermark_field_shutter), fields.shutter) { fields = fields.copy(shutter = it) }
                    WatermarkFieldRow(stringResource(R.string.watermark_field_iso), fields.iso) { fields = fields.copy(iso = it) }
                    WatermarkFieldRow(stringResource(R.string.watermark_field_date), fields.date) { fields = fields.copy(date = it) }
                    WatermarkFieldRow(stringResource(R.string.watermark_field_time), fields.time) { fields = fields.copy(time = it) }
                    WatermarkFieldRow(stringResource(R.string.watermark_field_location), fields.location) { fields = fields.copy(location = it) }
                    WatermarkFieldRow(stringResource(R.string.watermark_field_filter), fields.filter) { fields = fields.copy(filter = it) }
                }
                if (layout == WatermarkLayoutType.CARD) {
                    Text(stringResource(R.string.watermark_location_na), color = FujiSkin.muted, fontSize = 9.sp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.watermark_location), color = ComposeColor.White, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = locationEnabled,
                            onCheckedChange = { locationEnabled = it }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WatermarkChoice(
                            selected = locationMode == "PLACE",
                            label = stringResource(R.string.watermark_location_place),
                            onClick = { locationMode = "PLACE" }
                        )
                        WatermarkChoice(
                            selected = locationMode == "COORDS",
                            label = stringResource(R.string.watermark_location_coords),
                            onClick = { locationMode = "COORDS" }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.watermark_true_focal), color = ComposeColor.White, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = trueFocal, onCheckedChange = { trueFocal = it })
                }
                Text(stringResource(R.string.watermark_camera_name), color = ComposeColor.White, fontSize = 10.sp)
                TextField(
                    value = customModel,
                    onValueChange = { customModel = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.watermark_camera_name_hint)) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = ComposeColor.White,
                        unfocusedTextColor = ComposeColor.White,
                        focusedContainerColor = ComposeColor(0xFF20282A),
                        unfocusedContainerColor = ComposeColor(0xFF20282A)
                    )
                )
                Text(stringResource(R.string.watermark_save_mode), color = ComposeColor.White, fontSize = 10.sp)
                WatermarkChoice(
                    selected = saveMode == "watermarked",
                    label = stringResource(R.string.watermark_save_only),
                    onClick = { saveMode = "watermarked" }
                )
                WatermarkChoice(
                    selected = saveMode == "both",
                    label = stringResource(R.string.watermark_save_both),
                    onClick = { saveMode = "both" }
                )
                Button(
                    onClick = {
                        enabled = true
                        layout = WatermarkLayoutType.BAR
                        invert = false
                        sizeStep = 0
                        fields = WatermarkFieldVisibility()
                        locationEnabled = true
                        locationMode = "PLACE"
                        trueFocal = false
                        customModel = ""
                        saveMode = "watermarked"
                        story916 = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.watermark_reset))
                }
            }
        }
    }
}

@Composable
private fun WatermarkFieldRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = ComposeColor.White, fontSize = 9.sp, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun WatermarkChoice(selected: Boolean, label: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(
            label,
            color = if (selected) ComposeColor.White else ComposeColor(0xFFCCCCCC),
            fontSize = 9.sp
        )
    }
}

private fun WatermarkLayoutType.watermarkLayoutString(): Int = when (this) {
    WatermarkLayoutType.BAR -> R.string.watermark_layout_bar
    WatermarkLayoutType.CAPTION -> R.string.watermark_layout_caption
    WatermarkLayoutType.CARD -> R.string.watermark_layout_card
    WatermarkLayoutType.GALLERY -> R.string.watermark_layout_gallery
    WatermarkLayoutType.BLACK -> R.string.watermark_layout_black
}

private suspend fun loadWatermarkPreviewPhoto(context: Context): Bitmap = withContext(Dispatchers.IO) {
    val uri = runCatching {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            } else null
        }
    }.getOrNull()
    val latest = uri?.let {
        runCatching {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                stream.mark(Int.MAX_VALUE)
                BitmapFactory.decodeStream(stream, null, bounds)
                val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
                var sample = 1
                while (maxSide / sample > 1600) sample *= 2
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                context.contentResolver.openInputStream(it)?.use { input -> BitmapFactory.decodeStream(input, null, options) }
            }
        }.getOrNull()
    }
    latest ?: Bitmap.createBitmap(960, 640, Bitmap.Config.RGB_565).also { bitmap ->
        Canvas(bitmap).drawColor(Color.rgb(118, 118, 118))
    }
}

package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import androidx.compose.material3.MaterialTheme
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
        sourceBitmap = createWatermarkPreviewSample()
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
                    FujiSwitch(checked = enabled, onCheckedChange = { enabled = it })
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
                        FujiChip(
                            text = stringResource(item.watermarkLayoutString()),
                            selected = selected,
                            onClick = { layout = item },
                            modifier = Modifier.animateContentSize(tween(250))
                        )
                    }
                }
                if (layout == WatermarkLayoutType.CARD) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.watermark_story_916), color = ComposeColor.White, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f))
                        FujiSwitch(checked = story916, onCheckedChange = { story916 = it })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.watermark_invert), color = ComposeColor.White, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    FujiSwitch(checked = invert, onCheckedChange = { invert = it })
                }
                Text(
                    stringResource(R.string.watermark_size_value, sizeStep),
                    color = ComposeColor.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                FujiSlider(
                    value = sizeStep.toFloat(),
                    onValueChange = { sizeStep = it.toInt().coerceIn(-2, 2) },
                    valueRange = -2f..2f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
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
                        FujiSwitch(
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
                    FujiSwitch(checked = trueFocal, onCheckedChange = { trueFocal = it })
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
                FujiChip(
                    text = stringResource(R.string.watermark_reset),
                    selected = false,
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
                )
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
        FujiSwitch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun WatermarkChoice(selected: Boolean, label: String, onClick: () -> Unit) {
    FujiChip(text = label, selected = selected, onClick = onClick)
}

private fun WatermarkLayoutType.watermarkLayoutString(): Int = when (this) {
    WatermarkLayoutType.BAR -> R.string.watermark_layout_bar
    WatermarkLayoutType.CAPTION -> R.string.watermark_layout_caption
    WatermarkLayoutType.CARD -> R.string.watermark_layout_card
    WatermarkLayoutType.GALLERY -> R.string.watermark_layout_gallery
    WatermarkLayoutType.BLACK -> R.string.watermark_layout_black
}

/**
 * A clean, generated preview source. Never load the newest MediaStore image here:
 * that item may already contain a watermark and would be stamped a second time.
 */
private fun createWatermarkPreviewSample(): Bitmap {
    val width = 1600
    val height = 1000
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 0f, height.toFloat(),
        intArrayOf(
            Color.rgb(93, 144, 170),
            Color.rgb(190, 198, 183),
            Color.rgb(226, 205, 158)
        ),
        floatArrayOf(0f, 0.58f, 1f),
        android.graphics.Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null

    paint.color = Color.rgb(245, 211, 143)
    canvas.drawCircle(width * 0.76f, height * 0.28f, height * 0.075f, paint)

    val farRidge = android.graphics.Path().apply {
        moveTo(0f, height * 0.66f)
        lineTo(width * 0.18f, height * 0.37f)
        lineTo(width * 0.34f, height * 0.59f)
        lineTo(width * 0.51f, height * 0.31f)
        lineTo(width * 0.74f, height * 0.64f)
        lineTo(width.toFloat(), height * 0.40f)
        lineTo(width.toFloat(), height.toFloat())
        lineTo(0f, height.toFloat())
        close()
    }
    paint.color = Color.rgb(79, 105, 100)
    canvas.drawPath(farRidge, paint)

    val nearRidge = android.graphics.Path().apply {
        moveTo(0f, height * 0.76f)
        lineTo(width * 0.22f, height * 0.56f)
        lineTo(width * 0.41f, height * 0.73f)
        lineTo(width * 0.65f, height * 0.51f)
        lineTo(width.toFloat(), height * 0.72f)
        lineTo(width.toFloat(), height.toFloat())
        lineTo(0f, height.toFloat())
        close()
    }
    paint.color = Color.rgb(42, 69, 66)
    canvas.drawPath(nearRidge, paint)

    paint.color = Color.rgb(76, 126, 132)
    canvas.drawRect(0f, height * 0.78f, width.toFloat(), height.toFloat(), paint)
    paint.color = Color.rgb(166, 185, 170)
    paint.strokeWidth = 3f
    for (index in 0 until 18) {
        val y = height * (0.81f + index * 0.009f)
        val inset = (index % 4) * 24f
        canvas.drawLine(
            width * 0.12f + inset,
            y,
            width * 0.88f - inset,
            y + 2f,
            paint
        )
    }

    paint.color = Color.rgb(30, 48, 43)
    for (index in 0 until 7) {
        val x = width * (0.04f + index * 0.045f)
        val treeBase = height * (0.73f + (index % 3) * 0.025f)
        paint.strokeWidth = 9f
        canvas.drawLine(x, treeBase - height * 0.10f, x, treeBase, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, treeBase - height * 0.12f, height * 0.045f, paint)
        canvas.drawCircle(x - height * 0.027f, treeBase - height * 0.085f, height * 0.035f, paint)
        canvas.drawCircle(x + height * 0.029f, treeBase - height * 0.088f, height * 0.033f, paint)
    }
    return bitmap
}

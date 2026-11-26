package com.example.camui

import android.view.HapticFeedbackConstants

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln


@Composable
internal fun FujiCameraKey(
    label: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    iconColor: Color? = null,
    horizontalContent: Boolean = false,
    selected: Boolean = false,
    secondary: String? = null,
    scale: Float = 1f,
    onClick: () -> Unit
) {
    val safeScale = scale.coerceIn(Tuning.CAMERA_SCALE_MIN, Tuning.CAMERA_SCALE_MAX)
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) Tuning.BUTTON_PRESS_SCALE else 1f, tween(Tuning.BUTTON_PRESS_MS), label = "fuji-key-press")
    Column(
        modifier.graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .shadow(Tuning.CAMERA_BUTTON_SHADOW_DP.dp * safeScale, RoundedCornerShape(Tuning.CAMERA_BUTTON_RADIUS_DP.dp * safeScale))
            .clip(RoundedCornerShape(Tuning.CAMERA_BUTTON_RADIUS_DP.dp * safeScale))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2C2E), Color(0xFF1A1B1D))))
            .border(1.dp, if (selected) FujiCameraDesign.orange else FujiCameraDesign.stroke, RoundedCornerShape(Tuning.CAMERA_BUTTON_RADIUS_DP.dp * safeScale))
            .drawWithContent {
                drawContent()
                if (selected) drawCircle(FujiCameraDesign.orange, 3.dp.toPx(), Offset(size.width - 7.dp.toPx(), 7.dp.toPx()))
            }
            .clickable(interactionSource = interaction, indication = null) { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); onClick() }
            .padding(horizontal = 6.dp * safeScale, vertical = 5.dp * safeScale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(Modifier.fillMaxWidth().height(1.dp * safeScale)) { drawLine(Color.White.copy(alpha = 0.10f), androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx()) }
        if (horizontalContent && icon != null) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp * safeScale)
            ) {
                FujiIconGlyph(icon, Modifier.size(Tuning.CAMERA_BUTTON_ICON_DP.dp * safeScale),
                    iconColor ?: if (selected) FujiCameraDesign.orange else FujiCameraDesign.text)
                Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center) {
                    Text(label, color = FujiCameraDesign.text, fontSize = Tuning.CAMERA_BUTTON_LABEL_SP.sp * safeScale,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (secondary != null) Text(secondary, color = if (selected) FujiCameraDesign.orange else FujiCameraDesign.muted,
                        fontSize = Tuning.CAMERA_BUTTON_SECONDARY_SP.sp * safeScale, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            if (icon != null) FujiIconGlyph(icon, Modifier.size(Tuning.CAMERA_BUTTON_ICON_DP.dp * safeScale),
                iconColor ?: if (selected) FujiCameraDesign.orange else FujiCameraDesign.text)
            Text(label, color = FujiCameraDesign.text, fontSize = Tuning.CAMERA_BUTTON_LABEL_SP.sp * safeScale, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (secondary != null) Text(secondary, color = if (selected) FujiCameraDesign.orange else FujiCameraDesign.muted,
                fontSize = Tuning.CAMERA_BUTTON_SECONDARY_SP.sp * safeScale, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun FujiRoundTopKey(icon: String, selected: Boolean, diameter: Dp = 44.dp, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) Tuning.BUTTON_PRESS_SCALE else 1f, tween(Tuning.BUTTON_PRESS_MS), label = "top-key-press")
    Box(
        Modifier.size(diameter).graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFFE3E5E6), Color(0xFF777D81), Color(0xFF34383B))))
            .border(1.dp, if (selected) FujiCameraDesign.orange else Color(0xFFC7CBCE), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { FujiIconGlyph(icon, Modifier.size(diameter * 0.52f), if (selected) FujiCameraDesign.orange else Color.White) }
}


@Composable
internal fun FujiEvRuler(
    position: Float,
    minimum: Int,
    maximum: Int,
    heightDp: Dp = 24.dp,
    onPositionChange: (Int) -> Unit
) {
    val safeMin = minimum.coerceAtMost(maximum)
    val safeMax = maximum.coerceAtLeast(safeMin)
    Box(
        Modifier.fillMaxWidth().height(heightDp)
            .pointerInput(safeMin, safeMax) {
                detectTapGestures { point ->
                    val fraction = (point.x / size.width).coerceIn(0f, 1f)
                    onPositionChange((safeMin + fraction * (safeMax - safeMin)).toInt())
                }
            }
            .pointerInput(safeMin, safeMax) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onPositionChange((safeMin + fraction * (safeMax - safeMin)).toInt())
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * 0.72f
            drawLine(Color(0xFF777D81), androidx.compose.ui.geometry.Offset(0f, y),
                androidx.compose.ui.geometry.Offset(size.width, y), 1.dp.toPx())
            for (i in 0..32) {
                val x = size.width * i / 32f
                val major = i % 4 == 0
                drawLine(Color.White.copy(alpha = if (major) 0.95f else 0.45f),
                    androidx.compose.ui.geometry.Offset(x, if (major) 1.dp.toPx() else 6.dp.toPx()),
                    androidx.compose.ui.geometry.Offset(x, y),
                    if (major) 1.2.dp.toPx() else 0.7.dp.toPx())
            }
            val cursorX = size.width * position.coerceIn(0f, 1f)
            drawLine(Color.White, androidx.compose.ui.geometry.Offset(cursorX, 0f),
                androidx.compose.ui.geometry.Offset(cursorX, size.height), 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
internal fun FujiProFocusStrip(
    focusPosition: Float,
    scale: Float = 1f,
    automatic: Boolean,
    touchEnabled: Boolean,
    onFocusPosition: (Float) -> Unit,
    onAuto: () -> Unit,
    onTouchToggle: () -> Unit
) {
    val safeScale = scale.coerceIn(Tuning.CAMERA_SCALE_MIN, Tuning.CAMERA_SCALE_MAX)
    Row(
        Modifier.fillMaxWidth().height(36.dp * safeScale).clip(RoundedCornerShape(10.dp))
            .background(Color(0x70000000)).padding(horizontal = 8.dp * safeScale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp * safeScale)
    ) {
        Text(stringResource(R.string.camera_focus), color = Color.White, fontSize = 12.sp * safeScale, fontWeight = FontWeight.Bold)
        androidx.compose.material3.Slider(
            value = focusPosition.coerceIn(0f, 1f),
            onValueChange = onFocusPosition,
            modifier = Modifier.fillMaxWidth(0.40f).height(28.dp * safeScale),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = FujiCameraDesign.orange,
                inactiveTrackColor = Color(0xFF424649)
            )
        )
        Box(
            Modifier.height(28.dp * safeScale).clip(RoundedCornerShape(14.dp * safeScale))
                .background(if (automatic) FujiCameraDesign.orange else Color(0xFF35393B))
                .clickable(onClick = onAuto).padding(horizontal = 12.dp * safeScale),
            contentAlignment = Alignment.Center
        ) { Text(stringResource(R.string.camera_auto), color = if (automatic) Color.Black else Color.White, fontSize = 11.sp * safeScale, fontWeight = FontWeight.Bold) }
        Box(
            Modifier.size(32.dp * safeScale).clip(CircleShape)
                .background(if (touchEnabled) FujiCameraDesign.orange else Color(0xFF35393B))
                .clickable(onClick = onTouchToggle),
            contentAlignment = Alignment.Center
        ) { FujiIconGlyph("af", Modifier.size(18.dp * safeScale), Color.White) }
    }
}

internal fun formatCameraClock(hour: Int, minute: Int, second: Int): String =
    String.format(
        Locale.US,
        "%02dh%02dm%02ds",
        hour.coerceIn(0, 23),
        minute.coerceIn(0, 59),
        second.coerceIn(0, 59)
    )

@Composable
internal fun FujiProHud(
    shutter: String, aperture: String, focus: String, iso: String, kelvin: String, ev: String, batteryPercent: Int, hudHeight: Dp = 60.dp, scale: Float = 1f
) {
    val safeScale = scale.coerceIn(Tuning.CAMERA_SCALE_MIN, Tuning.CAMERA_SCALE_MAX)
    var clock by remember { mutableStateOf("--h--m--s") }
    LaunchedEffect(Unit) {
        val calendar = Calendar.getInstance()
        while (true) {
            calendar.timeInMillis = System.currentTimeMillis()
            clock = formatCameraClock(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
            )
            delay(1_000L)
        }
    }
    Row(
        Modifier.fillMaxWidth(Tuning.CAMERA_HUD_WIDTH_FRACTION).height(hudHeight).clip(RoundedCornerShape(16.dp))
            .background(Color(0x80000000)).padding(horizontal = 14.dp * safeScale, vertical = 5.dp * safeScale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(0.20f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SS" + shutter.removePrefix("1/"), color = FujiCameraDesign.blue, fontSize = 22.sp * safeScale, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Column(Modifier.weight(0.22f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp * safeScale)) {
                Text(aperture, color = FujiCameraDesign.blue, fontSize = 18.sp * safeScale, maxLines = 1)
                Text(focus, color = Color.White, fontSize = 11.sp * safeScale, maxLines = 1)
            }
            Text("ISO : $iso", color = Color.White, fontSize = 18.sp * safeScale, maxLines = 1)
        }
        Column(Modifier.weight(0.20f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(kelvin, color = FujiCameraDesign.blue, fontSize = 22.sp * safeScale, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Column(Modifier.weight(0.16f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("EV", color = Color.White, fontSize = 14.sp * safeScale)
            Text(ev, color = Color.White, fontSize = 24.sp * safeScale, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Column(Modifier.weight(0.22f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(20.dp * safeScale, 12.dp * safeScale)) {
                    drawRoundRect(Color.White, cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = Stroke(1.dp.toPx()))
                    drawRect(Color.White, size = androidx.compose.ui.geometry.Size(size.width * batteryPercent.coerceIn(0, 100) / 100f, size.height))
                    drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.3f),
                        size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height * 0.4f))
                }
                Text(" $batteryPercent", color = Color.White, fontSize = 13.sp * safeScale, maxLines = 1)
            }
            Text(clock, color = Color.White, fontSize = 12.sp * safeScale, maxLines = 1)
        }
    }
}

@Composable
internal fun FujiPhotoStatus(
    batteryPercent: Int, resolution: String, fps: Float?, targetFps: Float?, zoom: Float,
    minimumZoom: Float, maximumZoom: Float, onZoom: (Float) -> Unit, scale: Float = 1f
) {
    val safeScale = scale.coerceIn(Tuning.CAMERA_SCALE_MIN, Tuning.CAMERA_SCALE_MAX)
    var dragging by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf("--h--m--s") }
    LaunchedEffect(Unit) {
        val calendar = Calendar.getInstance()
        while (true) {
            calendar.timeInMillis = System.currentTimeMillis()
            clock = formatCameraClock(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
            )
            delay(1_000L)
        }
    }
    val minZoom = minimumZoom.takeIf { it > 0f } ?: 1f
    val maxZoom = maximumZoom.coerceAtLeast(minZoom)
    val fraction = if (maxZoom <= minZoom) 0f else
        (ln(zoom.coerceIn(minZoom, maxZoom) / minZoom) / ln(maxZoom / minZoom)).coerceIn(0f, 1f)
    val actualFps = fps?.takeIf { it.isFinite() && it > 0f }
    val lowFps = actualFps != null && targetFps != null && actualFps < targetFps * 0.8f
    Column(Modifier.fillMaxSize().padding(10.dp * safeScale)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(56.dp * safeScale, 26.dp * safeScale)) {
                        drawRoundRect(Color.White, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                        drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.32f),
                            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height * 0.36f))
                        val fillWidth = (size.width - 8.dp.toPx()) * batteryPercent.coerceIn(0, 100) / 100f
                        drawRoundRect(Color(0xFF777777), topLeft = androidx.compose.ui.geometry.Offset(4.dp.toPx(), 4.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(fillWidth, size.height - 8.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
                    }
                    Text("$batteryPercent%", color = Color.White, fontSize = 13.sp * safeScale, modifier = Modifier.padding(start = 5.dp * safeScale))
                }
                Text(clock, color = Color.White, fontSize = 12.sp * safeScale)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp * safeScale)) {
                    Text("W", color = Color.White, fontSize = 12.sp * safeScale)
                    Box(
                        Modifier.padding(horizontal = 5.dp * safeScale).width(72.dp * safeScale).height(24.dp * safeScale).clip(CircleShape)
                            .border(2.5.dp * safeScale, Color(0xFFCFCFCF), CircleShape)
                            .pointerInput(minZoom, maxZoom) {
                                detectTapGestures { point ->
                                    val f = (point.x / size.width).coerceIn(0f, 1f)
                                    onZoom(minZoom * exp(ln(maxZoom / minZoom) * f))
                                }
                            }
                            .pointerInput(minZoom, maxZoom) {
                                detectDragGestures(
                                    onDragStart = { dragging = true }, onDragEnd = { dragging = false },
                                    onDragCancel = { dragging = false },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val f = (change.position.x / size.width).coerceIn(0f, 1f)
                                        onZoom(minZoom * exp(ln(maxZoom / minZoom) * f))
                                    }
                                )
                            }
                    ) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(0.28f).align(Alignment.CenterStart)
                            .offset(x = 52.dp * safeScale * fraction).clip(RoundedCornerShape(4.dp)).background(Color.White))
                    }
                    Text("T", color = Color.White, fontSize = 12.sp * safeScale)
                }
                if (dragging) Text("%.1fx".format(Locale.US, zoom), color = FujiCameraDesign.orange, fontSize = 11.sp * safeScale)
            }
            Text(
                "$resolution " + (actualFps?.let { String.format(Locale.US, "%.2f", it) } ?: "--"),
                color = if (lowFps) FujiCameraDesign.orange else Color.White, fontSize = 13.sp * safeScale, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp * safeScale)).background(Color(0x70000000)).padding(horizontal = 8.dp * safeScale, vertical = 4.dp * safeScale)
            )
        }
    }
}

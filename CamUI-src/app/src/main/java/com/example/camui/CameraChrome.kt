package com.example.camui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln

internal object FujiCameraDesign {
    val background = Color(0xFF0E0F10)
    val surface = Color(0xFF1C1E20)
    val elevated = Color(0xFF2A2C2E)
    val stroke = Color(0xFF5A5D60)
    val text = Color(0xFFF2F2F2)
    val muted = Color(0xFF9A9DA1)
    val blue = Color(0xFF3B8EE6)
    val orange = Color(0xFFF0A030)
    val red = Color(0xFFD8283C)
    val green = Color(0xFF5ED08A)
    fun scale(widthDp: Float, heightDp: Float): Float =
        minOf(widthDp / 924f, heightDp / 427f).coerceIn(0.85f, 1.25f)
}

@Composable
internal fun FujiCameraKey(
    label: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    selected: Boolean = false,
    secondary: String? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(80), label = "fuji-key-press")
    Column(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2C2E), Color(0xFF1A1B1D))))
            .border(1.dp, if (selected) FujiCameraDesign.orange else FujiCameraDesign.stroke, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) FujiIconGlyph(icon, Modifier.size(22.dp),
            if (selected) FujiCameraDesign.orange else FujiCameraDesign.text)
        Text(label, color = FujiCameraDesign.text, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (secondary != null) Text(secondary, color = if (selected) FujiCameraDesign.orange else FujiCameraDesign.muted,
            fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun FujiRoundTopKey(icon: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(80), label = "top-key-press")
    Box(
        Modifier.size(44.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFFE3E5E6), Color(0xFF777D81), Color(0xFF34383B))))
            .border(1.dp, if (selected) FujiCameraDesign.orange else Color(0xFFC7CBCE), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { FujiIconGlyph(icon, Modifier.size(23.dp), if (selected) FujiCameraDesign.orange else Color.White) }
}

@Composable
internal fun FujiProHud(
    shutter: String, aperture: String, focus: String, iso: String, kelvin: String, ev: String, batteryPercent: Int
) {
    var clock by remember { mutableStateOf("--:--:--") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) { clock = formatter.format(Date()); delay(1_000L) }
    }
    Row(
        Modifier.fillMaxWidth(0.58f).height(60.dp).clip(RoundedCornerShape(16.dp))
            .background(Color(0x80000000)).padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(0.20f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SS$shutter", color = FujiCameraDesign.blue, fontSize = 19.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Column(Modifier.weight(0.22f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$aperture  $focus", color = FujiCameraDesign.blue, fontSize = 15.sp, maxLines = 1)
            Text("ISO : $iso", color = Color.White, fontSize = 13.sp, maxLines = 1)
        }
        Column(Modifier.weight(0.20f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(kelvin, color = FujiCameraDesign.blue, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Column(Modifier.weight(0.16f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("EV", color = Color.White, fontSize = 11.sp)
            Text(ev, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Column(Modifier.weight(0.22f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(17.dp, 10.dp)) {
                    drawRoundRect(Color.White, cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = Stroke(1.dp.toPx()))
                    drawRect(Color.White, size = androidx.compose.ui.geometry.Size(size.width * batteryPercent.coerceIn(0, 100) / 100f, size.height))
                    drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.3f),
                        size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height * 0.4f))
                }
                Text(" $batteryPercent", color = Color.White, fontSize = 11.sp, maxLines = 1)
            }
            Text(clock, color = Color.White, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun FujiPhotoStatus(
    batteryPercent: Int, resolution: String, fps: Float?, targetFps: Float?, zoom: Float,
    minimumZoom: Float, maximumZoom: Float, onZoom: (Float) -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf("--:--:--") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) { clock = formatter.format(Date()); delay(1_000L) }
    }
    val minZoom = minimumZoom.takeIf { it > 0f } ?: 1f
    val maxZoom = maximumZoom.coerceAtLeast(minZoom)
    val fraction = if (maxZoom <= minZoom) 0f else
        (ln(zoom.coerceIn(minZoom, maxZoom) / minZoom) / ln(maxZoom / minZoom)).coerceIn(0f, 1f)
    val actualFps = fps?.takeIf { it.isFinite() && it > 0f }
    val lowFps = actualFps != null && targetFps != null && actualFps < targetFps * 0.8f
    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(56.dp, 26.dp)) {
                        drawRoundRect(Color.White, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                        drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.32f),
                            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height * 0.36f))
                        val fillWidth = (size.width - 8.dp.toPx()) * batteryPercent.coerceIn(0, 100) / 100f
                        drawRoundRect(Color(0xFF777777), topLeft = androidx.compose.ui.geometry.Offset(4.dp.toPx(), 4.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(fillWidth, size.height - 8.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
                    }
                    Text("$batteryPercent%", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(start = 5.dp))
                }
                Text(clock, color = Color.White, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                    Text("W", color = Color.White, fontSize = 12.sp)
                    Box(
                        Modifier.padding(horizontal = 5.dp).width(72.dp).height(24.dp).clip(CircleShape)
                            .border(2.5.dp, Color(0xFFCFCFCF), CircleShape)
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
                            .offset(x = (72.dp * fraction) - 10.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
                    }
                    Text("T", color = Color.White, fontSize = 12.sp)
                }
                if (dragging) Text("%.1fx".format(Locale.US, zoom), color = FujiCameraDesign.orange, fontSize = 11.sp)
            }
            Text(
                "$resolution " + (actualFps?.let { String.format(Locale.US, "%.2f", it) } ?: "--"),
                color = if (lowFps) FujiCameraDesign.orange else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x70000000)).padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

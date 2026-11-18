package com.example.camui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

object FujiSkin {
    val body = Color(0xFF1A1A1A)
    val bodyDeep = Color(0xFF101010)
    val silver = Color(0xFFD9DBDD)
    val ivory = Color(0xFFF2F2EE)
    val lcd = Color(0xFF0B0D0D)
    val lcdGreen = Color(0xFF9EE6B0)
    val orange = Color(0xFFE8442B)
    val line = Color(0xFF657078)
    val muted = Color(0xFF899397)
}

/** Procedural leatherette surface; no bitmap allocation and no capture-pipeline involvement. */
@Composable
fun FujiLeatherSurface(
    modifier: Modifier = Modifier,
    simpleMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF292929), FujiSkin.body, FujiSkin.bodyDeep)
                )
            )
    ) {
        if (!simpleMode) {
            Canvas(Modifier.fillMaxSize()) {
                val step = 8.dp.toPx()
                var y = 2.dp.toPx()
                while (y < size.height) {
                    var x = 2.dp.toPx()
                    while (x < size.width) {
                        val shade = if (((x / step).toInt() + (y / step).toInt()) % 2 == 0) {
                            Color.White.copy(alpha = 0.025f)
                        } else {
                            Color.Black.copy(alpha = 0.09f)
                        }
                        drawCircle(shade, radius = 1.05.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
        }
        content()
    }
}

@Composable
fun FujiMetalTopPlate(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8F8F4), FujiSkin.silver, Color(0xFF9FA4A8), Color(0xFFE6E7E5))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.68f), RoundedCornerShape(12.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val spacing = 3.dp.toPx()
            var y = 2.dp.toPx()
            while (y < size.height) {
                drawLine(
                    Color.White.copy(alpha = 0.055f),
                    androidx.compose.ui.geometry.Offset(0f, y),
                    androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 0.45.dp.toPx()
                )
                y += spacing
            }
        }
        content()
        Row(
            Modifier.align(Alignment.CenterStart).padding(start = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) { repeat(2) { FujiScrew() } }
        Row(
            Modifier.align(Alignment.CenterEnd).padding(end = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) { repeat(2) { FujiScrew() } }
    }
}

@Composable
private fun FujiScrew() {
    Box(
        Modifier.size(5.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFFAFAF8), Color(0xFF74797C))))
            .border(0.5.dp, Color(0xFF666B6E), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.width(3.dp).height(0.6.dp).background(Color(0xFF55595B)))
    }
}

@Composable
fun FujiTopKey(
    label: String,
    icon: ImageVector,
    selected: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    val pressedOffset by animateDpAsState(if (selected) 1.dp else 0.dp, tween(90), label = "fuji-key")
    Column(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .combinedClickable(
                onClick = {
                    if (enabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onClick()
                    }
                },
                onLongClick = if (enabled) onLongClick else null
            )
            .padding(horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .offset(y = pressedOffset)
                .size(31.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFD6D8D8), Color(0xFF9DA2A5))
                    )
                )
                .border(1.dp, Color(0xFF777D80), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (enabled) Color(0xFF16191A) else Color(0xFF777777),
                modifier = Modifier.size(17.dp)
            )
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(2.dp).size(5.dp)
                        .clip(CircleShape).background(FujiSkin.orange)
                )
            }
        }
        MaterialText(
            label.uppercase(),
            color = Color(0xFF202426),
            fontSize = 7.sp,
            lineHeight = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.65.sp,
            maxLines = 1
        )
    }
}

@Composable
fun FujiPanel(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(FujiSkin.body)
            .border(1.dp, Color(0xFF9CA2A4), RoundedCornerShape(10.dp))
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(FujiSkin.ivory, FujiSkin.silver, Color(0xFFADB2B4))))
                .padding(start = 12.dp, end = 7.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialText(
                title.uppercase(),
                modifier = Modifier.weight(1f),
                color = Color(0xFF171A1B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            if (onBack != null) {
                MaterialText(
                    "DISP/BACK",
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2B2F30))
                        .combinedClickable(
                            onClick = onBack,
                            onLongClick = null
                        )
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    color = FujiSkin.ivory,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().background(FujiSkin.lcd).padding(10.dp),
            content = content
        )
    }
}

@Composable
fun FujiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val view = LocalView.current
    val knobX by animateDpAsState(if (checked) 22.dp else 2.dp, tween(150), label = "fuji-switch")
    Row(
        modifier = modifier.combinedClickable(
            enabled = enabled,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onCheckedChange(!checked)
            },
            onLongClick = null
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier.size(width = 44.dp, height = 24.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF34393B), Color(0xFF0C0D0E))))
                .border(1.dp, Color(0xFF737B7E), RoundedCornerShape(5.dp))
        ) {
            Box(
                Modifier.offset(x = knobX, y = 2.dp).size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(listOf(FujiSkin.ivory, FujiSkin.silver, Color(0xFF82898C))))
                    .border(0.6.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            )
        }
        Box(
            Modifier.size(6.dp).clip(CircleShape)
                .background(if (checked) FujiSkin.orange else Color(0xFF34383A))
        )
    }
}

@Composable
fun FujiChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Column(
        modifier.clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF383D3F), Color(0xFF181B1C))))
            .border(1.dp, if (selected) FujiSkin.orange else Color(0xFF4A5255), RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                },
                onLongClick = null
            )
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MaterialText(text, color = if (selected) FujiSkin.lcdGreen else Color(0xFFE2E4E3), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        if (selected) Spacer(Modifier.padding(top = 2.dp).width(12.dp).height(2.dp).background(FujiSkin.orange))
    }
}

@Composable
fun FujiTile(
    title: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.height(72.dp).clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF101516))
            .border(1.dp, if (selected) FujiSkin.orange else Color(0xFF354247), RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = null)
            .padding(9.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = FujiSkin.lcdGreen, modifier = Modifier.size(16.dp))
        MaterialText(title.uppercase(), color = Color(0xFFC7D0D1), fontSize = 8.sp, letterSpacing = 0.7.sp, maxLines = 1)
        MaterialText(value, color = FujiSkin.lcdGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun FujiListRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Column(
        modifier.fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = null)
            .padding(vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MaterialText(title, color = Color(0xFFE8EBE9), fontSize = 11.sp, modifier = Modifier.weight(1f))
            MaterialText(value, color = FujiSkin.lcdGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            MaterialText("  >", color = FujiSkin.lcdGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (description != null) MaterialText(description, color = FujiSkin.muted, fontSize = 8.sp, modifier = Modifier.padding(top = 3.dp))
        Box(Modifier.fillMaxWidth().padding(top = 7.dp).height(0.6.dp).background(Color(0xFF2A383B)))
    }
}

@Composable
fun FujiSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Tìm..."
) {
    Row(
        modifier.clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF060909))
            .border(1.dp, Color(0xFF3D5155), RoundedCornerShape(3.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MaterialText("⌕", color = FujiSkin.lcdGreen, fontSize = 15.sp)
        Spacer(Modifier.width(7.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) MaterialText(placeholder, color = FujiSkin.muted, fontSize = 10.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = FujiSkin.lcdGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            )
        }
        MaterialText("▏", color = FujiSkin.lcdGreen, fontSize = 11.sp)
    }
}

@Composable
fun FujiDial(
    value: String,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
    stepSize: Float = 22f
) {
    var rotation by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(rotation, tween(90), label = "fuji-dial-rotation")
    val view = LocalView.current
    Box(
        modifier.size(70.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFE2E4E2), Color(0xFF858C8F), Color(0xFF252A2C))))
            .border(4.dp, Color(0xFF282D2F), CircleShape)
            .pointerInput(stepSize) {
                var accumulated = 0f
                detectDragGestures { change, drag ->
                    change.consume()
                    accumulated += (drag.x - drag.y) / stepSize
                    val steps = accumulated.toInt()
                    if (steps != 0) {
                        accumulated -= steps
                        rotation += steps * 12f
                        onStep(steps)
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(5.dp)) {
            val radius = size.minDimension / 2f
            for (i in 0 until 36) {
                val angle = i * (Math.PI * 2.0 / 36.0)
                val inner = radius * 0.83f
                val outer = if (i % 3 == 0) radius * 0.99f else radius * 0.92f
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawLine(
                    Color(0xFF33393B),
                    androidx.compose.ui.geometry.Offset(cx + kotlin.math.cos(angle).toFloat() * inner, cy + kotlin.math.sin(angle).toFloat() * inner),
                    androidx.compose.ui.geometry.Offset(cx + kotlin.math.cos(angle).toFloat() * outer, cy + kotlin.math.sin(angle).toFloat() * outer),
                    strokeWidth = if (i % 3 == 0) 1.6.dp.toPx() else 0.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(Color(0xFF202527), radius * 0.68f, style = Stroke(width = 1.dp.toPx()))
        }
        Box(
            Modifier.size(47.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFF5F5F1), Color(0xFFB9BFC1), Color(0xFF7C8589))))
                .border(1.dp, Color(0xFF697174), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialText(value, color = Color(0xFF1A2022), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
        }
        Box(Modifier.align(Alignment.TopCenter).padding(top = 3.dp).size(3.dp).clip(CircleShape).background(FujiSkin.orange))
    }
}

@Composable
fun FujiScrollbar(
    fraction: Float,
    visibleFraction: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.width(3.dp)) {
        drawRoundRect(Color(0xFF334044), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2))
        val indicatorHeight = (size.height * visibleFraction.coerceIn(0.08f, 1f)).coerceAtLeast(8.dp.toPx())
        val top = (size.height - indicatorHeight) * fraction.coerceIn(0f, 1f)
        drawRoundRect(
            FujiSkin.lcdGreen,
            topLeft = androidx.compose.ui.geometry.Offset(0f, top),
            size = androidx.compose.ui.geometry.Size(size.width, indicatorHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2)
        )
    }
}


/** Two-stage mechanical shutter: half-press focuses; release triggers the existing capture action. */
@Composable
fun FujiShutterButton(
    enabled: Boolean,
    onHalfPress: () -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(90), label = "fuji-shutter")
    Box(
        modifier
            .size(76.dp)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF52585B), Color(0xFF111516))))
            .border(2.dp, Color(0xFF060808), CircleShape)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            pressed = true
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onHalfPress()
                            tryAwaitRelease()
                            pressed = false
                        }
                    },
                    onTap = {
                        if (enabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onCapture()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(62.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD6D8D8), Color(0xFF737B7F))))
                .border(2.dp, Color(0xFF222729), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(50.dp).clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFEEF0EE), Color(0xFF9DA4A7), Color(0xFF6F777A))))
                    .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    }
}


/** LCD-style fader with etched tick marks; replaces the default Material slider. */
@Composable
fun FujiSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = modifier.height(26.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
        val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
        val thumbX = (maxWidth * fraction).coerceIn(7.dp, (maxWidth - 7.dp).coerceAtLeast(7.dp))
        Canvas(Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val endX = size.width
            val currentX = endX * fraction
            drawLine(
                Color(0xFF344145),
                androidx.compose.ui.geometry.Offset(0f, centerY),
                androidx.compose.ui.geometry.Offset(endX, centerY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                FujiSkin.lcdGreen.copy(alpha = if (enabled) 0.88f else 0.35f),
                androidx.compose.ui.geometry.Offset(0f, centerY),
                androidx.compose.ui.geometry.Offset(currentX, centerY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            val tickCount = (steps + 1).coerceIn(1, 60)
            if (steps > 0) {
                for (i in 0..tickCount) {
                    val x = endX * i / tickCount
                    drawLine(
                        Color(0xFF778387),
                        androidx.compose.ui.geometry.Offset(x, centerY - 7.dp.toPx()),
                        androidx.compose.ui.geometry.Offset(x, centerY + 7.dp.toPx()),
                        strokeWidth = 0.7.dp.toPx()
                    )
                }
            }
        }
        Box(
            Modifier.offset(x = thumbX - 7.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFFFF), FujiSkin.silver, Color(0xFF8A9295))
                    )
                )
                .border(1.dp, if (enabled) FujiSkin.orange else Color(0xFF667075), CircleShape)
        )
        if (enabled) {
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(valueRange, steps) {
                        detectTapGestures { position ->
                            val raw = (position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val snapped = if (steps > 0) (raw * (steps + 1)).roundToInt().toFloat() / (steps + 1) else raw
                            onValueChange(valueRange.start + span * snapped)
                            onValueChangeFinished?.invoke()
                        }
                    }
                    .pointerInput(valueRange, steps) {
                        detectDragGestures(
                            onDragStart = { position ->
                                val raw = (position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                val snapped = if (steps > 0) (raw * (steps + 1)).roundToInt().toFloat() / (steps + 1) else raw
                                onValueChange(valueRange.start + span * snapped)
                            },
                            onDragEnd = { onValueChangeFinished?.invoke() },
                            onDragCancel = { onValueChangeFinished?.invoke() }
                        ) { change, _ ->
                            val raw = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val snapped = if (steps > 0) (raw * (steps + 1)).roundToInt().toFloat() / (steps + 1) else raw
                            onValueChange(valueRange.start + span * snapped)
                            change.consume()
                        }
                    }
            )
        }
    }
}

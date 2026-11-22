package com.example.camui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Fujifilm-style fixed-index ruler. The pointer remains at the center while
 * the scale translates beneath it. It deliberately does not use Material Slider.
 */
@Composable
internal fun FujiScaleRuler(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val view = LocalView.current
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fractionsPerStep = if (steps > 0) (steps + 1).toFloat() else 20f
    val currentFraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val latestFraction = rememberUpdatedState(currentFraction)
    val tickCount = (fractionsPerStep.roundToInt()).coerceIn(4, 120)
    val panelShape = RoundedCornerShape(6.dp)

    Row(
        modifier = modifier.height(56.dp)
            .clip(panelShape)
            .background(Color(0xFF0B0B0B))
            .border(1.dp, Color(0xFF414B4F), panelShape)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(width = 25.dp, height = 44.dp)
                .clip(RoundedCornerShape(3.dp))
                .clickable(enabled = enabled) {
                    val delta = if (steps > 0) span / (steps + 1) else span / 20f
                    onValueChange((value - delta).coerceIn(valueRange))
                    onValueChangeFinished?.invoke()
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text("−", color = Color(0xFFE6E8E7), fontSize = 14.sp)
        }
        Canvas(
            Modifier.weight(1f).fillMaxHeight()
                .pointerInput(valueRange, steps, enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragEnd = { onValueChangeFinished?.invoke() },
                        onDragCancel = { onValueChangeFinished?.invoke() }
                    ) { change, dragAmount ->
                        change.consume()
                        val sensitivity = if (steps > 0) (steps + 1).toFloat() else 20f
                        val deltaFraction = -dragAmount.x / size.width.coerceAtLeast(1) * (sensitivity / 5f)
                        val raw = (latestFraction.value + deltaFraction).coerceIn(0f, 1f)
                        val snapped = if (steps > 0) {
                            (raw * (steps + 1)).roundToInt().toFloat() / (steps + 1)
                        } else raw
                        val next = valueRange.start + span * snapped
                        if (abs(next - value) > span / (sensitivity * 8f)) {
                            onValueChange(next)
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                }

        ) {
            val centerY = size.height * 0.56f
            val centerX = size.width * 0.5f
            val rulerWidth = size.width * 1.7f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color(0xFFCFD5D5).toArgb()
                textAlign = Paint.Align.CENTER
                textSize = 9.dp.toPx()
                typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
            }
            for (index in 0..tickCount) {
                val fraction = index.toFloat() / tickCount
                val x = centerX + (fraction - currentFraction) * rulerWidth
                if (x < -8f || x > size.width + 8f) continue
                val major = if (tickCount > 20) index % ((tickCount / 4).coerceAtLeast(1)) == 0 else index % 5 == 0
                val tickHeight = if (major) size.height * 0.36f else size.height * 0.20f
                drawLine(
                    color = if (major) Color(0xFFBEC5C5) else Color(0xFF6B777B),
                    start = Offset(x, centerY - tickHeight * 0.5f),
                    end = Offset(x, centerY + tickHeight * 0.5f),
                    strokeWidth = if (major) 1.2.dp.toPx() else 0.7.dp.toPx()
                )
                if (major) {
                    val labelValue = valueRange.start + span * fraction
                    drawContext.canvas.nativeCanvas.drawText(
                        if (span <= 10f) String.format(java.util.Locale.US, "%.1f", labelValue) else labelValue.roundToInt().toString(),
                        x,
                        size.height - 1.dp.toPx(),
                        paint
                    )
                }
            }
            val pointer = Path().apply {
                moveTo(centerX - 5.dp.toPx(), 0f)
                lineTo(centerX + 5.dp.toPx(), 0f)
                lineTo(centerX, 7.dp.toPx())
                close()
            }
            drawPath(pointer, Color.White)
            drawLine(
                Color.White.copy(alpha = 0.78f),
                Offset(centerX, 7.dp.toPx()),
                Offset(centerX, centerY - 5.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
        Box(
            Modifier.size(width = 25.dp, height = 44.dp)
                .clip(RoundedCornerShape(3.dp))
                .clickable(enabled = enabled) {
                    val delta = if (steps > 0) span / (steps + 1) else span / 20f
                    onValueChange((value + delta).coerceIn(valueRange))
                    onValueChangeFinished?.invoke()
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text("+", color = Color(0xFFE6E8E7), fontSize = 16.sp)
        }
    }
}

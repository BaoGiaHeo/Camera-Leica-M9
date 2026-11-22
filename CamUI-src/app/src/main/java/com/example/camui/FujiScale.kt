package com.example.camui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.roundToInt

internal fun advanceFujiScalePosition(position: Float, deltaFraction: Float): Float {
    val current = position.takeIf { it.isFinite() } ?: 0.5f
    val delta = deltaFraction.takeIf { it.isFinite() } ?: 0f
    return (current + delta).coerceIn(0f, 1f)
}

/**
 * Pixel-style exposure/focus ruler. The centre cursor stays fixed while the
 * tick marks move beneath it; no Material Slider track or circular thumb is used.
 */
@Composable
internal fun FujiScale(
    label: String,
    valueText: String,
    position: Float,
    auto: Boolean,
    lowLabel: String,
    highLabel: String,
    stepFraction: Float,
    onPositionChange: (Float) -> Unit,
    onAuto: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val safePosition = position.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
    val safeStep = stepFraction.takeIf { it.isFinite() && it > 0f } ?: 0.01f
    val latestPosition = rememberUpdatedState(safePosition)
    val latestOnPositionChange = rememberUpdatedState(onPositionChange)
    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.LEFT
            textSize = 8f
            typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 3.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(21.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label.uppercase(),
                color = Color(0xFFF2F2F2),
                fontFamily = FujiTypography.uiFont,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.width(27.dp),
                maxLines = 1
            )
            Text(
                valueText,
                color = if (auto) Color(0xFFF2F2F2) else Color(0xFF4DA3FF),
                fontFamily = FujiTypography.uiFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            ScaleKey("-", onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPositionChange(advanceFujiScalePosition(safePosition, -safeStep))
            })
            ScaleKey("+", onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPositionChange(advanceFujiScalePosition(safePosition, safeStep))
            })
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = 20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (auto) Color(0xFF352411) else Color(0xFF15191A))
                    .border(1.dp, if (auto) Color(0xFFFFB300) else Color(0xFF596265), RoundedCornerShape(3.dp))
                    .clickable(onClick = onAuto),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "A",
                    color = if (auto) Color(0xFFFFB300) else Color(0xFFF2F2F2),
                    fontFamily = FujiTypography.uiFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            ScaleKey("BACK", onClick = onBack, wide = true)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(23.dp)
                .pointerInput(safeStep) {
                    var dragPosition = latestPosition.value
                    detectDragGestures(
                        onDragStart = { dragPosition = latestPosition.value },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val next = advanceFujiScalePosition(
                                dragPosition,
                                -dragAmount.x / size.width.coerceAtLeast(1).toFloat() * 0.72f
                            )
                            val oldDetent = (dragPosition / safeStep).roundToInt()
                            val newDetent = (next / safeStep).roundToInt()
                            dragPosition = next
                            latestOnPositionChange.value(next)
                            if (oldDetent != newDetent) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                }
        ) {
            Canvas(Modifier.fillMaxWidth().height(23.dp)) {
                val centerX = size.width * 0.5f
                val detents = (safePosition / safeStep).takeIf { it.isFinite() } ?: 0f
                val whole = kotlin.math.floor(detents).toInt()
                val remainder = detents - whole
                val stepPx = size.width / 24f
                val baseline = size.height * 0.68f
                for (index in -12..12) {
                    val x = centerX + (index - remainder) * stepPx
                    if (x < 0f || x > size.width) continue
                    val major = Math.floorMod(whole + index, 3) == 0
                    val tickTop = if (major) 2.dp.toPx() else 6.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = if (major) 0.95f else 0.42f),
                        start = Offset(x, tickTop),
                        end = Offset(x, baseline),
                        strokeWidth = if (major) 1.2.dp.toPx() else 0.7.dp.toPx()
                    )
                }
                // Physical end values stay below the ruler; tick indices are never
                // exposed as if they were EV, shutter or ISO values.
                val native = drawContext.canvas.nativeCanvas
                val baselineText = size.height - 0.5.dp.toPx()
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.textSize = 7.sp.toPx()
                native.drawText(lowLabel, 1.dp.toPx(), baselineText, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                native.drawText(highLabel, size.width - 1.dp.toPx(), baselineText, textPaint)

                val cursor = Path().apply {
                    moveTo(centerX - 5.dp.toPx(), 0.5.dp.toPx())
                    lineTo(centerX + 5.dp.toPx(), 0.5.dp.toPx())
                    lineTo(centerX, 6.dp.toPx())
                    close()
                }
                drawPath(cursor, Color.White)
                drawLine(
                    Color.White,
                    Offset(centerX, 6.dp.toPx()),
                    Offset(centerX, baseline),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun ScaleKey(label: String, onClick: () -> Unit, wide: Boolean = false) {
    Box(
        modifier = Modifier
            .size(width = if (wide) 42.dp else 22.dp, height = 20.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF15191A))
            .border(1.dp, Color(0xFF596265), RoundedCornerShape(3.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color(0xFFF2F2F2),
            fontFamily = FujiTypography.uiFont,
            fontWeight = FontWeight.Bold,
            fontSize = if (wide) 8.sp else 11.sp
        )
    }
}

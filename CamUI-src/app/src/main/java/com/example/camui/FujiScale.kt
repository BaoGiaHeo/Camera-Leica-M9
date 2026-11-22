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
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val safePosition = position.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
    val latestPosition = rememberUpdatedState(safePosition)
    val latestOnPositionChange = rememberUpdatedState(onPositionChange)
    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 9f
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0B0B), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF737B7D), RoundedCornerShape(5.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                label.uppercase(),
                color = Color(0xFFF2F2F2),
                fontFamily = FujiTypography.uiFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.width(34.dp),
                maxLines = 1
            )
            Text(
                valueText,
                color = Color(0xFF4DA3FF),
                fontFamily = FujiTypography.uiFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            ScaleKey("-", onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPositionChange(advanceFujiScalePosition(safePosition, -stepFraction))
            })
            ScaleKey("+", onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPositionChange(advanceFujiScalePosition(safePosition, stepFraction))
            })
            Box(
                modifier = Modifier
                    .size(width = 25.dp, height = 22.dp)
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
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(Unit) {
                    var dragPosition = latestPosition.value
                    detectDragGestures(
                        onDragStart = { dragPosition = latestPosition.value },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragPosition = advanceFujiScalePosition(
                                dragPosition,
                                -dragAmount.x / size.width.coerceAtLeast(1).toFloat() * 0.72f
                            )
                            latestOnPositionChange.value(dragPosition)
                        }
                    )
                }
        ) {
            Canvas(Modifier.fillMaxWidth().height(34.dp)) {
                val centerX = size.width * 0.5f
                val stepPx = size.width / 24f
                val tickPosition = safePosition * 100f
                val whole = floor(tickPosition).toInt()
                val remainder = tickPosition - whole
                val baseline = size.height * 0.66f
                for (index in -12..12) {
                    val x = centerX + (index - remainder) * stepPx
                    if (x < 0f || x > size.width) continue
                    val major = Math.floorMod(whole + index, 5) == 0
                    val tickTop = if (major) 4.dp.toPx() else 9.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = if (major) 0.95f else 0.48f),
                        start = Offset(x, tickTop),
                        end = Offset(x, baseline),
                        strokeWidth = if (major) 1.2.dp.toPx() else 0.7.dp.toPx()
                    )
                    if (major) {
                        drawContext.canvas.nativeCanvas.drawText(
                            ((whole + index) / 5).toString(),
                            x,
                            size.height - 1.dp.toPx(),
                            textPaint.apply { textSize = 8.sp.toPx() }
                        )
                    }
                }
                val cursor = Path().apply {
                    moveTo(centerX - 5.dp.toPx(), 1.dp.toPx())
                    lineTo(centerX + 5.dp.toPx(), 1.dp.toPx())
                    lineTo(centerX, 7.dp.toPx())
                    close()
                }
                drawPath(cursor, Color.White)
                drawLine(
                    Color.White,
                    Offset(centerX, 7.dp.toPx()),
                    Offset(centerX, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(lowLabel, color = Color(0xFF9CA7AA), fontFamily = FujiTypography.uiFont, fontSize = 8.sp)
            Text(highLabel, color = Color(0xFF9CA7AA), fontFamily = FujiTypography.uiFont, fontSize = 8.sp)
        }
    }
}

@Composable
private fun ScaleKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 22.dp)
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
            fontSize = 12.sp
        )
    }
}

package com.example.camui

import android.graphics.Paint
import android.os.SystemClock
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onPositionCommit: ((Float) -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val safePosition = position.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
    val safeStep = stepFraction.takeIf { it.isFinite() && it > 0f } ?: 0.01f
    val latestPosition = rememberUpdatedState(safePosition)
    val latestOnPositionChange = rememberUpdatedState(onPositionChange)
    val latestOnPositionCommit = rememberUpdatedState(onPositionCommit)
    val dragDrawPosition = remember { mutableFloatStateOf(safePosition) }
    val pendingPosition = remember { mutableFloatStateOf(safePosition) }
    val draggingState = remember { mutableStateOf(false) }
    var lastDispatchAtMs by remember { mutableLongStateOf(0L) }
    var dispatchJob by remember { mutableStateOf<Job?>(null) }
    val dispatchScope = rememberCoroutineScope()

    LaunchedEffect(safePosition) {
        if (!draggingState.value) {
            dragDrawPosition.floatValue = safePosition
            pendingPosition.floatValue = safePosition
        }
    }

    fun dispatchScalePosition(value: Float, force: Boolean = false) {
        val safeValue = value.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: safePosition
        dragDrawPosition.floatValue = safeValue
        pendingPosition.floatValue = safeValue
        val now = SystemClock.elapsedRealtime()
        val waitMs = (Tuning.CAMERA_SLIDER_COMMAND_INTERVAL_MS - (now - lastDispatchAtMs)).coerceAtLeast(0L)
        if (force || lastDispatchAtMs == 0L || waitMs == 0L) {
            dispatchJob?.cancel()
            dispatchJob = null
            lastDispatchAtMs = now
            latestOnPositionChange.value(safeValue)
        } else if (dispatchJob?.isActive != true) {
            dispatchJob = dispatchScope.launch {
                delay(waitMs)
                lastDispatchAtMs = SystemClock.elapsedRealtime()
                latestOnPositionChange.value(pendingPosition.floatValue)
                dispatchJob = null
            }
        }
    }

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
            .padding(horizontal = if (compact) 1.dp else 3.dp, vertical = if (compact) 0.dp else 1.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(if (compact) 22.dp else 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label.uppercase(),
                color = Color(0xFFF2F2F2),
                fontFamily = FujiTypography.uiFont,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 8.sp else 10.sp,
                modifier = Modifier.width(if (compact) 20.dp else 27.dp),
                maxLines = 1
            )
            Text(
                valueText,
                color = if (auto) Color(0xFFF2F2F2) else Color(0xFF4DA3FF),
                fontFamily = FujiTypography.uiFont,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 12.sp else 15.sp,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            if (!compact) {
                ScaleKey("-", onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    dispatchScalePosition(advanceFujiScalePosition(dragDrawPosition.floatValue, -safeStep), force = true)
                })
                ScaleKey("+", onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    dispatchScalePosition(advanceFujiScalePosition(dragDrawPosition.floatValue, safeStep), force = true)
                })
            }
        }

        Row(
            Modifier.fillMaxWidth().height(if (compact) 30.dp else 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f).height(if (compact) 30.dp else 48.dp)
                    .pointerInput(safeStep) {
                        var dragPosition = dragDrawPosition.floatValue
                        detectDragGestures(
                            onDragStart = {
                                draggingState.value = true
                                dragPosition = dragDrawPosition.floatValue
                            },
                            onDragEnd = {
                                dispatchScalePosition(pendingPosition.floatValue, force = true)
                                latestOnPositionCommit.value?.invoke(pendingPosition.floatValue)
                                draggingState.value = false
                            },
                            onDragCancel = {
                                dispatchScalePosition(pendingPosition.floatValue, force = true)
                                latestOnPositionCommit.value?.invoke(pendingPosition.floatValue)
                                draggingState.value = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val next = advanceFujiScalePosition(
                                    dragPosition,
                                    -dragAmount.x / size.width.coerceAtLeast(1).toFloat() * 0.72f
                                )
                                val oldDetent = (dragPosition / safeStep).roundToInt()
                                val newDetent = (next / safeStep).roundToInt()
                                dragPosition = next
                                dispatchScalePosition(next)
                                if (oldDetent != newDetent) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                    }
            ) {
                Canvas(Modifier.fillMaxWidth().height(if (compact) 18.dp else 23.dp).align(Alignment.Center)) {
                    val centerX = size.width * 0.5f
                    val detents = (dragDrawPosition.floatValue / safeStep).takeIf { it.isFinite() } ?: 0f
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
                    // End labels are actual camera values, not generic tick indices.
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
            // Auto is intentionally visible only while a Pro value is selected.
            Box(
                modifier = Modifier.width(if (compact) 30.dp else 38.dp).height(if (compact) 25.dp else 42.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (auto) Color(0xFF352411) else Color(0xFF15191A))
                    .border(
                        1.dp,
                        if (auto) Color(0xFFFFB300) else Color(0xFF596265),
                        RoundedCornerShape(5.dp)
                    )
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAuto()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "A",
                    color = if (auto) Color(0xFFFFB300) else Color(0xFFF2F2F2),
                    fontFamily = FujiTypography.uiFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 9.sp else 12.sp
                )
            }
        }
    }
}

@Composable
private fun ScaleKey(label: String, onClick: () -> Unit, wide: Boolean = false) {
    Box(
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(width = if (wide) 42.dp else 22.dp, height = 20.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF15191A))
                .border(1.dp, Color(0xFF596265), RoundedCornerShape(3.dp)),
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
}

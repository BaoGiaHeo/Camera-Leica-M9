package com.example.camui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.floor

internal enum class MultiFrameStage(val resourceId: Int) {
    CAPTURE(R.string.mf_stage_capture),
    ALIGN(R.string.mf_stage_align),
    MERGE(R.string.mf_stage_merge),
    TONE(R.string.mf_stage_tone),
    SAVE(R.string.mf_stage_save)
}

internal data class MultiFrameProgressUiState(
    val visible: Boolean,
    val modeLabel: String,
    val stage: MultiFrameStage,
    val progressPercent: Int,
    val capturedFrames: Int,
    val targetFrames: Int,
    val nightShot: Boolean,
    val queueCount: Int,
    val holdSteady: Boolean,
    val stabilityBars: Int,
    val elapsedSeconds: Int,
    val batteryTemperatureC: Int,
    val thermalStatus: Int,
    val errorCode: String? = null,
    val reducedMotion: Boolean = false,
    val simpleMode: Boolean = false
)

internal fun resolveMultiFrameStage(
    processingLabel: String,
    progressPercent: Int,
    capturing: Boolean
): MultiFrameStage {
    if (capturing) return MultiFrameStage.CAPTURE
    val label = processingLabel.lowercase(Locale.ROOT)
    return when {
        label.contains("watermark") || label.contains("save") ||
            label.contains("lưu") || label.contains("đóng watermark") -> MultiFrameStage.SAVE
        label.contains("render") || label.contains("tone") ||
            label.contains("phục hồi") || progressPercent >= 82 -> MultiFrameStage.TONE
        label.contains("ghép") || label.contains("gộp") || label.contains("merge") ||
            progressPercent in 28..81 -> MultiFrameStage.MERGE
        label.contains("align") || label.contains("căn chỉnh") ||
            label.contains("căn khung") -> MultiFrameStage.ALIGN
        else -> MultiFrameStage.CAPTURE
    }
}

internal fun multiFrameThermalColor(status: Int): Long = when {
    status >= 3 -> 0xFFFF4A3D
    status >= 1 -> 0xFFE5A13B
    else -> 0xFF26302F
}

internal fun multiFrameTemperatureColor(tempC: Int): Long = when {
    tempC > 43 -> 0xFFFF4A3D
    tempC >= 40 -> 0xFFE5A13B
    else -> 0xFF55D58A
}

internal fun multiFrameElapsedLabel(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", safe / 60, safe % 60)
}

@Composable
internal fun FujiMultiFrameProgressPill(
    state: MultiFrameProgressUiState,
    modifier: Modifier = Modifier,
    maxWidth: Dp,
    maxHeight: Dp
) {
    val context = LocalContext.current
    val fontFamily = FujiTypography.composeFamily(context)
    val pulseTransition = rememberInfiniteTransition(label = "multi-frame-current-segment")
    val currentSegmentAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420),
            repeatMode = RepeatMode.Reverse
        ),
        label = "multi-frame-current-segment-alpha"
    )
    val segmentCount = if (state.nightShot) 30 else 24
    val capturePercent = if (state.targetFrames > 0) {
        (state.capturedFrames.coerceAtLeast(0) * 100 / state.targetFrames).coerceIn(0, 100)
    } else 0
    val safePercent = if (state.stage == MultiFrameStage.CAPTURE) {
        maxOf(state.progressPercent, capturePercent).coerceIn(0, 100)
    } else {
        state.progressPercent.coerceIn(0, 100)
    }
    val filledSegments = floor(safePercent * segmentCount / 100f).toInt().coerceIn(0, segmentCount)
    val hasCurrentSegment = filledSegments < segmentCount && safePercent > 0
    val stageText = stringResource(state.stage.resourceId)
    val thermalLabel = when {
        state.thermalStatus >= 4 -> stringResource(R.string.mf_thermal_hot)
        state.thermalStatus in 1..3 -> stringResource(R.string.mf_thermal_warm)
        else -> ""
    }
    val temperatureText = if (state.batteryTemperatureC >= 0) {
        stringResource(R.string.mf_temperature_format, state.batteryTemperatureC)
    } else {
        "--°C"
    }

    AnimatedVisibility(
        visible = state.visible,
        modifier = modifier.width(maxWidth).heightIn(max = maxHeight),
        enter = if (state.reducedMotion) fadeIn(tween(1)) else {
            fadeIn(tween(200)) + scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(200)
            )
        },
        exit = fadeOut(tween(if (state.reducedMotion) 1 else 300))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xC70B0B0B))
                .border(1.dp, Color(0xFF3A4A4A), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.dp, Color(0xFFE8442B), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = state.modeLabel.uppercase(Locale.ROOT),
                        color = Color(0xFFE8442B),
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = stageText.uppercase(Locale.ROOT),
                    color = Color.White,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f)
                )
                if (state.queueCount > 0 && state.stage == MultiFrameStage.CAPTURE) {
                    Text(
                        text = "Q" + state.queueCount.coerceAtMost(9),
                        color = Color(0xFFB9C7C7),
                        fontFamily = fontFamily,
                        fontSize = 8.sp
                    )
                }
                Text(
                    text = safePercent.toString() + "%",
                    color = Color.White,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(segmentCount) { index ->
                    val segmentColor = when {
                        index < filledSegments -> Color(0xFFE8442B)
                        index == filledSegments && hasCurrentSegment -> Color.White.copy(
                            alpha = if (state.reducedMotion || state.simpleMode) 1f else currentSegmentAlpha
                        )
                        else -> Color(0xFF26302F)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(segmentColor)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.holdSteady) {
                        repeat(3) { index ->
                            val lit = index < state.stabilityBars.coerceIn(0, 3)
                            Box(
                                Modifier
                                    .width(2.dp)
                                    .height((4 + index * 2).dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(if (lit) Color(0xFF55D58A) else Color(0xFF53615F))
                            )
                        }
                        Text(
                            stringResource(R.string.mf_hold_steady),
                            color = Color(0xFFD7E1DF),
                            fontFamily = fontFamily,
                            fontSize = 7.sp,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            stringResource(R.string.mf_move_freely),
                            color = Color(0xFF55D58A),
                            fontFamily = fontFamily,
                            fontSize = 7.sp,
                            maxLines = 1
                        )
                    }
                }

                Text(
                    multiFrameElapsedLabel(state.elapsedSeconds),
                    color = Color(0xFFBAC6C4),
                    fontFamily = fontFamily,
                    fontSize = 7.sp
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "♨ " + temperatureText,
                    color = Color(multiFrameTemperatureColor(state.batteryTemperatureC)),
                    fontFamily = fontFamily,
                    fontSize = 7.sp,
                    maxLines = 1
                )
                if (thermalLabel.isNotEmpty()) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        thermalLabel.uppercase(Locale.ROOT),
                        color = Color(multiFrameThermalColor(state.thermalStatus)),
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.sp,
                        maxLines = 1
                    )
                }
                state.errorCode?.takeIf { it.isNotBlank() }?.let { code ->
                    Spacer(Modifier.width(3.dp))
                    Text(
                        code.take(8).uppercase(Locale.ROOT),
                        color = Color(0xFFFF4A3D),
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

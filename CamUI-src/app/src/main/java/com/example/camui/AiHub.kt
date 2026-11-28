package com.example.camui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale


@Composable
fun LunaDreamBottomSheet(
    onDismiss: () -> Unit,
    nightShotEnabled: Boolean,
    nightShotDurationSeconds: Int,
    onNightShotDurationChange: (Int) -> Unit,
    onNightShotToggle: (Boolean) -> Unit,
    selectedMode: String,
    targetHeightOverWidth: Float,
    onSelectedModeChange: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxPanelHeight = (configuration.screenHeightDp * 0.94f).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(250)) + slideInHorizontally(initialOffsetX = { it / 18 }, animationSpec = tween(250)),
            exit = fadeOut(tween(180)) + slideOutHorizontally(targetOffsetX = { it / 24 }, animationSpec = tween(180))
        ) {
        FujiPanel(
            title = "LUNADREAM ENGINE",
            modifier = Modifier.fillMaxSize().padding(10.dp),
            onBack = onDismiss
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Chọn mô hình trước khi chụp. Lựa chọn này được giữ trong suốt quá trình xử lý ảnh.",
                    color = FujiSkin.muted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                if (nightShotEnabled) {
                    Text(
                        "Night Shot đang bật. Tắt Night Shot trước khi chọn LunaDream.",
                        color = Color(0xFFFFB45C),
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
                val lunaModes = listOf(
                    Triple("luna15", R.string.lunadream_15_title, R.string.lunadream_15_subtitle),
                    Triple("luna20", R.string.lunadream_20_title, R.string.lunadream_20_subtitle),
                    Triple("luna25", R.string.lunadream_25_title, R.string.lunadream_25_subtitle)
                )
                lunaModes.chunked(2).forEach { modelPair ->
                    Row(
                        Modifier.fillMaxWidth().weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        modelPair.forEach { (modeId, titleRes, subtitleRes) ->
                            val outputTarget = lunaDreamOutputTarget(modeId)
                                ?: error("Missing LunaDream output specification for $modeId")
                            val outputDimensions = lunaDreamOutputDimensions(modeId, targetHeightOverWidth)
                                ?: error("Could not calculate LunaDream output dimensions for $modeId")
                            val resolutionLabel = String.format(
                                Locale.US,
                                "%,d × %,d px",
                                outputDimensions.first,
                                outputDimensions.second
                            )
                            val megapixelsLabel = "${outputTarget.targetPixels / 1_000_000L} MP"
                            Box(
                                Modifier.weight(1f).fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF27292B))
                                    .border(
                                        1.2.dp,
                                        if (selectedMode == modeId) Color(0xFFF0A030) else Color(0xFF66696B),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .padding(7.dp)
                            ) {
                                LunaDreamModeRow(
                                    title = stringResource(titleRes),
                                    subtitle = stringResource(subtitleRes),
                                    resolutionLabel = resolutionLabel,
                                    megapixelsLabel = megapixelsLabel,
                                    selected = selectedMode == modeId,
                                    enabled = !nightShotEnabled,
                                    intensity = when (modeId) {
                                        "luna15" -> 0.58f
                                        "luna20" -> 0.82f
                                        "luna25" -> 0.92f
                                        else -> 1f
                                    },
                                    phase = 0f,
                                    onCheckedChange = { onSelectedModeChange(if (it) modeId else "off") }
                                )
                            }
                        }
                        if (modelPair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Text(
                    if (selectedMode == "off") "LUNADREAM ĐANG TẮT" else "LUNADREAM · ${selectedMode.uppercase()}",
                    color = FujiSkin.lcdGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF55585A)))
                Text(
                    "LunaDream ghép nhiều khung hình, căn chỉnh chuyển động và xuất ảnh độ phân giải cao. Giữ máy ổn định khi chụp.",
                    color = Color(0xFFD5D7D8),
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF202224), RoundedCornerShape(12.dp)).padding(12.dp)
                )
                Text(
                    if (selectedMode == "off") "CHƯA CHỌN MÔ HÌNH" else "ĐANG CHỌN · ${selectedMode.uppercase()}",
                    color = Color(0xFFF0A030),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        }
    }
}

@Composable
private fun LunaDreamModeRow(
    title: String,
    subtitle: String,
    resolutionLabel: String,
    megapixelsLabel: String,
    selected: Boolean,
    intensity: Float,
    phase: Float,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF202426))
            .border(1.dp, if (selected) FujiSkin.orange else Color(0xFF465054), RoundedCornerShape(6.dp))
            .clickable(enabled = enabled) { if (enabled) onCheckedChange(!selected) }
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = FujiSkin.muted, fontSize = 9.sp, lineHeight = 13.sp, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(resolutionLabel, color = FujiSkin.lcdGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(megapixelsLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(10.dp))
        FujiSwitch(checked = selected, onCheckedChange = { if (enabled) onCheckedChange(it) })
    }
}

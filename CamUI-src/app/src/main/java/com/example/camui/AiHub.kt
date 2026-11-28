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


@Composable
fun LunaDreamBottomSheet(
    onDismiss: () -> Unit,
    nightShotEnabled: Boolean,
    nightShotDurationSeconds: Int,
    onNightShotDurationChange: (Int) -> Unit,
    onNightShotToggle: (Boolean) -> Unit,
    selectedMode: String,
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
            title = "LUNADREAM",
            modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = maxPanelHeight),
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
                    Triple("luna25", R.string.lunadream_25_title, R.string.lunadream_25_subtitle),
                    Triple("luna30", R.string.lunadream_30_title, R.string.lunadream_30_subtitle)
                )
                lunaModes.forEach { (modeId, titleRes, subtitleRes) ->
                    val resolutionLabel = when (modeId) {
                        "luna15" -> "FHD+"
                        "luna20" -> "UHD"
                        "luna25" -> "UHD+"
                        else -> "EUHD"
                    }
                    val megapixelsLabel = when (modeId) {
                        "luna15" -> "50 MP"
                        "luna20" -> "108 MP"
                        "luna25" -> "150 MP"
                        else -> "200 MP"
                    }
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
                Text(
                    if (selectedMode == "off") "LUNADREAM ĐANG TẮT" else "LUNADREAM · ${selectedMode.uppercase()}",
                    color = FujiSkin.lcdGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF354247)))
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF101516))
                        .border(1.dp, if (nightShotEnabled) FujiSkin.orange else Color(0xFF344247), RoundedCornerShape(3.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("NIGHT SHOT", color = if (selectedMode == "off") Color.White else Color.White.copy(alpha = 0.42f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (selectedMode == "off") "Chụp đêm và Star Trails dùng quy trình xử lý riêng."
                            else "Đang chọn LunaDream. Tắt mô hình trước khi dùng Night Shot.",
                            color = FujiSkin.muted.copy(alpha = if (selectedMode == "off") 1f else 0.55f),
                            fontSize = 9.sp,
                            lineHeight = 13.sp
                        )
                    }
                    Box {
                        FujiSwitch(
                            checked = nightShotEnabled,
                            onCheckedChange = { enabled ->
                                if (selectedMode == "off") onNightShotToggle(enabled)
                            }
                        )
                        if (selectedMode != "off") {
                            Box(
                                Modifier.matchParentSize()
                                    .clickable(
                                        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { }
                            )
                        }
                    }
                }
                if (nightShotEnabled) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf(5, 10, 3).forEach { seconds ->
                            FujiChip(
                                text = "${seconds}s",
                                selected = nightShotDurationSeconds == seconds,
                                onClick = {
                                    if (nightShotDurationSeconds != seconds) onNightShotDurationChange(seconds)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Text(
                    if (nightShotEnabled) "NIGHT SHOT · BẬT · ${nightShotDurationSeconds}s" else "NIGHT SHOT · TẮT",
                    color = if (nightShotEnabled) FujiSkin.orange else FujiSkin.muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Milky Way được bật/tắt trong Cài đặt → Chụp đêm.",
                    color = FujiSkin.muted,
                    fontSize = 9.sp
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

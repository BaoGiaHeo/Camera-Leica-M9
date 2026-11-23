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
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
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
                    "Choose a mode before capture. LunaDream keeps that selection throughout processing.",
                    color = FujiSkin.muted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                val lunaModes = listOf(
                    Triple("luna15", "LunaDream 1.5", "50MP · 1 khung (tối đa 2) · ưu tiên tốc độ"),
                    Triple("luna20", "LunaDream 2.0", "108MP · 3–5 khung · cân bằng tốc độ và chi tiết"),
                    Triple("luna25", "LunaDream 2.5", "150MP · 6–10 khung · căn chỉnh và ghép nâng cao"),
                    Triple("luna30", "LunaDream 3.0", "200MP · 12–16 khung · chất lượng cao nhất")
                )
                if (isLandscape) {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        lunaModes.chunked(2).forEachIndexed { rowIndex, rowModes ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowModes.forEach { (modeId, title, subtitle) ->
                                    Box(Modifier.weight(1f)) {
                                        LunaDreamModeRow(
                                            title = title,
                                            subtitle = subtitle,
                                            selected = selectedMode == modeId,
                                            intensity = when (modeId) {
                                                "luna15" -> 0.58f
                                                "luna20" -> 0.72f
                                                "luna25" -> 0.86f
                                                else -> 1f
                                            },
                                            phase = rowIndex.toFloat() * 0.5f,
                                            onCheckedChange = {
                                                onSelectedModeChange(if (it) modeId else "off")
                                            }
                                        )
                                    }
                                }
                                if (rowModes.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        lunaModes.forEach { (modeId, title, subtitle) ->
                            LunaDreamModeRow(
                                title = title,
                                subtitle = subtitle,
                                selected = selectedMode == modeId,
                                intensity = when (modeId) {
                                    "luna15" -> 0.58f
                                    "luna20" -> 0.72f
                                    "luna25" -> 0.86f
                                    else -> 1f
                                },
                                phase = 0f,
                                onCheckedChange = {
                                    onSelectedModeChange(if (it) modeId else "off")
                                }
                            )
                        }
                    }
                }
                Text(
                    if (selectedMode == "off") "LUNADREAM IS OFF" else "LUNADREAM · ${selectedMode.uppercase()}",
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
                        Text("NightShotMode", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Night capture and Star Trails use separate processing pipelines",
                            color = FujiSkin.muted, fontSize = 9.sp, lineHeight = 13.sp)
                    }
                    FujiSwitch(checked = nightShotEnabled, onCheckedChange = onNightShotToggle)
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
                    if (nightShotEnabled) "NIGHTSHOT · BẬT · ${nightShotDurationSeconds}s" else "NIGHTSHOT · TẮT",
                    color = if (nightShotEnabled) FujiSkin.orange else FujiSkin.muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Milky Way được bật/tắt trong Cài đặt → NightShotMode.",
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
    selected: Boolean,
    intensity: Float,
    phase: Float,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(FujiSkin.lcd)
            .border(1.dp, if (selected) FujiSkin.orange else Color(0xFF344247), RoundedCornerShape(4.dp))
            .clickable { onCheckedChange(!selected) }
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = FujiSkin.muted, fontSize = 9.sp, lineHeight = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        FujiSwitch(checked = selected, onCheckedChange = onCheckedChange)
    }
}

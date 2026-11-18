package com.example.camui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text as MaterialText
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

private val LunaYellow = Color(0xFFF5C518)
private val LunaMuted = Color(0xFFAAAAAA)
private val LunaRainbow = listOf(
    Color(0xFFFF5C8A), Color(0xFFFFB45C), Color(0xFFFFE66D),
    Color(0xFF67E8B5), Color(0xFF62C9FF), Color(0xFFAA83FF),
    Color(0xFFFF76D2)
)

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
                if (isLandscape) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) {
                            LunaDreamModeRow(
                                title = "LunaDream 1.5",
                                subtitle = "50MP · chụp một lần · xử lý trực tiếp, ưu tiên tốc độ",
                                selected = selectedMode == "luna15",
                                intensity = 0.58f,
                                phase = 0f,
                                onCheckedChange = { onSelectedModeChange(if (it) "luna15" else "off") }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            LunaDreamModeRow(
                                title = "LunaDream 2.0",
                                subtitle = "108MP · đa khung hình · ưu tiên phục hồi chi tiết tối đa",
                                selected = selectedMode == "luna20",
                                intensity = 1f,
                                phase = 0f,
                                onCheckedChange = { onSelectedModeChange(if (it) "luna20" else "off") }
                            )
                        }
                    }
                } else {
                    LunaDreamModeRow(
                        title = "LunaDream 1.5",
                        subtitle = "50MP · chụp một lần · xử lý trực tiếp, ưu tiên tốc độ",
                        selected = selectedMode == "luna15",
                        intensity = 0.58f,
                        phase = 0f,
                        onCheckedChange = { onSelectedModeChange(if (it) "luna15" else "off") }
                    )
                    LunaDreamModeRow(
                        title = "LunaDream 2.0",
                        subtitle = "108MP · đa khung hình · ưu tiên phục hồi chi tiết tối đa",
                        selected = selectedMode == "luna20",
                        intensity = 1f,
                        phase = 0f,
                        onCheckedChange = { onSelectedModeChange(if (it) "luna20" else "off") }
                    )
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

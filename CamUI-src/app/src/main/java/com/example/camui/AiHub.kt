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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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

private val LunaYellow = Color(0xFFF5C518)
private val LunaMuted = Color(0xFFAAAAAA)
private val LunaRainbow = listOf(
    Color(0xFFFF5C8A), Color(0xFFFFB45C), Color(0xFFFFE66D),
    Color(0xFF67E8B5), Color(0xFF62C9FF), Color(0xFFAA83FF),
    Color(0xFFFF76D2)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunaDreamBottomSheet(
    onDismiss: () -> Unit,
    nightShotEnabled: Boolean,
    nightShotDurationSeconds: Int,
    onCycleNightShot: () -> Unit,
    selectedMode: String,
    onSelectedModeChange: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * 0.84f).dp
    val transition = rememberInfiniteTransition(label = "lunadream-rainbow")
    val rainbowPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8_000, easing = LinearEasing)),
        label = "lunadream-rainbow-phase"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161616),
        contentColor = Color.White,
        scrimColor = Color(0x99000000),
        tonalElevation = 0.dp,
        dragHandle = {
            HorizontalDivider(
                modifier = Modifier.width(42.dp).padding(top = 10.dp, bottom = 8.dp),
                thickness = 4.dp,
                color = Color(0xFF666666)
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp).padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text("LunaDream", color = LunaYellow, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "Chọn chế độ trước khi chụp. LunaDream giữ nguyên lựa chọn trong suốt quá trình xử lý.",
                color = LunaMuted, fontSize = 12.sp, lineHeight = 17.sp
            )
            LunaDreamModeRow(
                title = "LunaDream 1.5",
                subtitle = "50MP · chụp một lần · xử lý trực tiếp, ưu tiên tốc độ",
                selected = selectedMode == "luna15",
                intensity = 0.58f,
                phase = rainbowPhase,
                onCheckedChange = { onSelectedModeChange(if (it) "luna15" else "off") }
            )
            LunaDreamModeRow(
                title = "LunaDream 2.0",
                subtitle = "108MP · đa khung hình · ưu tiên phục hồi chi tiết tối đa",
                selected = selectedMode == "luna20",
                intensity = 1f,
                phase = rainbowPhase,
                onCheckedChange = { onSelectedModeChange(if (it) "luna20" else "off") }
            )
            if (selectedMode == "off") {
                Text(
                    "LunaDream đang tắt",
                    color = LunaMuted, fontSize = 11.sp
                )
            }
            HorizontalDivider(color = Color(0xFF333333))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF202020)).clickable(onClick = onCycleNightShot)
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("NightShotMode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Chụp đêm và Star Trails có pipeline riêng.",
                        color = LunaMuted, fontSize = 11.sp, lineHeight = 15.sp
                    )
                    Text(
                        if (nightShotEnabled) "ĐANG BẬT · " + nightShotDurationSeconds + "s" else "NIGHTSHOT · TẮT",
                        color = if (nightShotEnabled) LunaYellow else Color(0xFF888888),
                        fontSize = 10.sp, fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (nightShotEnabled) nightShotDurationSeconds.toString() + "s" else "OFF",
                    color = if (nightShotEnabled) LunaYellow else LunaMuted,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Milky Way được bật/tắt trong Cài đặt → NightShotMode.",
                color = LunaMuted, fontSize = 11.sp, lineHeight = 16.sp
            )
            Spacer(Modifier.height(4.dp))
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
    val glowAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(480),
        label = "lunadream-toggle-glow"
    )
    val shape = RoundedCornerShape(16.dp)
    val rainbowBrush = Brush.sweepGradient(
        colors = LunaRainbow.map { it.copy(alpha = glowAlpha * intensity) }
    )
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(shape)
            .background(
                if (glowAlpha > 0.01f) {
                    Brush.sweepGradient(
                        colors = LunaRainbow.map { it.copy(alpha = glowAlpha * intensity * 0.12f) }
                    )
                } else {
                    Brush.linearGradient(listOf(Color(0xFF222222), Color(0xFF202020)))
                }
            )
            .then(
                if (glowAlpha > 0.01f) Modifier.border(
                    width = if (intensity > 0.9f) 1.8.dp else 1.2.dp,
                    brush = rainbowBrush,
                    shape = shape
                ) else Modifier.border(1.dp, Color(0xFF343434), shape)
            )
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn(tween(260)) + scaleIn(initialScale = 0.35f, animationSpec = tween(360)) +
                            slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = tween(360)),
                        exit = fadeOut(tween(220)) + scaleOut(targetScale = 0.35f, animationSpec = tween(260)) +
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.10f))
                                .border(0.7.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✦",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.graphicsLayer { rotationZ = phase * 360f }
                            )
                        }
                    }
                }
                Text(subtitle, color = LunaMuted, fontSize = 10.sp, lineHeight = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = selected, onCheckedChange = onCheckedChange)
        }
    }
}

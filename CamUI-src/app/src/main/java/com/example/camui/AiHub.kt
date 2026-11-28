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
import androidx.compose.foundation.layout.fillMaxHeight
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(220)) + slideInHorizontally(
                initialOffsetX = { it / 30 },
                animationSpec = tween(220)
            ),
            exit = fadeOut(tween(160)) + slideOutHorizontally(
                targetOffsetX = { it / 36 },
                animationSpec = tween(160)
            )
        ) {
            Column(
                Modifier.fillMaxSize()
                    .background(Color(0xFF171717))
                    .padding(start = 33.dp, end = 44.dp, top = 16.dp, bottom = 47.dp),
                verticalArrangement = Arrangement.spacedBy(23.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().height(31.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "LUNADREAM ENGINE",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.35.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.width(77.dp).height(31.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF888888), RoundedCornerShape(6.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Quay Lại",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
                Row(
                    Modifier.weight(1f).fillMaxWidth()
                        .clip(RoundedCornerShape(34.dp))
                        .border(1.dp, Color(0xFF777777), RoundedCornerShape(34.dp))
                        .padding(start = 36.dp, end = 84.dp),
                    horizontalArrangement = Arrangement.spacedBy(30.dp)
                ) {
                    Column(
                        Modifier.weight(1.1f).fillMaxHeight().padding(top = 28.dp, bottom = 70.dp),
                        verticalArrangement = Arrangement.spacedBy(23.dp)
                    ) {
                        LunaDreamModeCard(
                            modifier = Modifier.fillMaxWidth().height(88.dp),
                            title = "LunaDream 1.5",
                            description = "50MP Chất lượng ảnh sẽ Không được nét nhưng xử lý nhanh",
                            selected = selectedMode == "luna15",
                            enabled = !nightShotEnabled
                        ) { onSelectedModeChange(if (selectedMode == "luna15") "off" else "luna15") }
                        LunaDreamModeCard(
                            modifier = Modifier.fillMaxWidth().height(99.dp),
                            title = "LunaDream 2.0",
                            description = "108MP chất lượng nét hơn, Xử lý chậm so với 1.5 xíu",
                            selected = selectedMode == "luna20",
                            enabled = !nightShotEnabled
                        ) { onSelectedModeChange(if (selectedMode == "luna20") "off" else "luna20") }
                    }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().padding(top = 91.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LunaDreamModeCard(
                            modifier = Modifier.fillMaxWidth().height(83.dp),
                            title = "LunaDream 3.0",
                            description = "180MP Chất lượng nét hơn, Hình ảnh tốt hơn",
                            selected = selectedMode == "luna25",
                            enabled = !nightShotEnabled
                        ) { onSelectedModeChange(if (selectedMode == "luna25") "off" else "luna25") }
                        Column(
                            Modifier.fillMaxWidth().height(111.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF303030))
                                .border(1.dp, Color(0xFF444444), RoundedCornerShape(22.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "LƯU Ý:",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(7.dp))
                            Text(
                                "THUẬT TOÁN LUNADREAM ENGINE TRONG QUÁ TRÌNH PHÁT TRIỂN NÊN CHẤT LƯỢNG ẢNH NÉT THẬT NHƯNG CÓ THỂ BỊ MỜ NẾU GIỮ MÁY KHÔNG ỔN ĐỊNH",
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                            if (nightShotEnabled) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Night Shot đang bật. Hãy tắt Night Shot trước khi chọn LunaDream.",
                                    color = Color(0xFFFFB45C),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LunaDreamModeCard(
    modifier: Modifier,
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF555555))
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) Color(0xFFF0A030) else Color(0xFF8A8A8A),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.42f }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Spacer(Modifier.height(5.dp))
        Text(
            description,
            color = Color.White,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        if (selected) {
            Spacer(Modifier.height(6.dp))
            Text("ĐANG CHỌN", color = Color(0xFFFFD18A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

package com.example.camui

import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.material3.Text

private val LunaCyan = Color(0xFF69EAF0)
private val LunaMuted = Color(0xFFB9C5C8)
private val LunaPanel = Color(0xFF111C22)

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
    val localView = LocalView.current
    val dialogWindow = (localView.parent as? DialogWindowProvider)?.window
    DisposableEffect(dialogWindow) {
        val decor = dialogWindow?.decorView
        val previousVisibility = decor?.systemUiVisibility ?: 0
        val previousFlags = dialogWindow?.attributes?.flags ?: 0
        dialogWindow?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        decor?.systemUiVisibility = previousVisibility or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        onDispose {
            decor?.systemUiVisibility = previousVisibility
            dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            if (previousFlags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0) {
                dialogWindow?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(220)) + slideInHorizontally(initialOffsetX = { it / 35 }, animationSpec = tween(220)),
            exit = fadeOut(tween(160)) + slideOutHorizontally(targetOffsetX = { it / 40 }, animationSpec = tween(160))
        ) {
            BoxWithConstraints(
                Modifier.fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFF050A0D), Color(0xFF10212A), Color(0xFF05080B))))
            ) {
                val sideMargin = maxWidth * 0.032f
                val verticalMargin = maxHeight * 0.035f
                val headerHeight = (maxHeight * 0.105f).coerceIn(48.dp, 72.dp)
                val panelTopGap = maxHeight * 0.05f
                val layoutGap = maxHeight * 0.018f
                Column(
                    Modifier.fillMaxSize().padding(horizontal = sideMargin, vertical = verticalMargin),
                    verticalArrangement = Arrangement.spacedBy(panelTopGap)
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(headerHeight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            Text(
                                "LUNADREAM ENGINE",
                                color = Color.White,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.1.sp,
                                maxLines = 1
                            )
                            Box(
                                Modifier.padding(top = 4.dp).width(190.dp).height(2.dp)
                                    .background(Brush.horizontalGradient(listOf(LunaCyan, Color(0xFF408FC6), Color.Transparent)))
                            )
                        }
                        Row(
                            Modifier.width(148.dp).height((headerHeight * 0.78f).coerceAtLeast(42.dp))
                                .clip(RoundedCornerShape(9.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF18323A), Color(0xFF14232B))))
                                .border(1.5.dp, LunaCyan, RoundedCornerShape(9.dp))
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            FujiIconGlyph("back", Modifier.size(20.dp), Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Quay Lại", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    BoxWithConstraints(
                        Modifier.fillMaxWidth().weight(1f)
                            .shadow(18.dp, RoundedCornerShape(23.dp))
                            .clip(RoundedCornerShape(23.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF102830), Color(0xFF11212A), Color(0xFF0A151C))))
                            .border(2.dp, Color(0xFF50DCE8), RoundedCornerShape(23.dp))
                    ) {
                        val panelPadding = maxWidth * 0.042f
                        val panelHeight = maxHeight
                        val columnGap = maxWidth * 0.035f
                        val leftCardHeight = panelHeight * 0.31f
                        Canvas(Modifier.fillMaxSize()) {
                            val spacing = 22.dp.toPx()
                            var y = 0f
                            while (y < size.height) {
                                drawLine(Color.White.copy(alpha = 0.016f), Offset(size.width * 0.04f, y), Offset(size.width * 0.96f, y), 0.4.dp.toPx())
                                y += spacing
                            }
                            drawCircle(Color(0xFF6CE8EB).copy(alpha = 0.06f), size.minDimension * 0.25f, Offset(size.width * 0.5f, size.height * 0.45f))
                        }
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = panelPadding, vertical = panelHeight * 0.07f),
                            horizontalArrangement = Arrangement.spacedBy(columnGap)
                        ) {
                            Column(
                                Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(layoutGap)
                            ) {
                                LunaDreamModeCard(
                                    modifier = Modifier.fillMaxWidth().height(leftCardHeight),
                                    title = "LunaDream 1.5",
                                    description = "50MP Chất lượng ảnh sẽ Không được nét nhưng xử lý nhanh",
                                    selected = selectedMode == "luna15",
                                    enabled = !nightShotEnabled,
                                    icon = "bolt"
                                ) { onSelectedModeChange(if (selectedMode == "luna15") "off" else "luna15") }
                                LunaDreamModeCard(
                                    modifier = Modifier.fillMaxWidth().height(leftCardHeight),
                                    title = "LunaDream 2.0",
                                    description = "108MP chất lượng nét hơn, Xử lý chậm so với 1.5 xíu",
                                    selected = selectedMode == "luna20",
                                    enabled = !nightShotEnabled,
                                    icon = "balance"
                                ) { onSelectedModeChange(if (selectedMode == "luna20") "off" else "luna20") }
                            }
                            Column(
                                Modifier.weight(1f).fillMaxHeight().padding(top = panelHeight * 0.16f),
                                verticalArrangement = Arrangement.spacedBy(layoutGap)
                            ) {
                                LunaDreamModeCard(
                                    modifier = Modifier.fillMaxWidth().height(leftCardHeight),
                                    title = "LunaDream 3.0",
                                    description = "180MP Chất lượng nét hơn, Hình ảnh tốt hơn",
                                    selected = selectedMode == "luna30",
                                    enabled = !nightShotEnabled,
                                    recommended = true,
                                    icon = "camera"
                                ) { onSelectedModeChange(if (selectedMode == "luna30") "off" else "luna30") }

                                Column(
                                    Modifier.fillMaxWidth().weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF281D1E), Color(0xFF141B20))))
                                        .border(1.dp, Color(0xFFB96D70), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("LƯU Ý:", color = Color(0xFFFFB6A8), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                        Spacer(Modifier.weight(1f))
                                        Canvas(Modifier.size(21.dp)) {
                                            drawCircle(Color(0xFFFF8E82), style = Stroke(1.7.dp.toPx()))
                                            drawLine(Color(0xFFFF8E82), Offset(size.width / 2, size.height * 0.25f), Offset(size.width / 2, size.height * 0.59f), 1.8.dp.toPx())
                                            drawCircle(Color(0xFFFF8E82), 1.2.dp.toPx(), Offset(size.width / 2, size.height * 0.76f))
                                        }
                                    }
                                    Text(
                                        "THUẬT TOÁN LUNADREAM ENGINE ĐANG TRONG QUÁ TRÌNH PHÁT TRIỂN. CHẤT LƯỢNG ẢNH CÓ THỂ BỊ MỜ NẾU GIỮ MÁY KHÔNG ỔN ĐỊNH.",
                                        color = Color(0xFFE8E8E8),
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xAA0B1318))
                            .border(1.dp, Color(0xFF344D55), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FujiIconGlyph("moon", Modifier.size(19.dp), Color(0xFF8DEAF0))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("NIGHT SHOT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("3 / 5 / 10 giây · mục tiêu 8 FPS", color = LunaMuted, fontSize = 10.sp)
                        }
                        listOf(3, 5, 10).forEach { seconds ->
                            Box(
                                Modifier.padding(start = 5.dp).width(46.dp).height(29.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (nightShotEnabled && nightShotDurationSeconds == seconds) Color(0xFF1B6570) else Color(0xFF242D31))
                                    .border(1.dp, if (nightShotEnabled && nightShotDurationSeconds == seconds) LunaCyan else Color(0xFF4C5C62), RoundedCornerShape(7.dp))
                                    .clickable {
                                        onNightShotDurationChange(seconds)
                                        if (!nightShotEnabled) onNightShotToggle(true)
                                    },
                                contentAlignment = Alignment.Center
                            ) { Text("${seconds}s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                        Box(
                            Modifier.padding(start = 7.dp).width(46.dp).height(29.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (nightShotEnabled) Color(0xFF1B6570) else Color(0xFF242D31))
                                .border(1.dp, if (nightShotEnabled) LunaCyan else Color(0xFF4C5C62), RoundedCornerShape(7.dp))
                                .clickable { onNightShotToggle(!nightShotEnabled) },
                            contentAlignment = Alignment.Center
                        ) { Text(if (nightShotEnabled) "ON" else "OFF", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
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
    recommended: Boolean = false,
    icon: String,
    onClick: () -> Unit
) {
    val accent = if (selected || recommended) LunaCyan else Color(0xFF526B73)
    Box(modifier) {
        Column(
            Modifier.fillMaxSize()
                .shadow(if (selected) 12.dp else 4.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1C3037), LunaPanel)))
                .border(if (selected) 2.dp else 1.dp, accent, RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                when (icon) {
                    "bolt" -> Text("ϟ", color = Color(0xFFFFD36A), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    "balance" -> Text("⚖", color = LunaCyan, fontSize = 20.sp)
                    "camera" -> FujiIconGlyph("camera", Modifier.size(23.dp), LunaCyan)
                }
            }
            Text(description, color = LunaMuted, fontSize = 14.sp, lineHeight = 18.sp, maxLines = 2)
        }
        if (recommended) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 0.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF22545C))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) { Text("Khuyên dùng", color = Color(0xFFA9F3F2), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

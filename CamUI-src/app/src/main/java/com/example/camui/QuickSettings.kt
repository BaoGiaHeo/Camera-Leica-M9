package com.example.camui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickPanel(
    ratioIdx: Int,
    onRatio: (Int) -> Unit,
    deviceLandscape: Boolean,
    ultraHdr: Boolean,
    onUltraHdr: () -> Unit,
    metering: String,
    onMetering: () -> Unit,
    renderEngine: String,
    onRenderEngine: () -> Unit,
    livePhotoEnabled: Boolean,
    onLivePhotoChanged: (Boolean) -> Unit,
    nativeImageProcessingEnabled: Boolean,
    onNativeImageProcessingChanged: (Boolean) -> Unit,
    onToolbox: () -> Unit,
    onContentManagement: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOther by remember { mutableStateOf(false) }
    FujiPanel(
        title = if (showOther) "OTHER · DEVICE OPTIONS" else "Q MENU",
        modifier = modifier
            .widthIn(max = if (deviceLandscape) 560.dp else 620.dp)
            .fillMaxWidth()
            .heightIn(max = 440.dp),
        onBack = { if (showOther) showOther = false else onDismiss() }
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (showOther) {
                FujiListRow(
                    "Xử lý ảnh gốc thiết bị",
                    if (nativeImageProcessingEnabled) "ON" else "OFF",
                    onClick = { onNativeImageProcessingChanged(!nativeImageProcessingEnabled) },
                    description = "Ưu tiên ISP/OEM AUTO-HDR; sau chụp tiếp tục tối ưu dải sáng và chi tiết trên JPEG."
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (nativeImageProcessingEnabled) "ĐANG BẬT · ISP/OEM" else "ĐANG TẮT · LUNADREAM CÓ THỂ DÙNG",
                        color = FujiSkin.lcdGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    FujiSwitch(nativeImageProcessingEnabled, onNativeImageProcessingChanged)
                }
            } else {
                Text("ASPECT RATIO", color = FujiSkin.lcdGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    aspectRatios.forEachIndexed { index, _ ->
                        FujiChip(
                            aspectRatioDisplayLabel(index, deviceLandscape),
                            index == ratioIdx,
                            { onRatio(index) },
                            Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("LIVE PHOTO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(
                            if (livePhotoEnabled) "Đang bật · ảnh + clip chuyển động"
                            else "Đang tắt · ảnh tĩnh thông thường",
                            color = FujiSkin.muted,
                            fontSize = 8.sp
                        )
                    }
                    FujiSwitch(livePhotoEnabled, onLivePhotoChanged)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FujiTile(
                        "Render Engine", renderEngine, true, onRenderEngine,
                        modifier = Modifier.weight(1f)
                    )
                    FujiTile(
                        "Metering", metering, false, onMetering,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FujiTile(
                        "Toolbox", "Assistive tools", false, onToolbox,
                        icon = Icons.Default.Palette, modifier = Modifier.weight(1f)
                    )
                    FujiTile(
                        "Contents", "Frames", false, onContentManagement,
                        icon = Icons.Default.Tune, modifier = Modifier.weight(1f)
                    )
                    FujiTile(
                        "Other", "Device options", false, { showOther = true },
                        icon = Icons.Default.MoreHoriz, modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 3.dp)
                        .height(37.dp)
                        .background(Color(0xFF242A2C))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("MENU/OK  >  SETTINGS", color = FujiSkin.lcdGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "SETTINGS  ›",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onSettings)
                    )
                }
            }
        }
    }
}

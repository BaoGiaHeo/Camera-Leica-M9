package com.example.camui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    modifier: Modifier = Modifier
) {
    val isLandscape = deviceLandscape
    val shape = RoundedCornerShape(22.dp)
    var showOther by remember { mutableStateOf(false) }
    Column(
        modifier.fillMaxWidth().clip(shape).background(CamColors.Panel)
            .border(0.5.dp, CamColors.Stroke, shape).padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showOther) {
            Row(
                Modifier.fillMaxWidth().height(34.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                        .background(CamColors.Chip).animaTClickable { showOther = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Other", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(CamColors.Chip).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Xử lý ảnh gốc thiết bị",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        "Ưu tiên CameraX AUTO/HDR của nhà sản xuất nếu có; nếu không, dùng pipeline ISP mặc định.",
                        color = CamColors.Sub,
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.size(8.dp))
                Switch(
                    checked = nativeImageProcessingEnabled,
                    onCheckedChange = onNativeImageProcessingChanged
                )
            }
            Text(
                if (nativeImageProcessingEnabled) {
                    "Đang bật · LunaDream bị khóa. Ảnh dùng pipeline gốc được thiết bị công bố."
                } else {
                    "Đang tắt · Có thể sử dụng LunaDream."
                },
                color = if (nativeImageProcessingEnabled) CamColors.Yellow else CamColors.Sub,
                fontSize = 10.sp
            )
        } else {
            Text("Aspect Ratio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                aspectRatios.forEachIndexed { index, pair ->
                    Chip(aspectRatioDisplayLabel(index, isLandscape), index == ratioIdx, { onRatio(index) }, Modifier.weight(1f))
                }
            }
            // Ultra HDR is hidden from the UI; its camera pipeline remains intact.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(CamColors.Chip).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Live Photo", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        if (livePhotoEnabled) "Đang bật · tối đa 48MP"
                        else "Đang tắt · ảnh tĩnh thông thường",
                        color = if (livePhotoEnabled) CamColors.Yellow else CamColors.Sub,
                        fontSize = 10.sp
                    )
                }
                Switch(checked = livePhotoEnabled, onCheckedChange = onLivePhotoChanged)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FeatureCard("Render Engine", renderEngine, true, onRenderEngine, Modifier.weight(1f))
                SmallCard("Metering", metering, null, onMetering, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SmallCard("Toolbox", "Assistive tools", Icons.Default.Palette, onToolbox, Modifier.weight(1f))
                SmallCard("Contents", "Frames", Icons.Default.Tune, onContentManagement, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SmallCard("Other", "Device options", Icons.Default.MoreHoriz, { showOther = true }, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
            Row(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(CamColors.Chip).animaTClickable(onClick = onSettings).padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Settings", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.height(72.dp).clip(RoundedCornerShape(14.dp))
            .background(if (selected) CamColors.Yellow else CamColors.Chip)
            .animaTClickable(onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = if (selected) Color.Black else Color.White,
            fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        Text(value, color = if (selected) Color.Black else CamColors.Sub,
            fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun SmallCard(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.height(66.dp).clip(RoundedCornerShape(14.dp)).background(CamColors.Chip)
        .animaTClickable(onClick = onClick).padding(10.dp)) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            if (subtitle != null) Text(subtitle, color = CamColors.Sub, fontSize = 9.sp)
        }
        if (icon != null) Icon(icon, contentDescription = title, tint = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
    }
}

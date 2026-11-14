package com.example.camui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
    ultraHdr: Boolean,
    onUltraHdr: () -> Unit,
    stabilization: Boolean,
    onStabilization: () -> Unit,
    metering: String,
    onMetering: () -> Unit,
    renderEngine: String,
    onRenderEngine: () -> Unit,
    onToolbox: () -> Unit,
    onContentManagement: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(CamColors.Panel)
            .border(0.5.dp, CamColors.Stroke, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Aspect Ratio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            aspectRatios.forEachIndexed { index, pair ->
                Chip(pair.first, index == ratioIdx, { onRatio(index) }, Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FeatureCard("Render Engine", renderEngine, true, onRenderEngine, Modifier.weight(1f))
            FeatureCard("Ultra HDR", if (ultraHdr) "ON" else "OFF", ultraHdr, onUltraHdr, Modifier.weight(1f))
            FeatureCard("Stabilization", if (stabilization) "ON" else "OFF", stabilization,
                onStabilization, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SmallCard("Metering", metering, null, onMetering, Modifier.weight(1f))
            SmallCard("Toolbox", "Assistive tools", Icons.Default.Palette, onToolbox, Modifier.weight(1f))
            SmallCard("Contents", "LUT / Frames", Icons.Default.Tune, onContentManagement, Modifier.weight(1f))
        }
        Row(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
                .background(CamColors.Chip).clickable(onClick = onSettings).padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Settings", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
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
            .clickable(onClick = onClick).padding(10.dp),
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
        .clickable(onClick = onClick).padding(10.dp)) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            if (subtitle != null) Text(subtitle, color = CamColors.Sub, fontSize = 9.sp)
        }
        if (icon != null) Icon(icon, contentDescription = title, tint = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
    }
}

package com.example.camui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource

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
    faceTrackingMode: String,
    onFaceTrackingModeChanged: (String) -> Unit,
    onToolbox: () -> Unit,
    onWatermarkPanel: () -> Unit = {},
    watermarkEnabled: Boolean = true,
    watermarkLayoutLabel: String = "THANH",
    onContentManagement: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    FujiPanel(
        title = "Q MENU",
        modifier = modifier
            .widthIn(max = if (deviceLandscape) 560.dp else 620.dp)
            .fillMaxWidth()
            .heightIn(max = 440.dp),
        onBack = onDismiss
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                stringResource(R.string.face_tracking_section_title),
                color = FujiSkin.lcdGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                stringResource(R.string.face_tracking_section_description),
                color = FujiSkin.muted,
                fontSize = 8.sp
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FujiChip(
                    stringResource(R.string.face_tracking_off),
                    faceTrackingMode == "off",
                    { onFaceTrackingModeChanged("off") },
                    Modifier.weight(1f)
                )
                FujiChip(
                    stringResource(R.string.face_tracking_face),
                    faceTrackingMode == "face",
                    { onFaceTrackingModeChanged("face") },
                    Modifier.weight(1f)
                )
                FujiChip(
                    stringResource(R.string.face_tracking_face_eye),
                    faceTrackingMode == "face_eye",
                    { onFaceTrackingModeChanged("face_eye") },
                    Modifier.weight(1f)
                )
            }

            Text(
                "TỈ LỆ KHUNG HÌNH",
                color = FujiSkin.lcdGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
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

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FujiTile(
                    "BỘ MÁY KẾT XUẤT",
                    if (renderEngine == "Performance") "Hiệu năng" else "Tương thích",
                    true,
                    onRenderEngine,
                    modifier = Modifier.weight(1f)
                )
                FujiTile(
                    "ĐO SÁNG",
                    metering,
                    false,
                    onMetering,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FujiTile(
                    "HÌNH MỜ",
                    if (watermarkEnabled) "BẬT · " + watermarkLayoutLabel.uppercase() else "TẮT",
                    true,
                    onWatermarkPanel,
                    icon = "image",
                    modifier = Modifier.weight(1f)
                )
                FujiTile(
                    "CÔNG CỤ",
                    "Công cụ hỗ trợ",
                    false,
                    onToolbox,
                    icon = "tab_08",
                    modifier = Modifier.weight(1f)
                )
                FujiTile(
                    "NỘI DUNG",
                    "LUT",
                    false,
                    onContentManagement,
                    icon = "sliders",
                    modifier = Modifier.weight(1f)
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
                Text(
                    "▲▼ CHỌN · MENU/OK MỞ · DISP/BACK QUAY LẠI",
                    color = FujiSkin.lcdGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "CÀI ĐẶT ›",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.animaTClickable(onClick = onSettings)
                )
            }
        }
    }
}

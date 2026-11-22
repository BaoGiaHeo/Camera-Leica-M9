package com.example.camui

import androidx.compose.foundation.background
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
    displayModeLabel: String,
    onDisplayMode: () -> Unit,
    renderEngine: String,
    onRenderEngine: () -> Unit,
    livePhotoEnabled: Boolean,
    onLivePhotoChanged: (Boolean) -> Unit,
    nativeImageProcessingEnabled: Boolean,
    onNativeImageProcessingChanged: (Boolean) -> Unit,
    faceTrackingMode: String,
    onFaceTrackingModeChanged: (String) -> Unit,
    onToolbox: () -> Unit,
    onWatermarkPanel: () -> Unit = {},
    watermarkEnabled: Boolean = true,
    watermarkLayoutLabel: String = "BAR",
    onContentManagement: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOther by remember { mutableStateOf(false) }
    FujiPanel(
        title = if (showOther) stringResource(R.string.quick_other_device_options) else stringResource(R.string.quick_menu_title),
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
                    stringResource(R.string.quick_native_processing_title),
                    stringResource(if (nativeImageProcessingEnabled) R.string.common_on else R.string.common_off),
                    onClick = { onNativeImageProcessingChanged(!nativeImageProcessingEnabled) },
                    description = stringResource(R.string.quick_native_processing_description)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(if (nativeImageProcessingEnabled) R.string.quick_native_status_on else R.string.quick_native_status_off),
                        color = FujiSkin.lcdGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    FujiSwitch(nativeImageProcessingEnabled, onNativeImageProcessingChanged)
                }
            } else {
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
                Text(stringResource(R.string.quick_aspect_ratio), color = FujiSkin.lcdGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                        Text(stringResource(R.string.quick_live_photo_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(
                            stringResource(if (livePhotoEnabled) R.string.quick_live_photo_on else R.string.quick_live_photo_off),
                            color = FujiSkin.muted,
                            fontSize = 8.sp
                        )
                    }
                    FujiSwitch(livePhotoEnabled, onLivePhotoChanged)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FujiTile(
                        stringResource(R.string.quick_render_engine), renderEngine, true, onRenderEngine,
                        modifier = Modifier.weight(1f)
                    )
                    FujiTile(
                        stringResource(R.string.quick_metering), metering, false, onMetering,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FujiTile(
                        stringResource(R.string.quick_display_title),
                        displayModeLabel,
                        false,
                        onDisplayMode,
                        icon = "display",
                        modifier = Modifier.weight(1f)
                    )
                    FujiTile(
                        stringResource(R.string.quick_watermark),
                        if (watermarkEnabled) {
                            stringResource(R.string.common_on) + " · " + watermarkLayoutLabel.uppercase()
                        } else stringResource(R.string.common_off),
                        true,
                        onWatermarkPanel,
                        icon = "image",
                        modifier = Modifier.weight(1f)
                    )
                    FujiTile(
                        stringResource(R.string.quick_toolbox), stringResource(R.string.quick_toolbox_subtitle), false, onToolbox,
                        icon = "tab_08", modifier = Modifier.weight(1f)
                    )
                    FujiTile(
                        stringResource(R.string.quick_contents), stringResource(R.string.quick_contents_subtitle), false, onContentManagement,
                        icon = "sliders", modifier = Modifier.weight(1f)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FujiTile(stringResource(R.string.quick_other), stringResource(R.string.quick_other_subtitle), false, { showOther = true },
                        icon = "more", modifier = Modifier.weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 3.dp)
                        .height(37.dp)
                        .background(Color(0xFF242A2C))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.quick_settings_footer), color = FujiSkin.lcdGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.quick_settings_action),
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

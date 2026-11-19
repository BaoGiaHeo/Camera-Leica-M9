package com.example.camui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PixelOsdPreview() {
    Column(
        Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("OSD PREVIEW · PRO", color = FujiSkin.lcdGreen, fontSize = 11.sp)
        Box(
            Modifier.fillMaxWidth().weight(1f)
                .background(Color(0xFF050708))
                .border(2.dp, Color(0xFF344247))
        ) {
            PixelOsdLayer(
                Modifier.fillMaxSize(),
                PixelOsdFrame(
                    showGrid = true,
                    faceCenter = Offset(0.48f, 0.43f),
                    faceFocused = true,
                    focusLocked = true,
                    focusPoints = listOf(
                        Offset(0.22f, 0.28f),
                        Offset(0.26f, 0.30f),
                        Offset(0.68f, 0.61f),
                        Offset(0.72f, 0.64f)
                    ),
                    showLevel = true,
                    horizonRollDegrees = 0.6f,
                    showZebra = true,
                    zebraPoints = listOf(
                        Offset(0.81f, 0.18f),
                        Offset(0.84f, 0.20f),
                        Offset(0.87f, 0.22f)
                    )
                )
            )
        }
        Text("OSD PREVIEW · PHOTO", color = FujiSkin.lcdGreen, fontSize = 11.sp)
        Box(
            Modifier.fillMaxWidth().weight(1f)
                .background(Color(0xFF050708))
                .border(2.dp, Color(0xFF344247))
        ) {
            PixelOsdLayer(
                Modifier.fillMaxSize(),
                PixelOsdFrame(
                    showGrid = false,
                    showLevel = false,
                    showZebra = false,
                    focusPoints = emptyList()
                )
            )
        }
    }
}

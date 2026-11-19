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

private fun spritePixelColor(value: Char): Color? = when (value) {
    'W' -> Color(0xFFF2F2F2)
    'G' -> Color(0xFF5CFF7A)
    'R' -> Color(0xFFFF4A3D)
    'A' -> Color(0xFFFFB300)
    'B' -> Color(0xFF3DA5FF)
    else -> null
}

@Composable
fun PixelOsdIconSheet() {
    val sprites = PixelOsdSprites.entries()
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("ICON SHEET · PIXEL OSD", color = FujiSkin.lcdGreen, fontSize = 13.sp)
        Text(
            "Sprite source: character arrays · 1-bit / multi-colour palette · nearest pixel blocks",
            color = CamColors.Sub,
            fontSize = 9.sp
        )
        sprites.forEach { (name, rows) ->
            Row(
                Modifier.fillMaxWidth()
                    .background(FujiSkin.lcd)
                    .border(1.dp, Color(0xFF344247))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name, color = FujiSkin.lcdGreen, fontSize = 9.sp)
                    Text("${rows.first().length} × ${rows.size} logic px", color = CamColors.Sub, fontSize = 8.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelSpriteSample(rows, 1)
                    PixelSpriteSample(rows, 2)
                    PixelSpriteSample(rows, 4)
                }
            }
        }
    }
}

@Composable
private fun PixelSpriteSample(rows: Array<String>, zoom: Int) {
    val width = rows.maxOfOrNull { it.length } ?: 1
    val height = rows.size.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text("${zoom}x", color = CamColors.Sub, fontSize = 7.sp)
        androidx.compose.foundation.Canvas(
            Modifier.size((width * zoom).dp, (height * zoom).dp)
                .background(Color(0xFF050708))
        ) {
            val cellWidth = size.width / width.coerceAtLeast(1)
            val cellHeight = size.height / height
            rows.forEachIndexed { y, row ->
                row.forEachIndexed { x, character ->
                    spritePixelColor(character)?.let { color ->
                        drawRect(
                            color = color,
                            topLeft = Offset(x * cellWidth, y * cellHeight),
                            size = Size(cellWidth, cellHeight)
                        )
                    }
                }
            }
        }
    }
}

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

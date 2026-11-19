package com.example.camui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Developer-only visual typography inspection panel.
 *
 * The panel deliberately includes stacked Vietnamese marks and every known missing
 * punctuation code point so a device screenshot can be inspected for clipping and
 * fallback consistency at the font sizes currently used by the app.
 */
@Composable
fun FontSpecimenPanel() {
    val context = LocalContext.current
    val fontLoaded = remember { FujiTypography.isBundledFontLoaded(context) }
    val glyphRows = listOf(
        "VIETNAMESE UPPERCASE" to "Ắ Ằ Ẳ Ẵ Ặ Ấ Ầ Ẩ Ẫ Ậ Ế Ề Ể Ễ Ệ Ố Ồ Ổ Ỗ Ộ Ớ Ờ Ở Ỡ Ợ Ứ Ừ Ử Ữ Ự Ỳ Ỷ Ỹ Ỵ Đ Ơ Ư",
        "VIETNAMESE LOWERCASE" to "ắ ằ ẳ ẵ ặ ấ ầ ẩ ẫ ậ ế ề ể ễ ệ ố ồ ổ ỗ ộ ớ ờ ở ỡ ợ ứ ừ ử ữ ự ỳ ỷ ỹ ỵ đ ơ ư",
        "LATIN" to "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
        "DIGITS" to "0123456789",
        "OSD SYMBOLS" to "+ - / : . , % ° ′ ″ · ▲ ▼ < > ● K"
    )
    val requiredSamples = listOf(
        stringResource(R.string.font_specimen_vietnamese_sample),
        stringResource(R.string.font_specimen_english_sample)
    )
    val sizes = listOf(8, 10, 12, 14, 16, 20, 24)

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.font_specimen_title),
            color = CamColors.TextColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Row(
            Modifier.fillMaxWidth()
                .background(if (fontLoaded) FujiSkin.lcd else Color(0xFF321B18))
                .border(1.dp, if (fontLoaded) FujiSkin.lcdGreen else Color(0xFFFF4A3D))
                .padding(8.dp)
        ) {
            Text(
                if (fontLoaded) stringResource(R.string.font_specimen_loaded)
                else stringResource(R.string.font_specimen_missing),
                color = if (fontLoaded) FujiSkin.lcdGreen else Color(0xFFFF8278),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
        Text(
            stringResource(R.string.font_specimen_metadata),
            color = CamColors.Sub,
            fontSize = 9.sp
        )
        glyphRows.forEach { (label, glyphs) ->
            Column(
                Modifier.fillMaxWidth()
                    .background(FujiSkin.lcd)
                    .border(1.dp, Color(0xFF344247))
                    .padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(label, color = FujiSkin.lcdGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(glyphs, color = CamColors.TextColor, fontSize = 13.sp, lineHeight = 21.sp)
            }
        }
        Text(
            stringResource(R.string.font_specimen_fallback_note),
            color = CamColors.Sub,
            fontSize = 9.sp
        )
        Text(
            stringResource(R.string.font_specimen_size_title),
            color = FujiSkin.lcdGreen,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        sizes.forEach { size ->
            Column(
                Modifier.fillMaxWidth()
                    .background(FujiSkin.lcd)
                    .border(1.dp, Color(0xFF344247))
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
                Text(
                    "${size}sp",
                    color = FujiSkin.orange,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    requiredSamples[0],
                    color = CamColors.TextColor,
                    fontSize = size.sp,
                    lineHeight = (size * 1.55f).sp
                )
                Text(
                    requiredSamples[1],
                    color = CamColors.TextColor,
                    fontSize = size.sp,
                    lineHeight = (size * 1.35f).sp
                )
            }
        }
    }
}

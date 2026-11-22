package com.example.camui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Single-line UI label drawn around the font's real OS/2 sCapHeight.
 * The glyph cap is vertically centered; baseline = centerY + capHeight / 2.
 */
@Composable
internal fun FujiCapHeightText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    capHeightSp: Float = 8f,
    bold: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val baseTypeface = FujiTypography.uiTypeface(context)
    val typeface = remember(baseTypeface, bold) {
        if (bold) Typeface.create(baseTypeface, Typeface.BOLD) else baseTypeface
    }
    val metrics = remember(typeface) {
        PixelOsdFontMetrics.load(
            context,
            typeface,
            R.font.saira_semicondensed_semibold
        )
    }
    val capHeightPx = with(density) { capHeightSp.sp.toPx() }.coerceAtLeast(1f)
    val intrinsicWidthPx = remember(text, typeface, capHeightPx) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = metrics.textSizeForCapHeight(capHeightPx)
        }.measureText(text).coerceAtLeast(capHeightPx * 0.5f)
    }
    val minWidth = with(density) { (intrinsicWidthPx + capHeightPx * 0.4f).toDp() }
    val minHeight = with(density) { (capHeightPx * 1.55f).toDp() }

    Canvas(
        modifier = modifier
            .defaultMinSize(minWidth = minWidth, minHeight = minHeight)
            .heightIn(min = minHeight)
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textSize = metrics.textSizeForCapHeight(capHeightPx)
            textAlign = Paint.Align.CENTER
            this.color = color.toArgb()
            style = Paint.Style.FILL
            isDither = true
        }
        val measuredWidth = paint.measureText(text)
        val availableWidth = (size.width - capHeightPx * 0.2f).coerceAtLeast(1f)
        if (measuredWidth > availableWidth) {
            paint.textScaleX = (availableWidth / measuredWidth).coerceIn(0.55f, 1f)
        }
        val centerX = size.width * 0.5f
        val centerY = size.height * 0.5f
        val baseline = centerY + capHeightPx * 0.5f
        drawContext.canvas.nativeCanvas.drawText(text, centerX, baseline, paint)
    }
}

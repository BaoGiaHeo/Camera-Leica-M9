package com.example.camui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** TrueType OS/2 sCapHeight metric used to size OSD text by visible cap height. */
internal data class PixelOsdFontMetrics(
    val unitsPerEm: Int,
    val capHeightUnits: Int,
    val source: String
) {
    fun textSizeForCapHeight(capHeightPx: Float): Float {
        val ratio = capHeightUnits.toFloat() / unitsPerEm.coerceAtLeast(1)
        return (capHeightPx / ratio.coerceAtLeast(0.01f)).coerceAtLeast(1f)
    }

    companion object {
        private const val TAG = "FUJI_OSD_METRICS"

        fun load(context: Context, typeface: Typeface): PixelOsdFontMetrics {
            val parsed = runCatching {
                val bytes = context.resources.openRawResource(R.font.vcr_osd_mono_kk7).use { it.readBytes() }
                parseTrueType(bytes)
            }.getOrNull()
            if (parsed != null) {
                Log.i(
                    TAG,
                    "FONT_OS2 unitsPerEm=" + parsed.unitsPerEm +
                        " sCapHeight=" + parsed.capHeightUnits +
                        " capRatio=" + (parsed.capHeightUnits.toFloat() / parsed.unitsPerEm)
                )
                return parsed
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = 100f
            }
            val bounds = android.graphics.Rect()
            paint.getTextBounds("H", 0, 1, bounds)
            val fallback = PixelOsdFontMetrics(
                unitsPerEm = 1000,
                capHeightUnits = (bounds.height() * 10).coerceAtLeast(1),
                source = "Paint.getTextBounds(H) fallback"
            )
            Log.w(TAG, "FONT_OS2_UNAVAILABLE fallbackCapHeight=" + bounds.height() + " at textSize=100")
            return fallback
        }

        internal fun parseTrueType(bytes: ByteArray): PixelOsdFontMetrics? {
            if (bytes.size < 12) return null
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            val tableCount = buffer.getShort(4).toInt() and 0xFFFF
            var os2Offset = -1
            var os2Length = 0
            var headOffset = -1
            for (index in 0 until tableCount) {
                val record = 12 + index * 16
                if (record + 16 > bytes.size) return null
                val tag = String(bytes, record, 4, Charsets.US_ASCII)
                val offset = buffer.getInt(record + 8).toLong() and 0xFFFFFFFFL
                val length = buffer.getInt(record + 12).toLong() and 0xFFFFFFFFL
                if (offset > Int.MAX_VALUE || length > Int.MAX_VALUE) continue
                when (tag) {
                    "OS/2" -> {
                        os2Offset = offset.toInt()
                        os2Length = length.toInt()
                    }
                    "head" -> headOffset = offset.toInt()
                }
            }
            if (os2Offset < 0 || headOffset < 0 ||
                os2Offset + 90 > bytes.size || headOffset + 20 > bytes.size || os2Length < 90
            ) return null
            val unitsPerEm = buffer.getShort(headOffset + 18).toInt() and 0xFFFF
            val os2Version = buffer.getShort(os2Offset).toInt() and 0xFFFF
            if (os2Version < 2 || unitsPerEm <= 0) return null
            val capHeight = buffer.getShort(os2Offset + 88).toInt()
            if (capHeight <= 0 || capHeight >= unitsPerEm) return null
            return PixelOsdFontMetrics(unitsPerEm, capHeight, "OS/2.sCapHeight")
        }
    }
}

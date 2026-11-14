package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.util.Log

/**
 * Single-frame software tone mapping for devices without OEM HDR Extension or JPEG_R support,
 * plus a conservative 2x unsharp-mask pass when UHD enhancement is enabled.
 *
 * This CPU path streams one row at a time and avoids a second full-size pixel matrix. It improves
 * local tone and edge acutance, but cannot reconstruct clipped detail the sensor never recorded.
 */
fun applySoftwarePhotoEnhancement(
    context: Context,
    uri: Uri,
    softwareHdr: Boolean,
    uhdSharpening: Boolean
): Boolean {
    if (!softwareHdr && !uhdSharpening) return false
    // Prefer the OpenGL ES shader for modest frames; large sensor JPEGs and EGL/driver
    // failures automatically fall back to the bounded row-by-row CPU implementation.
    if (runCatching {
            tryApplyGpuPhotoEnhancement(context, uri, softwareHdr, uhdSharpening)
        }.getOrDefault(false)
    ) return true

    return runCatching {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return@runCatching false
        val working = if (bitmap.isMutable && bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return@runCatching false
        }

        val width = working.width
        val height = working.height
        if (width < 3 || height < 3) return@runCatching false

        // Keep the original neighboring rows in three reusable buffers. The output row is
        // written only after its source neighbors are already cached, avoiding recursive
        // sharpening from rows that were processed a moment earlier.
        var previousRow = IntArray(width)
        var currentRow = IntArray(width)
        var nextRow = IntArray(width)
        val outputRow = IntArray(width)
        val sharpenAmount = if (uhdSharpening) 2.0f else 0f

        fun luminance(pixel: Int): Float {
            val r = android.graphics.Color.red(pixel) / 255f
            val g = android.graphics.Color.green(pixel) / 255f
            val b = android.graphics.Color.blue(pixel) / 255f
            return r * 0.2126f + g * 0.7152f + b * 0.0722f
        }

        fun mapHdr(value: Float): Float {
            val shadowLift = 0.105f * (1f - value) * (1f - value)
            val highlight = ((value - 0.70f) / 0.30f).coerceIn(0f, 1f)
            val highlightCompression = 0.105f * highlight * highlight
            return (value + shadowLift - highlightCompression).coerceIn(0f, 1f)
        }

        working.getPixels(currentRow, 0, width, 0, 0, width, 1)
        currentRow.copyInto(previousRow)
        if (height > 1) {
            working.getPixels(nextRow, 0, width, 0, 1, width, 1)
        } else {
            currentRow.copyInto(nextRow)
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = currentRow[x]
                val alpha = android.graphics.Color.alpha(pixel)
                val red = android.graphics.Color.red(pixel).toFloat()
                val green = android.graphics.Color.green(pixel).toFloat()
                val blue = android.graphics.Color.blue(pixel).toFloat()
                val centerLuma = (red * 0.2126f + green * 0.7152f + blue * 0.0722f) / 255f

                var targetLuma = if (softwareHdr) mapHdr(centerLuma) else centerLuma
                if (uhdSharpening) {
                    val left = luminance(currentRow[(x - 1).coerceAtLeast(0)])
                    val right = luminance(currentRow[(x + 1).coerceAtMost(width - 1)])
                    val up = luminance(previousRow[x])
                    val down = luminance(nextRow[x])
                    val localBlur = (left + right + up + down) * 0.25f
                    val detail = centerLuma - localBlur
                    targetLuma = (targetLuma + detail * sharpenAmount).coerceIn(0f, 1f)
                }

                val scale = if (centerLuma > 0.012f) {
                    targetLuma / centerLuma
                } else {
                    if (targetLuma > centerLuma) 1f + (targetLuma - centerLuma) * 12f else 1f
                }
                outputRow[x] = android.graphics.Color.argb(
                    alpha,
                    (red * scale).toInt().coerceIn(0, 255),
                    (green * scale).toInt().coerceIn(0, 255),
                    (blue * scale).toInt().coerceIn(0, 255)
                )
            }
            working.setPixels(outputRow, 0, width, 0, y, width, 1)

            if (y + 1 < height) {
                val recycled = previousRow
                previousRow = currentRow
                currentRow = nextRow
                nextRow = recycled
                if (y + 2 < height) {
                    working.getPixels(nextRow, 0, width, 0, y + 2, width, 1)
                } else {
                    currentRow.copyInto(nextRow)
                }
            }
        }

        val written = context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            working.compress(Bitmap.CompressFormat.JPEG, 98, output)
        } ?: false
        if (!written) return@runCatching false

        context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }
        }
        Log.i(
            "FUJI_IMAGE_PROCESS",
            "Software HDR=$softwareHdr UHD sharpen=$uhdSharpening size=${width}x${height}"
        )
        true
    }.onFailure {
        Log.e("FUJI_IMAGE_PROCESS", "Enhancement failed; original capture is retained when possible", it)
    }.getOrDefault(false)
}

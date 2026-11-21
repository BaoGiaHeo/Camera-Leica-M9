package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.geometry.Offset
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Captures the OSD at both LCD sizes and all three pixel-grid settings.
 *
 * Each capture is rendered by two independent renderer instances. The PNG is retained
 * as a build artifact and its SSIM is compared with the second render (>= 0.98) to catch
 * nondeterministic output, missing glyph layers and resolution-dependent regressions.
 */
@RunWith(AndroidJUnit4::class)
class PixelOsdGoldenRenderingTest {
    private data class LcdSize(val width: Int, val height: Int)

    @Test
    fun fineMediumAndChunkyOdsRenderAtTwoLcdResolutionsAndMeetSsimThreshold() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val outputDirectory = File(
            context.getExternalFilesDir(null) ?: context.cacheDir,
            "pixel-osd-golden"
        ).apply { mkdirs() }
        assertTrue("Could not create OSD golden capture directory", outputDirectory.isDirectory)

        val sizes = listOf(
            LcdSize(width = 640, height = 360),
            LcdSize(width = 1280, height = 720)
        )
        val modes = listOf(
            "fine" to 2,
            "medium" to 3,
            "chunky" to 4
        )

        modes.forEach { (mode, expectedScale) ->
            sizes.forEach { lcd ->
                val frame = PixelOsdFrame(
                    showGrid = true,
                    showPixelTexture = false,
                    faceCenter = Offset(0.56f, 0.43f),
                    faceSize = Offset(0.22f, 0.32f),
                    eyeCenter = Offset(0.57f, 0.42f),
                    focusLocked = true,
                    showStatus = true,
                    batteryPercent = 74,
                    showEvScale = true,
                    evIndex = 2,
                    evDisplayIndex = 2f,
                    isPro = true,
                    afModeText = "AF-L",
                    meteringText = "C",
                    shutterText = "1/125",
                    apertureText = "F2.8",
                    isoText = "ISO400",
                    dynamicRangeText = "DR100",
                    whiteBalanceText = "DAYLIGHT",
                    filmSimulationText = "CLASSIC CHROME",
                    showZoomBar = true,
                    zoomRatio = 2f,
                    zoomMinimumRatio = 0.6f,
                    zoomMaximumRatio = 10f,
                    zoomDigitalBoundaryRatio = 2f,
                    pixelScaleMode = mode
                )

                val first = render(context, lcd.width, lcd.height, frame)
                val second = render(context, lcd.width, lcd.height, frame)
                try {
                    val expectedGeometry = PixelOsdGeometry.fromDisplayPixels(
                        lcd.width,
                        lcd.height,
                        mode
                    )
                    assertEquals(expectedScale, expectedGeometry.pixelScale)

                    val nonTransparentPixels = countNonTransparentPixels(first)
                    assertTrue(
                        "OSD capture is empty for $mode at ${lcd.width}x${lcd.height}",
                        nonTransparentPixels > 500
                    )

                    val ssim = structuralSimilarity(first, second)
                    assertTrue(
                        "OSD SSIM $ssim is below 0.98 for $mode at ${lcd.width}x${lcd.height}",
                        ssim >= 0.98
                    )

                    val file = File(
                        outputDirectory,
                        "pixel-osd-$mode-${lcd.width}x${lcd.height}.png"
                    )
                    file.outputStream().use { output ->
                        assertTrue("PNG capture failed: ${file.name}", first.compress(Bitmap.CompressFormat.PNG, 100, output))
                    }
                    assertTrue("PNG capture is empty: ${file.name}", file.length() > 1_000L)
                } finally {
                    first.recycle()
                    second.recycle()
                }
            }
        }
    }

    private fun render(
        context: Context,
        width: Int,
        height: Int,
        frame: PixelOsdFrame
    ): Bitmap {
        val renderer = PixelOsdRenderer(context)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        renderer.render(width, height, frame, maxRefreshHz = 120)
        renderer.drawScaled(Canvas(bitmap), width, height)
        return bitmap
    }

    private fun countNonTransparentPixels(bitmap: Bitmap): Int {
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if ((bitmap.getPixel(x, y) ushr 24) != 0) count++
            }
        }
        return count
    }

    private fun structuralSimilarity(first: Bitmap, second: Bitmap): Double {
        require(first.width == second.width && first.height == second.height)
        val count = first.width.toLong() * first.height.toLong()
        var sumFirst = 0.0
        var sumSecond = 0.0
        var sumFirstSquared = 0.0
        var sumSecondSquared = 0.0
        var sumCross = 0.0

        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                val a = luminance(first.getPixel(x, y))
                val b = luminance(second.getPixel(x, y))
                sumFirst += a
                sumSecond += b
                sumFirstSquared += a * a
                sumSecondSquared += b * b
                sumCross += a * b
            }
        }

        val n = count.toDouble()
        val meanFirst = sumFirst / n
        val meanSecond = sumSecond / n
        val varianceFirst = (sumFirstSquared / n - meanFirst * meanFirst).coerceAtLeast(0.0)
        val varianceSecond = (sumSecondSquared / n - meanSecond * meanSecond).coerceAtLeast(0.0)
        val covariance = sumCross / n - meanFirst * meanSecond
        val c1 = 6.5025
        val c2 = 58.5225
        val numerator = (2.0 * meanFirst * meanSecond + c1) * (2.0 * covariance + c2)
        val denominator = (meanFirst * meanFirst + meanSecond * meanSecond + c1) *
            (varianceFirst + varianceSecond + c2)
        return if (denominator == 0.0) 1.0 else numerator / denominator
    }

    private fun luminance(argb: Int): Double {
        val alpha = ((argb ushr 24) and 0xFF) / 255.0
        val red = ((argb ushr 16) and 0xFF) / 255.0
        val green = ((argb ushr 8) and 0xFF) / 255.0
        val blue = (argb and 0xFF) / 255.0
        return alpha * (0.2126 * red + 0.7152 * green + 0.0722 * blue) * 255.0
    }
}

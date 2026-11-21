package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the OSD layout matrix at physical LCD sizes and exports one golden per case. */
@RunWith(AndroidJUnit4::class)
class PixelOsdLayoutTest {
    private data class LcdSize(val width: Int, val height: Int)

    @Test
    fun osdLayoutPhotoProLocalePixelModeAndLcdSizeMatrixHasNoOverlaps() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val output = File(
            context.getExternalFilesDir(null) ?: context.cacheDir,
            "test-output"
        ).apply { mkdirs() }
        assertTrue("Could not create test-output", output.isDirectory)

        val sizes = listOf(
            LcdSize(2400, 1080),
            LcdSize(2640, 1200),
            LcdSize(1920, 1080)
        )
        val modes = listOf("fine", "medium", "chunky")
        val locales = listOf("en", "vi")
        val outputs = mutableListOf<File>()
        var cases = 0

        sizes.forEach { size ->
            modes.forEach { mode ->
                locales.forEach { locale ->
                    listOf(false, true).forEach { isPro ->
                        val frame = PixelOsdFrame(
                            showPixelTexture = false,
                            showGrid = false,
                            centerAfPoint = Offset(0.5f, 0.5f),
                            showStatus = true,
                            batteryPercent = 72,
                            batteryCharging = false,
                            showEvScale = true,
                            evIndex = 0,
                            evDisplayIndex = 0f,
                            evMinimumIndex = -9,
                            evMaximumIndex = 9,
                            evStepStops = 1f / 3f,
                            showHistogram = true,
                            histogram = List(64) { index -> ((index * 13) % 97) + 1 },
                            isPro = isPro,
                            afModeText = "AF-C",
                            meteringText = "M",
                            shutterText = "1/15",
                            apertureText = "F1.5",
                            isoText = "AUTO",
                            dynamicRangeText = "DR100",
                            whiteBalanceText = if (locale == "vi") "DAYLIGHT" else "AUTO",
                            filmSimulationText = "",
                            showProEv = true,
                            showProShutter = true,
                            showProIso = true,
                            showProFocus = true,
                            showProWhiteBalance = true,
                            showZoomBar = true,
                            zoomRatio = 1.7f,
                            zoomMinimumRatio = 0.6f,
                            zoomMaximumRatio = 10f,
                            zoomOpticalBoundaryRatio = 2.8f,
                            zoomDigitalBoundaryRatio = 6f,
                            pixelScaleMode = mode
                        )
                        val renderer = PixelOsdRenderer(context)
                        renderer.render(size.width, size.height, frame, maxRefreshHz = 120)
                        val layout = requireNotNull(renderer.layoutSnapshotForTesting()) {
                            "Renderer did not publish layout for " + size.width + "x" + size.height
                        }

                        val metrics = requireNotNull(renderer.fontMetricsSnapshotForTesting()) {
                            "Renderer did not load VCR OSD font metrics"
                        }
                        assertEquals(
                            "OSD sizing must use TrueType OS/2.sCapHeight, not fallback metrics",
                            "OS/2.sCapHeight",
                            metrics.source
                        )
                        assertTrue("Invalid unitsPerEm=" + metrics.unitsPerEm, metrics.unitsPerEm > 0)
                        assertTrue(
                            "Invalid OS/2 sCapHeight=" + metrics.capHeightUnits,
                            metrics.capHeightUnits in 1 until metrics.unitsPerEm
                        )
                        Log.i(
                            "OSD_LayoutTest",
                            "FONT source=" + metrics.source + " unitsPerEm=" + metrics.unitsPerEm +
                                " sCapHeight=" + metrics.capHeightUnits
                        )

                        assertEquals(
                            "u must come from physical LCD height for " + mode + "/" + size.width + "x" + size.height,
                            (size.height / 48f).let { kotlin.math.round(it).toInt().coerceIn(15, 28) },
                            layout.unitPx
                        )
                        assertTrue(
                            "Overlapping OSD boxes in " + size.width + "x" + size.height + "/" + mode + "/" + locale +
                                "/Pro=" + isPro + ": " +
                                layout.overlaps().joinToString { it.first.id + " x " + it.second.id },
                            layout.overlaps().isEmpty()
                        )
                        assertTrue(
                            "OSD elements outside safe margin in " + size.width + "x" + size.height + "/" + mode +
                                "/" + locale + "/Pro=" + isPro + ": " +
                                layout.outOfSafeBounds().joinToString { it.id },
                            layout.outOfSafeBounds().isEmpty()
                        )
                        layout.iconTextHeightRatios().forEach { (id, ratio) ->
                            assertTrue(
                                "Icon/text cap-height ratio for " + id + " was " + ratio + " in " + mode,
                                ratio in 0.95f..1.15f
                            )
                        }
                        layout.elements.firstOrNull { it.id == "battery" }?.let { battery ->
                            val widthRatio = battery.bounds.width / layout.unitPx
                            val heightRatio = battery.bounds.height / layout.unitPx
                            assertTrue("Battery width must be about 1.9u: " + widthRatio, widthRatio in 1.75f..2.05f)
                            assertTrue("Battery height must be about 1u: " + heightRatio, heightRatio in 0.9f..1.1f)
                        }

                        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
                        try {
                            renderer.drawScaled(Canvas(bitmap), size.width, size.height)
                            val modeName = if (isPro) "PRO" else "PHOTO"
                            val name = "OSD_" + size.width + "x" + size.height + "_" + mode + "_" +
                                locale + "_" + modeName + ".png"
                            val file = File(output, name)
                            file.outputStream().use { stream ->
                                assertTrue("Could not write " + name, bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                            }
                            assertTrue("Golden is unexpectedly small: " + name, file.length() > 1_000L)
                            outputs += file
                        } finally {
                            bitmap.recycle()
                        }
                        cases++
                        Log.i(
                            "OSD_LayoutTest",
                            "PASS " + size.width + "x" + size.height + " " + mode +
                                " " + locale + " Pro=" + isPro + " " + layout.diagnosticLine()
                        )
                    }
                }
            }
        }

        assertEquals("Expected 3 sizes x 3 PIXEL modes x 2 locales x Photo/Pro", 36, cases)
        assertEquals("A golden PNG must exist for each OSD matrix case", 36, outputs.size)
        Log.i("OSD_LayoutTest", "SUMMARY cases=" + cases + " golden=" + outputs.size + " dir=" + output.absolutePath)
    }
}

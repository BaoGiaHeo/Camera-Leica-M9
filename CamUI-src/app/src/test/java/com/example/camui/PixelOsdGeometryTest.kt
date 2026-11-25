package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelOsdGeometryTest {
    @Test
    fun evReadoutShowsOneSignedValueRoundedToOneDecimalPlace() {
        assertEquals("+0.0 EV", formatEvReadout(0f))
        assertEquals("+0.3 EV", formatEvReadout(1f / 3f))
        assertEquals("-1.0 EV", formatEvReadout(-1f))
        assertEquals("+0.0 EV", formatEvReadout(-0.01f))
        assertEquals("+0.0 EV", formatEvReadout(Float.NaN))
    }

    @Test
    fun proHudShowsEVAndWhiteBalanceAsLiveValues() {
        val plan = PixelOsdLayoutEngine.create(
            frame = PixelOsdFrame(
                isPro = true,
                showStatus = true,
                showProEv = true,
                showProWhiteBalance = true,
                evDisplayIndex = 1f,
                evStepStops = 1f / 3f,
                whiteBalanceText = "AUTO",
                whiteBalanceKelvinText = "5500K",
                showProShutter = true,
                showProIso = true,
                showProFocus = true
            ),
            widthPx = 2400,
            heightPx = 1080,
            pixelScale = 2,
            measureTextWidthPx = { value, cap -> value.length * cap * 0.55f }
        )
        val values = plan.elements.filter { it.section == "pro-bottom" }.mapNotNull { it.text }
        assertTrue("EV readout must show compensation", values.contains("0.3"))
        assertTrue("WB readout must show real Kelvin", values.contains("5500K"))
        assertTrue("Pro readouts must not overlap", plan.overlaps().isEmpty())
        assertTrue(
            "Pro readouts must stay inside safe area: " +
                plan.outOfSafeBounds().joinToString { "${it.id}=${it.bounds}" },
            plan.outOfSafeBounds().isEmpty()
        )
    }

    @Test
    fun fineIsTheDefaultAndUsesNativeDisplayPixelResolution() {
        val geometry = PixelOsdGeometry.fromDisplayPixels(1920, 1080)
        assertEquals(1, geometry.pixelScale)
        assertEquals(1920, geometry.logicWidth)
        assertEquals(1080, geometry.logicHeight)
    }

    @Test
    fun mediumAndChunkyUseTheirExplicitGridScale() {
        val medium = PixelOsdGeometry.fromDisplayPixels(1920, 1080, "medium")
        assertEquals(2, medium.pixelScale)
        assertEquals(960, medium.logicWidth)
        assertEquals(540, medium.logicHeight)

        val chunky = PixelOsdGeometry.fromDisplayPixels(1920, 1080, "chunky")
        assertEquals(4, chunky.pixelScale)
        assertEquals(480, chunky.logicWidth)
        assertEquals(270, chunky.logicHeight)
    }

    @Test
    fun middleLeftZoomBarIsVerticalCenteredAndPlacesTeleAboveWide() {
        val frame = PixelOsdFrame(
            showZoomBar = true,
            zoomBarPosition = "middle_left",
            zoomRatio = 2f,
            zoomMinimumRatio = 0.6f,
            zoomMaximumRatio = 10f
        )
        val plan = PixelOsdLayoutEngine.create(
            frame = frame,
            widthPx = 2400,
            heightPx = 1080,
            pixelScale = 2,
            measureTextWidthPx = { text, capHeight -> text.length * capHeight * 0.55f }
        )
        val track = requireNotNull(plan.zoomTrackBounds)
        val tele = requireNotNull(plan.elements.firstOrNull { it.id == "zoom-t" })
        val wide = requireNotNull(plan.elements.firstOrNull { it.id == "zoom-w" })

        assertEquals(1080f * 0.30f, track.height, 0.01f)
        assertEquals(1080f * 0.35f, track.top, 0.01f)
        assertEquals(1080f * 0.65f, track.bottom, 0.01f)
        assertTrue("T must appear above the vertical track", tele.bounds.bottom <= track.top)
        assertTrue("W must appear below the vertical track", wide.bounds.top >= track.bottom)
        assertEquals(
            "Zoom bar should be placed two LCD units to the right of the EV scale",
            plan.evLineX + plan.unitPx * 2f,
            track.left,
            0.01f
        )
    }

    @Test
    fun topCenterZoomBarUsesTheSpecifiedHorizontalFootprint() {
        val plan = PixelOsdLayoutEngine.create(
            frame = PixelOsdFrame(showZoomBar = true, zoomBarPosition = "top_center"),
            widthPx = 2400,
            heightPx = 1080,
            pixelScale = 2,
            measureTextWidthPx = { text, capHeight -> text.length * capHeight * 0.55f }
        )
        val track = requireNotNull(plan.zoomTrackBounds)
        val wide = requireNotNull(plan.elements.firstOrNull { it.id == "zoom-w" })
        val tele = requireNotNull(plan.elements.firstOrNull { it.id == "zoom-t" })

        assertEquals(2400f * 0.18f, track.width, 0.01f)
        assertEquals(plan.unitPx * 0.9f, track.height, 0.01f)
        assertEquals(1200f, (track.left + track.right) / 2f, 0.01f)
        assertTrue(wide.bounds.right <= track.left)
        assertTrue(tele.bounds.left >= track.right)
    }

    @Test
    fun zoomBarAnchorsRemainStableAcrossPixelScalesAndLandscapeLcdSizes() {
        val sizes = listOf(2400 to 1080, 2640 to 1200, 1920 to 1080)
        val scales = listOf(2, 3, 4)
        sizes.forEach { (width, height) ->
            scales.forEach { scale ->
                val top = PixelOsdLayoutEngine.create(
                    frame = PixelOsdFrame(showZoomBar = true, zoomBarPosition = "top_center"),
                    widthPx = width,
                    heightPx = height,
                    pixelScale = scale,
                    measureTextWidthPx = { text, capHeight -> text.length * capHeight * 0.55f }
                ).zoomTrackBounds!!
                assertEquals(width / 2f, (top.left + top.right) / 2f, 1f)
                assertEquals(width * 0.18f, top.width, 1f)

                val left = PixelOsdLayoutEngine.create(
                    frame = PixelOsdFrame(showZoomBar = true, zoomBarPosition = "middle_left"),
                    widthPx = width,
                    heightPx = height,
                    pixelScale = scale,
                    measureTextWidthPx = { text, capHeight -> text.length * capHeight * 0.55f }
                ).zoomTrackBounds!!
                assertEquals(height / 2f, (left.top + left.bottom) / 2f, 1f)
                assertEquals(height * 0.30f, left.height, 1f)
            }
        }
    }

    @Test
    fun histogramIsPlacedBelowResolutionAndFpsWithinTheEvScaleTopBoundary() {
        val sizes = listOf(2400 to 1080, 2640 to 1200, 1920 to 1080)
        val scales = listOf(2, 3, 4)
        sizes.forEach { (width, height) ->
            scales.forEach { scale ->
                val plan = PixelOsdLayoutEngine.create(
                    frame = PixelOsdFrame(
                        showHistogram = true,
                        histogram = listOf(1, 3, 5, 2),
                        previewFps = 59.94f
                    ),
                    widthPx = width,
                    heightPx = height,
                    pixelScale = scale,
                    measureTextWidthPx = { text, capHeight -> text.length * capHeight * 0.55f }
                )
                val histogram = requireNotNull(plan.histogramBounds)
                val fps = requireNotNull(plan.elements.firstOrNull { it.id == "status-fps" })
                assertTrue(histogram.width > 0f)
                assertTrue(histogram.height > 0f)
                assertTrue(histogram.left > plan.evLineX)
                assertTrue(histogram.top > plan.evBottom)
                assertTrue(histogram.right <= width - plan.safeMarginPx)
                assertTrue(histogram.bottom <= height - plan.safeMarginPx)
                assertTrue(histogram.top >= fps.bounds.bottom)
            }
        }
    }

    @Test
    fun manualTouchOverrideKeepsAfReticleVisibleOverFaceBox() {
        assertTrue(shouldDrawCenterAfReticle(
            centerAfPointAvailable = true,
            faceBoxVisible = true,
            manualOverrideActive = true
        ))
        assertTrue(shouldDrawCenterAfReticle(
            centerAfPointAvailable = true,
            faceBoxVisible = true,
            manualOverrideActive = false
        ))
        assertTrue(shouldDrawCenterAfReticle(
            centerAfPointAvailable = true,
            faceBoxVisible = false,
            manualOverrideActive = false
        ))
        assertFalse(shouldDrawCenterAfReticle(
            centerAfPointAvailable = false,
            faceBoxVisible = false,
            manualOverrideActive = true
        ))
        assertTrue(shouldDrawCenterAfReticle(
            centerAfPointAvailable = true,
            faceBoxVisible = false,
            manualOverrideActive = false
        ))
    }

    @Test
    fun afAndEyeReticlesUseScreenRelativeSizeAndReadableStroke() {
        assertEquals(32, centerAfReticleHalfSize(540))
        assertEquals(19, eyeReticleHalfSize(22, 2))
        assertEquals(2, afReticleStrokeWidth(22, 2))
    }

    @Test
    fun normalizedCoordinatesAreRoundedToIntegerLogicPixels() {
        val geometry = PixelOsdGeometry.fromDisplayPixels(1920, 1080, "fine")
        assertEquals(0, geometry.x(0f))
        assertEquals(960, geometry.x(0.5f))
        assertEquals(1919, geometry.x(1f))
        assertEquals(0, geometry.y(0f))
        assertEquals(540, geometry.y(0.5f))
        assertEquals(1079, geometry.y(1f))
    }

    @Test
    fun normalizedCoordinatesClampOutsideTheDisplayArea() {
        val geometry = PixelOsdGeometry.fromDisplayPixels(1600, 900, "fine")
        assertEquals(0, geometry.x(-0.2f))
        assertEquals(geometry.logicWidth - 1, geometry.x(1.2f))
        assertEquals(0, geometry.y(-0.2f))
        assertEquals(geometry.logicHeight - 1, geometry.y(1.2f))
        assertTrue(geometry.pixelScale == 1)
    }

    @Test
    fun unknownPixelModeSafelyFallsBackToFine() {
        assertEquals(
            1,
            PixelOsdGeometry.fromDisplayPixels(1920, 1080, "unknown").pixelScale
        )
    }
}

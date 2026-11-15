package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoQualityAnalyzerTest {
    @Test
    fun uniformWhiteImageFlagsHighlightClipping() {
        val pixels = IntArray(16 * 16) { 0xFFFFFFFF.toInt() }
        val report = PhotoQualityAnalyzer.analyzePixels(16, 16, pixels)

        assertTrue(report.highlightClippingLabel.startsWith("100.00%"))
        assertTrue(report.recommendation.contains("highlight"))
    }

    @Test
    fun uniformBlackImageFlagsShadowClippingAndSoftEdges() {
        val pixels = IntArray(16 * 16) { 0xFF000000.toInt() }
        val report = PhotoQualityAnalyzer.analyzePixels(16, 16, pixels)

        assertTrue(report.shadowClippingLabel.startsWith("100.00%"))
        assertTrue(report.sharpnessLabel.startsWith("Thấp"))
        assertTrue(report.recommendation.contains("bệt đen"))
    }

    @Test
    fun texturedCheckerPatternHasMoreEdgeEnergyThanFlatImage() {
        val width = 64
        val height = 64
        val flat = IntArray(width * height) { 0xFF808080.toInt() }
        val checker = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            val value = if (((x / 4) + (y / 4)) % 2 == 0) 32 else 224
            (0xFF shl 24) or (value shl 16) or (value shl 8) or value
        }

        val flatReport = PhotoQualityAnalyzer.analyzePixels(width, height, flat)
        val checkerReport = PhotoQualityAnalyzer.analyzePixels(width, height, checker)

        assertTrue(checkerReport.sharpnessLabel.startsWith("Cao"))
        assertTrue(flatReport.sharpnessLabel.startsWith("Thấp"))
    }

    @Test
    fun invalidPixelBufferReturnsInsufficientData() {
        val report = PhotoQualityAnalyzer.analyzePixels(20, 20, IntArray(10))
        assertEquals("Không đủ dữ liệu", report.sharpnessLabel)
    }
}

package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LunaDreamTilePlannerTest {
    @Test
    fun threePointTwoKByTwentyFourHundredTwelveTileTestProducesCompleteCoreCoverage() {
        val plan = LunaDreamTilePlanner.plan(3_200, 2_400, 512, 32)
        assertEquals(7, plan.columns)
        assertEquals(5, plan.rows)
        assertEquals(35, plan.tileCount)
        val totalCorePixels = plan.tiles.sumOf {
            it.core.width.toLong() * it.core.height
        }
        assertEquals(7_680_000L, totalCorePixels)
        assertTrue(plan.tiles.all {
            it.expanded.left <= it.core.left &&
                it.expanded.top <= it.core.top &&
                it.expanded.right >= it.core.right &&
                it.expanded.bottom >= it.core.bottom
        })
    }

    @Test
    fun twoHundredMegapixelCanvasIsPlannedWithoutFullFrameAllocation() {
        val plan = LunaDreamTilePlanner.plan(16_320, 12_240, 512, 32)
        assertEquals(32, plan.columns)
        assertEquals(24, plan.rows)
        assertEquals(768, plan.tileCount)
        assertTrue(plan.largestWorkingTilePixels <= 576L * 576L)
        val estimated = LunaDreamTilePlanner.estimatedWorkingSetBytes(plan, concurrentTiles = 4)
        assertTrue("estimated tile working set must remain below 800 MB", estimated < 800L * 1024L * 1024L)
    }

    @Test
    fun thermalStatusReducesParallelTileCount() {
        assertEquals(4, LunaDreamTilePlanner.recommendedConcurrency(android.os.PowerManager.THERMAL_STATUS_NONE))
        assertEquals(2, LunaDreamTilePlanner.recommendedConcurrency(android.os.PowerManager.THERMAL_STATUS_MODERATE))
        assertEquals(1, LunaDreamTilePlanner.recommendedConcurrency(android.os.PowerManager.THERMAL_STATUS_SEVERE))
    }

    @Test
    fun outputMapperPreservesCropAndMirrorsHorizontalCoordinate() {
        val crop = TileRect(100, 200, 1_100, 700)
        val normal = LunaDreamOutputMapper(1_600, 1_200, crop)
        val mirrored = LunaDreamOutputMapper(1_600, 1_200, crop, mirrorHorizontally = true)
        val point = normal.mapOutputPixel(0, 0, 1_000, 500)
        val mirrorPoint = mirrored.mapOutputPixel(0, 0, 1_000, 500)
        assertTrue(point.first < crop.left + 2f)
        assertTrue(mirrorPoint.first > crop.right - 2f)
        assertTrue(point.second < crop.top + 2f)
    }

    @Test
    fun outputMapperRotatesIntoSourceCoordinatesWithoutLeavingCrop() {
        val crop = TileRect(0, 0, 1_000, 600)
        val mapper = LunaDreamOutputMapper(1_000, 600, crop, clockwiseRotationDegrees = 90)
        val point = mapper.mapOutputPixel(0, 0, 600, 1_000)
        assertTrue(point.first in 0f..1_000f)
        assertTrue(point.second in 0f..600f)
        assertTrue(point.first > 990f)
        assertTrue(point.second < 10f)
    }
}

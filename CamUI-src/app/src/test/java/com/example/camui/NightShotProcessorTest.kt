package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NightShotProcessorTest {
    @Test
    fun middleDetailStripeIncludesItsBottomHaloRow() {
        val plan = lunaDreamDetailStripeReadPlan(top = 64, rowCount = 64, imageHeight = 1024)
        assertEquals(65, plan.sourceRowsToRead)
        assertTrue(!plan.duplicateFinalRow)
    }

    @Test
    fun finalFullHeightStripeDoesNotReadPastBitmap() {
        val plan = lunaDreamDetailStripeReadPlan(top = 960, rowCount = 64, imageHeight = 1024)
        assertEquals(64, plan.sourceRowsToRead)
        assertTrue(plan.duplicateFinalRow)
    }

    @Test
    fun finalPartialDetailStripeDuplicatesItsLastValidRow() {
        val plan = lunaDreamDetailStripeReadPlan(top = 960, rowCount = 40, imageHeight = 1000)
        assertEquals(40, plan.sourceRowsToRead)
        assertTrue(plan.duplicateFinalRow)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDetailStripeBoundsAreRejected() {
        lunaDreamDetailStripeReadPlan(top = 990, rowCount = 20, imageHeight = 1000)
    }
}

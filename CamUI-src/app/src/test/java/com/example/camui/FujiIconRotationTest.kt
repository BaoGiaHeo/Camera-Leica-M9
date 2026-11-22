package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FujiIconRotationTest {
    @Test
    fun orientationUsesTwelveDegreeHysteresisAroundQuadrantBoundary() {
        val tracker = FujiIconOrientationTracker()
        assertEquals(0, tracker.update(44))
        assertEquals(0, tracker.update(56))
        assertEquals(90, tracker.update(57))
        assertEquals(90, tracker.update(46))
        assertEquals(0, tracker.update(33))
    }

    @Test
    fun orientationWrapsAcrossZeroWithoutAFullTurn() {
        val tracker = FujiIconOrientationTracker(initialQuadrant = 3)
        assertEquals(270, tracker.update(359))
        assertEquals(270, tracker.update(15))
        assertEquals(0, tracker.update(57))
    }

    @Test
    fun invalidOrientationDoesNotChangeCurrentQuadrant() {
        val tracker = FujiIconOrientationTracker(initialQuadrant = 1)
        assertNull(tracker.update(-1))
        assertNull(tracker.update(360))
        assertEquals(90, tracker.update(90))
    }

    @Test
    fun iconRotationSubtractsCurrentLandscapeWindowRotation() {
        assertEquals(0, iconRotationRelativeToWindow(90, 1))
        assertEquals(180, iconRotationRelativeToWindow(0, 2))
        assertEquals(270, iconRotationRelativeToWindow(0, 1))
        assertEquals(90, iconRotationRelativeToWindow(0, 3))
    }
}

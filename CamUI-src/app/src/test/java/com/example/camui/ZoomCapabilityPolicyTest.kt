package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomCapabilityPolicyTest {
    @Test
    fun tenTimesTelephotoStopsAtTheOpticalBoundaryWithoutDigitalCrop() {
        val assessment = assessOpticalZoom(listOf(26f, 260f), mainEquivalent35Mm = 26f)
        val plan = resolveZoomExecutionPlan(100f, 0.6f, 60f, assessment)
        assertEquals(10f, plan.cameraControlZoomRatio, 0.0001f)
        assertEquals(1f, plan.digitalCropRatio, 0.0001f)
        assertEquals(10f, plan.totalSelectableMaximum, 0.0001f)
        assertEquals(10f, plan.effectiveZoomRatio, 0.0001f)
    }

    @Test
    fun telephotoMaximumIsClampedByTheActiveCameraApiUpperBound() {
        val assessment = assessOpticalZoom(listOf(26f, 130f), mainEquivalent35Mm = 26f)
        val plan = resolveZoomExecutionPlan(40f, 1f, 4f, assessment)
        assertEquals(4f, plan.cameraControlZoomRatio, 0.0001f)
        assertEquals(1f, plan.digitalCropRatio, 0.0001f)
        assertEquals(4f, plan.totalSelectableMaximum, 0.0001f)
        assertEquals(4f, plan.effectiveZoomRatio, 0.0001f)
    }

    @Test
    fun nonTeleCameraNeverExceedsTwoTimesAndUsesCropWhenHalStopsEarlier() {
        val assessment = assessOpticalZoom(listOf(26f), mainEquivalent35Mm = 26f)
        val plan = resolveZoomExecutionPlan(2f, 1f, 1f, assessment)
        assertEquals(1f, plan.cameraControlZoomRatio, 0.0001f)
        assertEquals(2f, plan.digitalCropRatio, 0.0001f)
        assertEquals(2f, plan.totalSelectableMaximum, 0.0001f)

        val capped = resolveZoomExecutionPlan(2.5f, 1f, 10f, assessment)
        assertEquals(2f, capped.effectiveZoomRatio, 0.0001f)
        assertEquals(2f, capped.cameraControlZoomRatio, 0.0001f)
    }

    @Test
    fun oneCameraIdWithMultipleFocalLengthsUsesTheFullEquivalentRange() {
        val range = calculateEquivalentFocalRange35Mm(
            focalLengthsMm = listOf(4.5f, 13.5f),
            sensorWidthMm = 6.4f,
            sensorHeightMm = 4.8f
        )
        val expectedShortest = 4.5f * 43.27f / 8f
        val expectedLongest = 13.5f * 43.27f / 8f
        val shortest = checkNotNull(range.shortest)
        val longest = checkNotNull(range.longest)
        assertEquals(expectedShortest, shortest, 0.001f)
        assertEquals(expectedLongest, longest, 0.001f)
        val assessment = assessOpticalZoom(
            listOf(shortest, longest),
            mainEquivalent35Mm = shortest
        )
        assertTrue(assessment.hasOpticalTelephoto)
        assertEquals(3f, assessment.opticalRatio!!, 0.001f)
    }

    @Test
    fun invalidSensorGeometryDoesNotInventEquivalentFocalLengths() {
        val range = calculateEquivalentFocalRange35Mm(
            focalLengthsMm = listOf(4.5f, 13.5f),
            sensorWidthMm = 0f,
            sensorHeightMm = 4.8f
        )
        assertEquals(null, range.shortest)
        assertEquals(null, range.longest)
    }

    @Test
    fun cameraWithoutTelephotoIsCappedAtTwoTimesEvenWhenHalReportsMore() {
        val assessment = assessOpticalZoom(listOf(13f, 26f), mainEquivalent35Mm = 26f)
        assertFalse(assessment.hasOpticalTelephoto)
        assertEquals(2f, selectableCameraZoomMax(12f, assessment), 0.0001f)
    }

    @Test
    fun threeTimesTelephotoPassesTheOnePointEightThreshold() {
        val assessment = assessOpticalZoom(listOf(26f, 78f), mainEquivalent35Mm = 26f)
        assertTrue(assessment.hasOpticalTelephoto)
        assertEquals(3f, assessment.opticalRatio!!, 0.0001f)
        assertEquals(12f, selectableCameraZoomMax(12f, assessment), 0.0001f)
    }

    @Test
    fun periscopeUsesTheTenTimesOpticalBoundaryEvenWhenHalAdvertisesMore() {
        val assessment = assessOpticalZoom(listOf(26f, 260f), mainEquivalent35Mm = 26f)
        assertTrue(assessment.hasOpticalTelephoto)
        assertEquals(10f, assessment.opticalRatio!!, 0.0001f)
        assertEquals(10f, selectableCameraZoomMax(80f, assessment), 0.0001f)
        assertEquals(10f, maxTotalSelectableZoom(80f, assessment), 0.0001f)
        assertEquals(4f, maxTotalSelectableZoom(4f, assessment), 0.0001f)
    }

    @Test
    fun fiveTimesPeriscopeUsesFiveTimesAsTheHardMaximum() {
        val assessment = assessOpticalZoom(listOf(24f, 120f), mainEquivalent35Mm = 24f)
        assertTrue(assessment.hasOpticalTelephoto)
        assertEquals(5f, assessment.opticalRatio!!, 0.0001f)
        val plan = resolveZoomExecutionPlan(8f, 0.6f, 20f, assessment)
        assertEquals(5f, plan.totalSelectableMaximum, 0.0001f)
        assertEquals(5f, plan.cameraControlZoomRatio, 0.0001f)
        assertEquals(1f, plan.digitalCropRatio, 0.0001f)
    }

    @Test
    fun opticalBoundaryIsFlooredToOneDecimalAndNeverRoundedUp() {
        val assessment = assessOpticalZoom(listOf(24f, 119.9f), mainEquivalent35Mm = 24f)
        assertEquals(4.9f, assessment.opticalRatio!!, 0.0001f)
        assertEquals(4.9f, maxTotalSelectableZoom(10f, assessment), 0.0001f)
    }

    @Test
    fun rejectsInvalidLensValuesAndInvalidCameraMaximum() {
        val assessment = assessOpticalZoom(listOf(Float.NaN, -1f, 26f))
        assertFalse(assessment.hasOpticalTelephoto)
        assertNotNull(assessment.mainEquivalent35Mm)
        assertEquals(1f, selectableCameraZoomMax(Float.NaN, assessment), 0.0001f)
    }

    @Test
    fun opticalZoomBoundaryComesFromTheLongestPhysicalLensNotTheHalLimit() {
        val tele = assessOpticalZoom(listOf(13f, 26f, 78f), mainEquivalent35Mm = 26f)
        assertEquals(3f, opticalZoomBoundaryRatio(tele), 0.0001f)
        assertEquals(3f, opticalZoomBoundaryRatio(tele, minimumZoomRatio = 0.6f), 0.0001f)

        val noTele = assessOpticalZoom(listOf(13f, 26f), mainEquivalent35Mm = 26f)
        assertEquals(1f, opticalZoomBoundaryRatio(noTele), 0.0001f)
    }

    @Test
    fun opticalBoundaryNeverFallsBelowCameraMinimum() {
        val assessment = assessOpticalZoom(listOf(26f), mainEquivalent35Mm = 26f)
        assertEquals(0.6f, opticalZoomBoundaryRatio(assessment, 0.6f), 0.0001f)
        assertEquals(1f, opticalZoomBoundaryRatio(assessment), 0.0001f)
    }

    @Test
    fun zoomBarUsesLogarithmicPosition() {
        assertEquals(0f, logarithmicZoomPosition(0.6f, 0.6f, 100f), 0.0001f)
        assertEquals(1f, logarithmicZoomPosition(100f, 0.6f, 100f), 0.0001f)
        val midpoint = logarithmicZoomPosition( sqrtProduct(0.6f, 100f), 0.6f, 100f)
        assertEquals(0.5f, midpoint, 0.0001f)
    }

    private fun sqrtProduct(a: Float, b: Float) = kotlin.math.sqrt(a * b)
    @Test
    fun nonTeleCameraMovesFromOneToTwoTimesInAboutOnePointTwoSeconds() {
        val zoom = advanceLogarithmicZoomRatio(1f, 1, 0.6f, 2f, 0.05f, 1.2f)
        var current = 1f
        repeat(24) {
            current = advanceLogarithmicZoomRatio(current, 1, 0.6f, 2f, 0.05f, 1.2f)
        }
        assertTrue(zoom > 1f)
        assertEquals(2f, current, 0.0001f)
    }

    @Test
    fun opticalZoomOutFromTenTimesCompletesTheSelectedRange() {
        var current = 10f
        repeat(130) {
            val next = advanceLogarithmicZoomRatio(
                currentZoom = current,
                direction = -1,
                minimumRatio = 0.6f,
                maximumRatio = 10f,
                deltaSeconds = 0.05f,
                traversalSeconds = 6.5f
            )
            assertTrue(next <= current)
            current = next
        }
        assertEquals(0.6f, current, 0.0002f)
    }

    @Test
    fun zoomHoldIsMonotonicAndStopsAtItsRequestedLimits() {
        var zoomIn = 1f
        repeat(200) {
            val next = advanceLogarithmicZoomRatio(zoomIn, 1, 0.6f, 10f, 0.05f, 6.5f)
            assertTrue(next >= zoomIn)
            zoomIn = next
        }
        assertEquals(10f, zoomIn, 0.0001f)

        var zoomOut = 1f
        repeat(200) {
            val next = advanceLogarithmicZoomRatio(zoomOut, -1, 0.6f, 10f, 0.05f, 6.5f)
            assertTrue(next <= zoomOut)
            zoomOut = next
        }
        assertEquals(0.6f, zoomOut, 0.0001f)
    }

}

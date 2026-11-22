package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessingModeControllerTest {
    @Test
    fun enablingEachPrimaryModeDisablesAllConflictingModes() {
        val base = ProcessingModeSnapshot(
            lunaDreamMode = "luna15",
            nightShotMode = "star_trails",
            hdrToneMappingEnabled = true,
            nativeProcessingEnabled = true,
            livePhotoEnabled = true
        )
        val cases = listOf(
            ProcessingPrimaryMode.LUNA_DREAM_15,
            ProcessingPrimaryMode.LUNA_DREAM_20,
            ProcessingPrimaryMode.NIGHT_SHOT,
            ProcessingPrimaryMode.HDR_TONE_MAPPING,
            ProcessingPrimaryMode.NATIVE_PROCESSING
        )
        cases.forEach { selected ->
            val next = ProcessingModeController.transition(base, selected, true)
            assertEquals(selected, next.activePrimaryMode())
            val activeFlags = listOf(
                next.lunaDreamMode != "off",
                next.nightShotMode != "off",
                next.hdrToneMappingEnabled,
                next.nativeProcessingEnabled
            ).count { it }
            assertEquals("Only one primary processor must remain active for $selected", 1, activeFlags)
        }
    }

    @Test
    fun enablingLunaDream20OrNightShotDisablesLivePhoto() {
        val current = ProcessingModeSnapshot(livePhotoEnabled = true)
        val luna20 = ProcessingModeController.transition(
            current, ProcessingPrimaryMode.LUNA_DREAM_20, true
        )
        val night = ProcessingModeController.transition(
            current, ProcessingPrimaryMode.NIGHT_SHOT, true, nightShotValue = "star_trails"
        )
        assertFalse(luna20.livePhotoEnabled)
        assertFalse(night.livePhotoEnabled)
        assertTrue(ProcessingModeController.livePhotoLocked(luna20))
        assertTrue(ProcessingModeController.livePhotoLocked(night))
    }

    @Test
    fun disablingActiveModeDoesNotRestorePreviousProcessor() {
        val first = ProcessingModeController.transition(
            ProcessingModeSnapshot(lunaDreamMode = "luna15"),
            ProcessingPrimaryMode.NIGHT_SHOT,
            true
        )
        val off = ProcessingModeController.transition(
            first,
            ProcessingPrimaryMode.NIGHT_SHOT,
            false
        )
        assertEquals("off", off.lunaDreamMode)
        assertEquals("off", off.nightShotMode)
        assertEquals(ProcessingPrimaryMode.NONE, off.activePrimaryMode())
    }

    @Test
    fun activeModeLocksAllOtherPrimaryModesAndProvidesReason() {
        val state = ProcessingModeSnapshot(lunaDreamMode = "luna20")
        assertFalse(ProcessingModeController.isLocked(state, ProcessingPrimaryMode.LUNA_DREAM_20))
        assertTrue(ProcessingModeController.isLocked(state, ProcessingPrimaryMode.HDR_TONE_MAPPING))
        assertEquals(
            ProcessingModeLockReason.LUNA_DREAM_20,
            ProcessingModeController.lockReason(state, ProcessingPrimaryMode.NIGHT_SHOT)
        )
        assertNull(ProcessingModeController.lockReason(ProcessingModeSnapshot(), ProcessingPrimaryMode.NATIVE_PROCESSING))
    }

    @Test
    fun nightShotValueIsPreservedForStarTrailsAndStreetModes() {
        val stars = ProcessingModeController.transition(
            ProcessingModeSnapshot(), ProcessingPrimaryMode.NIGHT_SHOT, true, "star_trails"
        )
        val street = ProcessingModeController.transition(
            ProcessingModeSnapshot(), ProcessingPrimaryMode.NIGHT_SHOT, true, "street_trails"
        )
        assertEquals("star_trails", stars.nightShotMode)
        assertEquals("street_trails", street.nightShotMode)
    }
}

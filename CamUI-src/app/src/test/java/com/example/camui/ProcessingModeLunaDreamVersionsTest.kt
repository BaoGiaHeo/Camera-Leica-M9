package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessingModeLunaDreamVersionsTest {
    @Test
    fun eachLunaDreamModeIsPersistableAndExclusive() {
        val modes = listOf(
            ProcessingPrimaryMode.LUNA_DREAM_15 to "luna15",
            ProcessingPrimaryMode.LUNA_DREAM_20 to "luna20",
            ProcessingPrimaryMode.LUNA_DREAM_25 to "luna25",
            ProcessingPrimaryMode.LUNA_DREAM_30 to "luna30"
        )
        modes.forEach { (primary, stored) ->
            val enabled = ProcessingModeController.transition(
                ProcessingModeSnapshot(nightShotMode = "night", hdrToneMappingEnabled = true),
                primary,
                enabled = true
            )
            assertEquals(stored, enabled.lunaDreamMode)
            assertEquals("off", enabled.nightShotMode)
            assertTrue(!enabled.hdrToneMappingEnabled)
            assertEquals(primary, enabled.activePrimaryMode())
            assertTrue(ProcessingModeController.livePhotoLocked(enabled))
            val disabled = ProcessingModeController.transition(enabled, primary, enabled = false)
            assertEquals("off", disabled.lunaDreamMode)
        }
    }
}

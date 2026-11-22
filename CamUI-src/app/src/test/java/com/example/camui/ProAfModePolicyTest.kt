package com.example.camui

import android.hardware.camera2.CameraCharacteristics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProAfModePolicyTest {
    @Test
    fun mapsAfSAndAfCToSupportedCamera2Modes() {
        val modes = intArrayOf(
            CameraCharacteristics.CONTROL_AF_MODE_AUTO,
            CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
            CameraCharacteristics.CONTROL_AF_MODE_OFF
        )
        assertEquals(
            CameraCharacteristics.CONTROL_AF_MODE_AUTO,
            camera2AfModeForProSelection("AF-S", modes)
        )
        assertEquals(
            CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
            camera2AfModeForProSelection("AF-C", modes)
        )
        assertEquals(
            CameraCharacteristics.CONTROL_AF_MODE_OFF,
            camera2AfModeForProSelection("MF", modes)
        )
    }

    @Test
    fun continuousFocusFallsBackToContinuousVideoOnlyWhenAdvertised() {
        assertEquals(
            CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO,
            camera2AfModeForProSelection(
                "AF-C",
                intArrayOf(CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            )
        )
    }

    @Test
    fun unsupportedModesAreNotInvented() {
        assertNull(camera2AfModeForProSelection("AF-S", intArrayOf(0)))
        assertNull(camera2AfModeForProSelection("MF", null))
        assertNull(camera2AfModeForProSelection("UNKNOWN", intArrayOf(0, 1, 2, 3, 4)))
    }
}

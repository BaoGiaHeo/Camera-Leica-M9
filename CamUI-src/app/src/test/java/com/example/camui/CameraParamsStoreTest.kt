package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraParamsStoreTest {
    @Test
    fun cameraIdsHaveIndependentStableNamespaces() {
        val idA = CameraParamsStore.keyPrefix("0")
        val idB = CameraParamsStore.keyPrefix("1")
        val idUnicode = CameraParamsStore.keyPrefix("camera/超広角")
        assertNotEquals(idA, idB)
        assertNotEquals(idA, idUnicode)
        assertEquals(idA, CameraParamsStore.keyPrefix("0"))
    }

    @Test
    fun aSnapshotRoundTripsEveryCaptureParameter() {
        val expected = CameraParamsSnapshot(
            evIndex = -2,
            manualExposure = true,
            manualIsoEnabled = true,
            manualShutterEnabled = true,
            manualIso = 1_600,
            manualShutterNs = 25_000_000L,
            manualWhiteBalanceEnabled = true,
            whiteBalanceKelvin = 3_800,
            afMode = "MF",
            manualFocusDiopters = 2.25f,
            zoomRatio = 2.4f,
            meteringIndex = 2,
            touchAfEnabled = true,
            afLocked = true
        )
        val saved = CameraParamsStore.toPreferenceEntries("0", expected)
        val actual = CameraParamsStore.load(saved, "0")
        assertEquals(expected, actual)
    }

    @Test
    fun switchingCameraIdsNeverReadsTheOtherCamerasState() {
        val cameraA = CameraParamsStore.toPreferenceEntries(
            "0",
            CameraParamsSnapshot(evIndex = -3, manualIso = 3_200, afMode = "MF", zoomRatio = 3f)
        )
        val cameraB = CameraParamsStore.toPreferenceEntries(
            "2",
            CameraParamsSnapshot(evIndex = 1, manualIso = 200, afMode = "AF-C", zoomRatio = 1.4f)
        )
        val combined = cameraA + cameraB
        assertEquals(-3, CameraParamsStore.load(combined, "0").evIndex)
        assertEquals(3_200, CameraParamsStore.load(combined, "0").manualIso)
        assertEquals("MF", CameraParamsStore.load(combined, "0").afMode)
        assertEquals(1, CameraParamsStore.load(combined, "2").evIndex)
        assertEquals(200, CameraParamsStore.load(combined, "2").manualIso)
        assertEquals("AF-C", CameraParamsStore.load(combined, "2").afMode)
    }

    @Test
    fun legacyGlobalPreferencesMigrateAsFallbackOnly() {
        val legacy = mapOf<String, Any?>(
            "pro_ev_index" to -1,
            "pro_manual_iso_enabled" to true,
            "pro_manual_iso" to 800,
            "pro_manual_wb_enabled" to true,
            "pro_manual_wb_kelvin" to 4_200,
            "pro_af_mode" to "AF-C",
            "camera_zoom_ratio" to 1.8f
        )
        val migrated = CameraParamsStore.load(legacy, "logical-camera-0")
        assertEquals(-1, migrated.evIndex)
        assertTrue(migrated.manualIsoEnabled)
        assertEquals(800, migrated.manualIso)
        assertTrue(migrated.manualWhiteBalanceEnabled)
        assertEquals(4_200, migrated.whiteBalanceKelvin)
        assertEquals("AF-C", migrated.afMode)
        assertEquals(1.8f, migrated.zoomRatio, 0.0001f)

        val cameraSpecific = legacy + CameraParamsStore.toPreferenceEntries(
            "logical-camera-0",
            CameraParamsSnapshot(evIndex = 2, manualIso = 100)
        )
        assertEquals(2, CameraParamsStore.load(cameraSpecific, "logical-camera-0").evIndex)
        assertEquals(-1, CameraParamsStore.load(cameraSpecific, "unseen-camera").evIndex)
    }

    @Test
    fun corruptValuesAreSanitizedToSafeCameraRanges() {
        val prefix = CameraParamsStore.keyPrefix("0")
        val invalid = mapOf<String, Any?>(
            prefix + "manual_iso" to -1,
            prefix + "manual_shutter_ns" to 0L,
            prefix + "wb_kelvin" to 99_000,
            prefix + "af_mode" to "UNSUPPORTED",
            prefix + "manual_focus_diopters" to Float.NaN,
            prefix + "zoom_ratio" to Float.NaN,
            prefix + "metering" to 99
        )
        val clean = CameraParamsStore.load(invalid, "0")
        assertEquals(50, clean.manualIso)
        assertEquals(1_000_000L, clean.manualShutterNs)
        assertEquals(10_000, clean.whiteBalanceKelvin)
        assertEquals("AF-S", clean.afMode)
        assertEquals(0f, clean.manualFocusDiopters, 0f)
        assertEquals(1f, clean.zoomRatio, 0f)
        assertEquals(3, clean.meteringIndex)
    }
}

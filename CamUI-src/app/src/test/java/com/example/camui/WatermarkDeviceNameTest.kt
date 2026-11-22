package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class WatermarkDeviceNameTest {
    @Test
    fun customNameHasHighestPriority() {
        assertEquals(
            "My Camera",
            resolveWatermarkDeviceName(
                customName = " My Camera ",
                marketName = "Market name",
                globalDeviceName = "Device name",
                manufacturer = "Xiaomi",
                model = "25053RT47C"
            )
        )
    }

    @Test
    fun marketNameHasPriorityOverGlobalDeviceName() {
        assertEquals(
            "REDMI Turbo 4 Pro",
            resolveWatermarkDeviceName(
                customName = "",
                marketName = "REDMI Turbo 4 Pro",
                globalDeviceName = "My phone",
                manufacturer = "Xiaomi",
                model = "25053RT47C"
            )
        )
    }

    @Test
    fun globalDeviceNameIsUsedBeforeEmbeddedModelMap() {
        assertEquals(
            "Gia Bao Phone",
            resolveWatermarkDeviceName(
                customName = null,
                marketName = null,
                globalDeviceName = "Gia Bao Phone",
                manufacturer = "Xiaomi",
                model = "25053RT47C"
            )
        )
    }

    @Test
    fun embeddedPopularModelNameReplacesRawProductCode() {
        assertEquals(
            "REDMI Turbo 4 Pro",
            resolveWatermarkDeviceName(
                customName = null,
                marketName = null,
                globalDeviceName = null,
                manufacturer = "Xiaomi",
                model = "25053RT47C"
            )
        )
    }

    @Test
    fun missingMarketingManufacturerAndModelValuesAreHidden() {
        assertEquals("", resolveWatermarkDeviceName(null, null, null, null, null))
    }

    @Test
    fun fallbackCombinesManufacturerAndNormalizedModel() {
        assertEquals(
            "Google Pixel 9 Pro",
            resolveWatermarkDeviceName(
                customName = null,
                marketName = null,
                globalDeviceName = null,
                manufacturer = "Google",
                model = "Google Pixel 9 Pro"
            )
        )
    }
}

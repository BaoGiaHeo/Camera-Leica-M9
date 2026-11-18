package com.example.camui

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupSmokeTest {
    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test
    fun coldStartRemainsResumedAfterCameraInitialization() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            // ActivityScenario.launch() already waits for the activity to reach RESUMED.
            // Do not wait for a completely idle UI: a live camera preview continuously posts frames.
            Thread.sleep(3_000L)
            scenario.onActivity { activity ->
                assertFalse("MainActivity unexpectedly finished during startup", activity.isFinishing)
                assertFalse("MainActivity was destroyed during startup", activity.isDestroyed)
                assertEquals(
                    "MainActivity must remain resumed after a cold launch",
                    Lifecycle.State.RESUMED,
                    activity.lifecycle.currentState
                )
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun cameraActivitySupportsLandscapeConfiguration() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        try {
            scenario.onActivity { activity ->
                assertEquals(
                    "Activity must allow sensor-driven orientation",
                    ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
                    activity.requestedOrientation
                )
            }
            setLandscape(automation, instrumentation)
            scenario.onActivity { activity ->
                assertEquals(
                    "Camera screen must enter landscape when the emulator rotates",
                    Configuration.ORIENTATION_LANDSCAPE,
                    activity.resources.configuration.orientation
                )
                assertTrue("Activity must remain alive after rotation", !activity.isFinishing && !activity.isDestroyed)
            }
        } finally {
            restorePortrait(automation)
            scenario.close()
        }
    }

    @Test
    fun landscapeQuickSettingsAndSettingsRemainAccessible() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val device = UiDevice.getInstance(instrumentation)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            setLandscape(automation, instrumentation)
            assertTrue(
                "Quick Settings button must remain visible in landscape",
                device.wait(Until.hasObject(By.desc("Quick settings")), 10_000L)
            )
            device.findObject(By.desc("Quick settings")).click()

            assertTrue("Quick Settings panel must show aspect ratio", waitForAny(device, 5_000L, By.text("Aspect Ratio")))
            assertTrue("Quick Settings panel must show Settings entry", waitForAny(device, 5_000L, By.text("Settings")))
            assertTrue("Quick Settings panel must show Toolbox entry", waitForAny(device, 5_000L, By.text("Toolbox")))

            device.findObject(By.text("Settings")).click()
            assertTrue("Settings Capture section must be accessible", waitForAny(device, 8_000L, By.text("Capture & Quality")))
            assertTrue("Settings Imaging section must be accessible", waitForAny(device, 5_000L, By.text("NightShotMode")))
            assertTrue("Settings data/about entries must remain accessible", waitForAny(device, 5_000L, By.text("About")))
            assertTrue("Settings screen must remain alive", device.wait(Until.hasObject(By.text("Focus & Lens")), 5_000L))
        } finally {
            restorePortrait(automation)
            scenario.close()
        }
    }

    @Test
    fun landscapeFilterAndLunaDreamPanelsFitAndRemainReachable() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val device = UiDevice.getInstance(instrumentation)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            setLandscape(automation, instrumentation)
            assertTrue("Camera controls must be visible after rotation", device.wait(Until.hasObject(By.desc("Quick settings")), 10_000L))

            val scrollX = (device.displayWidth * 0.88f).toInt()
            val fromY = (device.displayHeight * 0.82f).toInt()
            val toY = (device.displayHeight * 0.28f).toInt()
            var filterButton = findFirst(device, By.text("Bộ lọc"), By.text("Filter"), By.textContains("Bộ lọc"), By.textContains("Filter"))
            repeat(5) {
                if (filterButton == null) {
                    device.swipe(scrollX, fromY, scrollX, toY, 18)
                    filterButton = findFirst(device, By.text("Bộ lọc"), By.text("Filter"), By.textContains("Bộ lọc"), By.textContains("Filter"))
                }
            }
            assertNotNull("Filter control must be reachable in landscape", filterButton)
            filterButton!!.click()
            assertTrue(
                "Filter dialog title must be visible",
                waitForAny(device, 8_000L, By.text("Bộ lọc phim"), By.text("Film filters"))
            )
            assertTrue("Filter list must expose selectable looks", waitForAny(device, 5_000L, By.text("Provia")))
            val closeButton = findFirst(device, By.text("Đóng"), By.text("Close"))
            assertNotNull("Filter dialog must provide a close action", closeButton)
            closeButton!!.click()

            var lunaButton = findFirst(device, By.text("LunaDream"))
            repeat(4) {
                if (lunaButton == null) {
                    device.swipe(scrollX, fromY, scrollX, toY, 18)
                    lunaButton = findFirst(device, By.text("LunaDream"))
                }
            }
            assertNotNull("LunaDream control must be reachable in landscape", lunaButton)
            lunaButton!!.click()
            assertTrue("LunaDream 1.5 option must be visible", device.wait(Until.hasObject(By.text("LunaDream 1.5")), 8_000L))
            assertTrue("LunaDream 2.0 option must be visible", device.wait(Until.hasObject(By.text("LunaDream 2.0")), 5_000L))

            val mode15 = device.findObject(By.text("LunaDream 1.5"))
            val mode20 = device.findObject(By.text("LunaDream 2.0"))
            assertTrue(
                "LunaDream model cards must be arranged side-by-side in landscape",
                mode15.visibleBounds.centerX() < mode20.visibleBounds.centerX()
            )
            // The test only inspects the options; it never toggles either model.
            device.pressBack()
        } finally {
            restorePortrait(automation)
            scenario.close()
        }
    }

    private fun setLandscape(
        automation: android.app.UiAutomation,
        instrumentation: android.app.Instrumentation
    ) {
        automation.executeShellCommand("settings put system accelerometer_rotation 0").close()
        automation.executeShellCommand("settings put system user_rotation 1").close()
        instrumentation.waitForIdleSync()
        Thread.sleep(2_000L)
    }

    private fun restorePortrait(automation: android.app.UiAutomation) {
        automation.executeShellCommand("settings put system user_rotation 0").close()
        automation.executeShellCommand("settings put system accelerometer_rotation 1").close()
    }

    private fun waitForAny(device: UiDevice, timeoutMs: Long, vararg selectors: androidx.test.uiautomator.BySelector): Boolean {
        return selectors.any { selector -> device.wait(Until.hasObject(selector), timeoutMs) }
    }

    private fun findFirst(device: UiDevice, vararg selectors: androidx.test.uiautomator.BySelector): UiObject2? {
        return selectors.firstNotNullOfOrNull { selector -> runCatching { device.findObject(selector) }.getOrNull() }
    }
}

package com.example.camui

import android.Manifest
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class LanguageImmediateUpdateTest {
    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test
    fun selectingVietnameseAndEnglishUpdatesResourceStringsWithoutRestart() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
        val originalPreference = preferences.getString("app_language", "system") ?: "system"
        preferences.edit().putString("app_language", "en").commit()

        val device = UiDevice.getInstance(instrumentation)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val originalActivity = AtomicReference<MainActivity>()

        try {
            completeFirstRunActivationIfNeeded(device)
            scenario.onActivity { originalActivity.set(it) }

            val quickMenuEntry = device.wait(Until.findObject(By.desc("Quick settings")), 10_000L)
                ?: device.wait(Until.findObject(By.text("Q")), 2_000L)
            assertNotNull("Camera quick menu must be visible", quickMenuEntry)
            quickMenuEntry!!.click()

            val settingsEntry = device.wait(
                Until.findObject(By.text("SETTINGS  ›")),
                5_000L
            )
            assertNotNull("Settings shortcut must be visible in the quick menu", settingsEntry)
            settingsEntry!!.click()

            val languageEntry = findInSettingsTabs(device)
            assertNotNull("Language category must be reachable in the settings tab list", languageEntry)
            languageEntry!!.click()

            val vietnameseOption = device.wait(Until.findObject(By.text("TIẾNG VIỆT")), 5_000L)
            assertNotNull("Vietnamese option must be visible", vietnameseOption)
            vietnameseOption!!.click()

            assertTrue(
                "Resource-backed language description must switch to Vietnamese immediately",
                device.wait(
                    Until.hasObject(By.text("Chọn ngôn ngữ hiển thị. Thay đổi được áp dụng ngay.")),
                    5_000L
                )
            )

            val englishOption = device.wait(Until.findObject(By.text("ENGLISH")), 5_000L)
            assertNotNull("English option must remain visible after switching to Vietnamese", englishOption)
            englishOption!!.click()

            assertTrue(
                "Resource-backed language description must switch back to English immediately",
                device.wait(
                    Until.hasObject(By.text("Choose the display language. Changes apply immediately.")),
                    5_000L
                )
            )

            scenario.onActivity { current ->
                assertSame(
                    "Changing the app language must not recreate MainActivity",
                    originalActivity.get(),
                    current
                )
                assertTrue("MainActivity must remain active after language changes", !current.isFinishing && !current.isDestroyed)
            }
        } finally {
            scenario.close()
            preferences.edit().putString("app_language", originalPreference).commit()
        }
    }

    private fun findInSettingsTabs(device: UiDevice): androidx.test.uiautomator.UiObject2? {
        fun findLanguage(): androidx.test.uiautomator.UiObject2? {
            return runCatching { device.findObject(By.textContains("Language")) }.getOrNull()
                ?: runCatching { device.findObject(By.textContains("Ngôn ngữ")) }.getOrNull()
        }

        repeat(8) {
            findLanguage()?.let { return it }
            val x = (device.displayWidth * 0.14f).toInt()
            device.swipe(
                x,
                (device.displayHeight * 0.82f).toInt(),
                x,
                (device.displayHeight * 0.26f).toInt(),
                16
            )
        }
        return findLanguage()
    }

    private fun waitForAny(
        device: UiDevice,
        timeoutMs: Long,
        vararg selectors: androidx.test.uiautomator.BySelector
    ): androidx.test.uiautomator.UiObject2? {
        val deadline = android.os.SystemClock.uptimeMillis() + timeoutMs
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            selectors.forEach { selector ->
                runCatching { device.findObject(selector) }.getOrNull()?.let { return it }
            }
            Thread.sleep(100L)
        }
        return null
    }
}

package com.example.camui

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * Completes the first-install introduction when a test starts with clean app data.
 * Upgraded/previously activated installations are left untouched.
 */
internal fun completeFirstRunActivationIfNeeded(device: UiDevice) {
    val englishTitle = device.wait(Until.findObject(By.text("SELECT LANGUAGE")), 750L)
    val vietnameseTitle = if (englishTitle == null) {
        device.wait(Until.findObject(By.text("CHỌN NGÔN NGỮ")), 750L)
    } else null
    if (englishTitle == null && vietnameseTitle == null) return

    val englishOption = device.findObject(By.text("ENGLISH"))
    englishOption?.click()

    // Choosing a language advances directly to Storage & Battery; there is no
    // Continue button on this step. The production panel auto-advances after 2.5s
    // and also accepts a tap, so tapping it keeps instrumentation fast and stable.
    val storage = device.wait(Until.findObject(By.text("STORAGE & BATTERY")), 2_500L)
        ?: device.wait(Until.findObject(By.text("LƯU TRỮ & PIN")), 2_500L)
    checkNotNull(storage) { "Activation storage step must be visible after language selection." }
    storage.click()

    val publisher = device.wait(Until.findObject(By.text("PUBLISHER")), 2_500L)
        ?: device.wait(Until.findObject(By.text("NHÀ PHÁT HÀNH")), 2_500L)
    checkNotNull(publisher) { "Activation publisher step must be visible." }
    val ok = device.wait(Until.findObject(By.text("OK")), 3_000L)
    checkNotNull(ok) { "Activation publisher step must expose OK." }.click()
    device.wait(Until.hasObject(By.desc("Quick settings")), 5_000L)
}

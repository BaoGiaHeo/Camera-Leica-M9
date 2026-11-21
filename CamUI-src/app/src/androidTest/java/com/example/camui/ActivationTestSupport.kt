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

    val next = device.wait(Until.findObject(By.text("CONTINUE")), 1_500L)
        ?: device.wait(Until.findObject(By.text("TIẾP TỤC")), 1_500L)
    checkNotNull(next) { "Activation language step must expose Continue." }.click()

    val storage = device.wait(Until.findObject(By.text("STORAGE & BATTERY")), 1_500L)
        ?: device.wait(Until.findObject(By.text("LƯU TRỮ & PIN")), 1_500L)
    checkNotNull(storage) { "Activation storage step must be visible." }
    // The storage panel accepts a tap to continue; the production screen also
    // advances automatically after the specified 2.5 second viewing interval.
    storage.click()

    val publisher = device.wait(Until.findObject(By.text("PUBLISHER")), 1_500L)
        ?: device.wait(Until.findObject(By.text("NHÀ PHÁT HÀNH")), 1_500L)
    checkNotNull(publisher) { "Activation publisher step must be visible." }
    val ok = device.wait(Until.findObject(By.text("OK")), 3_000L)
    checkNotNull(ok) { "Activation publisher step must expose OK." }.click()
    device.wait(Until.hasObject(By.desc("Quick settings")), 5_000L)
}

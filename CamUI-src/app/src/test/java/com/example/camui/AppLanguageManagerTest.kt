package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageManagerTest {
    @Test
    fun onlyEnglishAndVietnamesePreferencesAreAccepted() {
        assertEquals("en", AppLanguageManager.normalizePreference("en"))
        assertEquals("vi", AppLanguageManager.normalizePreference("vi"))
        assertEquals("en", AppLanguageManager.normalizePreference("system"))
        assertEquals("en", AppLanguageManager.normalizePreference("fr"))
    }

    @Test
    fun legacySystemPreferenceMigratesFromDeviceLanguage() {
        assertEquals("vi", AppLanguageManager.resolveLanguage("system", "vi"))
        assertEquals("vi", AppLanguageManager.resolveLanguage("system", "vi-VN"))
        assertEquals("en", AppLanguageManager.resolveLanguage("system", "en-US"))
        assertEquals("en", AppLanguageManager.resolveLanguage("system", "ja-JP"))
        assertEquals("en", AppLanguageManager.resolveLanguage("system", null))
    }

    @Test
    fun explicitLanguageOverridesDeviceLanguage() {
        assertEquals("en", AppLanguageManager.resolveLanguage("en", "vi-VN"))
        assertEquals("vi", AppLanguageManager.resolveLanguage("vi", "en-US"))
    }
}

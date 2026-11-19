package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageManagerTest {
    @Test
    fun onlySystemEnglishAndVietnamesePreferencesAreAccepted() {
        assertEquals("system", AppLanguageManager.normalizePreference("system"))
        assertEquals("en", AppLanguageManager.normalizePreference("en"))
        assertEquals("vi", AppLanguageManager.normalizePreference("vi"))
    }

    @Test
    fun legacyLanguagePreferencesMigrateToSystem() {
        listOf("ja", "ja-JP", "ko", "ko-KR", "zh", "zh-CN", "fr", "", null)
            .forEach { legacy ->
                assertEquals("Legacy preference $legacy must migrate to SYSTEM",
                    "system",
                    AppLanguageManager.normalizePreference(legacy))
            }
    }

    @Test
    fun systemVietnameseLocaleResolvesToVietnamese() {
        assertEquals("vi", AppLanguageManager.resolveLanguage("system", "vi"))
        assertEquals("vi", AppLanguageManager.resolveLanguage("system", "vi-VN"))
        assertEquals("vi", AppLanguageManager.resolveLanguage("system", "VI_vn"))
    }

    @Test
    fun everyNonVietnameseSystemLocaleResolvesToEnglish() {
        listOf("en", "en-US", "ja-JP", "ko-KR", "zh-CN", "fr-FR", null)
            .forEach { locale ->
                assertEquals("System locale $locale must resolve to English",
                    "en",
                    AppLanguageManager.resolveLanguage("system", locale))
            }
    }

    @Test
    fun explicitLanguageOverridesSystemLocale() {
        assertEquals("en", AppLanguageManager.resolveLanguage("en", "vi-VN"))
        assertEquals("vi", AppLanguageManager.resolveLanguage("vi", "en-US"))
    }
}

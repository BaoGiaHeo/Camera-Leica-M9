package com.example.camui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Central language preference and runtime language state.
 *
 * The only persisted languages are English and Vietnamese. Legacy SYSTEM or
 * unknown values are migrated once using the device language at upgrade time.
 */
object AppLanguageManager {
    private const val PREFS_NAME = "fujifilm_settings"
    private const val LANGUAGE_KEY = "app_language"

    private var initialized = false

    var currentLanguage by mutableStateOf("en")
        private set

    var selectedPreference by mutableStateOf("en")
        private set

    fun normalizePreference(value: String?): String {
        return when (value?.trim()?.lowercase()) {
            "vi", "vietnamese" -> "vi"
            "en", "english" -> "en"
            else -> "en"
        }
    }

    fun resolveLanguage(preference: String?, systemLanguageTag: String?): String {
        val raw = preference?.trim()?.lowercase()
        if (raw == "system" || raw.isNullOrBlank()) {
            val language = systemLanguageTag
                ?.trim()
                ?.lowercase()
                ?.replace('_', '-')
                .orEmpty()
            return if (language == "vi" || language.startsWith("vi-")) "vi" else "en"
        }
        return normalizePreference(preference)
    }

    fun migrateLegacyPreference(value: String?, context: Context): String {
        return if (value?.trim()?.lowercase() == "system" || value.isNullOrBlank()) {
            resolveLanguage("system", systemLanguageTag(context))
        } else {
            normalizePreference(value)
        }
    }

    @Synchronized
    fun initialize(context: Context) {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = preferences.getString(LANGUAGE_KEY, "system")
        val normalized = migrateLegacyPreference(stored, context)
        if (stored != normalized) {
            preferences.edit().putString(LANGUAGE_KEY, normalized).apply()
        }
        selectedPreference = normalized
        currentLanguage = normalized
        initialized = true
    }

    @Synchronized
    fun setLanguage(context: Context, preference: String): String {
        val normalized = normalizePreference(preference)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, normalized)
            .apply()
        selectedPreference = normalized
        currentLanguage = resolveLanguage(normalized, systemLanguageTag(context))
        initialized = true
        return currentLanguage
    }

    @Synchronized
    fun refreshSystemLanguage(context: Context) {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = migrateLegacyPreference(preferences.getString(LANGUAGE_KEY, "system"), context)
        if (preferences.getString(LANGUAGE_KEY, null) != stored) {
            preferences.edit().putString(LANGUAGE_KEY, stored).apply()
        }
        selectedPreference = stored
        currentLanguage = stored
        initialized = true
    }

    fun localizedContext(context: Context, language: String = currentLanguage): Context {
        val resolvedLanguage = if (language == "vi") "vi" else "en"
        val configuration = android.content.res.Configuration(context.resources.configuration)
        val locale = java.util.Locale.forLanguageTag(resolvedLanguage)
        configuration.setLocales(android.os.LocaleList.forLanguageTags(resolvedLanguage))
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    fun languageFor(context: Context): String {
        if (initialized) return currentLanguage
        val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = preferences.getString(LANGUAGE_KEY, "system")
        val resolved = migrateLegacyPreference(stored, context)
        if (stored != resolved) preferences.edit().putString(LANGUAGE_KEY, resolved).apply()
        return resolved
    }

    private fun systemLanguageTag(context: Context): String {
        val locales = context.resources.configuration.locales
        return if (!locales.isEmpty) locales[0].toLanguageTag()
        else java.util.Locale.getDefault().toLanguageTag()
    }
}

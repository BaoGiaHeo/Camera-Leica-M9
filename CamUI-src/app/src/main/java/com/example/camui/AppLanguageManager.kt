package com.example.camui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Central language preference and runtime language state.
 *
 * Supported preferences are SYSTEM, en and vi. Legacy or unknown values are
 * normalized to SYSTEM so older backups cannot activate removed locales.
 */
object AppLanguageManager {
    private const val PREFS_NAME = "fujifilm_settings"
    private const val LANGUAGE_KEY = "app_language"

    private var initialized = false

    var currentLanguage by mutableStateOf("en")
        private set

    var selectedPreference by mutableStateOf("system")
        private set

    fun normalizePreference(value: String?): String {
        return when (value?.trim()?.lowercase()) {
            "system" -> "system"
            "en", "english" -> "en"
            "vi", "vietnamese" -> "vi"
            else -> "system"
        }
    }

    fun resolveLanguage(preference: String?, systemLanguageTag: String?): String {
        return when (normalizePreference(preference)) {
            "en" -> "en"
            "vi" -> "vi"
            else -> {
                val language = systemLanguageTag
                    ?.trim()
                    ?.lowercase()
                    ?.replace('_', '-')
                    .orEmpty()
                if (language == "vi" || language.startsWith("vi-")) "vi" else "en"
            }
        }
    }

    @Synchronized
    fun initialize(context: Context) {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = preferences.getString(LANGUAGE_KEY, "system")
        val normalized = normalizePreference(stored)
        if (stored != normalized) {
            preferences.edit().putString(LANGUAGE_KEY, normalized).apply()
        }
        selectedPreference = normalized
        currentLanguage = resolveLanguage(normalized, systemLanguageTag(context))
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
        val stored = normalizePreference(preferences.getString(LANGUAGE_KEY, "system"))
        selectedPreference = stored
        if (stored == "system") {
            currentLanguage = resolveLanguage(stored, systemLanguageTag(context))
        }
        initialized = true
    }

    fun languageFor(context: Context): String {
        if (initialized) return currentLanguage
        val preference = normalizePreference(
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(LANGUAGE_KEY, "system")
        )
        return resolveLanguage(preference, systemLanguageTag(context))
    }

    private fun systemLanguageTag(context: Context): String {
        val locales = context.resources.configuration.locales
        return if (!locales.isEmpty) locales[0].toLanguageTag()
        else java.util.Locale.getDefault().toLanguageTag()
    }
}

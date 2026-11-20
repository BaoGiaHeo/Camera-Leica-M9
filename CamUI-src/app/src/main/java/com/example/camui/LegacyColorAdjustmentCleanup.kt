package com.example.camui

import android.content.Context
import android.util.Log
import java.util.Locale

/**
 * Removes obsolete per-filter colour-panel preferences without touching
 * film simulation recipes or the saved photographs.
 *
 * The cleanup is deliberately idempotent: a failed preference commit leaves
 * the migration marker unset so the next launch can retry safely.
 */
internal object LegacyColorAdjustmentCleanup {
    internal const val LEGACY_FILTER_PREFIX = "tone_pad."
    internal const val LEGACY_SETTINGS_PREFIX = "tone_pad_"

    private const val TAG = "LegacyColorCleanup"
    private const val SETTINGS_FILE = "fujifilm_settings"
    private const val MIGRATION_MARKER = "legacy_color_panel_cleanup_v1"

    private val preferenceFiles = listOf(
        "fujifilm_settings",
        "fujifilm_camera_presets",
        "fujifilm_film_recipes",
        "fujifilm_contents"
    )

    internal fun isObsoletePreferenceKey(key: String): Boolean {
        val normalized = key.lowercase(Locale.ROOT)
        return normalized.startsWith(LEGACY_FILTER_PREFIX) ||
            normalized.startsWith(LEGACY_SETTINGS_PREFIX) ||
            normalized.contains(".$LEGACY_FILTER_PREFIX")
    }

    fun migrateOnce(context: Context) {
        val settings = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
        if (settings.getBoolean(MIGRATION_MARKER, false)) return

        var removedCount = 0
        var allCommitsSucceeded = true

        preferenceFiles.forEach { fileName ->
            val preferences = runCatching {
                context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            }.onFailure { error ->
                allCommitsSucceeded = false
                Log.w(TAG, "Could not open a preference file during cleanup: $fileName", error)
            }.getOrNull() ?: return@forEach

            val obsoleteKeys = preferences.all.keys.filter(::isObsoletePreferenceKey)
            if (obsoleteKeys.isEmpty()) return@forEach

            val committed = runCatching {
                preferences.edit().also { editor ->
                    obsoleteKeys.forEach(editor::remove)
                }.commit()
            }.onFailure { error ->
                Log.w(TAG, "Could not remove obsolete preferences from $fileName", error)
            }.getOrDefault(false)

            if (committed) {
                removedCount += obsoleteKeys.size
            } else {
                allCommitsSucceeded = false
                Log.w(TAG, "Preference cleanup commit failed for $fileName")
            }
        }

        if (allCommitsSucceeded) {
            val markerSaved = runCatching {
                settings.edit().putBoolean(MIGRATION_MARKER, true).commit()
            }.onFailure { error ->
                Log.w(TAG, "Could not save cleanup migration marker", error)
            }.getOrDefault(false)
            if (!markerSaved) {
                Log.w(TAG, "Cleanup migration marker was not saved; cleanup will retry on next launch")
            }
        }

        Log.i(TAG, "Legacy preference cleanup removed $removedCount entries; complete=$allCommitsSucceeded")
    }
}

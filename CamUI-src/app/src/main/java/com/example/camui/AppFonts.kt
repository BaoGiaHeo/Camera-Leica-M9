package com.example.camui

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import androidx.compose.ui.text.font.FontFamily

/**
 * Loads the optional Arial assets once. Missing files must never prevent startup or APK builds.
 * Add arial.ttf and arial_bold.ttf under app/src/main/assets/fonts/.
 */
internal object AppFonts {
    const val REGULAR_ASSET = "fonts/arial.ttf"
    const val BOLD_ASSET = "fonts/arial_bold.ttf"
    private const val TAG = "FUJI_APP_FONTS"

    @Volatile private var regularFace: Typeface? = null
    @Volatile private var boldFace: Typeface? = null
    @Volatile private var composeFamily: FontFamily? = null
    @Volatile private var regularAssetLoaded = false
    @Volatile private var boldAssetLoaded = false
    private val lock = Any()

    fun regular(context: Context): Typeface {
        regularFace?.let { return it }
        return synchronized(lock) {
            regularFace ?: load(context, REGULAR_ASSET, Typeface.SANS_SERIF) { regularAssetLoaded = it }
                .also { regularFace = it }
        }
    }

    fun bold(context: Context): Typeface {
        boldFace?.let { return it }
        return synchronized(lock) {
            boldFace ?: load(
                context,
                BOLD_ASSET,
                Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            ) { boldAssetLoaded = it }.also { boldFace = it }
        }
    }

    fun isRegularAssetLoaded(context: Context): Boolean {
        regular(context)
        return regularAssetLoaded
    }

    fun isBoldAssetLoaded(context: Context): Boolean {
        bold(context)
        return boldAssetLoaded
    }

    fun family(context: Context): FontFamily {
        composeFamily?.let { return it }
        return synchronized(lock) {
            composeFamily ?: FontFamily(regular(context)).also { composeFamily = it }
        }
    }

    private fun load(
        context: Context,
        asset: String,
        fallback: Typeface,
        onLoaded: (Boolean) -> Unit
    ): Typeface = runCatching {
        Typeface.createFromAsset(context.applicationContext.assets, asset).also { onLoaded(true) }
    }.onFailure {
        onLoaded(false)
        Log.w(TAG, "Optional font asset $asset unavailable; using Android SansSerif.", it)
    }.getOrDefault(fallback)

    internal fun clearCacheForTests() {
        synchronized(lock) {
            regularFace = null
            boldFace = null
            composeFamily = null
            regularAssetLoaded = false
            boldAssetLoaded = false
        }
    }
}

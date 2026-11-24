package com.example.camui

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

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
    private val lock = Any()

    fun regular(context: Context): Typeface {
        regularFace?.let { return it }
        return synchronized(lock) {
            regularFace ?: load(context, REGULAR_ASSET, Typeface.SANS_SERIF).also { regularFace = it }
        }
    }

    fun bold(context: Context): Typeface {
        boldFace?.let { return it }
        return synchronized(lock) {
            boldFace ?: load(context, BOLD_ASSET, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))
                .also { boldFace = it }
        }
    }

    fun family(context: Context): FontFamily {
        composeFamily?.let { return it }
        return synchronized(lock) {
            composeFamily ?: FontFamily(
                Font(regular(context), FontWeight.Normal, FontStyle.Normal),
                Font(bold(context), FontWeight.Bold, FontStyle.Normal)
            ).also { composeFamily = it }
        }
    }

    private fun load(context: Context, asset: String, fallback: Typeface): Typeface =
        runCatching {
            Typeface.createFromAsset(context.applicationContext.assets, asset)
        }.onFailure {
            Log.w(TAG, "Optional font asset $asset unavailable; using Android SansSerif.", it)
        }.getOrDefault(fallback)

    internal fun clearCacheForTests() {
        synchronized(lock) {
            regularFace = null
            boldFace = null
            composeFamily = null
        }
    }
}

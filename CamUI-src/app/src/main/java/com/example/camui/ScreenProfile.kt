package com.example.camui

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal enum class ScreenWindowSizeClass { COMPACT, MEDIUM, EXPANDED }
internal enum class ScreenAspectClass { NEAR_SQUARE, SIXTEEN_NINE, EIGHTEEN_NINE, TWENTY_NINE, TWENTY_TWO_TEN, SIXTEEN_TEN, OTHER }

internal data class ScreenLayoutMetrics(
    val topPlateHeight: Dp,
    val leftColumnWidth: Dp,
    val rightColumnWidth: Dp,
    val proControlHeight: Dp
)

internal data class ScreenProfile(
    val windowWidthPx: Int,
    val windowHeightPx: Int,
    val densityDpi: Int,
    val density: Float,
    val fontScale: Float,
    val widthDp: Float,
    val heightDp: Float,
    val aspectClass: ScreenAspectClass,
    val windowSizeClass: ScreenWindowSizeClass,
    val refreshRateHz: Float,
    val systemBarInsets: Rect,
    val cutoutInsets: Rect,
    val mandatoryGestureInsets: Rect,
    val tappableElementInsets: Rect,
    val cutoutBounds: List<Rect>,
    val roundedCornerRadiiPx: List<Int>,
    val foldingFeatureBounds: Rect? = null,
    val foldingFeatureSeparating: Boolean = false,
    val foldingFeatureOrientation: String? = null
) {
    fun layoutFor(widthDp: Float = this.widthDp, heightDp: Float = this.heightDp): ScreenLayoutMetrics {
        val safeWidth = widthDp.takeIf { it.isFinite() && it > 0f } ?: this.widthDp
        val safeHeight = heightDp.takeIf { it.isFinite() && it > 0f } ?: this.heightDp
        return ScreenLayoutMetrics(
            topPlateHeight = (safeHeight * 0.14f).coerceIn(52f, 68f).dp,
            leftColumnWidth = (safeWidth * 0.12f).coerceIn(96f, 132f).dp,
            rightColumnWidth = (safeWidth * 0.19f).coerceIn(150f, 200f).dp,
            proControlHeight = (safeHeight * 0.10f).coerceIn(56f, 84f).dp
        )
    }

    fun diagnosticLine(): String =
        "window=${windowWidthPx}x${windowHeightPx}px/${widthDp}x${heightDp}dp " +
            "densityDpi=$densityDpi density=$density fontScale=$fontScale aspect=$aspectClass " +
            "sizeClass=$windowSizeClass refreshRate=${refreshRateHz}Hz " +
            "systemBars=$systemBarInsets cutout=$cutoutInsets gestures=$mandatoryGestureInsets " +
            "tappable=$tappableElementInsets rounded=$roundedCornerRadiiPx " +
            "fold=${foldingFeatureBounds ?: "none"} separating=$foldingFeatureSeparating"

    companion object {
        fun read(activity: Activity): ScreenProfile {
            val metrics = DisplayMetrics()
            val bounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.windowManager.currentWindowMetrics.bounds
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getRealMetrics(metrics)
                Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
            }
            val densityMetrics = activity.resources.displayMetrics
            val density = densityMetrics.density.takeIf { it > 0f } ?: 1f
            val widthDp = bounds.width() / density
            val heightDp = bounds.height() / density
            val aspect = classifyAspect(widthDp, heightDp)
            // This camera UI is locked to landscape: use available window width
            // rather than the short side, which incorrectly classified phones as COMPACT.
            val sizeClass = when {
                widthDp < 600f -> ScreenWindowSizeClass.COMPACT
                widthDp < 840f -> ScreenWindowSizeClass.MEDIUM
                else -> ScreenWindowSizeClass.EXPANDED
            }
            val rootView = activity.window.decorView
            val insets = ViewCompat.getRootWindowInsets(rootView)
            fun safeInsets(type: Int): Rect {
                val value = insets?.getInsets(type)
                return if (value == null) Rect() else Rect(value.left, value.top, value.right, value.bottom)
            }
            val systemBars = safeInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = safeInsets(WindowInsetsCompat.Type.displayCutout())
            val gestures = safeInsets(WindowInsetsCompat.Type.mandatorySystemGestures())
            val tappable = safeInsets(WindowInsetsCompat.Type.tappableElement())
            val nativeInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) rootView.rootWindowInsets else null
            val cutoutRects = insets?.displayCutout?.boundingRects?.map(::Rect).orEmpty()
            val rounded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && nativeInsets != null) {
                listOf(
                    RoundedCorner.POSITION_TOP_LEFT,
                    RoundedCorner.POSITION_TOP_RIGHT,
                    RoundedCorner.POSITION_BOTTOM_RIGHT,
                    RoundedCorner.POSITION_BOTTOM_LEFT
                ).map { position -> nativeInsets.getRoundedCorner(position)?.radius ?: 0 }
            } else emptyList()
            val refreshRate = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display?.refreshRate
                else {
                    @Suppress("DEPRECATION")
                    activity.windowManager.defaultDisplay.refreshRate
                }
            }.getOrNull()?.takeIf { it.isFinite() && it > 0f } ?: 60f
            return ScreenProfile(
                windowWidthPx = bounds.width(),
                windowHeightPx = bounds.height(),
                densityDpi = densityMetrics.densityDpi,
                density = density,
                fontScale = activity.resources.configuration.fontScale,
                widthDp = widthDp,
                heightDp = heightDp,
                aspectClass = aspect,
                windowSizeClass = sizeClass,
                refreshRateHz = refreshRate,
                systemBarInsets = systemBars,
                cutoutInsets = cutout,
                mandatoryGestureInsets = gestures,
                tappableElementInsets = tappable,
                cutoutBounds = cutoutRects,
                roundedCornerRadiiPx = rounded
            )
        }

        fun classifyAspect(widthDp: Float, heightDp: Float): ScreenAspectClass {
            val short = minOf(widthDp, heightDp).coerceAtLeast(1f)
            val ratio = maxOf(widthDp, heightDp) / short
            return when {
                ratio < 1.35f -> ScreenAspectClass.NEAR_SQUARE
                kotlin.math.abs(ratio - 1.6f) < 0.08f -> ScreenAspectClass.SIXTEEN_TEN
                kotlin.math.abs(ratio - 1.78f) < 0.08f -> ScreenAspectClass.SIXTEEN_NINE
                kotlin.math.abs(ratio - 2.0f) < 0.10f -> ScreenAspectClass.EIGHTEEN_NINE
                kotlin.math.abs(ratio - 2.22f) < 0.008f -> ScreenAspectClass.TWENTY_NINE
                kotlin.math.abs(ratio - 2.2f) < 0.008f -> ScreenAspectClass.TWENTY_TWO_TEN
                else -> ScreenAspectClass.OTHER
            }
        }
    }
}

@Composable
internal fun rememberScreenProfile(): ScreenProfile {
    val context = LocalContext.current
    val activity = remember(context) {
        var current = context
        while (current is android.content.ContextWrapper && current !is Activity) {
            current = current.baseContext
        }
        current as? Activity
    }
    val configuration = LocalConfiguration.current
    var profile by remember(
        activity,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.densityDpi,
        configuration.fontScale,
        configuration.uiMode
    ) {
        mutableStateOf(activity?.let(ScreenProfile::read))
    }
    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose { }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val job = scope.launch {
            runCatching {
                WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { layoutInfo ->
                    val feature = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>()
                        .firstOrNull { it.isSeparating || it.occlusionType == FoldingFeature.OcclusionType.FULL }
                    val latest = ScreenProfile.read(activity)
                    profile = latest.copy(
                        foldingFeatureBounds = feature?.bounds?.let(::Rect),
                        foldingFeatureSeparating = feature?.isSeparating == true,
                        foldingFeatureOrientation = feature?.orientation?.toString()
                    )
                    android.util.Log.i("FUJI_SCREEN_PROFILE", "FOLDING ${profile?.diagnosticLine()}")
                }
            }.onFailure {
                android.util.Log.w("FUJI_SCREEN_PROFILE", "Folding feature observation unavailable", it)
            }
        }
        onDispose {
            job.cancel()
            scope.cancel()
        }
    }
    return profile ?: remember(activity, configuration) {
        activity?.let(ScreenProfile::read) ?: ScreenProfile(
            0, 0, 160, 1f, configuration.fontScale,
            configuration.screenWidthDp.toFloat(), configuration.screenHeightDp.toFloat(),
            ScreenProfile.classifyAspect(configuration.screenWidthDp.toFloat(), configuration.screenHeightDp.toFloat()),
            ScreenWindowSizeClass.COMPACT, 60f,
            Rect(), Rect(), Rect(), Rect(), emptyList(), emptyList()
        )
    }
}

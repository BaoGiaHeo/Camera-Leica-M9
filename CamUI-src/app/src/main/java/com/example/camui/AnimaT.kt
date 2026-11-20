package com.example.camui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/** Milestones that deserve a small tactile tick while the displayed lens ratio crosses them. */
internal fun zoomHapticMilestonesCrossed(from: Float, to: Float): List<Float> {
    if (!from.isFinite() || !to.isFinite() || from == to) return emptyList()
    return listOf(1f, 2f, 5f).filter { milestone ->
        (from < milestone && to >= milestone) || (from > milestone && to <= milestone)
    }
}

/** App-owned Compose motion system for page transitions, content reflow and touch feedback. */
object AnimaT {
    private val motionEasing = FastOutSlowInEasing

    /** Android accessibility/developer animation switches; no permission is required. */
    fun isSystemReducedMotion(context: Context): Boolean = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) <= 0f || Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f
        ) <= 0f
    }.getOrDefault(false)

    /** Shared right-to-left page entrance, with performance and accessibility fallbacks. */
    fun enter(
        performanceMode: Boolean = false,
        reducedMotion: Boolean = false
    ): EnterTransition = when {
        reducedMotion -> EnterTransition.None
        performanceMode -> fadeIn(animationSpec = tween(durationMillis = 90))
        else -> slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 18 },
            animationSpec = tween(durationMillis = 240, easing = motionEasing)
        ) +
            fadeIn(animationSpec = tween(durationMillis = 220, easing = motionEasing)) +
            scaleIn(
                initialScale = 0.99f,
                animationSpec = tween(durationMillis = 240, easing = motionEasing)
            )
    }

    /** Clean reverse drift using the same easing and shorter exit timing. */
    fun exit(
        performanceMode: Boolean = false,
        reducedMotion: Boolean = false
    ): ExitTransition = when {
        reducedMotion -> ExitTransition.None
        performanceMode -> fadeOut(animationSpec = tween(durationMillis = 70))
        else -> slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 32 },
            animationSpec = tween(durationMillis = 190, easing = motionEasing)
        ) +
            fadeOut(animationSpec = tween(durationMillis = 180, easing = motionEasing)) +
            scaleOut(
                targetScale = 0.995f,
                animationSpec = tween(durationMillis = 190, easing = motionEasing)
            )
    }
}

fun Modifier.animaTContent(): Modifier = composed {
    val context = LocalContext.current
    val reducedMotion = remember(context) { AnimaT.isSystemReducedMotion(context) }
    if (reducedMotion) this else animateContentSize(
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
    )
}

fun Modifier.animaTClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current
    val reducedMotion = remember(context) { AnimaT.isSystemReducedMotion(context) }
    val hapticFeedback = LocalHapticFeedback.current
    val hapticsEnabled = remember(context) {
        context.getSharedPreferences("fujifilm_settings", 0)
            .getBoolean("haptic_feedback_enabled", true)
    }
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.985f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "AnimaT press feedback"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled
    ) {
        if (hapticsEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onClick()
    }
}


/**
 * A tiny page-level tilt using the supported graphicsLayer API. It runs on
 * entry only, reverses direction when returning to the camera, and is skipped
 * entirely for reduced-motion or thermal/performance mode.
 */
fun Modifier.animaTPageRotation(
    pageKey: String,
    returningToCamera: Boolean,
    performanceMode: Boolean,
    reducedMotion: Boolean
): Modifier = composed {
    val rotation = remember { Animatable(0f) }
    var hasAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(pageKey) {
        if (!hasAppeared) {
            hasAppeared = true
            rotation.snapTo(0f)
        } else if (performanceMode || reducedMotion) {
            rotation.snapTo(0f)
        } else {
            rotation.snapTo(if (returningToCamera) -1.0f else 1.6f)
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
    }
    this.graphicsLayer {
        rotationZ = rotation.value
    }
}

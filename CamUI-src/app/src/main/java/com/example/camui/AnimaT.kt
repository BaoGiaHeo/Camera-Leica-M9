package com.example.camui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/** App-owned Compose motion system for page transitions, content reflow and touch feedback. */
object AnimaT {
    private val motionEasing = FastOutSlowInEasing

    /** Soft right-to-left entrance shared by top-level pages and Settings detail pages. */
    fun enter(): EnterTransition =
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 18 },
            animationSpec = tween(durationMillis = 320, easing = motionEasing)
        ) +
            fadeIn(animationSpec = tween(durationMillis = 270, easing = motionEasing)) +
            scaleIn(
                initialScale = 0.99f,
                animationSpec = tween(durationMillis = 320, easing = motionEasing)
            )

    /** Light reverse drift with a clean fade; avoids the old abrupt shrink effect. */
    fun exit(): ExitTransition =
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 32 },
            animationSpec = tween(durationMillis = 220, easing = motionEasing)
        ) +
            fadeOut(animationSpec = tween(durationMillis = 210, easing = motionEasing)) +
            scaleOut(
                targetScale = 0.995f,
                animationSpec = tween(durationMillis = 220, easing = motionEasing)
            )
}

fun Modifier.animaTContent(): Modifier = animateContentSize(
    animationSpec = spring(dampingRatio = 0.90f, stiffness = 360f)
)

fun Modifier.animaTClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val hapticsEnabled = remember(context) {
        context.getSharedPreferences("fujifilm_settings", 0)
            .getBoolean("haptic_feedback_enabled", true)
    }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 145, easing = FastOutSlowInEasing),
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

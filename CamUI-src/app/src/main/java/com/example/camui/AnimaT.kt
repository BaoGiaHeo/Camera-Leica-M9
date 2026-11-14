package com.example.camui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/** App-owned Compose motion system for page transitions, content reflow and touch feedback. */
object AnimaT {
    fun enter(): EnterTransition =
        fadeIn(tween(230, easing = FastOutSlowInEasing)) +
            scaleIn(initialScale = 0.965f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 430f))

    fun exit(): ExitTransition =
        fadeOut(tween(150, easing = FastOutSlowInEasing)) +
            scaleOut(targetScale = 0.985f, animationSpec = tween(160, easing = FastOutSlowInEasing))
}

fun Modifier.animaTContent(): Modifier = animateContentSize(
    animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f)
)

fun Modifier.animaTClickable(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 720f),
        label = "AnimaT press feedback"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

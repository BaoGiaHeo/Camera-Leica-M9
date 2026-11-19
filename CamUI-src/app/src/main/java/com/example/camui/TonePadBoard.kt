package com.example.camui

import android.graphics.Rect
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val tonePadBayer4 = arrayOf(
    intArrayOf(0, 8, 2, 10),
    intArrayOf(12, 4, 14, 6),
    intArrayOf(3, 11, 1, 9),
    intArrayOf(15, 7, 13, 5)
)

@Composable
fun TonePadBoard(
    filterName: String,
    onToneChanged: (TonePadParameters) -> Unit = {},
    advancedContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("fujifilm_settings", 0)
    }
    val preferencePrefix = remember(filterName) { TonePadMath.preferencePrefix(filterName) }
    var paleRich by remember(filterName) {
        mutableFloatStateOf(preferences.getFloat("$preferencePrefix.x", 0f).coerceIn(-1f, 1f))
    }
    var darkBright by remember(filterName) {
        mutableFloatStateOf(preferences.getFloat("$preferencePrefix.y", 0f).coerceIn(-1f, 1f))
    }
    var intensity by remember(filterName) {
        mutableFloatStateOf(preferences.getFloat("$preferencePrefix.intensity", 0.65f).coerceIn(0f, 1f))
    }
    var puckDragging by remember { mutableStateOf(false) }
    var fingerX by remember { mutableFloatStateOf(paleRich) }
    var fingerY by remember { mutableFloatStateOf(darkBright) }
    val guideEnabled = remember {
        preferences.getBoolean("tone_pad_guide_enabled", true)
    }
    var guideVisible by remember {
        mutableStateOf(guideEnabled && !preferences.getBoolean("tone_pad_guide_seen", false))
    }
    var guideReplayToken by remember { mutableStateOf(0) }
    val guideProgress = remember { Animatable(0f) }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var advancedExpanded by remember { mutableStateOf(false) }
    var snapEnabled by remember { mutableStateOf(true) }
    val lastTapAt = remember { mutableLongStateOf(0L) }
    val snapEnabledState by rememberUpdatedState(snapEnabled)

    val currentGuideVisible by rememberUpdatedState(guideVisible)
    val updatePosition by rememberUpdatedState<(Float, Float) -> Unit> { x, y ->
        val nx = x.coerceIn(-1f, 1f)
        val ny = y.coerceIn(-1f, 1f)
        paleRich = nx
        darkBright = ny
        fingerX = nx
        fingerY = ny
        preferences.edit()
            .putFloat("$preferencePrefix.x", nx)
            .putFloat("$preferencePrefix.y", ny)
            .apply()
        onToneChanged(TonePadParameters(nx, ny, intensity))
    }
    val setDragging by rememberUpdatedState<(Boolean) -> Unit> { dragging ->
        puckDragging = dragging
    }
    val resetPosition by rememberUpdatedState<() -> Unit> {
        updatePosition(0f, 0f)
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    LaunchedEffect(intensity, filterName) {
        preferences.edit().putFloat("$preferencePrefix.intensity", intensity).apply()
        onToneChanged(TonePadParameters(paleRich, darkBright, intensity))
    }

    LaunchedEffect(guideVisible, guideReplayToken) {
        if (guideVisible) {
            repeat(3) {
                guideProgress.animateTo(1f, animationSpec = tween(620))
                guideProgress.animateTo(0f, animationSpec = tween(620))
            }
            guideVisible = false
            preferences.edit().putBoolean("tone_pad_guide_seen", true).apply()
        }
    }

    DisposableEffect(view) {
        onDispose {
            view.systemGestureExclusionRects = emptyList()
            puckDragging = false
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.tone_pad_title),
                    color = CamColors.TextColor,
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    stringResource(R.string.tone_pad_filter_label, filterName),
                    color = CamColors.Sub,
                    fontSize = 9.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
                        .background(FujiSkin.lcd)
                        .border(1.dp, FujiSkin.lcdGreen, RoundedCornerShape(4.dp))
                        .clickable {
                            scope.launch {
                                guideProgress.snapTo(0f)
                                guideReplayToken++
                                guideVisible = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("?", color = FujiSkin.lcdGreen, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(4.dp))
                        .background(FujiSkin.lcd)
                        .border(1.dp, FujiSkin.lcdGreen, RoundedCornerShape(4.dp))
                        .clickable { resetPosition() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.tone_pad_reset), color = FujiSkin.lcdGreen, fontSize = 8.sp)
                }
            }
        }

        BoxWithConstraints(
            Modifier.fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF090E10))
                .border(1.dp, Color(0xFF4A5A5F), RoundedCornerShape(4.dp))
                .onGloballyPositioned { coordinates ->
                    val origin = coordinates.positionInWindow()
                    val bounds = Rect(
                        origin.x.roundToInt(),
                        origin.y.roundToInt(),
                        (origin.x + coordinates.size.width).roundToInt(),
                        (origin.y + coordinates.size.height).roundToInt()
                    )
                    view.systemGestureExclusionRects = listOf(bounds)
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        down.consume()
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        val startedAt = SystemClock.uptimeMillis()
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        val height = size.height.toFloat().coerceAtLeast(1f)
                        val downX = (down.position.x / width).coerceIn(0f, 1f)
                        val downY = (down.position.y / height).coerceIn(0f, 1f)

                        if (currentGuideVisible) {
                            guideVisible = false
                            preferences.edit().putBoolean("tone_pad_guide_seen", true).apply()
                        }
                        setDragging(true)
                        var activeMoved = false
                        var pointerMoved = false
                        var longPressReset = false
                        var resetThisGesture = false
                        val previousTap = lastTapAt.longValue
                        if (startedAt - previousTap <= 320L && previousTap > 0L) {
                            resetPosition()
                            lastTapAt.longValue = 0L
                            resetThisGesture = true
                        } else {
                            var snappedX = downX
                            var snappedY = downY
                            if (snapEnabledState) {
                                snappedX = (snappedX * 8f).roundToInt() / 8f
                                snappedY = (snappedY * 6f).roundToInt() / 6f
                            }
                            updatePosition(snappedX * 2f - 1f, 1f - snappedY * 2f)
                            activeMoved = true
                        }

                        var pointerReleased = false
                        try {
                            var gestureActive = true
                            while (gestureActive) {
                                val event = withTimeoutOrNull(40L) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                }
                                if (event == null) {
                                    if (!resetThisGesture && !pointerMoved && !longPressReset &&
                                        SystemClock.uptimeMillis() - startedAt >= 600L
                                    ) {
                                        resetPosition()
                                        longPressReset = true
                                        resetThisGesture = true
                                    }
                                    continue
                                }
                                if (event.changes.any { it.id != down.id && it.pressed }) {
                                    resetThisGesture = true
                                    gestureActive = false
                                    continue
                                }
                                val activeChange = event.changes.firstOrNull { it.id == down.id }
                                if (activeChange == null) {
                                    gestureActive = false
                                    continue
                                }
                                activeChange.consume()
                                if (!activeChange.pressed) {
                                    pointerReleased = true
                                    gestureActive = false
                                    continue
                                }
                                val dx = activeChange.position.x - down.position.x
                                val dy = activeChange.position.y - down.position.y
                                if (!pointerMoved && (abs(dx) > view.resources.displayMetrics.density * 2f ||
                                            abs(dy) > view.resources.displayMetrics.density * 2f)
                                ) {
                                    pointerMoved = true
                                    activeMoved = true
                                }
                                if (!resetThisGesture && activeMoved) {
                                    var nx = (activeChange.position.x / width).coerceIn(0f, 1f)
                                    var ny = (activeChange.position.y / height).coerceIn(0f, 1f)
                                    val heldForMs = SystemClock.uptimeMillis() - startedAt
                                    if (snapEnabledState && heldForMs < 300L) {
                                        nx = (nx * 8f).roundToInt() / 8f
                                        ny = (ny * 6f).roundToInt() / 6f
                                    }
                                    updatePosition(nx * 2f - 1f, 1f - ny * 2f)
                                }
                            }
                        } finally {
                            if (pointerReleased && !pointerMoved && !resetThisGesture) {
                                lastTapAt.longValue = SystemClock.uptimeMillis()
                            }
                            setDragging(false)
                            view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
        ) {
            Canvas(Modifier.fillMaxWidth().height(190.dp)) {
                val columns = 9
                val rows = 7
                val cellWidth = size.width / columns
                val cellHeight = size.height / rows
                for (row in 0 until rows) {
                    for (column in 0 until columns) {
                        val x = column / (columns - 1f) * 2f - 1f
                        val y = 1f - row / (rows - 1f) * 2f
                        val baseRed = (0.53f + x * 0.055f).coerceIn(0f, 1f)
                        val baseGreen = 0.48f
                        val baseBlue = (0.46f - x * 0.045f).coerceIn(0f, 1f)
                        val color = TonePadMath.apply(
                            baseRed,
                            baseGreen,
                            baseBlue,
                            x,
                            y,
                            intensity
                        )
                        val threshold = tonePadBayer4[row % 4][column % 4] / 16f
                        val dither = if (threshold > 0.5f) 0.025f else -0.015f
                        val shaded = Color(
                            (color.red + dither).coerceIn(0f, 1f),
                            (color.green + dither).coerceIn(0f, 1f),
                            (color.blue + dither).coerceIn(0f, 1f)
                        )
                        drawRect(
                            shaded,
                            Offset(column * cellWidth, row * cellHeight),
                            Size(cellWidth + 0.5f, cellHeight + 0.5f)
                        )
                    }
                }
                val axis = Color.White.copy(alpha = 0.35f)
                drawLine(axis, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1.dp.toPx())
                drawLine(axis, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.dp.toPx())

                val centerX = ((paleRich + 1f) * 0.5f * size.width).coerceIn(0f, size.width)
                val centerY = ((1f - darkBright) * 0.5f * size.height).coerceIn(0f, size.height)
                val logicPixel = (minOf(cellWidth, cellHeight) / 6f).coerceAtLeast(1f)
                val radius = if (puckDragging) 5 else 4
                for (row in -radius - 1..radius + 1) {
                    for (column in -radius - 1..radius + 1) {
                        val px = (centerX + column * logicPixel).roundToInt().toFloat()
                        val py = (centerY + row * logicPixel).roundToInt().toFloat()
                        drawRect(
                            Color.White.copy(alpha = 0.20f),
                            Offset(px, py),
                            Size(logicPixel, logicPixel)
                        )
                    }
                }
                for (row in -radius..radius) {
                    for (column in -radius..radius) {
                        val px = (centerX + column * logicPixel).roundToInt().toFloat()
                        val py = (centerY + row * logicPixel).roundToInt().toFloat()
                        drawRect(
                            Color(0xCC000000),
                            Offset(px, py),
                            Size(logicPixel, logicPixel)
                        )
                    }
                }
                for (row in -3..3) {
                    for (column in -3..3) {
                        if ((abs(row) == 3 && abs(column) == 3) ||
                            (abs(row) == 3 && abs(column) == 2) ||
                            (abs(row) == 2 && abs(column) == 3)
                        ) continue
                        val px = (centerX + column * logicPixel).roundToInt().toFloat()
                        val py = (centerY + row * logicPixel).roundToInt().toFloat()
                        drawRect(
                            Color.White,
                            Offset(px, py),
                            Size(logicPixel, logicPixel)
                        )
                    }
                }

                if (guideVisible) {
                    val handX = size.width * (0.38f + 0.30f * guideProgress.value)
                    val handY = size.height * (0.58f - 0.24f * guideProgress.value)
                    val handPixel = (logicPixel * 1.3f).roundToInt().toFloat().coerceAtLeast(1f)
                    val handPattern = listOf(
                        "....W.",
                        "...WW.",
                        "WWWWWW",
                        ".W.WW.",
                        ".W..W."
                    )
                    handPattern.forEachIndexed { row, pattern ->
                        pattern.forEachIndexed { column, symbol ->
                            if (symbol == 'W') {
                                drawRect(
                                    Color.White,
                                    Offset(handX + column * handPixel, handY + row * handPixel),
                                    Size(handPixel, handPixel)
                                )
                            }
                        }
                    }
                }
            }

            if (puckDragging) {
                val readout = stringResource(
                    R.string.tone_pad_value_readout,
                    (darkBright * 100f).roundToInt(),
                    (paleRich * 100f).roundToInt()
                )
                val readoutX = (
                    maxWidth * ((fingerX + 1f) * 0.5f) - 42.dp
                ).coerceIn(0.dp, (maxWidth - 84.dp).coerceAtLeast(0.dp))
                val readoutY = (
                    maxHeight * ((1f - fingerY) * 0.5f) - 27.dp
                ).coerceIn(0.dp, (maxHeight - 27.dp).coerceAtLeast(0.dp))
                Text(
                    readout,
                    color = Color.White,
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.TopStart)
                        .offset(x = readoutX, y = readoutY)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }

            if (guideVisible) {
                Text(
                    stringResource(R.string.tone_pad_guide_title),
                    color = FujiSkin.lcdGreen,
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.TopCenter)
                        .padding(top = 7.dp)
                        .background(Color(0xAA000000), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.tone_pad_bright_dark), color = CamColors.Sub, fontSize = 8.sp)
            Text(stringResource(R.string.tone_pad_pale_rich), color = CamColors.Sub, fontSize = 8.sp)
        }
        Text(
            stringResource(R.string.tone_pad_palette_label, (intensity * 100f).roundToInt()),
            color = CamColors.Sub,
            fontSize = 9.sp
        )
        Slider(
            value = intensity,
            onValueChange = { intensity = it.coerceIn(0f, 1f) },
            valueRange = 0f..1f
        )
        if (guideEnabled) {
            Text(
                stringResource(R.string.tone_pad_hint),
                color = CamColors.Sub,
                fontSize = 8.sp
            )
        }
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .background(FujiSkin.lcd)
                .border(1.dp, Color(0xFF344247), RoundedCornerShape(3.dp))
                .clickable { advancedExpanded = !advancedExpanded }
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.tone_pad_advanced), color = CamColors.TextColor, fontSize = 9.sp)
            Text(if (advancedExpanded) "−" else "+", color = FujiSkin.lcdGreen, fontSize = 12.sp)
        }
        AnimatedVisibility(advancedExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.tone_pad_snap), color = CamColors.Sub, fontSize = 9.sp)
                    Text(
                        if (snapEnabled) stringResource(R.string.tone_pad_on) else stringResource(R.string.tone_pad_off),
                        color = if (snapEnabled) FujiSkin.lcdGreen else CamColors.Sub,
                        fontSize = 9.sp,
                        modifier = Modifier.clickable { snapEnabled = !snapEnabled }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                advancedContent()
            }
        }
    }
}

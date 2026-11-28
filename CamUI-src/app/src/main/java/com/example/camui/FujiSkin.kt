package com.example.camui

import androidx.compose.ui.res.stringResource
import androidx.compose.material3.CircularProgressIndicator
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.min

/**
 * Small preference-aware feedback layer shared by the physical-looking controls.
 * The generated tones are deliberately short low-volume UI ticks; the shutter itself
 * continues to use the bundled camera-shutter recording in the capture pipeline.
 */
object FujiFeedback {
    @Volatile private var generator: ToneGenerator? = null

    private fun tone(context: Context, tone: Int, durationMs: Int, enabledKey: String) {
        if (!context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
                .getBoolean(enabledKey, true)
        ) return
        runCatching {
            val player = generator ?: synchronized(this) {
                generator ?: ToneGenerator(AudioManager.STREAM_SYSTEM, 18).also { generator = it }
            }
            player.startTone(tone, durationMs)
        }
    }

    fun keyTick(context: Context) =
        tone(context, ToneGenerator.TONE_PROP_BEEP, 16, "ui_sound_enabled")

    fun switchClack(context: Context) =
        tone(context, ToneGenerator.TONE_PROP_ACK, 25, "ui_sound_enabled")

    fun dialClick(context: Context) =
        tone(context, ToneGenerator.TONE_PROP_BEEP2, 22, "dial_click_enabled")
}

object FujiSkin {
    val body = FujiCameraDesign.surface
    val bodyDeep = FujiCameraDesign.background
    val silver = Color(0xFFC1C1C1)
    val ivory = FujiCameraDesign.text
    val lcd = Color(0xFF080808)
    val lcdGreen = FujiCameraDesign.green
    val orange = FujiCameraDesign.orange
    val line = FujiCameraDesign.stroke
    val muted = FujiCameraDesign.muted
    val manualBlue = FujiCameraDesign.blue
}

/**
 * Cached, screen-sized leatherette texture shared by the camera UI.
 * The image is center-cropped by Compose; the translucent radial overlay adds a
 * restrained edge vignette without allocating a bitmap during recomposition.
 */
@Composable
fun FujiLeatherSurface(
    modifier: Modifier = Modifier,
    simpleMode: Boolean = false,
    lightMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val targetWidth = (configuration.screenWidthDp * density.density).roundToInt().coerceAtLeast(1)
    val targetHeight = (configuration.screenHeightDp * density.density).roundToInt().coerceAtLeast(1)
    val leatherBitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        targetWidth,
        targetHeight
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeResource(context.resources, R.drawable.fuji_leather_texture, options)
                var sample = 1
                while (options.outWidth / (sample * 2) >= targetWidth &&
                    options.outHeight / (sample * 2) >= targetHeight
                ) sample *= 2
                val decoded = BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.fuji_leather_texture,
                    BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                    }
                ) ?: return@runCatching null
                decoded.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(modifier.background(if (lightMode) Color(0xFFF1F1F1) else FujiSkin.bodyDeep)) {
        val image = leatherBitmap
        if (image != null && !lightMode) {
            Box(
                Modifier.fillMaxSize().drawWithCache {
                    val destinationWidth = size.width.roundToInt().coerceAtLeast(1)
                    val destinationHeight = size.height.roundToInt().coerceAtLeast(1)
                    val sourceAspect = image.width.toFloat() / image.height.coerceAtLeast(1)
                    val destinationAspect = destinationWidth.toFloat() / destinationHeight
                    val sourceWidth: Int
                    val sourceHeight: Int
                    val sourceX: Int
                    val sourceY: Int
                    if (sourceAspect > destinationAspect) {
                        sourceHeight = image.height
                        sourceWidth = (image.height * destinationAspect).roundToInt().coerceIn(1, image.width)
                        sourceX = (image.width - sourceWidth) / 2
                        sourceY = 0
                    } else {
                        sourceWidth = image.width
                        sourceHeight = (image.width / destinationAspect).roundToInt().coerceIn(1, image.height)
                        sourceX = 0
                        sourceY = (image.height - sourceHeight) / 2
                    }
                    onDrawBehind {
                        drawImage(
                            image = image,
                            srcOffset = IntOffset(sourceX, sourceY),
                            srcSize = IntSize(sourceWidth, sourceHeight),
                            dstSize = IntSize(destinationWidth, destinationHeight)
                        )
                    }
                }
            )
        }
        if (!lightMode) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        if (!simpleMode && !lightMode) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.24f))
                    )
                )
            )
        }
        content()
    }
}

@Composable
fun FujiMetalTopPlate(
    modifier: Modifier = Modifier,
    height: Dp = 60.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth().height(height)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFF2F2EE), Color(0xFFE5E6E3), Color(0xFFD3D5D8))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.82f), RoundedCornerShape(12.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Fine horizontal brushed-metal grain; the base remains tile-like and has no dark centre band.
            val lineGap = 2.4.dp.toPx()
            var y = 0.5.dp.toPx()
            var index = 0
            while (y < size.height) {
                val alpha = if (index % 3 == 0) 0.075f else 0.032f
                drawLine(
                    Color.White.copy(alpha = alpha),
                    Offset(0f, y),
                    Offset(size.width, y),
                    strokeWidth = 0.35.dp.toPx()
                )
                if (index % 4 == 0) {
                    drawLine(
                        Color(0xFF7D8589).copy(alpha = 0.045f),
                        Offset(0f, y + 0.8.dp.toPx()),
                        Offset(size.width, y + 0.8.dp.toPx()),
                        strokeWidth = 0.25.dp.toPx()
                    )
                }
                y += lineGap
                index++
            }
            drawLine(Color.White.copy(alpha = 0.85f), Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx())
            drawLine(Color(0xFF777D80).copy(alpha = 0.65f), Offset(0f, size.height - 0.5.dp.toPx()), Offset(size.width, size.height - 0.5.dp.toPx()), 1.dp.toPx())
        }
        content()
        Row(
            Modifier.align(Alignment.CenterStart).padding(start = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { repeat(2) { FujiScrew() } }
        Row(
            Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { repeat(2) { FujiScrew() } }
    }
}

@Composable
private fun FujiScrew() {
    Box(
        Modifier.size(6.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFF8F8F5), Color(0xFF9CA2A4), Color(0xFF6B7174))))
            .border(0.55.dp, Color(0xFF62686B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(3.5.dp)) {
            val stroke = 0.45.dp.toPx()
            drawLine(Color(0xFF414648), Offset(0f, 0f), Offset(size.width, size.height), stroke)
            drawLine(Color(0xFF414648), Offset(size.width, 0f), Offset(0f, size.height), stroke)
        }
    }
}

@Composable
fun FujiIconGlyph(
    name: String,
    modifier: Modifier = Modifier,
    tint: Color = FujiSkin.ivory
) {
    Canvas(modifier.size(22.dp)) {
        val unit = min(size.width, size.height) / 24f
        val stroke = 1.6.dp.toPx()
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = stroke) {
            drawLine(tint, point(x1, y1), point(x2, y2), width, StrokeCap.Round)
        }
        fun circle(x: Float, y: Float, r: Float, fill: Boolean = false) {
            drawCircle(tint, r * unit, point(x, y), style = if (fill) androidx.compose.ui.graphics.drawscope.Fill else Stroke(stroke, cap = StrokeCap.Round))
        }
        fun poly(vararg xy: Float) {
            val path = Path()
            if (xy.size >= 4) {
                path.moveTo(xy[0] * unit, xy[1] * unit)
                var i = 2
                while (i + 1 < xy.size) { path.lineTo(xy[i] * unit, xy[i + 1] * unit); i += 2 }
                drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
        when (name) {
            "wrench" -> {
                line(14f, 4f, 19f, 9f, 2.0.dp.toPx())
                line(17.5f, 5.5f, 8f, 15f, 2.1.dp.toPx())
                circle(6.3f, 17f, 3.1f)
                line(4f, 19.3f, 2.5f, 21f)
                line(13f, 4f, 17f, 2.5f)
            }
            "display" -> {
                drawRoundRect(
                    tint,
                    point(2.5f, 3.5f),
                    androidx.compose.ui.geometry.Size(19f * unit, 14f * unit),
                    androidx.compose.ui.geometry.CornerRadius(1.8f * unit),
                    style = Stroke(stroke)
                )
                line(12f, 17.5f, 12f, 21f)
                line(7f, 21f, 17f, 21f)
                line(5.5f, 8f, 18.5f, 8f, 1.1.dp.toPx())
                line(5.5f, 12f, 14f, 12f, 1.1.dp.toPx())
            }
            "camera" -> {
                drawRoundRect(
                    tint,
                    point(3f, 7f),
                    androidx.compose.ui.geometry.Size(18f * unit, 14f * unit),
                    androidx.compose.ui.geometry.CornerRadius(2.2f * unit),
                    style = Stroke(stroke)
                )
                line(7f, 7f, 9f, 4f)
                line(9f, 4f, 15f, 4f)
                circle(12f, 14f, 4f)
                circle(18f, 9f, 0.8f, true)
            }
            "settings", "gear" -> {
                circle(12f, 12f, 8f)
                circle(12f, 12f, 2.8f)
                line(12f, 1.5f, 12f, 4f)
                line(12f, 20f, 12f, 22.5f)
                line(1.5f, 12f, 4f, 12f)
                line(20f, 12f, 22.5f, 12f)
                line(4.3f, 4.3f, 6.1f, 6.1f)
                line(17.9f, 17.9f, 19.7f, 19.7f)
                line(17.9f, 6.1f, 19.7f, 4.3f)
                line(4.3f, 19.7f, 6.1f, 17.9f)
            }
            "flash", "flash_on", "flash_off" -> {
                poly(13f, 2.5f, 6.5f, 13.5f, 11.5f, 13.5f, 9.5f, 21.5f, 17.5f, 10.5f, 12.5f, 10.5f, 13f, 2.5f)
                if (name == "flash_off") line(3f, 4f, 21f, 21f, stroke)
            }
            "timer" -> {
                circle(12f, 13f, 8.2f)
                line(12f, 3f, 12f, 5f)
                line(9f, 2.5f, 15f, 2.5f)
                line(12f, 13f, 12f, 7.5f)
                line(12f, 13f, 16.5f, 15f)
            }
            "film" -> {
                drawRoundRect(tint, point(3f, 4f), androidx.compose.ui.geometry.Size(18f * unit, 16f * unit), androidx.compose.ui.geometry.CornerRadius(1.6f * unit), style = Stroke(stroke))
                for (y in listOf(7f, 12f, 17f)) {
                    circle(5f, y, 0.7f, true); circle(19f, y, 0.7f, true)
                }
                line(8f, 4f, 8f, 20f, 1.1.dp.toPx()); line(16f, 4f, 16f, 20f, 1.1.dp.toPx())
            }
            "sliders", "tune" -> {
                line(3f, 6f, 21f, 6f); line(3f, 12f, 21f, 12f); line(3f, 18f, 21f, 18f)
                circle(8f, 6f, 2f); circle(16f, 12f, 2f); circle(11f, 18f, 2f)
            }
            "histogram" -> {
                line(3f, 20f, 21f, 20f, 1.1.dp.toPx())
                drawLine(tint, point(5f, 19f), point(5f, 14f), stroke)
                drawLine(tint, point(9.5f, 19f), point(9.5f, 8f), stroke)
                drawLine(tint, point(14f, 19f), point(14f, 11f), stroke)
                drawLine(tint, point(18.5f, 19f), point(18.5f, 4f), stroke)
            }
            "af" -> {
                line(4f, 9f, 4f, 4f); line(4f, 4f, 9f, 4f)
                line(15f, 4f, 20f, 4f); line(20f, 4f, 20f, 9f)
                line(4f, 15f, 4f, 20f); line(4f, 20f, 9f, 20f)
                line(15f, 20f, 20f, 20f); line(20f, 20f, 20f, 15f)
                circle(12f, 12f, 1.4f, true)
            }
            "close" -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
            "sun" -> {
                circle(12f, 12f, 5f)
                line(12f, 1.5f, 12f, 4f)
                line(12f, 20f, 12f, 22.5f)
                line(1.5f, 12f, 4f, 12f)
                line(20f, 12f, 22.5f, 12f)
                line(4.5f, 4.5f, 6.6f, 6.6f)
                line(17.4f, 17.4f, 19.5f, 19.5f)
                line(17.4f, 6.6f, 19.5f, 4.5f)
                line(4.5f, 19.5f, 6.6f, 17.4f)
            }
            "moon" -> {
                val path = Path().apply {
                    moveTo(15f * unit, 3f * unit)
                    cubicTo(7f * unit, 3f * unit, 4f * unit, 9f * unit, 6f * unit, 15f * unit)
                    cubicTo(8f * unit, 21f * unit, 16f * unit, 22f * unit, 21f * unit, 16f * unit)
                    cubicTo(14f * unit, 18f * unit, 10f * unit, 11f * unit, 15f * unit, 3f * unit)
                }
                drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            "tab_01" -> {
                line(3f, 9f, 3f, 3f); line(3f, 3f, 9f, 3f); line(15f, 3f, 21f, 3f); line(21f, 3f, 21f, 9f)
                line(3f, 15f, 3f, 21f); line(3f, 21f, 9f, 21f); line(15f, 21f, 21f, 21f); line(21f, 21f, 21f, 15f)
                line(12f, 7f, 12f, 17f, 1.1.dp.toPx()); line(7f, 12f, 17f, 12f, 1.1.dp.toPx())
            }
            "tab_02" -> { circle(12f, 12f, 9f); circle(12f, 12f, 5.2f); circle(12f, 12f, 1.2f, true) }
            "tab_03" -> {
                drawRect(tint, point(3f, 3f), androidx.compose.ui.geometry.Size(18f * unit, 18f * unit), style = Stroke(stroke))
                drawRect(tint, point(7f, 7f), androidx.compose.ui.geometry.Size(10f * unit, 10f * unit), style = Stroke(stroke))
            }
            "tab_04" -> {
                val path = Path().apply { moveTo(3f * unit, 18f * unit); cubicTo(4f * unit, 7f * unit, 20f * unit, 7f * unit, 21f * unit, 18f * unit) }
                drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round)); line(3f, 18f, 21f, 18f)
            }
            "tab_05" -> {
                val path = Path().apply { moveTo(15f * unit, 3f * unit); cubicTo(6f * unit, 4f * unit, 4f * unit, 17f * unit, 14f * unit, 21f * unit); cubicTo(8f * unit, 15f * unit, 9f * unit, 7f * unit, 15f * unit, 3f * unit) }
                drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            "tab_06" -> {
                drawRect(tint, point(3f, 3f), androidx.compose.ui.geometry.Size(18f * unit, 18f * unit), style = Stroke(stroke))
                line(3f, 3f, 21f, 21f); line(21f, 3f, 3f, 21f)
            }
            "tab_07" -> {
                val path = Path().apply { moveTo(4f * unit, 9f * unit); lineTo(8f * unit, 9f * unit); lineTo(14f * unit, 4f * unit); lineTo(14f * unit, 20f * unit); lineTo(8f * unit, 15f * unit); lineTo(4f * unit, 15f * unit); close() }
                drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                line(17f, 9f, 20f, 6f); line(17f, 15f, 20f, 18f)
            }
            "tab_08" -> {
                circle(12f, 12f, 9f); line(3f, 12f, 21f, 12f)
                val path = Path().apply { moveTo(12f * unit, 3f * unit); cubicTo(6f * unit, 8f * unit, 6f * unit, 16f * unit, 12f * unit, 21f * unit) }
                drawPath(path, tint, style = Stroke(stroke))
                val path2 = Path().apply { moveTo(12f * unit, 3f * unit); cubicTo(18f * unit, 8f * unit, 18f * unit, 16f * unit, 12f * unit, 21f * unit) }
                drawPath(path2, tint, style = Stroke(stroke))
            }
            "tab_09" -> { circle(12f, 12f, 9f); line(12f, 10f, 12f, 16f); circle(12f, 7f, 0.8f, true) }
            else -> { circle(12f, 12f, 8f); line(8f, 12f, 16f, 12f) }
        }
    }
}

@Composable
fun FujiTopKey(
    label: String,
    icon: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressedOffset by animateDpAsState(if (pressed) 1.5.dp else 0.dp, tween(80), label = "fuji-key-press")
    Column(
        modifier = modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
            .semantics { contentDescription = if (label.equals("Q", true)) "Quick settings" else label.uppercase() }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    if (enabled) {
                        if (view.context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
                                .getBoolean("haptic_feedback_enabled", true)
                        ) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        FujiFeedback.keyTick(view.context)
                        onClick()
                    }
                },
                onLongClick = if (enabled) onLongClick else null
            )
            .padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.offset(y = 1.dp).size(44.dp).clip(CircleShape)
                    .background(Color(0xFF8A9093).copy(alpha = 0.36f))
                    .border(1.dp, Color(0xFF777D80), CircleShape)
            )
            Box(
                Modifier.offset(y = pressedOffset).size(44.dp).clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFF4F4F1), Color(0xFFE0E2E1), Color(0xFFC9CCCF))
                        )
                    )
                    .border(1.dp, Color(0xFF7D8588), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Color.White.copy(alpha = 0.72f), size.minDimension * 0.43f, Offset(size.width * 0.38f, size.height * 0.33f), style = Stroke(0.7.dp.toPx()))
                }
                if (label.equals("Q", true)) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF32383A), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        Text("Q", color = Color(0xFF202426), fontSize = 16.sp, fontWeight = FontWeight.Black, lineHeight = 17.sp)
                    }
                } else {
                    FujiIconGlyph(icon, Modifier.size(22.dp), Color(0xE01B1B1B))
                    FujiIconGlyph(icon, Modifier.size(22.dp).offset(0.6.dp, 0.6.dp), Color.White.copy(alpha = 0.70f))
                }
            }
        }
        Text(
            label.uppercase(),
            color = Color(0xFFF2F2F2),
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.72.sp,
            maxLines = 1
        )
        Box(
            Modifier.padding(top = 1.dp).width(18.dp).height(0.5.dp)
                .background(Color.White.copy(alpha = 0.70f))
        )
        if (selected) {
            Box(Modifier.padding(top = 1.dp).size(4.dp).clip(CircleShape).background(FujiSkin.orange))
        } else {
            Spacer(Modifier.height(5.dp))
        }
    }
}

internal const val FUJI_KEY_HOLD_THRESHOLD_MS = 180L

internal fun isFujiKeyHold(durationMillis: Long): Boolean =
    durationMillis >= FUJI_KEY_HOLD_THRESHOLD_MS

@Composable
fun FujiKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onPressStart: (() -> Unit)? = null,
    onPressEnd: (() -> Unit)? = null
) {
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val clickPressed by interaction.collectIsPressedAsState()
    val hasPressCallbacks = onPressStart != null || onPressEnd != null
    val latestPressStart by rememberUpdatedState(onPressStart)
    val latestPressEnd by rememberUpdatedState(onPressEnd)
    val latestClick by rememberUpdatedState(onClick)
    var rawPressed by remember { mutableStateOf(false) }
    val isPressed = if (hasPressCallbacks) rawPressed else clickPressed
    val y by animateDpAsState(if (isPressed) 1.dp else 0.dp, tween(Tuning.BUTTON_PRESS_MS), label = "fuji-wide-key")
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        if (isPressed) Tuning.BUTTON_PRESS_SCALE else 1f,
        tween(Tuning.BUTTON_PRESS_MS),
        label = "fuji-wide-key-scale"
    )

    val inputModifier = if (hasPressCallbacks) {
        Modifier.pointerInput(enabled, hasPressCallbacks) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )
                if (!enabled) return@awaitEachGesture

                down.consume()
                view.requestUnbufferedDispatch(down.id.value.toInt())
                val startedAt = SystemClock.uptimeMillis()
                CrashContextDiagnostics.recordUiEvent(
                    "KEY_DOWN label=${label.uppercase()} x=${down.position.x.roundToInt()} y=${down.position.y.roundToInt()}"
                )
                rawPressed = true
                android.util.Log.d(
                    "FUJI_KEY",
                    "ACTION_DOWN label=$label x=${down.position.x} y=${down.position.y} enabled=$enabled"
                )
                latestPressStart?.invoke()

                var releasedNormally = false
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null) break
                        if (!change.pressed) {
                            change.consume()
                            releasedNormally = true
                            break
                        }
                        change.consume()
                    }
                } finally {
                    rawPressed = false
                    latestPressEnd?.invoke()
                    val durationMs = SystemClock.uptimeMillis() - startedAt
                    val wasHold = isFujiKeyHold(durationMs)
                    android.util.Log.d(
                        "FUJI_KEY",
                        "ACTION_UP label=$label durationMs=$durationMs held=$wasHold released=$releasedNormally"
                    )
                    CrashContextDiagnostics.recordUiEvent(
                        "KEY_UP label=${label.uppercase()} duration_ms=$durationMs held=$wasHold released=$releasedNormally"
                    )
                    if (releasedNormally && !wasHold && enabled) {
                        if (view.context.getSharedPreferences(
                                "fujifilm_settings",
                                Context.MODE_PRIVATE
                            ).getBoolean("haptic_feedback_enabled", true)
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                        FujiFeedback.keyTick(view.context)
                        latestClick()
                    }
                }
            }
        }
    } else {
        Modifier.combinedClickable(
            enabled = enabled,
            interactionSource = interaction,
            indication = null,
            onClick = {
                if (view.context.getSharedPreferences(
                        "fujifilm_settings",
                        Context.MODE_PRIVATE
                    ).getBoolean("haptic_feedback_enabled", true)
                ) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                FujiFeedback.keyTick(view.context)
                latestClick()
            },
            onLongClick = null
        )
    }

    Box(
        modifier.heightIn(min = 36.dp)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2C2E), Color(0xFF1A1B1D))))
            .border(1.dp, Color(0xFF5A5D60), RoundedCornerShape(12.dp))
            .then(inputModifier)
            .padding(horizontal = 3.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label.uppercase(),
            modifier = Modifier
                .offset(y = y),
            color = if (enabled) Color(0xFFE8ECE9) else Color(0xFF858B8D),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
            maxLines = 1
        )
    }
}

@Composable
fun FujiScreenScaffold(
    title: String,
    breadcrumb: String? = null,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showVersion: Boolean = true,
    showHeader: Boolean = true,
    footerActions: Boolean = false,
    onFooterBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val kk7Font = FujiTypography.composeFamily(androidx.compose.ui.platform.LocalContext.current)
    val cleanVersionName = BuildConfig.VERSION_NAME.replace(Regex("(?i)dream$"), "").trim()
    Column(
        modifier.fillMaxSize()
            .clip(RoundedCornerShape(5.dp))
            .background(FujiSkin.body)
            .border(1.dp, Color(0xFF9CA2A4), RoundedCornerShape(5.dp))
    ) {
        if (showHeader) {
            Row(
                Modifier.fillMaxWidth().height(48.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0xFFF2F2EE), FujiSkin.silver, Color(0xFFD3D5D8))))
                    .padding(start = 12.dp, end = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(title.uppercase(), color = Color(0xFF171A1B), fontSize = 10.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 0.85.sp, maxLines = 1)
                    breadcrumb?.takeIf { it.isNotBlank() }?.let { crumb ->
                        Text("  ›  ", color = Color(0xFF454D50), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(crumb.uppercase(), color = Color(0xFF333A3D), fontSize = 9.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.55.sp, maxLines = 1)
                    }
                }
                if (onBack != null) {
                    FujiKey("THOÁT", onClick = onBack, modifier = Modifier.width(82.dp).height(34.dp))
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .background(FujiSkin.lcd)
                .padding(horizontal = 9.dp, vertical = 7.dp),
            content = content
        )
        if (footerActions) {
            Row(
                Modifier.fillMaxWidth().height(48.dp)
                    .background(Color(0xFF101516))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FujiKey(
                    "QUAY LẠI",
                    onClick = onFooterBack ?: {},
                    modifier = Modifier.width(112.dp).height(36.dp)
                )
                FujiKey(
                    "THOÁT",
                    onClick = onBack ?: {},
                    modifier = Modifier.width(88.dp).height(36.dp)
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(27.dp)
                    .background(Color(0xFF101516))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.settings_footer_hint),
                    color = FujiSkin.lcdGreen,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1
                )
                if (showVersion) {
                    Text(
                        stringResource(R.string.fujifilm_mode_version, cleanVersionName).uppercase(),
                        color = FujiSkin.muted,
                        fontSize = 7.sp,
                        fontFamily = kk7Font,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun FujiPanel(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    breadcrumb: String? = null,
    embedded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    if (embedded) {
        Column(modifier.fillMaxSize(), content = content)
    } else {
        FujiScreenScaffold(
            title = title,
            breadcrumb = breadcrumb,
            modifier = modifier,
            onBack = onBack,
            content = content
        )
    }
}

@Composable
fun FujiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val view = LocalView.current
    val knobX by animateDpAsState(
        if (checked) 23.dp else 2.dp,
        spring(dampingRatio = 0.68f, stiffness = 520f),
        label = "fuji-switch"
    )
    Row(
        modifier = modifier.combinedClickable(
            enabled = enabled,
            onClick = {
                if (view.context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
                        .getBoolean("haptic_feedback_enabled", true)
                ) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                FujiFeedback.switchClack(view.context)
                onCheckedChange(!checked)
            },
            onLongClick = null
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier.size(width = 46.dp, height = 24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF292E30), Color(0xFF07090A))))
                .border(1.dp, Color(0xFF7C8588), RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier.offset(x = knobX, y = 2.dp).size(18.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFF5F5F1), Color(0xFFD2D6D7), Color(0xFF899195))))
                    .border(0.7.dp, Color.White.copy(alpha = 0.58f), RoundedCornerShape(3.dp))
            )
        }
        Box(Modifier.size(5.dp).clip(CircleShape).background(if (checked) FujiSkin.orange else Color(0xFF34383A)))
        Text(
            if (checked) "ON" else "OFF",
            color = if (checked) FujiSkin.lcdGreen else FujiSkin.muted,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun FujiChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Column(
        modifier.clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF383D3F), Color(0xFF181B1C))))
            .border(1.dp, if (selected) FujiSkin.orange else Color(0xFF4A5255), RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = {
                    if (view.context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
                            .getBoolean("haptic_feedback_enabled", true)
                    ) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    FujiFeedback.keyTick(view.context)
                    onClick()
                },
                onLongClick = null
            )
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, color = if (selected) FujiSkin.lcdGreen else Color(0xFFE2E4E3), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        if (selected) Spacer(Modifier.padding(top = 2.dp).width(12.dp).height(2.dp).background(FujiSkin.orange))
    }
}

@Composable
fun FujiTile(
    title: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: String? = null,
    modifier: Modifier = Modifier
) {
    val kk7Font = FujiTypography.composeFamily(androidx.compose.ui.platform.LocalContext.current)
    Column(
        modifier.height(72.dp).clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF101516))
            .border(1.dp, if (selected) FujiSkin.orange else Color(0xFF354247), RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = null)
            .padding(9.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (icon != null) FujiIconGlyph(icon, Modifier.size(16.dp), FujiSkin.lcdGreen)
        Text(title.uppercase(), color = Color(0xFFC7D0D1), fontSize = 8.sp, letterSpacing = 0.7.sp, maxLines = 1)
        Text(value, color = FujiSkin.lcdGreen, fontFamily = kk7Font, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun FujiListRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    adjustable: Boolean = false,
    navigate: Boolean = true
) {
    val kk7Font = FujiTypography.composeFamily(androidx.compose.ui.platform.LocalContext.current)
    Column(
        modifier.fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = null)
            .padding(vertical = 6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color(0xFFE8EBE9), fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(
                when {
                    adjustable -> "<  $value  >"
                    else -> value
                },
                color = FujiSkin.lcdGreen,
                fontSize = 10.sp,
                fontFamily = kk7Font,
                maxLines = 1
            )
            if (navigate && !adjustable) Text("  ›", color = FujiSkin.lcdGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (description != null) Text(description, color = FujiSkin.muted, fontSize = 8.sp, modifier = Modifier.padding(top = 3.dp))
        Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(0.6.dp).background(Color(0xFF2A383B)))
    }
}

@Composable
fun FujiSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Tìm..."
) {
    val kk7Font = FujiTypography.composeFamily(androidx.compose.ui.platform.LocalContext.current)
    val cursorTransition = rememberInfiniteTransition(label = "fuji-search-cursor")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(530), repeatMode = RepeatMode.Reverse),
        label = "fuji-search-cursor-alpha"
    )
    Row(
        modifier.clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF060909))
            .border(1.dp, Color(0xFF536C6E), RoundedCornerShape(2.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FujiIconGlyph("tab_02", Modifier.size(15.dp), FujiSkin.lcdGreen)
        Spacer(Modifier.width(7.dp))
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(FujiSkin.lcdGreen.copy(alpha = cursorAlpha)),
                textStyle = TextStyle(color = FujiSkin.lcdGreen, fontSize = 10.sp, fontFamily = kk7Font),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text(placeholder, color = FujiSkin.muted, fontSize = 10.sp)
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
fun FujiDial(
    value: String,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
    stepSize: Float = 22f
) {
    val kk7Font = FujiTypography.composeFamily(androidx.compose.ui.platform.LocalContext.current)
    var rotation by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(rotation, tween(90), label = "fuji-dial-rotation")
    val view = LocalView.current
    Box(
        modifier.size(70.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFE2E4E2), Color(0xFF858C8F), Color(0xFF252A2C))))
            .border(4.dp, Color(0xFF282D2F), CircleShape)
            .pointerInput(stepSize) {
                var accumulated = 0f
                detectDragGestures { change, drag ->
                    change.consume()
                    accumulated += (drag.x - drag.y) / stepSize
                    val steps = accumulated.toInt()
                    if (steps != 0) {
                        accumulated -= steps
                        rotation += steps * 12f
                        onStep(steps)
                        if (view.context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
                                .getBoolean("haptic_feedback_enabled", true)
                        ) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        FujiFeedback.dialClick(view.context)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(5.dp).graphicsLayer { rotationZ = animatedRotation }) {
            val radius = size.minDimension / 2f
            for (i in 0 until 36) {
                val angle = i * (Math.PI * 2.0 / 36.0)
                val inner = radius * 0.83f
                val outer = if (i % 3 == 0) radius * 0.99f else radius * 0.92f
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawLine(
                    Color(0xFF33393B),
                    androidx.compose.ui.geometry.Offset(cx + kotlin.math.cos(angle).toFloat() * inner, cy + kotlin.math.sin(angle).toFloat() * inner),
                    androidx.compose.ui.geometry.Offset(cx + kotlin.math.cos(angle).toFloat() * outer, cy + kotlin.math.sin(angle).toFloat() * outer),
                    strokeWidth = if (i % 3 == 0) 1.6.dp.toPx() else 0.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(Color(0xFF202527), radius * 0.68f, style = Stroke(width = 1.dp.toPx()))
        }
        Box(
            Modifier.size(47.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFF5F5F1), Color(0xFFB9BFC1), Color(0xFF7C8589))))
                .border(1.dp, Color(0xFF697174), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(value, color = Color(0xFF1A2022), fontFamily = kk7Font, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
        }
        Box(Modifier.align(Alignment.TopCenter).padding(top = 3.dp).size(3.dp).clip(CircleShape).background(FujiSkin.orange))
    }
}

@Composable
fun FujiScrollbar(
    fraction: Float,
    visibleFraction: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.width(3.dp)) {
        drawRoundRect(Color(0xFF334044), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2))
        val indicatorHeight = (size.height * visibleFraction.coerceIn(0.08f, 1f)).coerceAtLeast(8.dp.toPx())
        val top = (size.height - indicatorHeight) * fraction.coerceIn(0f, 1f)
        drawRoundRect(
            FujiSkin.lcdGreen,
            topLeft = androidx.compose.ui.geometry.Offset(0f, top),
            size = androidx.compose.ui.geometry.Size(size.width, indicatorHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2)
        )
    }
}


/** Two-stage mechanical shutter: half-press focuses; release triggers the existing capture action. */
@Composable
fun FujiShutterButton(
    enabled: Boolean,
    onHalfPress: () -> Unit,
    onCapture: () -> Unit,
    onLongPress: () -> Unit = onCapture,
    onRelease: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(if (pressed) Tuning.BUTTON_PRESS_SCALE else 1f, tween(Tuning.BUTTON_PRESS_MS), label = "fuji-shutter")
    Box(
        modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF52585B), Color(0xFF111516))))
            .border(2.dp, Color(0xFF060808), CircleShape)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            pressed = true
                            if (view.context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
                                    .getBoolean("haptic_feedback_enabled", true)
                            ) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onHalfPress()
                            tryAwaitRelease()
                            pressed = false
                            onRelease()
                        }
                    },
                    onLongPress = {
                        if (enabled) {
                            if (view.context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
                                    .getBoolean("haptic_feedback_enabled", true)
                            ) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onLongPress()
                        }
                    },
                    onTap = {
                        if (enabled) {
                            if (view.context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
                                    .getBoolean("haptic_feedback_enabled", true)
                            ) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onCapture()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (!enabled) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(0.96f),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
        Box(
            Modifier.fillMaxSize(0.82f).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD6D8D8), Color(0xFF737B7F))))
                .border(2.dp, Color(0xFF222729), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.fillMaxSize(0.80f).clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFEEF0EE), Color(0xFF9DA4A7), Color(0xFF6F777A))))
                    .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    }
}


/** LCD-style fader with etched tick marks; replaces the default Material slider. */
@Composable
fun FujiSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = modifier.height(26.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
        val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
        val thumbX = (maxWidth * fraction).coerceIn(7.dp, (maxWidth - 7.dp).coerceAtLeast(7.dp))
        Canvas(Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val endX = size.width
            val currentX = endX * fraction
            drawLine(
                Color(0xFF344145),
                androidx.compose.ui.geometry.Offset(0f, centerY),
                androidx.compose.ui.geometry.Offset(endX, centerY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                FujiSkin.lcdGreen.copy(alpha = if (enabled) 0.88f else 0.35f),
                androidx.compose.ui.geometry.Offset(0f, centerY),
                androidx.compose.ui.geometry.Offset(currentX, centerY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            val tickCount = (steps + 1).coerceIn(1, 60)
            if (steps > 0) {
                for (i in 0..tickCount) {
                    val x = endX * i / tickCount
                    drawLine(
                        Color(0xFF778387),
                        androidx.compose.ui.geometry.Offset(x, centerY - 7.dp.toPx()),
                        androidx.compose.ui.geometry.Offset(x, centerY + 7.dp.toPx()),
                        strokeWidth = 0.7.dp.toPx()
                    )
                }
            }
        }
        Box(
            Modifier.offset(x = thumbX - 7.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFFFF), FujiSkin.silver, Color(0xFF8A9295))
                    )
                )
                .border(1.dp, if (enabled) FujiSkin.orange else Color(0xFF667075), CircleShape)
        )
        if (enabled) {
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(valueRange, steps) {
                        detectTapGestures { position ->
                            val raw = (position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val snapped = if (steps > 0) (raw * (steps + 1)).roundToInt().toFloat() / (steps + 1) else raw
                            onValueChange(valueRange.start + span * snapped)
                            onValueChangeFinished?.invoke()
                        }
                    }
                    .pointerInput(valueRange, steps) {
                        detectDragGestures(
                            onDragStart = { position ->
                                val raw = (position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                val snapped = if (steps > 0) (raw * (steps + 1)).roundToInt().toFloat() / (steps + 1) else raw
                                onValueChange(valueRange.start + span * snapped)
                            },
                            onDragEnd = { onValueChangeFinished?.invoke() },
                            onDragCancel = { onValueChangeFinished?.invoke() }
                        ) { change, _ ->
                            val raw = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val snapped = if (steps > 0) (raw * (steps + 1)).roundToInt().toFloat() / (steps + 1) else raw
                            onValueChange(valueRange.start + span * snapped)
                            change.consume()
                        }
                    }
            )
        }
    }
}

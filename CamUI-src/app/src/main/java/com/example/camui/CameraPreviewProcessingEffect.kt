package com.example.camui

import android.graphics.BlendMode
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Deterministic preview-only LunaDream aurora and NightShot star renderer.
 * API 33+ uses one AGSL pass for aurora mesh, prism sweep and rainbow edge;
 * older devices use soft Canvas gradients. The captured bitmap is never modified.
 */
internal object CameraPreviewProcessingEffect {
    private const val TAG = "FUJI_PREVIEW_EFFECT"
    private const val SEED = 0x5F17A
    private const val AGSL = """
        uniform float2 uResolution;
        uniform float uTime;
        uniform float uStrength;
        uniform float uHold;
        uniform float uBloom;
        uniform float uDensity;
        float wave(float h, float s) {
            return 0.5 + 0.5 * cos(6.2831853 * (h + s));
        }
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / uResolution;
            float t = uTime;
            float2 c0 = float2(0.18 + 0.10 * sin(t * 0.68), 0.24 + 0.12 * cos(t * 0.51));
            float2 c1 = float2(0.42 + 0.13 * sin(t * 0.44 + 1.2), 0.70 + 0.11 * cos(t * 0.73));
            float2 c2 = float2(0.72 + 0.10 * sin(t * 0.59 + 2.1), 0.29 + 0.12 * cos(t * 0.61 + 0.8));
            float2 c3 = float2(0.84 + 0.08 * sin(t * 0.76 + 3.1), 0.76 + 0.09 * cos(t * 0.49 + 2.6));
            float2 c4 = float2(0.31 + 0.11 * sin(t * 0.52 + 4.2), 0.48 + 0.10 * cos(t * 0.67 + 3.7));
            float b0 = exp(-dot((uv - c0), (uv - c0)) * 18.0);
            float b1 = exp(-dot((uv - c1), (uv - c1)) * 14.0);
            float b2 = exp(-dot((uv - c2), (uv - c2)) * 19.0);
            float b3 = exp(-dot((uv - c3), (uv - c3)) * 16.0);
            float b4 = exp(-dot((uv - c4), (uv - c4)) * 15.0);
            float blob = clamp((b0 + b1 + b2 + b3 + b4) * 0.32, 0.0, 1.0);
            float edgeDistance = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
            float edgeMask = 1.0 - smoothstep(0.30, 0.49, edgeDistance);
            float hue = mod((fragCoord.x + fragCoord.y * 1.4) /
                            (uResolution.x + uResolution.y * 1.4) + t * 0.15, 1.0);
            float3 rgbHue = float3(wave(hue, 0.0), wave(hue, 0.3333), wave(hue, 0.6667));
            float3 pastel = clamp(float3(0.80) + (rgbHue - 0.5) * 0.30, 0.0, 1.0);
            float auraAlpha = mix(0.11, 0.39, edgeMask) * blob * uStrength;
            float sweepTime = mod(t, 2.4);
            float sweepActive = 1.0 - step(0.90, sweepTime);
            float sweepPosition = -0.22 + 1.48 * (sweepTime / 0.90);
            float sweepCoord = uv.x + 0.364 * uv.y;
            float prism = exp(-pow((sweepCoord - sweepPosition) * 5.5, 2.0)) *
                          sweepActive * 0.10 * uStrength;
            float perimeter = min(min(uv.x, 1.0 - uv.x) * uResolution.x,
                                  min(uv.y, 1.0 - uv.y) * uResolution.y);
            float border = 1.0 - smoothstep(2.0 * uDensity, 10.0 * uDensity, perimeter);
            float breath = 0.80 + 0.20 * (0.5 + 0.5 * sin(t * 6.2831853 * 0.6));
            float borderAlpha = border * 0.70 * breath * uStrength * (1.0 + 0.25 * uHold);
            float3 rgb = pastel * (auraAlpha + prism) + rgbHue * borderAlpha;
            rgb *= (1.0 + 0.25 * uBloom);
            float alpha = clamp(auraAlpha + prism + borderAlpha, 0.0, 0.92);
            return half4(half3(clamp(rgb, 0.0, 1.0)), half(alpha));
        }
    """

    private data class Star(
        val x: Float, val y: Float, val size: Float, val layer: Int,
        val hue: Int, val threshold: Float, val frequency: Float, val phase: Float
    )

    private var runtimeShader: RuntimeShader? = null
    private var shaderFailureLogged = false
    private val starField = buildStarField()

    private fun buildStarField(): List<Star> {
        val random = Random(SEED)
        return List(140) { index ->
            val layer = when {
                index < 98 -> 0
                index < 133 -> 1
                else -> 2
            }
            val x = random.nextFloat()
            var y = random.nextFloat()
            if (x in 0.30f..0.70f && y in 0.28f..0.72f) y = if (y < 0.5f) y * 0.5f else 0.72f + (y - 0.72f) * 0.5f
            Star(
                x.coerceIn(0.02f, 0.98f), y.coerceIn(0.02f, 0.98f),
                when (layer) {
                    0 -> 0.8f + random.nextFloat() * 0.6f
                    1 -> 1.4f + random.nextFloat()
                    else -> 2.4f + random.nextFloat() * 1.6f
                },
                layer,
                when (random.nextInt(100)) {
                    in 0..54 -> 0
                    in 55..79 -> 1
                    else -> 2
                },
                random.nextFloat() * 0.88f,
                0.25f + random.nextFloat() * 0.85f,
                random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        mode: String,
        timeSeconds: Float,
        progressPercent: Int,
        reducedMotion: Boolean,
        simpleMode: Boolean,
        holdSteady: Boolean,
        idleLunaBorder: Boolean = false,
        activationProgress: Float = 1f,
        completionBloom: Boolean = false,
        density: Float = 1f
    ) {
        if (width <= 0f || height <= 0f) return
        val time = if (reducedMotion || simpleMode) 0f else timeSeconds.takeIf { it.isFinite() } ?: 0f
        val started = SystemClock.elapsedRealtimeNanos()
        when (mode) {
            "luna" -> {
                if (idleLunaBorder && progressPercent <= 0) {
                    val borderTime = if (activationProgress < 1f) activationProgress * 6.6667f else time
                    drawIdleBorder(canvas, width, height, borderTime, activationProgress, density)
                } else if (Build.VERSION.SDK_INT >= 33 && !simpleMode) {
                    drawAuroraShader(canvas, width, height, time, holdSteady, completionBloom, density)
                } else {
                    drawAuroraFallback(canvas, width, height, time, holdSteady, simpleMode, completionBloom, density)
                }
            }
            else -> drawNightSky(canvas, width, height, time, progressPercent, reducedMotion, simpleMode, completionBloom, density)
        }
        val elapsedUs = (SystemClock.elapsedRealtimeNanos() - started) / 1_000L
        if (elapsedUs > 3_000L) {
            Log.w(TAG, "SLOW_FRAME mode=" + mode + " renderUs=" + elapsedUs +
                " size=" + width.toInt() + "x" + height.toInt())
        }
    }

    private fun screenBlend(paint: Paint) {
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }

    private fun drawAuroraShader(
        canvas: Canvas, width: Float, height: Float, time: Float,
        hold: Boolean, bloom: Boolean, density: Float
    ) {
        val shader = runCatching { runtimeShader ?: RuntimeShader(AGSL).also { runtimeShader = it } }
            .getOrElse {
                if (!shaderFailureLogged) {
                    Log.w(TAG, "AGSL unavailable; Canvas fallback enabled", it)
                    shaderFailureLogged = true
                }
                drawAuroraFallback(canvas, width, height, time, hold, false, bloom)
                return
            }
        runCatching {
            shader.setFloatUniform("uResolution", width, height)
            shader.setFloatUniform("uTime", time)
            shader.setFloatUniform("uStrength", 1f)
            shader.setFloatUniform("uHold", if (hold) 1f else 0f)
            shader.setFloatUniform("uBloom", if (bloom) 1f else 0f)
            shader.setFloatUniform("uDensity", density.coerceAtLeast(0.5f))
            canvas.drawColor(android.graphics.Color.argb(if (hold) 26 else 20, 0, 0, 0))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = shader
                if (Build.VERSION.SDK_INT >= 29) blendMode = BlendMode.SCREEN
            }
            canvas.drawRect(0f, 0f, width, height, paint)
            drawPrismChromaticShift(canvas, width, height, time, density)
            drawSparkles(canvas, width, height, time, false, density)
        }.onFailure {
            Log.w(TAG, "AGSL rendering failed; Canvas fallback enabled", it)
            drawAuroraFallback(canvas, width, height, time, hold, false, bloom)
        }
    }

    private fun drawAuroraFallback(
        canvas: Canvas, width: Float, height: Float, time: Float,
        hold: Boolean, simple: Boolean, bloom: Boolean, density: Float
    ) {
        val shortSide = min(width, height)
        val veil = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(if (hold) 26 else 20, 0, 0, 0) }
        canvas.drawRect(0f, 0f, width, height, veil)
        if (!simple) {
            val colors = intArrayOf(0xFFFFA8C7.toInt(), 0xFFFFD1A8.toInt(), 0xFFD8E7B8.toInt(), 0xFFA9E6E0.toInt(), 0xFFB9C7FF.toInt())
            val centres = floatArrayOf(0.20f, 0.42f, 0.70f, 0.85f, 0.32f, 0.24f, 0.72f, 0.25f, 0.76f, 0.50f)
            for (i in 0 until 5) {
                val phase = i * 1.256637f
                val cx = (centres[i] + 0.10f * sin(time * 0.70f + phase)) * width
                val cy = (centres[i + 5] + 0.09f * cos(time * 0.52f + phase * 1.13f)) * height
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        cx, cy, shortSide * 0.52f,
                        intArrayOf(alphaColor(colors[i], 35), alphaColor(colors[i], 18), alphaColor(colors[i], 0)),
                        floatArrayOf(0f, 0.42f, 1f), Shader.TileMode.CLAMP
                    )
                    screenBlend(this)
                }
                canvas.drawCircle(cx, cy, shortSide * 0.52f, paint)
            }
            drawPrism(canvas, width, height, time)
        }
        val breath = 0.8f + 0.2f * (0.5f + 0.5f * sin(time * 2f * PI.toFloat() * 0.6f))
        val borderAlpha = (0.70f * breath * (if (hold) 1.25f else 1f) * (if (bloom) 1.2f else 1f)).coerceIn(0f, 0.9f)
        drawRainbowBorder(canvas, width, height, time, borderAlpha)
        drawSparkles(canvas, width, height, time, simple)
    }

    private fun drawIdleBorder(
        canvas: Canvas, width: Float, height: Float, time: Float,
        activation: Float, density: Float
    ) {
        val alpha = if (activation < 1f) activation.coerceIn(0f, 1f) else 0.35f
        drawRainbowBorder(canvas, width, height, time, alpha * (0.9f + 0.1f * sin(time * 3.77f)), density)
    }

    private fun drawRainbowBorder(
        canvas: Canvas, width: Float, height: Float, time: Float,
        alpha: Float, density: Float
    ) {
        val rect = RectF(1f, 1f, width - 1f, height - 1f)
        val colors = intArrayOf(
            0xFFFF6F91.toInt(), 0xFFFFC16F.toInt(), 0xFFD9E66F.toInt(),
            0xFF65DDB1.toInt(), 0xFF64C8FF.toInt(), 0xFF9C91FF.toInt(),
            0xFFE18BFA.toInt(), 0xFFFF6F91.toInt()
        )
        val sweep = SweepGradient(width * 0.5f, height * 0.5f, colors, null)
        sweep.setLocalMatrix(android.graphics.Matrix().apply {
            setRotate((time * 54f) % 360f, width * 0.5f, height * 0.5f)
        })
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f * density
            shader = sweep
            this.alpha = (alpha * 90f).toInt().coerceIn(0, 255)
            maskFilter = BlurMaskFilter(10f * density, BlurMaskFilter.Blur.NORMAL)
            screenBlend(this)
        }
        canvas.drawRoundRect(rect, 10f, 10f, glow)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            shader = sweep
            this.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        }
        canvas.drawRoundRect(rect, 10f, 10f, edge)
    }

    private fun drawPrism(canvas: Canvas, width: Float, height: Float, time: Float) {
        val phase = time % 2.4f
        if (phase > 0.9f) return
        val p = phase / 0.9f
        val cx = (-0.20f + p * 1.40f) * width
        val cy = height * 0.5f
        val sx = cx - width * 0.25f
        val sy = cy + height * 0.091f
        val ex = cx + width * 0.25f
        val ey = cy - height * 0.091f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = width * 0.18f
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                sx, sy, ex, ey,
                intArrayOf(0x00FFFFFF, 0x1CFF9FC4, 0x1C9FEAFF, 0x1CFFFFFF, 0x00FFFFFF),
                null, Shader.TileMode.CLAMP
            )
            maskFilter = BlurMaskFilter(width * 0.035f, BlurMaskFilter.Blur.NORMAL)
            screenBlend(this)
        }
        canvas.drawLine(sx, sy, ex, ey, paint)
    }

    private fun drawSparkles(
        canvas: Canvas, width: Float, height: Float, time: Float,
        static: Boolean, density: Float
    ) {
        val random = Random(SEED xor 0x77A)
        repeat(16) { index ->
            val x = (0.03f + random.nextFloat() * 0.94f) * width
            val y = (0.03f + random.nextFloat() * 0.94f) * height
            val nearEdge = min(min(x, width - x) / width, min(y, height - y) / height) < 0.26f
            val size = (4f + random.nextFloat() * 6f) * density
            val freq = 0.4f + random.nextFloat() * 0.6f
            val phase = random.nextFloat() * 2f * PI.toFloat()
            val twinkle = if (static) 0.78f else 0.55f + 0.45f * (0.5f + 0.5f * sin(time * 2f * PI.toFloat() * freq + phase))
            if (!nearEdge) return@repeat
            val color = when (index % 3) {
                0 -> 0xFFFFFFFF.toInt()
                1 -> 0xFFFFE6B8.toInt()
                else -> 0xFFBFD6FF.toInt()
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.alpha = (twinkle * 185f).toInt().coerceIn(0, 255)
                style = Paint.Style.STROKE
                strokeWidth = 0.7f * density
            }
            canvas.drawLine(x - size, y, x + size, y, paint)
            canvas.drawLine(x, y - size, x, y + size, paint)
            if (index % 2 == 0) {
                paint.strokeWidth = 0.6f * density
                canvas.drawLine(x - size * 0.55f, y - size * 0.55f, x + size * 0.55f, y + size * 0.55f, paint)
                canvas.drawLine(x - size * 0.55f, y + size * 0.55f, x + size * 0.55f, y - size * 0.55f, paint)
            }
        }
    }

    private fun drawNightSky(
        canvas: Canvas, width: Float, height: Float, time: Float,
        progress: Int, reducedMotion: Boolean, simple: Boolean, bloom: Boolean,
        density: Float
    ) {
        val maxStars = if (simple) 60 else 140
        val visibleCount = ceil(maxStars * progress.coerceIn(0, 100) / 100f).toInt()
        val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.5f, height * 0.5f, max(width, height) * 0.74f,
                intArrayOf(0x00000000, 0x1F00010A, 0x3B00010A),
                floatArrayOf(0.45f, 0.78f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, vignette)
        drawMilkyWay(canvas, width, height)
        starField.take(visibleCount).forEach { star ->
            val fade = ((progress.coerceIn(0, 100) / 100f - star.threshold) / 0.12f).coerceIn(0f, 1f)
            if (fade <= 0f) return@forEach
            val drift = when (star.layer) {
                0 -> 0.20f
                1 -> 0.70f
                else -> 1.30f
            }
            val x = (star.x * width + time * drift) % width
            val y = (star.y * height + time * drift * 0.37f) % height
            val twinkle = if (reducedMotion || simple) 0.85f else {
                0.65f + 0.35f * sin(2f * PI.toFloat() * star.frequency * time + star.phase)
            }
            val alpha = (fade * twinkle).coerceIn(0f, 1f)
            val color = when (star.hue) {
                1 -> 0xFFFFE6B8.toInt()
                2 -> 0xFFBFD6FF.toInt()
                else -> 0xFFFFFFFF.toInt()
            }
            val size = star.size * density
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.alpha = (alpha * 64f).toInt().coerceIn(0, 255)
            }
            canvas.drawCircle(x, y, size * 2.5f, glow)
            val core = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
            }
            canvas.drawCircle(x, y, size * 0.5f, core)
            if (star.layer == 2) {
                val flare = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    this.alpha = (alpha * 190f).toInt().coerceIn(0, 255)
                    strokeWidth = 0.6f
                }
                val length = size * 6f
                canvas.drawLine(x - length, y, x + length, y, flare)
                canvas.drawLine(x, y - length, x, y + length, flare)
            }
        }
        if (!reducedMotion && !simple) drawMeteor(canvas, width, height, time, progress, density)
        if (bloom) {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width * 0.5f, height * 0.5f, max(width, height) * 0.78f,
                    intArrayOf(0x35FFFFFF, 0x0AFFFFFF, 0x00FFFFFF),
                    floatArrayOf(0f, 0.56f, 1f), Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width, height, glow)
        }
    }

    private fun drawMilkyWay(canvas: Canvas, width: Float, height: Float) {
        val band = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width, height * 0.28f,
                intArrayOf(0x000C1E4A, 0x16172F6B, 0x1B4D3578, 0x001A2B66),
                floatArrayOf(0f, 0.35f, 0.68f, 1f), Shader.TileMode.CLAMP
            )
            maskFilter = BlurMaskFilter(height * 0.08f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.save()
        canvas.rotate(-20f, width * 0.5f, height * 0.5f)
        canvas.drawRect(-width * 0.25f, height * 0.30f, width * 1.25f, height * 0.58f, band)
        canvas.restore()
    }

    private fun drawMeteor(
        canvas: Canvas, width: Float, height: Float, time: Float, progress: Int,
        density: Float
    ) {
        val phase = time % 6.7f
        if (phase > 0.78f || progress < 12) return
        val fraction = phase / 0.78f
        val x = width * (0.12f + fraction * 0.74f)
        val y = height * (0.16f + fraction * 0.40f)
        val length = width * 0.14f
        val angle = 0.38f
        val tx = x - cos(angle) * length
        val ty = y - sin(angle) * length
        val trail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1f * density
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                tx.toFloat(), ty.toFloat(), x, y,
                intArrayOf(0x00FFFFFF, 0x66BFD6FF, 0xF2FFFFFF),
                floatArrayOf(0f, 0.68f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawLine(tx.toFloat(), ty.toFloat(), x, y, trail)
        canvas.drawCircle(x, y, 1.5f * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    }

    private fun alphaColor(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
}

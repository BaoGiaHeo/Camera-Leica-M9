package com.example.camui

import android.content.Context
import android.animation.ValueAnimator
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.view.View
import java.util.LinkedHashMap
import java.util.WeakHashMap
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.math.max

/**
 * Applies a 3D LUT to the real CameraX PreviewView RenderNode.
 *
 * API 33+ uses a 3D LUT atlas in AGSL. API 31-32 use a ColorMatrix approximation when a
 * runtime 3D shader is unavailable. CameraX PreviewView is COMPATIBLE/TextureView-backed.
 */
internal object CameraLutPreviewEffect {
    private const val TAG = "FUJI_LIVE_LUT"
    private const val MAX_CACHED_PREVIEWS = 4

    private const val AGSL = """
        uniform shader source;
        uniform shader lutAtlas;
        uniform float uLutSize;
        uniform float uTileColumns;
        uniform float uMix;
        uniform float uHighlight;
        uniform float uShadow;
        uniform float uColorChrome;
        uniform float uToneX;
        uniform float uToneY;
        uniform float uSharpness;
        uniform float uDenoise;
        uniform float uRadius;
        uniform float uDebugTint;

        float3 lutAt(float r, float g, float b) {
            float tileX = mod(b, uTileColumns);
            float tileY = floor(b / uTileColumns);
            float2 samplePoint = float2(
                tileX * uLutSize + r + 0.5,
                tileY * uLutSize + g + 0.5
            );
            return lutAtlas.eval(samplePoint).rgb;
        }

        float3 applyLut(float3 rgb) {
            float3 p = clamp(rgb, 0.0, 1.0) * (uLutSize - 1.0);
            float3 base = floor(p);
            float3 f = p - base;
            float r0 = base.r;
            float g0 = base.g;
            float b0 = base.b;
            float r1 = min(r0 + 1.0, uLutSize - 1.0);
            float g1 = min(g0 + 1.0, uLutSize - 1.0);
            float b1 = min(b0 + 1.0, uLutSize - 1.0);
            float3 c000 = lutAt(r0, g0, b0);
            float3 c100 = lutAt(r1, g0, b0);
            float3 c010 = lutAt(r0, g1, b0);
            float3 c001 = lutAt(r0, g0, b1);
            float3 c110 = lutAt(r1, g1, b0);
            float3 c101 = lutAt(r1, g0, b1);
            float3 c011 = lutAt(r0, g1, b1);
            float3 c111 = lutAt(r1, g1, b1);

            if (f.r >= f.g && f.g >= f.b) {
                return c000 + f.r * (c100 - c000) +
                    f.g * (c110 - c100) + f.b * (c111 - c110);
            }
            if (f.r >= f.b && f.b > f.g) {
                return c000 + f.r * (c100 - c000) +
                    f.b * (c101 - c100) + f.g * (c111 - c101);
            }
            if (f.b > f.r && f.r >= f.g) {
                return c000 + f.b * (c001 - c000) +
                    f.r * (c101 - c001) + f.g * (c111 - c101);
            }
            if (f.g > f.r && f.r >= f.b) {
                return c000 + f.g * (c010 - c000) +
                    f.r * (c110 - c010) + f.b * (c111 - c110);
            }
            if (f.g >= f.b && f.b > f.r) {
                return c000 + f.g * (c010 - c000) +
                    f.b * (c011 - c010) + f.r * (c111 - c011);
            }
            return c000 + f.b * (c001 - c000) +
                f.g * (c011 - c001) + f.r * (c111 - c011);
        }

        float3 rgbToHsl(float3 rgb) {
            float maximum = max(rgb.r, max(rgb.g, rgb.b));
            float minimum = min(rgb.r, min(rgb.g, rgb.b));
            float delta = maximum - minimum;
            float lightness = (maximum + minimum) * 0.5;
            float hue = 0.0;
            float saturation = 0.0;
            if (delta > 0.000001) {
                saturation = delta / max(1.0 - abs(2.0 * lightness - 1.0), 0.000001);
                if (maximum == rgb.r) {
                    hue = mod((rgb.g - rgb.b) / delta, 6.0) / 6.0;
                } else if (maximum == rgb.g) {
                    hue = ((rgb.b - rgb.r) / delta + 2.0) / 6.0;
                } else {
                    hue = ((rgb.r - rgb.g) / delta + 4.0) / 6.0;
                }
                if (hue < 0.0) hue += 1.0;
            }
            return float3(hue, clamp(saturation, 0.0, 1.0), clamp(lightness, 0.0, 1.0));
        }

        float3 hslToRgb(float3 hsl) {
            float chroma = (1.0 - abs(2.0 * hsl.z - 1.0)) * hsl.y;
            float h6 = mod(mod(hsl.x, 1.0) + 1.0, 1.0) * 6.0;
            float x = chroma * (1.0 - abs(mod(h6, 2.0) - 1.0));
            float3 rgb = float3(0.0);
            if (h6 < 1.0) rgb = float3(chroma, x, 0.0);
            else if (h6 < 2.0) rgb = float3(x, chroma, 0.0);
            else if (h6 < 3.0) rgb = float3(0.0, chroma, x);
            else if (h6 < 4.0) rgb = float3(0.0, x, chroma);
            else if (h6 < 5.0) rgb = float3(x, 0.0, chroma);
            else rgb = float3(chroma, 0.0, x);
            float matchValue = hsl.z - chroma * 0.5;
            return clamp(rgb + float3(matchValue), 0.0, 1.0);
        }

        float3 applyColorRecipe(float3 rgb) {
            float luma = clamp(dot(rgb, float3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);
            float highlightWeight = clamp((luma - 0.55) / 0.45, 0.0, 1.0);
            float shadowWeight = clamp((0.45 - luma) / 0.45, 0.0, 1.0);
            float tonalShift = uHighlight * highlightWeight * 0.12 +
                uShadow * shadowWeight * 0.12;
            rgb += float3(tonalShift);

            float exposed = exp2(uToneY);
            rgb *= exposed;
            float exposedLuma = clamp(dot(rgb, float3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);
            float roll = max(0.0, exposedLuma - 0.72) * max(0.0, uToneY) * 0.22;
            rgb -= float3(roll);

            float toneContrast = 1.0 + abs(uToneX) * 0.18;
            rgb = (rgb - float3(0.5)) * toneContrast + float3(0.5);
            float toneChroma = 1.0 + uToneX * 0.36;
            float chromeChroma = max(abs(rgb.r - exposedLuma), max(abs(rgb.g - exposedLuma), abs(rgb.b - exposedLuma)));
            float chromeFactor = 1.0 + uColorChrome * (0.05 + chromeChroma * 0.18);
            rgb = float3(exposedLuma) + (rgb - float3(exposedLuma)) * toneChroma * chromeFactor;
            return clamp(rgb, 0.0, 1.0);
        }

        float3 mapColour(float3 inputColour) {
            float3 lutColour = applyLut(inputColour);
            return applyColorRecipe(mix(inputColour, lutColour, uMix));
        }

        float3 applyDetail(float2 coordinate, float3 centre) {
            if (uSharpness <= 0.001 && uDenoise <= 0.001) return centre;
            float2 dx = float2(max(1.0, uRadius), 0.0);
            float2 dy = float2(0.0, max(1.0, uRadius));
            float3 topLeft = mapColour(float3(source.eval(coordinate - dx - dy).rgb));
            float3 top = mapColour(float3(source.eval(coordinate - dy).rgb));
            float3 topRight = mapColour(float3(source.eval(coordinate + dx - dy).rgb));
            float3 left = mapColour(float3(source.eval(coordinate - dx).rgb));
            float3 right = mapColour(float3(source.eval(coordinate + dx).rgb));
            float3 bottomLeft = mapColour(float3(source.eval(coordinate - dx + dy).rgb));
            float3 bottom = mapColour(float3(source.eval(coordinate + dy).rgb));
            float3 bottomRight = mapColour(float3(source.eval(coordinate + dx + dy).rgb));
            float3 neighbourhood = (topLeft + top + topRight + left + right + bottomLeft + bottom + bottomRight) * 0.125;
            float3 denoised = mix(centre, neighbourhood, uDenoise * 0.35);
            return clamp(denoised + (centre - neighbourhood) * (uSharpness * 0.55), 0.0, 1.0);
        }

        half4 main(float2 fragCoord) {
            half4 original = source.eval(fragCoord);
            float3 mapped = mapColour(float3(original.rgb));
            mapped = applyDetail(fragCoord, mapped);
            mapped = mix(mapped, float3(1.0, 0.015, 0.005), uDebugTint * 0.84);
            return half4(half3(clamp(mapped, 0.0, 1.0)), original.a);
        }
    """

    internal data class PreparedEffect(
        val shader: RuntimeShader,
        val lutSize: Int,
        val tileColumns: Int,
        val atlas: Bitmap
    )

    private val preparedCache = LinkedHashMap<String, PreparedEffect>()
    private data class ViewEffectState(
        var baseEffect: RenderEffect?,
        var appliedEffect: RenderEffect?
    )
    private val viewEffects = WeakHashMap<View, ViewEffectState>()

    private fun hasActiveColorRecipe(context: Context): Boolean {
        val params = ColorPipelineParams.from(context, "None")
        return kotlin.math.abs(params.toneX) > 0.001f ||
            kotlin.math.abs(params.toneY) > 0.001f ||
            kotlin.math.abs(params.highlights) > 0.001f ||
            kotlin.math.abs(params.shadows) > 0.001f ||
            params.colorChrome > 0.001f ||
            params.sharpness > 0.001f ||
            params.denoise > 0.001f ||
            params.intensity < 0.999f
    }

    private fun identityLut(): CubeLut {
        val values = FloatArray(2 * 2 * 2 * 3)
        var index = 0
        for (red in 0..1) {
            for (green in 0..1) {
                for (blue in 0..1) {
                    values[index++] = red.toFloat()
                    values[index++] = green.toFloat()
                    values[index++] = blue.toFloat()
                }
            }
        }
        return CubeLut(
            size = 2,
            domainMin = floatArrayOf(0f, 0f, 0f),
            domainMax = floatArrayOf(1f, 1f, 1f),
            values = values
        )
    }

    @Synchronized
    fun prepare(context: Context, preset: String, forceIdentity: Boolean = false): PreparedEffect? {
        if (preset.isBlank()) return null
        if (Build.VERSION.SDK_INT < 33) return null
        if (preset == "None" && !hasActiveColorRecipe(context) && !forceIdentity) return null
        preparedCache[preset]?.let { return it }

        val lut = if (preset == "None") identityLut() else readCameraLookLut(context, preset) ?: return null
        val size = lut.size
        if (size < 2) return null

        val columns = ceil(sqrt(size.toDouble())).toInt().coerceAtLeast(1)
        val rows = ceil(size.toDouble() / columns.toDouble()).toInt()
        val atlasWidth = columns * size
        val atlasHeight = rows * size
        val pixels = IntArray(atlasWidth * atlasHeight)
        val sample = FloatArray(3)

        for (blue in 0 until size) {
            val tileX = blue % columns
            val tileY = blue / columns
            for (green in 0 until size) {
                for (red in 0 until size) {
                    lut.sampleInto(
                        red.toFloat() / (size - 1),
                        green.toFloat() / (size - 1),
                        blue.toFloat() / (size - 1),
                        sample
                    )
                    val x = tileX * size + red
                    val y = tileY * size + green
                    val argb = (0xFF shl 24) or
                        ((sample[0] * 255f + 0.5f).toInt().coerceIn(0, 255) shl 16) or
                        ((sample[1] * 255f + 0.5f).toInt().coerceIn(0, 255) shl 8) or
                        (sample[2] * 255f + 0.5f).toInt().coerceIn(0, 255)
                    pixels[y * atlasWidth + x] = argb
                }
            }
        }

        val atlas = Bitmap.createBitmap(atlasWidth, atlasHeight, Bitmap.Config.ARGB_8888)
        atlas.setPixels(pixels, 0, atlasWidth, 0, 0, atlasWidth, atlasHeight)
        val shader = RuntimeShader(AGSL)
        shader.setInputBuffer(
            "lutAtlas",
            BitmapShader(atlas, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        )
        shader.setFloatUniform("uLutSize", size.toFloat())
        shader.setFloatUniform("uTileColumns", columns.toFloat())
        val prepared = PreparedEffect(shader, size, columns, atlas)
        preparedCache[preset] = prepared
        while (preparedCache.size > MAX_CACHED_PREVIEWS) {
            val eldest = preparedCache.keys.firstOrNull() ?: break
            preparedCache.remove(eldest)
        }
        Log.i(TAG, "LUT_PREVIEW_PREPARED preset=$preset size=${size}3D atlas=${atlasWidth}x$atlasHeight")
        return prepared
    }

    private fun updateRecipeUniforms(context: Context, shader: RuntimeShader, preset: String, view: View) {
        val params = ColorPipelineParams.from(context, preset)
        shader.setFloatUniform("uMix", params.intensity)
        shader.setFloatUniform("uHighlight", params.highlights)
        shader.setFloatUniform("uShadow", params.shadows)
        shader.setFloatUniform("uColorChrome", params.colorChrome)
        shader.setFloatUniform("uToneX", params.toneX)
        shader.setFloatUniform("uToneY", params.toneY)
        shader.setFloatUniform("uSharpness", params.sharpness)
        shader.setFloatUniform("uDenoise", params.denoise)
        val width = view.width.coerceAtLeast(1)
        val height = view.height.coerceAtLeast(1)
        shader.setFloatUniform("uRadius", (minOf(width, height) * 0.001f).coerceAtLeast(1f))
        shader.setFloatUniform("uDebugTint", 0f)
    }

    private fun colorMatrix(context: Context, preset: String, debugTint: Boolean): ColorMatrix {
        val params = ColorPipelineParams.from(context, preset)
        val saturation = (1f + params.toneX * 0.36f + params.colorChrome * 0.05f).coerceIn(0.35f, 1.55f)
        val exposure = (Math.pow(2.0, params.toneY.toDouble()).toFloat()).coerceIn(0.5f, 2f)
        val contrast = (1f + kotlin.math.abs(params.toneX) * 0.18f).coerceIn(0.8f, 1.2f)
        val warmth = params.toneX * 4f + (params.highlights - params.shadows) * 1.5f
        val redScale = exposure * contrast
        val greenScale = exposure * contrast
        val blueScale = exposure * contrast
        val offset = 128f * (1f - contrast)
        val matrix = ColorMatrix()
        matrix.set(floatArrayOf(
            redScale, 0f, 0f, 0f, offset + warmth,
            0f, greenScale, 0f, 0f, offset,
            0f, 0f, blueScale, 0f, offset - warmth,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(ColorMatrix().apply { setSaturation(saturation) })
        if (debugTint) {
            matrix.postConcat(ColorMatrix(floatArrayOf(
                1.15f, 0f, 0f, 0f, 12f,
                0f, 0.28f, 0f, 0f, 0f,
                0f, 0f, 0.22f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        return matrix
    }

    fun install(
        context: Context,
        view: View,
        preset: String,
        prepared: PreparedEffect?,
        baseEffect: RenderEffect? = null,
        debugTint: Boolean = false
    ): String {
        if (Build.VERSION.SDK_INT < 31) {
            clear(view)
            return "UNSUPPORTED_API_31_REQUIRED"
        }
        if (preset.isBlank() || (preset == "None" && prepared == null && !hasActiveColorRecipe(context) && !debugTint)) {
            clear(view)
            return "OFF"
        }

        val state = synchronized(viewEffects) {
            val existing = viewEffects[view]
            if (existing == null) {
                ViewEffectState(baseEffect = baseEffect, appliedEffect = null).also { viewEffects[view] = it }
            } else {
                existing.baseEffect = baseEffect
                existing
            }
        }

        if (Build.VERSION.SDK_INT in 31..32) {
            val filter = ColorMatrixColorFilter(colorMatrix(context, preset, debugTint))
            val colourEffect = RenderEffect.createColorFilterEffect(filter)
            val composed = baseEffect?.let { RenderEffect.createChainEffect(colourEffect, it) } ?: colourEffect
            state.appliedEffect = composed
            view.setRenderEffect(composed)
            Log.i(TAG, "PREVIEW_FILTER_APPLIED preset=$preset path=ColorMatrix api=${Build.VERSION.SDK_INT}")
            return "ColorMatrix"
        }

        val effect = prepared ?: run {
            clear(view)
            return "LUT_UNAVAILABLE"
        }
        updateRecipeUniforms(context, effect.shader, preset, view)
        effect.shader.setFloatUniform("uDebugTint", if (debugTint) 1f else 0f)
        val targetMix = ColorPipelineParams.from(context, preset).intensity
        effect.shader.setFloatUniform("uMix", 0f)
        val lutEffect = RenderEffect.createRuntimeShaderEffect(effect.shader, "source")
        val composed = baseEffect?.let { base -> RenderEffect.createChainEffect(lutEffect, base) } ?: lutEffect
        state.appliedEffect = composed
        view.setRenderEffect(composed)
        val animator = ValueAnimator.ofFloat(0f, targetMix.coerceIn(0f, 1f)).apply {
            duration = Tuning.FILTER_PREVIEW_CROSSFADE_MS.toLong()
            addUpdateListener { value ->
                runCatching { effect.shader.setFloatUniform("uMix", value.animatedValue as Float) }
            }
        }
        animator.start()
        Log.i(TAG, "PREVIEW_FILTER_APPLIED preset=$preset path=AGSL api=${Build.VERSION.SDK_INT}")
        return "AGSL"
    }

    /** Update cached uniforms during tone-pad and adjustment drags; no LUT atlas is rebuilt. */
    @Synchronized
    fun updateRecipe(context: Context, view: View, preset: String, baseEffect: RenderEffect? = null) {
        if (Build.VERSION.SDK_INT < 31) return
        if (Build.VERSION.SDK_INT in 31..32) {
            install(context, view, preset, null, baseEffect)
            return
        }
        val prepared = preparedCache[preset]
            ?: (if (preset == "None" && hasActiveColorRecipe(context)) prepare(context, preset) else null)
            ?: return
        if (viewEffects[view]?.appliedEffect == null) {
            install(context, view, preset, prepared, baseEffect)
        } else {
            updateRecipeUniforms(context, prepared.shader, preset, view)
        }
    }

    fun setDebugTint(
        context: Context,
        view: View,
        preset: String,
        baseEffect: RenderEffect?,
        enabled: Boolean
    ): String {
        if (Build.VERSION.SDK_INT in 31..32) {
            return install(context, view, preset, null, baseEffect, debugTint = enabled)
        }
        if (Build.VERSION.SDK_INT < 31) return "UNSUPPORTED"
        val prepared = preparedCache[preset] ?: prepare(context, preset, forceIdentity = true)
            ?: return "LUT_UNAVAILABLE"
        if (viewEffects[view]?.appliedEffect == null) install(context, view, preset, prepared, baseEffect)
        prepared.shader.setFloatUniform("uDebugTint", if (enabled) 1f else 0f)
        return "AGSL"
    }

    fun clear(view: View) {
        if (Build.VERSION.SDK_INT < 31) return
        val state = synchronized(viewEffects) { viewEffects.remove(view) }
        if (state != null) {
            // View has no public RenderEffect getter. This effect owns the installed
            // chain and restores the base effect explicitly supplied by the zoom renderer.
            view.setRenderEffect(state.baseEffect)
        }
    }
}

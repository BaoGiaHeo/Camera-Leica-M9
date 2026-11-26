package com.example.camui

import android.content.Context
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

/**
 * Applies a 3D LUT to the real CameraX PreviewView RenderNode.
 *
 * API 33+ uses AGSL through RenderEffect. Older Android versions are left untouched rather
 * than displaying a false "live LUT" state. Still-photo rendering continues to use the
 * same CubeLut sampler and the full CPU adjustment pipeline.
 */
internal object CameraLutPreviewEffect {
    private const val TAG = "FUJI_LIVE_LUT"
    private const val MAX_CACHED_PREVIEWS = 4

    private const val AGSL = """
        uniform shader source;
        uniform shader lutAtlas;
        uniform float uLutSize;
        uniform float uTileColumns;
        uniform float uIntensity;
        uniform float uToneX;
        uniform float uToneY;
        uniform float uHighlight;
        uniform float uShadow;
        uniform float uColorChrome;
        uniform float uSharpness;
        uniform float uNoiseReduction;
        uniform float uSpatialRadius;
        uniform float uTestRed;

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

        float3 applyColorPipeline(float3 rawRgb) {
            float3 lutRgb = applyLut(rawRgb);
            float3 rgb = mix(rawRgb, lutRgb, uIntensity);

            float lumaBeforeExposure = clamp(dot(rgb, float3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);
            float highlightRollOff = clamp((lumaBeforeExposure - 0.55) / 0.45, 0.0, 1.0);
            float exposure = exp2(uToneY);
            float appliedExposure = 1.0 + (exposure - 1.0) * (1.0 - 0.75 * highlightRollOff);
            rgb *= appliedExposure;

            float toneLuma = clamp(dot(rgb, float3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);
            float toneSaturation = 1.0 + uToneX * 0.50;
            float toneContrast = 1.0 + uToneX * 0.18;
            rgb = float3(toneLuma) + (rgb - float3(toneLuma)) * toneSaturation;
            rgb = (rgb - float3(0.5)) * toneContrast + float3(0.5);

            float luma = clamp(dot(rgb, float3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);
            float highlightWeight = clamp((luma - 0.55) / 0.45, 0.0, 1.0);
            float shadowWeight = clamp((0.45 - luma) / 0.45, 0.0, 1.0);
            float tonalShift = uHighlight * highlightWeight * 0.12 +
                uShadow * shadowWeight * 0.12;
            rgb += float3(tonalShift);

            float chromeLuma = clamp(dot(rgb, float3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);
            float chroma = max(abs(rgb.r - chromeLuma),
                max(abs(rgb.g - chromeLuma), abs(rgb.b - chromeLuma)));
            float chromeFactor = 1.0 + uColorChrome * (0.05 + chroma * 0.18);
            rgb = float3(chromeLuma) + (rgb - float3(chromeLuma)) * chromeFactor;
            return clamp(rgb, 0.0, 1.0);
        }

        half4 main(float2 fragCoord) {
            float radius = max(uSpatialRadius, 1.0);
            float3 center = applyColorPipeline(float3(source.eval(fragCoord).rgb));
            float3 north = applyColorPipeline(float3(source.eval(fragCoord + float2(0.0, -radius)).rgb));
            float3 south = applyColorPipeline(float3(source.eval(fragCoord + float2(0.0, radius)).rgb));
            float3 west = applyColorPipeline(float3(source.eval(fragCoord + float2(-radius, 0.0)).rgb));
            float3 east = applyColorPipeline(float3(source.eval(fragCoord + float2(radius, 0.0)).rgb));
            float3 average = (center * 4.0 + north + south + west + east) * 0.125;
            float3 denoised = mix(center, average, uNoiseReduction);
            float3 outputRgb = denoised + (center - average) * uSharpness * 0.85;
            outputRgb = mix(outputRgb, float3(1.0, 0.0, 0.0), uTestRed);
            return half4(half3(clamp(outputRgb, 0.0, 1.0)), half(1.0));
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
        val preferences = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
        return kotlin.math.abs(preferences.getFloat("film_custom_highlight", 0f)) > 0.001f ||
            kotlin.math.abs(preferences.getFloat("film_custom_shadow", 0f)) > 0.001f ||
            preferences.getFloat("film_custom_color_chrome", 0f) > 0.001f ||
            preferences.getFloat("film_custom_grain", 0f) > 0.001f ||
            kotlin.math.abs(preferences.getFloat("film_custom_temperature", 0f)) > 0.001f ||
            kotlin.math.abs(preferences.getFloat("film_custom_tone", 0f)) > 0.001f ||
            kotlin.math.abs(preferences.getFloat("film_custom_saturation", 1f) - 1f) > 0.001f ||
            kotlin.math.abs(preferences.getFloat("film_custom_hue", 0f)) > 0.001f ||
            kotlin.math.abs(preferences.getFloat("film_custom_lightness", 0f)) > 0.001f
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
    fun prepare(context: Context, preset: String): PreparedEffect? {
        if (preset.isBlank()) return null
        if (Build.VERSION.SDK_INT < 33) return null
        if (preset == "None" && !hasActiveColorRecipe(context)) return null
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

    private fun updateRecipeUniforms(context: Context, shader: RuntimeShader) {
        val preferences = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
        shader.setFloatUniform("uMix", 1f)
        shader.setFloatUniform("uHighlight", preferences.getFloat("film_custom_highlight", 0f).coerceIn(-1f, 1f))
        shader.setFloatUniform("uShadow", preferences.getFloat("film_custom_shadow", 0f).coerceIn(-1f, 1f))
        shader.setFloatUniform("uColorChrome", preferences.getFloat("film_custom_color_chrome", 0f).coerceIn(0f, 1f))
        shader.setFloatUniform("uGrain", preferences.getFloat("film_custom_grain", 0f).coerceIn(0f, 1f))
        shader.setFloatUniform("uTemperature", preferences.getFloat("film_custom_temperature", 0f).coerceIn(-1f, 1f))
        shader.setFloatUniform("uTone", preferences.getFloat("film_custom_tone", 0f).coerceIn(-1f, 1f))
        shader.setFloatUniform("uSaturation", preferences.getFloat("film_custom_saturation", 1f).coerceIn(0.5f, 1.5f))
        shader.setFloatUniform("uHueShift", preferences.getFloat("film_custom_hue", 0f).coerceIn(-0.5f, 0.5f))
        shader.setFloatUniform("uLightnessShift", preferences.getFloat("film_custom_lightness", 0f).coerceIn(-0.5f, 0.5f))
    }

    fun install(
        context: Context,
        view: View,
        preset: String,
        prepared: PreparedEffect?,
        baseEffect: RenderEffect? = null
    ): String {
        if (Build.VERSION.SDK_INT < 33) {
            clear(view)
            return "UNSUPPORTED_API_33_REQUIRED"
        }
        if (preset.isBlank() || (preset == "None" && prepared == null)) {
            clear(view)
            return "OFF"
        }
        val effect = prepared as? PreparedEffect ?: run {
            clear(view)
            return "LUT_UNAVAILABLE"
        }
        updateRecipeUniforms(context, effect.shader)
        val lutEffect = RenderEffect.createRuntimeShaderEffect(effect.shader, "source")
        val state = synchronized(viewEffects) {
            val existing = viewEffects[view]
            if (existing == null) {
                ViewEffectState(baseEffect = baseEffect, appliedEffect = null).also {
                    viewEffects[view] = it
                }
            } else {
                existing.baseEffect = baseEffect
                existing
            }
        }
        val composed = baseEffect?.let { base ->
            RenderEffect.createChainEffect(lutEffect, base)
        } ?: lutEffect
        state.appliedEffect = composed
        view.setRenderEffect(composed)
        Log.i(TAG, "LUT_PREVIEW_APPLIED preset=$preset path=PreviewView.RenderEffect")
        return "AGSL_LUT"
    }

    /** Update uniforms on the cached shader during Tone Pad drags without rebuilding its LUT atlas. */
    @Synchronized
    fun updateRecipe(context: Context, view: View, preset: String, baseEffect: RenderEffect? = null) {
        if (Build.VERSION.SDK_INT < 33) return
        val prepared = (preparedCache[preset]
            ?: (if (preset == "None" && hasActiveColorRecipe(context)) prepare(context, preset) else null))
            ?: return
        if (viewEffects[view]?.appliedEffect == null) {
            install(context, view, preset, prepared, baseEffect)
        } else {
            updateRecipeUniforms(context, prepared.shader)
        }
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

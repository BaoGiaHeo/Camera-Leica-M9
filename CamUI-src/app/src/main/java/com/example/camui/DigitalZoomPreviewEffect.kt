package com.example.camui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import android.view.View

/**
 * Applies the same Lanczos-3 reconstruction idea to the live viewfinder on Android 13+.
 * The shader is attached to PreviewView's hardware-rendered output, not to its layout
 * bounds, so the LCD rectangle stays fixed during digital zoom. Older Android versions
 * fall back to a centered View transform; still photos always use DigitalZoomProcessor.
 */
internal class DigitalZoomPreviewEffect {
    private val runtimeShader: RuntimeShader? = if (Build.VERSION.SDK_INT >= 33) {
        runCatching { RuntimeShader(FRAGMENT_SHADER) }
            .onFailure { Log.w(TAG, "AGSL Lanczos preview shader unavailable; use View scale fallback", it) }
            .getOrNull()
    } else null

    private val renderEffect: RenderEffect? = if (Build.VERSION.SDK_INT >= 33) {
        runtimeShader?.let { shader ->
            runCatching { RenderEffect.createRuntimeShaderEffect(shader, "inputShader") }
                .onFailure { Log.w(TAG, "Could not create the Lanczos preview RenderEffect", it) }
                .getOrNull()
        }
    } else null
    private var renderEffectApplied = false

    fun apply(view: View, digitalCropRatio: Float): String {
        val ratio = digitalCropRatio.takeIf { it.isFinite() && it > 1.001f } ?: 1f
        if (ratio <= 1f) {
            clear(view)
            return "NONE"
        }

        if (Build.VERSION.SDK_INT >= 33 && runtimeShader != null && renderEffect != null &&
            view.width > 0 && view.height > 0
        ) {
            runtimeShader.setFloatUniform("viewSize", view.width.toFloat(), view.height.toFloat())
            runtimeShader.setFloatUniform("cropScale", ratio)
            if (!renderEffectApplied) {
                view.setRenderEffect(renderEffect)
                renderEffectApplied = true
            }
            view.scaleX = 1f
            view.scaleY = 1f
            return "AGSL_LANCZOS3"
        }

        if (Build.VERSION.SDK_INT >= 31 && renderEffectApplied) {
            view.setRenderEffect(null)
            renderEffectApplied = false
        }
        view.scaleX = ratio
        view.scaleY = ratio
        return "VIEW_SCALE_FALLBACK"
    }

    fun currentRenderEffect(): RenderEffect? =
        renderEffect.takeIf { renderEffectApplied }

    fun clear(view: View) {
        if (Build.VERSION.SDK_INT >= 31 && renderEffectApplied) {
            view.setRenderEffect(null)
            renderEffectApplied = false
        }
        view.scaleX = 1f
        view.scaleY = 1f
    }

    private companion object {
        const val TAG = "FUJI_DIGITAL_ZOOM_PREVIEW"

        const val FRAGMENT_SHADER = """
            uniform shader inputShader;
            uniform float2 viewSize;
            uniform float cropScale;

            float sinc(float x) {
                if (abs(x) < 0.00001) return 1.0;
                float p = 3.141592653589793 * x;
                return sin(p) / p;
            }

            float lanczos3(float x) {
                x = abs(x);
                if (x >= 3.0) return 0.0;
                return sinc(x) * sinc(x / 3.0);
            }

            half4 main(float2 coord) {
                float safeScale = max(cropScale, 1.0);
                float2 center = viewSize * 0.5;
                float2 source = (coord - center) / safeScale + center;
                float2 base = floor(source);
                half4 accum = half4(0.0);
                float weightSum = 0.0;

                for (int iy = -2; iy <= 3; iy++) {
                    float sy = clamp(base.y + float(iy), 0.0, viewSize.y - 1.0);
                    float wy = lanczos3(source.y - (base.y + float(iy)));
                    for (int ix = -2; ix <= 3; ix++) {
                        float sx = clamp(base.x + float(ix), 0.0, viewSize.x - 1.0);
                        float wx = lanczos3(source.x - (base.x + float(ix)));
                        float weight = wx * wy;
                        accum += inputShader.eval(float2(sx, sy)) * half(weight);
                        weightSum += weight;
                    }
                }
                return accum / half(max(weightSum, 0.00001));
            }
        """
    }
}

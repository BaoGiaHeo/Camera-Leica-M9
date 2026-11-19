package com.example.camui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * Applies TonePadMath's OKLab adjustment to a rendered film-look preview through AGSL uniforms.
 * The base bitmap is rendered once; drag input only updates the shader parameters.
 */
@RequiresApi(33)
internal class TonePadLiveShader {
    private val shader = RuntimeShader(SOURCE)
    val composeEffect: ComposeRenderEffect =
        RenderEffect.createRuntimeShaderEffect(shader, "source").asComposeRenderEffect()

    fun update(parameters: TonePadParameters) {
        shader.setFloatUniform("paleRich", parameters.paleRich.coerceIn(-1f, 1f))
        shader.setFloatUniform("darkBright", parameters.darkBright.coerceIn(-1f, 1f))
        shader.setFloatUniform("intensity", parameters.intensity.coerceIn(0f, 1f))
    }

    private companion object {
        val SOURCE = """
            uniform shader source;
            uniform float paleRich;
            uniform float darkBright;
            uniform float intensity;

            float3 srgbToLinear(float3 c) {
                float3 low = c / 12.92;
                float3 high = pow((c + 0.055) / 1.055, float3(2.4));
                return mix(low, high, step(float3(0.04045), c));
            }
            float3 linearToSrgb(float3 c) {
                float3 low = c * 12.92;
                float3 high = 1.055 * pow(max(c, float3(0.0)), float3(1.0 / 2.4)) - 0.055;
                return mix(low, high, step(float3(0.0031308), c));
            }
            half4 main(float2 p) {
                half4 pixel = source.eval(p);
                float amount = clamp(intensity, 0.0, 1.0);
                if (amount <= 0.0001 || (abs(paleRich) <= 0.0001 && abs(darkBright) <= 0.0001)) return pixel;

                float3 rgb = srgbToLinear(float3(pixel.rgb));
                float l = 0.4122214708 * rgb.r + 0.5363325363 * rgb.g + 0.0514459929 * rgb.b;
                float m = 0.2119034982 * rgb.r + 0.6806995451 * rgb.g + 0.1073969566 * rgb.b;
                float s = 0.0883024619 * rgb.r + 0.2817188376 * rgb.g + 0.6299787005 * rgb.b;
                float3 roots = pow(max(float3(l, m, s), float3(0.0)), float3(1.0 / 3.0));

                float L = 0.2104542553 * roots.x + 0.7936177850 * roots.y - 0.0040720468 * roots.z;
                float a = 1.9779984951 * roots.x - 2.4285922050 * roots.y + 0.4505937099 * roots.z;
                float b = 0.0259040371 * roots.x + 0.7827717662 * roots.y - 0.8086757660 * roots.z;
                L = clamp(L + clamp(darkBright, -1.0, 1.0) * 0.14 * amount, 0.0, 1.0);
                float chroma = 1.0 + clamp(paleRich, -1.0, 1.0) * 0.55 * amount;
                a *= chroma;
                b *= chroma;

                float lp = L + 0.3963377774 * a + 0.2158037573 * b;
                float mp = L - 0.1055613458 * a - 0.0638541728 * b;
                float sp = L - 0.0894841775 * a - 1.2914855480 * b;
                float3 cubed = float3(lp * lp * lp, mp * mp * mp, sp * sp * sp);
                float3 resultLinear = float3(
                    4.0767416621 * cubed.x - 3.3077115913 * cubed.y + 0.2309699292 * cubed.z,
                    -1.2684380046 * cubed.x + 2.6097574011 * cubed.y - 0.3413193965 * cubed.z,
                    -0.0041960863 * cubed.x - 0.7034186147 * cubed.y + 1.7076147010 * cubed.z
                );
                return half4(half3(clamp(linearToSrgb(resultLinear), 0.0, 1.0)), pixel.a);
            }
        """.trimIndent()
    }
}

package com.example.camui

import android.graphics.Color
import java.nio.FloatBuffer

/** NAFNet ONNX tensors use RGB channel order in NCHW layout. */
internal object HdmpAiColorLayout {
    fun writeRgb(pixels: IntArray, target: FloatBuffer) {
        require(target.remaining() >= pixels.size * 3) { "RGB tensor buffer is too small" }
        for (pixel in pixels) target.put(Color.red(pixel) / 255f)
        for (pixel in pixels) target.put(Color.green(pixel) / 255f)
        for (pixel in pixels) target.put(Color.blue(pixel) / 255f)
    }
}

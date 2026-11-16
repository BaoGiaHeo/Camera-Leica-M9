package com.example.camui

import java.nio.FloatBuffer

/** NAFNet ONNX tensors use RGB channel order in NCHW layout. */
internal object HdmpAiColorLayout {
    fun writeRgb(pixels: IntArray, target: FloatBuffer) {
        require(target.remaining() >= pixels.size * 3) { "RGB tensor buffer is too small" }
        for (pixel in pixels) target.put(((pixel ushr 16) and 0xFF) / 255f)
        for (pixel in pixels) target.put(((pixel ushr 8) and 0xFF) / 255f)
        for (pixel in pixels) target.put((pixel and 0xFF) / 255f)
    }
}

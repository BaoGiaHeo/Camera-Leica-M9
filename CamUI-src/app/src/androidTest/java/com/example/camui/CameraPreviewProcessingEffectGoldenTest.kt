package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraPreviewProcessingEffectGoldenTest {
    private val times = listOf(0f, 0.5f, 1f, 2f, 4f)

    @Test
    fun exportsLunaDreamAndNightShotGoldensOnLightAndDarkPreviews() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val output = File(context.getExternalFilesDir(null) ?: context.cacheDir, "test-output/effects")
        assertTrue("Could not create effect golden output folder", output.mkdirs() || output.isDirectory)
        var exported = 0

        listOf("luna", "night").forEach { mode ->
            listOf("dark", "light").forEach { background ->
                times.forEach { time ->
                    val bitmap = Bitmap.createBitmap(960, 540, Bitmap.Config.ARGB_8888)
                    try {
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(if (background == "light") Color.WHITE else Color.BLACK)
                        val started = System.nanoTime()
                        CameraPreviewProcessingEffect.draw(
                            canvas = canvas,
                            width = bitmap.width.toFloat(),
                            height = bitmap.height.toFloat(),
                            mode = mode,
                            timeSeconds = time,
                            progressPercent = 68,
                            reducedMotion = false,
                            simpleMode = false,
                            holdSteady = true,
                            completionBloom = false
                        )
                        val renderUs = (System.nanoTime() - started) / 1_000L
                        val name = mode + "_" + background + "_t" + time.toString().replace('.', '_') + ".png"
                        val file = File(output, name)
                        file.outputStream().use { stream ->
                            assertTrue("PNG write failed: " + name, bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                        }
                        assertTrue("Golden file is too small: " + name, file.length() > 2_000L)

                        val metrics = sampleMetrics(bitmap)
                        if (background == "light") {
                            assertTrue("Light preview became muddy/dark: " + name + " brightness=" + metrics.first, metrics.first > 215f)
                        } else if (mode == "luna") {
                            assertTrue("Aurora has insufficient colour saturation: " + name + " saturation=" + metrics.second, metrics.second > 0.035f)
                        }
                        Log.i(
                            "FUJI_EFFECT_GOLDEN",
                            "PASS mode=" + mode + " background=" + background + " t=" + time +
                                " avgLuma=" + metrics.first + " avgSat=" + metrics.second +
                                " renderUs=" + renderUs + " file=" + file.absolutePath
                        )
                        exported++
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }
        assertEquals("Expected 2 effects x 2 backgrounds x 5 times", 20, exported)
        Log.i("FUJI_EFFECT_GOLDEN", "SUMMARY exported=" + exported + " dir=" + output.absolutePath)
    }

    @Test
    fun reducedMotionProducesStaticAuroraAndNightSkyFrames() {
        listOf("luna", "night").forEach { mode ->
            val first = render(mode, 0f, reducedMotion = true)
            val second = render(mode, 4f, reducedMotion = true)
            try {
                assertTrue("Reduced-motion output must be static for " + mode, first.sameAs(second))
            } finally {
                first.recycle()
                second.recycle()
            }
        }
    }

    private fun render(mode: String, time: Float, reducedMotion: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(480, 270, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        CameraPreviewProcessingEffect.draw(
            canvas = canvas,
            width = bitmap.width.toFloat(),
            height = bitmap.height.toFloat(),
            mode = mode,
            timeSeconds = time,
            progressPercent = 68,
            reducedMotion = reducedMotion,
            simpleMode = false,
            holdSteady = false
        )
        return bitmap
    }

    private fun sampleMetrics(bitmap: Bitmap): Pair<Float, Float> {
        val stepX = (bitmap.width / 120).coerceAtLeast(1)
        val stepY = (bitmap.height / 68).coerceAtLeast(1)
        var luminance = 0.0
        var saturation = 0.0
        var count = 0
        val hsv = FloatArray(3)
        for (y in 0 until bitmap.height step stepY) {
            for (x in 0 until bitmap.width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                luminance += 0.2126 * red + 0.7152 * green + 0.0722 * blue
                Color.RGBToHSV(red, green, blue, hsv)
                saturation += hsv[1]
                count++
            }
        }
        val safeCount = count.coerceAtLeast(1)
        return (luminance / safeCount).toFloat() to (saturation / safeCount).toFloat()
    }
}

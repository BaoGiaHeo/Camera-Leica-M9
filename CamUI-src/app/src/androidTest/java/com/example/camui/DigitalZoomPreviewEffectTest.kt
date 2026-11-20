package com.example.camui

import android.content.Context
import android.os.Build
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DigitalZoomPreviewEffectTest {
    @Test
    fun api33PlusUsesAgslLanczosForDigitalPreviewAndKeepsViewBoundsScaleNeutral() {
        assumeTrue("AGSL RuntimeShader requires Android 13+", Build.VERSION.SDK_INT >= 33)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preview = View(context).apply { layout(0, 0, 640, 360) }
        val renderer = DigitalZoomPreviewEffect()

        val mode = renderer.apply(preview, 2f)

        assertEquals("AGSL_LANCZOS3", mode)
        assertEquals(1f, preview.scaleX, 0.0001f)
        assertEquals(1f, preview.scaleY, 0.0001f)
        assertTrue("Preview view bounds should remain the original LCD bounds", preview.width == 640 && preview.height == 360)

        renderer.clear(preview)
        assertEquals(1f, preview.scaleX, 0.0001f)
        assertEquals(1f, preview.scaleY, 0.0001f)
    }

    @Test
    fun oneTimesZoomRemovesThePreviewCropEffect() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preview = View(context).apply { layout(0, 0, 640, 360) }
        val renderer = DigitalZoomPreviewEffect()

        assertEquals("NONE", renderer.apply(preview, 1f))
        assertEquals(1f, preview.scaleX, 0.0001f)
        assertEquals(1f, preview.scaleY, 0.0001f)
        renderer.clear(preview)
    }
}

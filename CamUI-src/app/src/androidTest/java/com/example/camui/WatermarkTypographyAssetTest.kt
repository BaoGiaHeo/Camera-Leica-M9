package com.example.camui

import android.content.Context
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatermarkTypographyAssetTest {
    @Test
    fun allThreeBundledWatermarkFontsLoadAtRegularWeight() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val jost = context.resources.getFont(R.font.jost_watermark)
        val inter = context.resources.getFont(R.font.inter_watermark)
        val ibmPlexMono = context.resources.getFont(R.font.ibm_plex_mono_watermark)

        assertNotNull(jost)
        assertNotNull(inter)
        assertNotNull(ibmPlexMono)
        assertEquals(Typeface.NORMAL, jost.style)
        assertEquals(Typeface.NORMAL, inter.style)
        assertEquals(Typeface.NORMAL, ibmPlexMono.style)
        assertEquals(400, jost.weight)
        assertEquals(400, inter.weight)
        assertEquals(400, ibmPlexMono.weight)
        assertNotEquals(jost.toString(), inter.toString())
        assertNotEquals(inter.toString(), ibmPlexMono.toString())
    }

    @Test
    fun appUiUsesTheSameSairaFaceAsWatermarkAndPreviewUsesOnlyKk7() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val watermarkUiFace = context.resources.getFont(R.font.saira_semicondensed_semibold)
        val appUiFace = FujiTypography.uiTypeface(context)
        val previewFace = FujiTypography.androidTypeface(context)
        val kk7Face = context.resources.getFont(R.font.vcr_osd_mono_kk7)

        assertEquals(watermarkUiFace.toString(), appUiFace.toString())
        assertEquals(kk7Face.toString(), previewFace.toString())
        assertNotEquals(appUiFace.toString(), previewFace.toString())
    }

    @Test
    fun watermarkTypefaceResourcesAreDistinctFromTheVcrOsdResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val jost = context.resources.getFont(R.font.jost_watermark)
        val inter = context.resources.getFont(R.font.inter_watermark)
        val ibmPlexMono = context.resources.getFont(R.font.ibm_plex_mono_watermark)
        val vcrOsd = context.resources.getFont(R.font.vcr_osd_mono_kk7)

        assertNotEquals(vcrOsd.toString(), jost.toString())
        assertNotEquals(vcrOsd.toString(), inter.toString())
        assertNotEquals(vcrOsd.toString(), ibmPlexMono.toString())
    }
}

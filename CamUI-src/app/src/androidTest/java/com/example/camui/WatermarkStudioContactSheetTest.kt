package com.example.camui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.LinearGradient
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the production watermark renderer for every supported Studio style using a
 * deterministic 640x360 source photograph, then writes an eight-cell contact sheet
 * to app-specific external storage for visual review from the CI artifact.
 */
@RunWith(AndroidJUnit4::class)
class WatermarkStudioContactSheetTest {
    private val watermarkPreferenceKeys = setOf(
        "watermark_enabled",
        "watermark_template",
        "watermark_style",
        "watermark_logo_size",
        "watermark_brand",
        "watermark_show_time",
        "watermark_show_specs",
        "watermark_show_location",
        "watermark_show_camera_name",
        "watermark_show_lens",
        "watermark_show_focal",
        "watermark_show_aperture",
        "watermark_show_shutter",
        "watermark_show_iso",
        "watermark_show_ev",
        "watermark_show_filter",
        "watermark_show_night",
        "watermark_show_copyright",
        "watermark_show_megapixels",
        "watermark_custom_location",
        "watermark_custom_model",
        "watermark_custom_copyright",
        "selected_filter",
        "lunadream_mode",
        "nightshot_mode"
    )

    @Test
    fun rendersAllEightLayoutsAndExportsContactSheet() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = context.contentResolver
        val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
        val originalPreferences = snapshotPreferences(prefs, watermarkPreferenceKeys)
        val outputDirectory = File(
            context.getExternalFilesDir(null) ?: context.cacheDir,
            "watermark-layout-golden"
        ).apply { mkdirs() }
        assertTrue("Could not create watermark contact-sheet directory", outputDirectory.isDirectory)

        val sourceUri = createJpegRow(context, "watermark-layout-source")
        val renderedUris = mutableListOf<Uri>()
        val renderedBitmaps = mutableListOf<Pair<String, Bitmap>>()
        try {
            writeSamplePhoto(context, sourceUri)
            val saved = prefs.edit()
                .putBoolean("watermark_enabled", true)
                .putString("watermark_style", "White")
                .putString("watermark_logo_size", "M")
                .putString("watermark_brand", "fujifilm")
                .putBoolean("watermark_show_time", false)
                .putBoolean("watermark_show_specs", true)
                .putBoolean("watermark_show_location", true)
                .putString("watermark_custom_location", "TEST LOCATION")
                .putBoolean("watermark_show_camera_name", true)
                .putBoolean("watermark_show_lens", true)
                .putBoolean("watermark_show_focal", true)
                .putBoolean("watermark_show_aperture", true)
                .putBoolean("watermark_show_shutter", true)
                .putBoolean("watermark_show_iso", true)
                .putBoolean("watermark_show_ev", true)
                .putBoolean("watermark_show_filter", true)
                .putBoolean("watermark_show_night", true)
                .putBoolean("watermark_show_copyright", true)
                .putBoolean("watermark_show_megapixels", true)
                .putString("watermark_custom_model", "TEST CAMERA")
                .putString("watermark_custom_copyright", "Baoaka06")
                .putString("selected_filter", "Classic Chrome")
                .putString("lunadream_mode", "off")
                .putString("nightshot_mode", "5s")
                .commit()
            assertTrue("Could not save test watermark preferences", saved)

            val layouts = watermarkStudioLayouts
            assertTrue("Expected exactly eight Watermark Studio styles", layouts.size == 8)
            layouts.forEach { (layoutId, _) ->
                prefs.edit().putString("watermark_template", layoutId).commit()
                val outputUri = createJpegRow(context, "watermark-layout-$layoutId")
                renderedUris += outputUri
                resolver.openInputStream(sourceUri).use { input ->
                    requireNotNull(input) { "Could not read source photo for $layoutId" }
                    resolver.openOutputStream(outputUri, "wt").use { output ->
                        requireNotNull(output) { "Could not open destination photo for $layoutId" }
                        input.copyTo(output)
                    }
                }
                resolver.update(
                    outputUri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null
                )

                assertTrue(
                    "Production watermark renderer failed for layout $layoutId",
                    applyCameraWatermark(
                        context = context,
                        uri = outputUri,
                        focal = "24 mm",
                        aperture = "F1.8",
                        shutter = "1/125 s",
                        iso = "200",
                        preservePhotoAspectRatio = true
                    )
                )
                val bitmap = resolver.openInputStream(outputUri).use { input ->
                    requireNotNull(input) { "Could not decode rendered photo for $layoutId" }
                    BitmapFactory.decodeStream(input)
                }
                requireNotNull(bitmap) { "Rendered output bitmap is null for $layoutId" }
                // Each renderer style has its own output canvas; the photo rectangle
                // itself must remain native-sized and never be resampled by watermarking.
                val expectedPhotoGeometry = WatermarkGeometry.build(
                    640,
                    360,
                    WatermarkLayoutSettings(
                        layout = WatermarkLayoutType.fromPreference(layoutId)
                    ),
                    WatermarkPhotoMetadata()
                )
                assertEquals(
                    "Unexpected watermark canvas width for $layoutId",
                    expectedPhotoGeometry.canvasWidth,
                    bitmap.width
                )
                assertEquals(
                    "Unexpected watermark canvas height for $layoutId",
                    expectedPhotoGeometry.canvasHeight,
                    bitmap.height
                )
                assertEquals(640f, expectedPhotoGeometry.photoRect.width, 0.01f)
                assertEquals(360f, expectedPhotoGeometry.photoRect.height, 0.01f)
                renderedBitmaps += layoutId to bitmap
            }

            val cellWidth = 320
            val cellHeight = 200
            val sheet = Bitmap.createBitmap(
                cellWidth * 4,
                cellHeight * 2,
                Bitmap.Config.ARGB_8888
            )
            try {
                val canvas = Canvas(sheet)
                canvas.drawColor(Color.rgb(20, 25, 29))
                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 13f
                    typeface = android.graphics.Typeface.create(
                        "sans-serif-medium",
                        android.graphics.Typeface.NORMAL
                    )
                }
                renderedBitmaps.forEachIndexed { index, (layoutId, bitmap) ->
                    val column = index % 4
                    val row = index / 4
                    val left = column * cellWidth
                    val top = row * cellHeight
                    val photoRect = RectF(
                        left + 6f,
                        top + 6f,
                        left + cellWidth - 6f,
                        top + 176f
                    )
                    canvas.drawBitmap(
                        bitmap,
                        null,
                        photoRect,
                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    )
                    canvas.drawText(
                        layoutId.uppercase(),
                        left + 8f,
                        top + 194f,
                        labelPaint
                    )
                }
                val contactSheet = File(outputDirectory, "watermark-studio-8-layouts.png")
                contactSheet.outputStream().use { output ->
                    assertTrue(
                        "Could not write watermark contact sheet",
                        sheet.compress(Bitmap.CompressFormat.PNG, 100, output)
                    )
                }
                assertTrue(
                    "Watermark contact sheet was not exported",
                    contactSheet.isFile && contactSheet.length() > 20_000L
                )
            } finally {
                sheet.recycle()
            }

            renderedBitmaps.forEach { (layoutId, bitmap) ->
                val file = File(outputDirectory, "watermark-$layoutId.png")
                file.outputStream().use { output ->
                    assertTrue(
                        "Could not export watermark image for $layoutId",
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    )
                }
                assertTrue("Rendered export is missing: $layoutId", file.length() > 5_000L)
            }
        } finally {
            renderedBitmaps.forEach { (_, bitmap) -> if (!bitmap.isRecycled) bitmap.recycle() }
            renderedUris.forEach { uri -> runCatching { resolver.delete(uri, null, null) } }
            runCatching { resolver.delete(sourceUri, null, null) }
            restorePreferences(prefs, originalPreferences)
        }
    }

    private fun snapshotPreferences(
        preferences: android.content.SharedPreferences,
        keys: Set<String>
    ): Map<String, Any?> = keys.associateWith { key -> preferences.all[key] }

    private fun restorePreferences(
        preferences: android.content.SharedPreferences,
        snapshot: Map<String, Any?>
    ) {
        val editor = preferences.edit()
        snapshot.forEach { (key, value) ->
            when (value) {
                null -> editor.remove(key)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toMutableSet())
                else -> editor.remove(key)
            }
        }
        check(editor.commit()) { "Could not restore watermark preferences after instrumentation test" }
    }

    private fun createJpegRow(context: Context, suffix: String): Uri {
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, ".wm-golden-$suffix-${UUID.randomUUID()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FujifilmMode/.WatermarkGolden/")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        )
        return requireNotNull(uri) { "Could not create temporary MediaStore row: $suffix" }
    }

    private fun writeSamplePhoto(context: Context, uri: Uri) {
        val bitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = LinearGradient(
                0f,
                0f,
                640f,
                360f,
                intArrayOf(
                    Color.rgb(34, 73, 103),
                    Color.rgb(164, 128, 96),
                    Color.rgb(231, 199, 152)
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, 640f, 360f, paint)
            paint.shader = null
            paint.color = Color.rgb(38, 51, 45)
            canvas.drawCircle(440f, 148f, 92f, paint)
            paint.color = Color.rgb(215, 192, 155)
            canvas.drawRect(90f, 215f, 550f, 300f, paint)
            context.contentResolver.openOutputStream(uri, "wt").use { output ->
                requireNotNull(output) { "Could not write sample photo" }
                assertTrue(
                    "Could not encode source photo",
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
                )
            }
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null
            )
        } finally {
            bitmap.recycle()
        }
    }
}

package com.example.camui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.provider.MediaStore
import android.util.Log
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.min
import kotlin.math.roundToInt

internal data class DigitalZoomProcessingResult(
    val width: Int,
    val height: Int,
    val digitalCropRatio: Float,
    val method: String,
    val exifPreserved: Boolean
)

internal data class DigitalZoomCropBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

internal fun calculateDigitalZoomCropBounds(
    sourceWidth: Int,
    sourceHeight: Int,
    digitalCropRatio: Float
): DigitalZoomCropBounds? {
    if (sourceWidth < 32 || sourceHeight < 32 ||
        !digitalCropRatio.isFinite() || digitalCropRatio <= 1f
    ) return null
    // Derive height from the rounded width so the source crop keeps the original
    // aspect ratio instead of accumulating independent X/Y rounding distortion.
    val cropWidth = (sourceWidth / digitalCropRatio).toInt().coerceIn(16, sourceWidth)
    val cropHeight = (cropWidth.toDouble() * sourceHeight.toDouble() / sourceWidth.toDouble())
        .roundToInt().coerceIn(16, sourceHeight)
    val left = (sourceWidth - cropWidth) / 2
    val top = (sourceHeight - cropHeight) / 2
    return DigitalZoomCropBounds(left, top, left + cropWidth, top + cropHeight)
}

/**
 * Center crop + Lanczos-3 enlargement for digital zoom beyond CameraX's
 * supported range. The GPU renders bounded tiles instead of allocating a
 * full-resolution framebuffer. The JPEG is replaced only after render+encode.
 */
private const val DIGITAL_ZOOM_TILE_SIZE = 768

internal object DigitalZoomProcessor {
    private const val TAG = "FUJI_DIGITAL_ZOOM"
    private const val JPEG_QUALITY = 98

    private val exifTags = listOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        "ISOSpeedRatings",
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP
    )

    fun applyLanczosCropToUri(
        context: Context,
        uri: Uri,
        digitalCropRatio: Float
    ): DigitalZoomProcessingResult? {
        val ratio = digitalCropRatio.takeIf { it.isFinite() && it > 1.001f } ?: return null
        var decoder: BitmapRegionDecoder? = null
        var sourceRegion: Bitmap? = null
        var output: Bitmap? = null
        var gl: LanczosGlContext? = null
        val outputFile = File(context.cacheDir, "fuji-digital-${System.nanoTime()}.jpg")
        val backupFile = File(context.cacheDir, "fuji-digital-backup-${System.nanoTime()}.jpg")

        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            } ?: error("Cannot read the captured JPEG")

            val width = bounds.outWidth
            val height = bounds.outHeight
            require(width >= 128 && height >= 128) { "Invalid JPEG dimensions: ${width}x${height}" }

            val cropBounds = calculateDigitalZoomCropBounds(width, height, ratio)
                ?: error("Cannot compute a safe center crop for the requested digital zoom")
            val cropWidth = cropBounds.width
            val cropHeight = cropBounds.height
            val cropRect = Rect(cropBounds.left, cropBounds.top, cropBounds.right, cropBounds.bottom)
            val sourceExif = readExifAttributes(context, uri)

            val runtime = Runtime.getRuntime()
            val usedHeap = runtime.totalMemory() - runtime.freeMemory()
            val availableHeap = (runtime.maxMemory() - usedHeap).coerceAtLeast(0L)
            val requiredHeap = width.toLong() * height * 4L + cropWidth.toLong() * cropHeight * 4L
            require(requiredHeap + 64L * 1024L * 1024L <= availableHeap) {
                "Insufficient heap for lossless-size digital crop: estimated=${requiredHeap / 1048576L}MB " +
                    "available=${availableHeap / 1048576L}MB"
            }

            val input = context.contentResolver.openInputStream(uri)
                ?: error("Cannot reopen JPEG for region decoding")
            decoder = try {
                BitmapRegionDecoder.newInstance(input, false)
            } finally {
                input.close()
            } ?: error("JPEG region decoder unavailable")

            gl = LanczosGlContext.create()
            require(cropWidth <= gl.maxTextureSize && cropHeight <= gl.maxTextureSize) {
                "Crop source ${cropWidth}x${cropHeight} exceeds GL texture limit ${gl.maxTextureSize}"
            }

            sourceRegion = decoder.decodeRegion(
                cropRect,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inSampleSize = 1
                }
            ) ?: error("Could not decode the center crop region")
            require(sourceRegion!!.width >= 64 && sourceRegion!!.height >= 64) {
                "Decoded crop region is too small"
            }

            output = gl.renderLanczos3(sourceRegion!!, width, height, DIGITAL_ZOOM_TILE_SIZE)
            // JPEG_INTERMEDIATE_ALLOWLIST: processed digital-zoom cache JPEG; the camera pipeline applies WatermarkFinalizer/ImageSink afterwards.
            FileOutputStream(outputFile).use { stream ->
                // JPEG_INTERMEDIATE_ALLOWLIST: encode the digital-zoom intermediate cache file, not a published final photo.
                check(output!!.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                    "JPEG compression failed"
                }
                stream.flush()
                stream.fd.sync()
            }

            // Keep the original on disk until the replacement has been written completely.
            context.contentResolver.openInputStream(uri)?.use { source ->
                // JPEG_INTERMEDIATE_ALLOWLIST: rollback backup of the original capture before replacing it with the zoom intermediate.
                FileOutputStream(backupFile).use { backup ->
                    source.copyTo(backup, 1024 * 1024)
                    backup.flush()
                    backup.fd.sync()
                }
            } ?: error("Cannot back up the captured JPEG before replacement")

            try {
                // JPEG_INTERMEDIATE_ALLOWLIST: publish the digital-zoom intermediate; final watermark/filter pass follows this stage.
                context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                    outputFile.inputStream().use { source -> source.copyTo(destination, 1024 * 1024) }
                    destination.flush()
                } ?: error("Cannot replace the captured JPEG")
            } catch (writeFailure: Throwable) {
                runCatching {
                    // JPEG_INTERMEDIATE_ALLOWLIST: rollback restore of the original capture if the intermediate replacement fails.
                    context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                        backupFile.inputStream().use { source -> source.copyTo(destination, 1024 * 1024) }
                        destination.flush()
                    }
                }.onFailure { restoreFailure ->
                    Log.e(TAG, "Could not restore original JPEG after a failed digital-crop write", restoreFailure)
                }
                throw writeFailure
            }

            val exifPreserved = restoreExifAttributes(context, uri, sourceExif, width, height)
            runCatching {
                context.contentResolver.update(
                    uri,
                    ContentValues().apply {
                        put(MediaStore.Images.Media.WIDTH, width)
                        put(MediaStore.Images.Media.HEIGHT, height)
                        put(MediaStore.Images.Media.ORIENTATION, 0)
                    },
                    null,
                    null
                )
            }

            DigitalZoomProcessingResult(
                width = width,
                height = height,
                digitalCropRatio = ratio,
                method = "OpenGL ES 2.0 Lanczos-3 tiled",
                exifPreserved = exifPreserved
            ).also {
                Log.i(
                    TAG,
                    "DIGITAL_CROP_APPLIED ratio=$ratio rect=${cropRect.flattenToString()} " +
                        "output=${width}x${height} method=${it.method} exifPreserved=$exifPreserved"
                )
            }
        } catch (failure: Throwable) {
            Log.e(TAG, "DIGITAL_CROP_FAILED ratio=$ratio; capture file left untouched until final replacement", failure)
            null
        } finally {
            runCatching { decoder?.recycle() }
            runCatching { sourceRegion?.recycle() }
            runCatching { output?.recycle() }
            runCatching { gl?.close() }
            runCatching { outputFile.delete() }
            runCatching { backupFile.delete() }
        }
    }

    private fun readExifAttributes(context: Context, uri: Uri): Map<String, String> =
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                val exif = ExifInterface(descriptor.fileDescriptor)
                exifTags.mapNotNull { tag -> exif.getAttribute(tag)?.let { tag to it } }.toMap()
            }.orEmpty()
        }.onFailure { Log.w(TAG, "Could not read source EXIF before crop", it) }
            .getOrDefault(emptyMap())

    private fun restoreExifAttributes(
        context: Context,
        uri: Uri,
        attributes: Map<String, String>,
        width: Int,
        height: Int
    ): Boolean = runCatching {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "rw")
            ?: return@runCatching false
        descriptor.use {
            ExifInterface(it.fileDescriptor).apply {
                attributes.forEach { (tag, value) -> setAttribute(tag, value) }
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, width.toString())
                setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, height.toString())
                setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
                setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
                saveAttributes()
            }
        }
        true
    }.onFailure { Log.w(TAG, "Could not restore source EXIF after digital crop", it) }
        .getOrDefault(false)
}

private class LanczosGlContext private constructor(
    private val display: EGLDisplay,
    private val eglContext: EGLContext,
    private val surface: EGLSurface,
    private val program: Int,
    val maxTextureSize: Int
) : Closeable {
    private var sourceTexture = 0
    private val positions: FloatBuffer =
        ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
            position(0)
        }
    private val readback = ByteBuffer.allocateDirect(DIGITAL_ZOOM_TILE_SIZE * DIGITAL_ZOOM_TILE_SIZE * 4)
        .order(ByteOrder.nativeOrder())
    private val pixels = IntArray(DIGITAL_ZOOM_TILE_SIZE * DIGITAL_ZOOM_TILE_SIZE)

    fun renderLanczos3(source: Bitmap, outputWidth: Int, outputHeight: Int, tileSize: Int): Bitmap {
        check(EGL14.eglMakeCurrent(display, surface, surface, eglContext)) {
            "Could not make Lanczos EGL context current"
        }

        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        sourceTexture = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sourceTexture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, source, 0)
        check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "Could not upload crop bitmap to GPU" }

        GLES20.glUseProgram(program)
        val positionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        positions.position(0)
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, 0, positions)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sourceTexture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uSource"), 0)
        GLES20.glUniform2f(
            GLES20.glGetUniformLocation(program, "uSourceSize"),
            source.width.toFloat(), source.height.toFloat()
        )
        GLES20.glUniform2f(
            GLES20.glGetUniformLocation(program, "uOutputSize"),
            outputWidth.toFloat(), outputHeight.toFloat()
        )
        GLES20.glUniform2f(
            GLES20.glGetUniformLocation(program, "uCropScale"),
            outputWidth.toFloat() / source.width,
            outputHeight.toFloat() / source.height
        )
        val outputOffsetLocation = GLES20.glGetUniformLocation(program, "uOutputOffset")
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)

        try {
            var top = 0
            while (top < outputHeight) {
                val tileHeight = min(tileSize, outputHeight - top)
                var left = 0
                while (left < outputWidth) {
                    val tileWidth = min(tileSize, outputWidth - left)
                    GLES20.glViewport(0, 0, tileWidth, tileHeight)
                    GLES20.glUniform2f(
                        outputOffsetLocation,
                        left.toFloat(),
                        (outputHeight - top - tileHeight).toFloat()
                    )
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                    check(GLES20.glGetError() == GLES20.GL_NO_ERROR) {
                        "Lanczos draw failed for tile at $left,$top"
                    }

                    readback.position(0)
                    GLES20.glReadPixels(
                        0, 0, tileWidth, tileHeight,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, readback
                    )
                    check(GLES20.glGetError() == GLES20.GL_NO_ERROR) {
                        "Lanczos pixel readback failed for tile at $left,$top"
                    }

                    for (glY in 0 until tileHeight) {
                        for (x in 0 until tileWidth) {
                            val offset = (glY * tileWidth + x) * 4
                            val red = readback.get(offset).toInt() and 0xFF
                            val green = readback.get(offset + 1).toInt() and 0xFF
                            val blue = readback.get(offset + 2).toInt() and 0xFF
                            val alpha = readback.get(offset + 3).toInt() and 0xFF
                            pixels[glY * tileWidth + x] =
                                android.graphics.Color.argb(alpha, red, green, blue)
                        }
                        output.setPixels(
                            pixels,
                            glY * tileWidth,
                            tileWidth,
                            left,
                            top + tileHeight - 1 - glY,
                            tileWidth,
                            1
                        )
                    }
                    left += tileWidth
                }
                top += tileHeight
            }
            return output
        } catch (failure: Throwable) {
            output.recycle()
            throw failure
        }
    }

    override fun close() {
        if (display == EGL14.EGL_NO_DISPLAY) return
        runCatching {
            if (sourceTexture != 0) GLES20.glDeleteTextures(1, intArrayOf(sourceTexture), 0)
            GLES20.glDeleteProgram(program)
        }
        runCatching {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, eglContext)
            EGL14.eglTerminate(display)
        }
    }

    companion object {
        private const val FRAGMENT_SHADER = """
            precision highp float;
            uniform sampler2D uSource;
            uniform vec2 uSourceSize;
            uniform vec2 uOutputSize;
            uniform vec2 uOutputOffset;
            uniform vec2 uCropScale;

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

            void main() {
                float outX = uOutputOffset.x + gl_FragCoord.x - 0.5;
                float outYBottom = uOutputOffset.y + gl_FragCoord.y - 0.5;
                float outYTop = uOutputSize.y - 1.0 - outYBottom;
                float srcX = (outX + 0.5) / uCropScale.x - 0.5;
                float srcY = (outYTop + 0.5) / uCropScale.y - 0.5;
                vec2 base = floor(vec2(srcX, srcY));
                vec4 accum = vec4(0.0);
                float totalWeight = 0.0;

                for (int iy = -2; iy <= 3; iy++) {
                    float sy = clamp(base.y + float(iy), 0.0, uSourceSize.y - 1.0);
                    float wy = lanczos3(srcY - (base.y + float(iy)));
                    for (int ix = -2; ix <= 3; ix++) {
                        float sx = clamp(base.x + float(ix), 0.0, uSourceSize.x - 1.0);
                        float wx = lanczos3(srcX - (base.x + float(ix)));
                        float weight = wx * wy;
                        vec2 uv = vec2((sx + 0.5) / uSourceSize.x, (sy + 0.5) / uSourceSize.y);
                        accum += texture2D(uSource, uv) * weight;
                        totalWeight += weight;
                    }
                }
                gl_FragColor = clamp(accum / max(totalWeight, 0.00001), 0.0, 1.0);
            }
        """

        fun create(): LanczosGlContext {
            var display = EGL14.EGL_NO_DISPLAY
            var context = EGL14.EGL_NO_CONTEXT
            var surface = EGL14.EGL_NO_SURFACE
            var program = 0
            try {
                display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                check(display != EGL14.EGL_NO_DISPLAY) { "EGL display unavailable" }
                val versions = IntArray(2)
                check(EGL14.eglInitialize(display, versions, 0, versions, 1)) { "EGL initialization failed" }

                val attributes = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE
                )
                val configs = arrayOfNulls<EGLConfig>(1)
                val count = IntArray(1)
                check(EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0) && count[0] > 0) {
                    "No EGL ES2 pbuffer configuration"
                }
                val config = configs[0] ?: error("EGL config is null")
                context = EGL14.eglCreateContext(
                    display, config, EGL14.EGL_NO_CONTEXT,
                    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
                )
                check(context != EGL14.EGL_NO_CONTEXT) { "EGL context creation failed" }
                surface = EGL14.eglCreatePbufferSurface(
                    display, config,
                    intArrayOf(EGL14.EGL_WIDTH, DIGITAL_ZOOM_TILE_SIZE, EGL14.EGL_HEIGHT, DIGITAL_ZOOM_TILE_SIZE, EGL14.EGL_NONE), 0
                )
                check(surface != EGL14.EGL_NO_SURFACE) { "EGL tile surface creation failed" }
                check(EGL14.eglMakeCurrent(display, surface, surface, context)) {
                    "Could not make EGL tile surface current"
                }

                val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
                val vertex = compileShader(
                    GLES20.GL_VERTEX_SHADER,
                    "attribute vec2 aPosition; void main(){ gl_Position=vec4(aPosition,0.0,1.0); }"
                )
                program = GLES20.glCreateProgram()
                check(program != 0) { "Lanczos program allocation failed" }
                GLES20.glAttachShader(program, vertex)
                GLES20.glAttachShader(program, fragment)
                GLES20.glLinkProgram(program)
                GLES20.glDeleteShader(vertex)
                GLES20.glDeleteShader(fragment)
                val link = IntArray(1)
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, link, 0)
                check(link[0] == GLES20.GL_TRUE) {
                    "Lanczos program link failed: ${GLES20.glGetProgramInfoLog(program)}"
                }
                val maxTexture = IntArray(1)
                GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTexture, 0)
                check(maxTexture[0] > 0) { "GPU did not report a valid texture limit" }
                return LanczosGlContext(display, context, surface, program, maxTexture[0])
            } catch (failure: Throwable) {
                if (display != EGL14.EGL_NO_DISPLAY) {
                    runCatching {
                        if (program != 0) GLES20.glDeleteProgram(program)
                        if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
                        if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                        EGL14.eglTerminate(display)
                    }
                }
                throw failure
            }
        }

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            check(shader != 0) { "Shader allocation failed" }
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            if (status[0] != GLES20.GL_TRUE) {
                val message = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                error("Lanczos shader compile failed: $message")
            }
            return shader
        }
    }
}

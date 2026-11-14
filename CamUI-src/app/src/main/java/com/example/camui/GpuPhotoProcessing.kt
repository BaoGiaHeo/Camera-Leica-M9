package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * GPU image filter using an OpenGL ES 2.0 fragment shader.
 * Kept deliberately bounded to small/medium frames; large sensor images use the row-based CPU path.
 */
internal fun tryApplyGpuPhotoEnhancement(
    context: Context,
    uri: Uri,
    softwareHdr: Boolean,
    uhdSharpening: Boolean
): Boolean {
    var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    var surface: EGLSurface = EGL14.EGL_NO_SURFACE
    var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    var program = 0
    var texture = 0
    var source: Bitmap? = null
    var output: Bitmap? = null

    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0 || width.toLong() * height.toLong() > 4_000_000L) return false

        source = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return false

        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return false
        val versions = IntArray(2)
        check(EGL14.eglInitialize(display, versions, 0, versions, 1)) { "eglInitialize failed" }

        val configAttributes = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val configCount = IntArray(1)
        check(EGL14.eglChooseConfig(display, configAttributes, 0, configs, 0, 1, configCount, 0) &&
            configCount[0] > 0) { "No EGL ES2 config" }
        val config = configs[0] ?: error("EGL config is null")
        val contextAttributes = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            display, config, EGL14.EGL_NO_CONTEXT, contextAttributes, 0
        )
        check(eglContext != EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed" }

        val surfaceAttributes = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        surface = EGL14.eglCreatePbufferSurface(display, config, surfaceAttributes, 0)
        check(surface != EGL14.EGL_NO_SURFACE) { "eglCreatePbufferSurface failed" }
        check(EGL14.eglMakeCurrent(display, surface, surface, eglContext)) { "eglMakeCurrent failed" }

        val maxTextureSize = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)
        if (width > maxTextureSize[0] || height > maxTextureSize[0]) return false

        val vertexSource = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
                vTexCoord = aTexCoord;
            }
        """.trimIndent()
        val fragmentSource = """
            precision highp float;
            varying vec2 vTexCoord;
            uniform sampler2D uImage;
            uniform vec2 uTexel;
            uniform float uHdr;
            uniform float uSharpen;
            float lum(vec3 c) {
                return dot(c, vec3(0.2126, 0.7152, 0.0722));
            }
            void main() {
                vec4 color = texture2D(uImage, vTexCoord);
                float baseLum = lum(color.rgb);
                float targetLum = baseLum;
                if (uHdr > 0.5) {
                    float shadowLift = 0.105 * (1.0 - baseLum) * (1.0 - baseLum);
                    float highlight = clamp((baseLum - 0.70) / 0.30, 0.0, 1.0);
                    float highlightCompression = 0.105 * highlight * highlight;
                    targetLum = clamp(baseLum + shadowLift - highlightCompression, 0.0, 1.0);
                }
                if (uSharpen > 0.5) {
                    float leftLum = lum(texture2D(uImage, vTexCoord - vec2(uTexel.x, 0.0)).rgb);
                    float rightLum = lum(texture2D(uImage, vTexCoord + vec2(uTexel.x, 0.0)).rgb);
                    float upLum = lum(texture2D(uImage, vTexCoord + vec2(0.0, uTexel.y)).rgb);
                    float downLum = lum(texture2D(uImage, vTexCoord - vec2(0.0, uTexel.y)).rgb);
                    float blurLum = (leftLum + rightLum + upLum + downLum) * 0.25;
                    targetLum = clamp(targetLum + (baseLum - blurLum) * 1.8, 0.0, 1.0);
                }
                float scale = baseLum > 0.012 ? targetLum / baseLum : 1.0;
                gl_FragColor = vec4(clamp(color.rgb * scale, 0.0, 1.0), color.a);
            }
        """.trimIndent()

        fun compileShader(type: Int, code: String): Int {
            val shader = GLES20.glCreateShader(type)
            check(shader != 0) { "glCreateShader failed" }
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val message = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                error("Shader compile failed: $message")
            }
            return shader
        }

        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        check(linkStatus[0] == 1) { "Shader link failed: ${GLES20.glGetProgramInfoLog(program)}" }

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        texture = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, source, 0)

        val positions = floatBuffer(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
        val textureCoords = floatBuffer(floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f))
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(program)
        val positionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        val texLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        positions.position(0)
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, 0, positions)
        textureCoords.position(0)
        GLES20.glEnableVertexAttribArray(texLocation)
        GLES20.glVertexAttribPointer(texLocation, 2, GLES20.GL_FLOAT, false, 0, textureCoords)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uImage"), 0)
        GLES20.glUniform2f(
            GLES20.glGetUniformLocation(program, "uTexel"),
            1f / width.toFloat(),
            1f / height.toFloat()
        )
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uHdr"), if (softwareHdr) 1f else 0f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uSharpen"), if (uhdSharpening) 1f else 0f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFinish()
        val glError = GLES20.glGetError()
        check(glError == GLES20.GL_NO_ERROR) { "OpenGL processing error: $glError" }

        val pixels = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
        pixels.position(0)
        val pixelArray = IntArray(width * height)
        for (glY in 0 until height) {
            val bitmapY = height - 1 - glY
            for (x in 0 until width) {
                val red = pixels.get().toInt() and 0xff
                val green = pixels.get().toInt() and 0xff
                val blue = pixels.get().toInt() and 0xff
                val alpha = pixels.get().toInt() and 0xff
                pixelArray[bitmapY * width + x] = android.graphics.Color.argb(alpha, red, green, blue)
            }
        }
        output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixelArray, 0, width, 0, 0, width, height)
        val written = context.contentResolver.openOutputStream(uri, "wt")?.use {
            output.compress(Bitmap.CompressFormat.JPEG, 98, it)
        } ?: false
        if (!written) return false
        context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }
        }
        Log.i("FUJI_IMAGE_PROCESS", "GPU path finished for ${width}x${height}")
        true
    } catch (error: Throwable) {
        Log.w("FUJI_IMAGE_PROCESS", "GPU path unavailable; CPU fallback will be used", error)
        false
    } finally {
        if (texture != 0) GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
        if (program != 0) GLES20.glDeleteProgram(program)
        source?.recycle()
        output?.recycle()
        if (display != EGL14.EGL_NO_DISPLAY) {
            runCatching {
                EGL14.eglMakeCurrent(
                    display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
                )
                if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
                if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
                EGL14.eglTerminate(display)
            }
        }
    }
}

private fun floatBuffer(values: FloatArray): FloatBuffer =
    ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder())
        .asFloatBuffer().apply { put(values); position(0) }

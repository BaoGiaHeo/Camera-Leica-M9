package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Extracts only app-bundled wordmarks and shutter effects from camera-assets.zip.
 * Files are copied to the app cache once and reused by preview, export and MediaPlayer.
 */
object CameraAssetStore {
    private const val ARCHIVE_NAME = "camera-assets.zip"
    private val lock = Any()

    fun file(context: Context, assetPath: String): File? {
        if (!assetPath.startsWith("watermark-brands/") &&
            !assetPath.startsWith("shutter-sounds/")) return null

        val safeName = assetPath.substringAfterLast('/')
        val directory = File(context.cacheDir, "fujifilm-camera-assets")
        val target = File(directory, safeName)
        if (target.isFile && target.length() > 0L) return target

        synchronized(lock) {
            if (target.isFile && target.length() > 0L) return target
            if (!directory.exists() && !directory.mkdirs()) return null
            val temporary = File(directory, "$safeName.tmp")
            var extracted = false
            val succeeded = runCatching {
                context.assets.open(ARCHIVE_NAME).use { input ->
                    ZipInputStream(input).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null && !extracted) {
                            if (entry.name == assetPath && !entry.isDirectory) {
                                temporary.outputStream().use { output -> zip.copyTo(output) }
                                extracted = true
                            }
                            zip.closeEntry()
                            if (!extracted) entry = zip.nextEntry
                        }
                    }
                }
            }.isSuccess
            if (!succeeded || !extracted) {
                temporary.delete()
                return null
            }
            if (!temporary.renameTo(target)) {
                runCatching { temporary.copyTo(target, overwrite = true) }
                temporary.delete()
            }
            return target.takeIf { it.isFile && it.length() > 0L }
        }
    }

    fun bitmap(context: Context, assetPath: String): Bitmap? =
        file(context, assetPath)?.let { BitmapFactory.decodeFile(it.absolutePath) }
}

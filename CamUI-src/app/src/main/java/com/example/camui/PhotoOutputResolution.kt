package com.example.camui
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 211MP pixel-count interpolation for ordinary 1x JPEGs, not extra sensor detail. */
object PhotoOutputResolution {
 private const val TAG = "FUJI_211MP"
 private const val TARGET_PIXELS = 211_000_000L
 fun interpolateTo211Mp(context: Context, uri: Uri): Boolean {
  val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
  val temp = File(context.cacheDir, "fuji-211mp-$stamp.jpg")
  val backup = File(context.cacheDir, "fuji-211mp-original-$stamp.jpg")
  var source: Bitmap? = null
  var output: Bitmap? = null
  var replacing = false
  return try {
   val tags = readExif(context, uri)
   source = context.contentResolver.openInputStream(uri)?.use { input ->
    BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply {
     inPreferredConfig = Bitmap.Config.RGB_565
     inDither = true
    })
   } ?: error("Cannot decode original JPEG")
   val src = source!!
   val sourcePixels = src.width.toLong() * src.height.toLong()
   require(sourcePixels > 0L) { "Invalid source size" }
   if (sourcePixels >= TARGET_PIXELS) return true
   val scale = sqrt(TARGET_PIXELS.toDouble() / sourcePixels.toDouble())
   var width = (src.width * scale).roundToInt().coerceAtLeast(src.width)
   var height = (src.height * scale).roundToInt().coerceAtLeast(src.height)
   while (width.toLong() * height.toLong() > TARGET_PIXELS) {
    if (width >= height) width-- else height--
   }
   val outputPixels = width.toLong() * height.toLong()
   val outputBytes = outputPixels * 2L
   val safeHeap = (Runtime.getRuntime().maxMemory().toDouble() * 0.82).toLong()
   require(outputBytes + src.allocationByteCount.toLong() < safeHeap) {
    "Insufficient safe heap for 211MP export"
   }
   output = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
   Canvas(output!!).drawBitmap(src, null, Rect(0, 0, width, height),
    Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply { isAntiAlias = true })
   FileOutputStream(temp).use { stream ->
    check(output!!.compress(Bitmap.CompressFormat.JPEG, 96, stream)) { "JPEG encoding failed" }
    stream.flush()
    stream.fd.sync()
   }
   check(temp.length() > 0L)
   writeExif(temp, tags)
   context.contentResolver.openInputStream(uri)?.use { input ->
    FileOutputStream(backup).use { stream -> input.copyTo(stream); stream.fd.sync() }
   } ?: error("Cannot back up source image")
   replacing = true
   val copied = context.contentResolver.openOutputStream(uri, "wt")?.use { dest ->
    temp.inputStream().use { it.copyTo(dest) }; true
   } ?: false
   check(copied) { "MediaStore replacement failed" }
   context.contentResolver.openFileDescriptor(uri, "rw")?.use { fd ->
    ExifInterface(fd.fileDescriptor).apply {
     setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
     saveAttributes()
    }
   }
   context.contentResolver.update(uri, ContentValues().apply {
    put(MediaStore.Images.Media.ORIENTATION, 0)
   }, null, null)
   Log.i(TAG, "211MP interpolation: ${width}x${height}")
   true
  } catch (failure: Throwable) {
   if (replacing && backup.exists()) runCatching {
    context.contentResolver.openOutputStream(uri, "wt")?.use { dest ->
     backup.inputStream().use { it.copyTo(dest) }
    } ?: error("Rollback failed")
   }.onFailure { Log.e(TAG, "Original restore failed", it) }
   Log.e(TAG, "211MP interpolation skipped; keep source", failure)
   false
  } finally {
   source?.let { if (!it.isRecycled) it.recycle() }
   output?.let { if (!it.isRecycled) it.recycle() }
   runCatching { temp.delete() }
   runCatching { backup.delete() }
  }
 }
 private fun readExif(context: Context, uri: Uri): Map<String, String> {
  val keys = listOf(ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL,
   ExifInterface.TAG_DATETIME_DIGITIZED, ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL,
   ExifInterface.TAG_SOFTWARE, ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_F_NUMBER,
   ExifInterface.TAG_ISO_SPEED_RATINGS, ExifInterface.TAG_FOCAL_LENGTH,
   ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_FLASH)
  return runCatching {
   context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
    val exif = ExifInterface(fd.fileDescriptor)
    keys.mapNotNull { key -> exif.getAttribute(key)?.let { key to it } }.toMap()
   }.orEmpty()
  }.getOrDefault(emptyMap())
 }
 private fun writeExif(file: File, tags: Map<String, String>) {
  ExifInterface(file.absolutePath).apply {
   tags.forEach { (key, value) -> setAttribute(key, value) }
   setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
   saveAttributes()
  }
 }
}

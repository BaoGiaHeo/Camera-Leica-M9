package com.example.camui

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

internal data class LunaDreamXmpMetadata(
    val mode: String,
    val frameCount: Int,
    val width: Int,
    val height: Int,
    val requestedBasePixels: Long,
    val actualFilePixels: Long,
    val isAspectCropped: Boolean,
    val availableStrategy: String,
    val activeCaptureStrategy: String,
    val sensorPixelArray: String?,
    val maximumResolutionJpeg: String?
)

/**
 * Adds one standard Adobe XMP APP1 segment immediately after JPEG SOI.
 * The compressed image scan data is copied byte-for-byte; no decode or re-encode occurs.
 */
internal object LunaDreamXmpWriter {
    private const val TAG = "FUJI_LUNADREAM_XMP"
    private const val XMP_NAMESPACE = "https://fujifilm.mode/ns/lunadream/1.0/"
    private val xmpIdentifier = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(StandardCharsets.US_ASCII)

    fun write(context: Context, uri: Uri, metadata: LunaDreamXmpMetadata): Boolean {
        val stamp = System.currentTimeMillis()
        val staged = File(context.cacheDir, "lunadream-xmp-$stamp.jpg")
        val backup = File(context.cacheDir, "lunadream-xmp-backup-$stamp.jpg")
        var replacing = false
        val startedAt = android.os.SystemClock.elapsedRealtime()
        return try {
            val xml = buildPacket(metadata)
            val xmlBytes = xml.toByteArray(StandardCharsets.UTF_8)
            val payloadSize = xmpIdentifier.size + xmlBytes.size
            val segmentLength = payloadSize + 2
            require(segmentLength <= 0xFFFF) { "XMP APP1 packet exceeds JPEG segment limit" }

            val input = context.contentResolver.openInputStream(uri)
                ?: error("Could not open final LunaDream JPEG")
            input.use { source ->
                val buffered = BufferedInputStream(source, 256 * 1024)
                FileOutputStream(staged).use { destination ->
                    val soi = ByteArray(2)
                    readFully(buffered, soi)
                    check(soi[0] == 0xFF.toByte() && soi[1] == 0xD8.toByte()) {
                        "Saved image is not a JPEG stream"
                    }
                    destination.write(soi)
                    destination.write(0xFF)
                    destination.write(0xE1)
                    destination.write((segmentLength ushr 8) and 0xFF)
                    destination.write(segmentLength and 0xFF)
                    destination.write(xmpIdentifier)
                    destination.write(xmlBytes)
                    buffered.copyTo(destination, 256 * 1024)
                    destination.flush()
                    destination.fd.sync()
                }
            }
            check(staged.isFile && staged.length() > 4L) { "XMP staging JPEG is empty" }

            context.contentResolver.openInputStream(uri)?.use { source ->
                FileOutputStream(backup).use { destination ->
                    source.copyTo(destination, 256 * 1024)
                    destination.flush()
                    destination.fd.sync()
                }
            } ?: error("Could not back up final LunaDream JPEG")

            replacing = true
            context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                FileInputStream(staged).use { source ->
                    source.copyTo(destination, 256 * 1024)
                    destination.flush()
                }
            } ?: error("Could not replace final JPEG with XMP metadata")

            verifyXmpPrefix(context, uri)
            Log.i(
                TAG,
                "XMP_WRITTEN mode=" + metadata.mode +
                    " frames=" + metadata.frameCount +
                    " output=" + metadata.width + "x" + metadata.height +
                    " requestedBasePixels=" + metadata.requestedBasePixels +
                    " actualFilePixels=" + metadata.actualFilePixels +
                    " aspectCropped=" + metadata.isAspectCropped +
                    " availableStrategy=" + metadata.availableStrategy +
                    " activeCaptureStrategy=" + metadata.activeCaptureStrategy +
                    " elapsed=" + (android.os.SystemClock.elapsedRealtime() - startedAt) + "ms"
            )
            true
        } catch (failure: Throwable) {
            if (replacing && backup.isFile) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                        FileInputStream(backup).use { source ->
                            source.copyTo(destination, 256 * 1024)
                            destination.flush()
                        }
                    } ?: error("Could not restore original JPEG after XMP failure")
                }.onFailure {
                    Log.e(TAG, "XMP_FAILURE_ROLLBACK_FAILED", it)
                }
            }
            Log.e(TAG, "XMP_WRITE_FAILED; final JPEG was restored when possible", failure)
            false
        } finally {
            runCatching { staged.delete() }
            runCatching { backup.delete() }
        }
    }

    private fun buildPacket(metadata: LunaDreamXmpMetadata): String {
        val mode = xmlEscape(metadata.mode)
        val available = xmlEscape(metadata.availableStrategy)
        val active = xmlEscape(metadata.activeCaptureStrategy)
        val sensor = xmlEscape(metadata.sensorPixelArray ?: "unknown")
        val maximum = xmlEscape(metadata.maximumResolutionJpeg ?: "unknown")
        val reconstruction = when (metadata.mode) {
            "LunaDream 1.5" -> "single-frame noise-map denoise and detail recovery"
            "LunaDream 2.0" -> "3-level LK multi-frame fusion and edge-aware reconstruction"
            "LunaDream 2.5" -> "4-level LK multi-frame fusion and edge-aware reconstruction"
            "LunaDream 3.0" -> "5-level LK multi-frame fusion and edge-aware reconstruction"
            else -> "LunaDream computational reconstruction"
        }
        return """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:ld="$XMP_NAMESPACE"
                  xmp:CreatorTool="FujiFilm Mode"
                  ld:Mode="$mode"
                  ld:FrameCount="${metadata.frameCount}"
                  ld:OutputWidth="${metadata.width}"
                  ld:OutputHeight="${metadata.height}"
                  ld:RequestedBasePixels="${metadata.requestedBasePixels}"
                  ld:ActualFilePixels="${metadata.actualFilePixels}"
                  ld:AspectCropped="${metadata.isAspectCropped}"
                  ld:AvailableStrategy="$available"
                  ld:ActiveCaptureStrategy="$active"
                  ld:SensorPixelArray="$sensor"
                  ld:MaximumResolutionJpeg="$maximum"
                  ld:Reconstruction="$reconstruction"/>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()
    }

    private fun verifyXmpPrefix(context: Context, uri: Uri) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffered = BufferedInputStream(input, 16 * 1024)
            val prefix = ByteArray(2 + 4 + xmpIdentifier.size)
            readFully(buffered, prefix)
            check(prefix[0] == 0xFF.toByte() && prefix[1] == 0xD8.toByte()) {
                "Final JPEG SOI marker is missing after XMP write"
            }
            check(prefix[2] == 0xFF.toByte() && prefix[3] == 0xE1.toByte()) {
                "XMP APP1 marker is missing after JPEG SOI"
            }
            val length = ((prefix[4].toInt() and 0xFF) shl 8) or (prefix[5].toInt() and 0xFF)
            check(length >= xmpIdentifier.size + 2) { "XMP APP1 marker length is invalid" }
            for (index in xmpIdentifier.indices) {
                check(prefix[6 + index] == xmpIdentifier[index]) { "XMP identifier verification failed" }
            }
        } ?: error("Could not reopen final JPEG to verify XMP")
    }

    private fun readFully(input: BufferedInputStream, destination: ByteArray) {
        var offset = 0
        while (offset < destination.size) {
            val count = input.read(destination, offset, destination.size - offset)
            if (count < 0) error("Unexpected end of JPEG while reading marker header")
            offset += count
        }
    }

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

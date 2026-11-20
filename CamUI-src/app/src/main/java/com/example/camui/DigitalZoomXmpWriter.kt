package com.example.camui

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Writes a standards-compatible Adobe XMP APP1 packet into a JPEG.
 * Existing XMP APP1 packets are replaced; EXIF and other JPEG segments are copied.
 */
internal object DigitalZoomXmpWriter {
    private const val TAG = "FUJI_XMP"
    private val XMP_HEADER = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(StandardCharsets.US_ASCII)

    fun writeDigitalZoom(
        context: Context,
        uri: Uri,
        requestedZoomRatio: Float,
        cameraControlZoomRatio: Float,
        digitalCropRatio: Float
    ): Boolean {
        if (!requestedZoomRatio.isFinite() || !cameraControlZoomRatio.isFinite() ||
            !digitalCropRatio.isFinite() || digitalCropRatio <= 1.001f
        ) return false

        val original = File(context.cacheDir, "fuji-xmp-source-${System.nanoTime()}.jpg")
        val rewritten = File(context.cacheDir, "fuji-xmp-output-${System.nanoTime()}.jpg")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(original).use { output ->
                    input.copyTo(output, 1024 * 1024)
                    output.flush()
                    output.fd.sync()
                }
            } ?: error("Cannot open image to add XMP")

            FileInputStream(original).use { input ->
                FileOutputStream(rewritten).use { output ->
                    injectXmp(
                        input = BufferedInputStream(input),
                        output = BufferedOutputStream(output),
                        xmlPacket = buildDigitalZoomXml(
                            requestedZoomRatio,
                            cameraControlZoomRatio,
                            digitalCropRatio
                        )
                    )
                    output.flush()
                    output.fd.sync()
                }
            }

            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                    FileInputStream(rewritten).use { source -> source.copyTo(destination, 1024 * 1024) }
                    destination.flush()
                } ?: error("Cannot write XMP JPEG back to MediaStore")
            } catch (writeFailure: Throwable) {
                // A failed MediaStore write should not destroy the captured original.
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                        FileInputStream(original).use { source -> source.copyTo(destination, 1024 * 1024) }
                        destination.flush()
                    }
                }.onFailure { restoreFailure ->
                    Log.e(TAG, "Could not restore source JPEG after an XMP write failure", restoreFailure)
                }
                throw writeFailure
            }

            Log.i(
                TAG,
                String.format(
                    Locale.US,
                    "XMP_DIGITAL_WRITTEN requested=%.2fx camera=%.2fx crop=%.4fx uri=%s",
                    requestedZoomRatio,
                    cameraControlZoomRatio,
                    digitalCropRatio,
                    uri
                )
            )
            true
        } catch (failure: Throwable) {
            Log.e(TAG, "Could not write DIGITAL XMP metadata", failure)
            false
        } finally {
            runCatching { original.delete() }
            runCatching { rewritten.delete() }
        }
    }

    internal fun buildDigitalZoomXml(
        requestedZoomRatio: Float,
        cameraControlZoomRatio: Float,
        digitalCropRatio: Float
    ): String {
        val requested = requestedZoomRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
        val camera = cameraControlZoomRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
        val crop = digitalCropRatio.takeIf { it.isFinite() && it >= 1f } ?: 1f
        return """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about="" xmlns:fuji="https://ns.fujifilmmode.app/xmp/1.0/">
                  <fuji:ZoomType>DIGITAL</fuji:ZoomType>
                  <fuji:RequestedZoomRatio>${String.format(Locale.US, "%.3f", requested)}</fuji:RequestedZoomRatio>
                  <fuji:CameraControlZoomRatio>${String.format(Locale.US, "%.3f", camera)}</fuji:CameraControlZoomRatio>
                  <fuji:DigitalCropRatio>${String.format(Locale.US, "%.5f", crop)}</fuji:DigitalCropRatio>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()
    }

    private fun injectXmp(input: InputStream, output: OutputStream, xmlPacket: String) {
        val data = DataInputStream(input)
        val soiHigh = data.readUnsignedByte()
        val soiLow = data.readUnsignedByte()
        require(soiHigh == 0xFF && soiLow == 0xD8) { "Source is not a JPEG stream" }
        output.write(0xFF)
        output.write(0xD8)

        val xmpPayload = XMP_HEADER + xmlPacket.toByteArray(StandardCharsets.UTF_8)
        val segmentLength = xmpPayload.size + 2
        require(segmentLength <= 0xFFFF) { "XMP APP1 packet exceeds JPEG segment length" }
        output.write(0xFF)
        output.write(0xE1)
        output.write((segmentLength shr 8) and 0xFF)
        output.write(segmentLength and 0xFF)
        output.write(xmpPayload)

        while (true) {
            val prefix = data.read()
            if (prefix < 0) break
            require(prefix == 0xFF) { "Unexpected byte before JPEG scan data" }

            var marker = data.read()
            while (marker == 0xFF) marker = data.read()
            require(marker >= 0) { "Truncated JPEG marker" }

            if (marker == 0xDA) {
                val high = data.readUnsignedByte()
                val low = data.readUnsignedByte()
                val length = (high shl 8) or low
                require(length >= 2) { "Invalid JPEG scan header" }
                output.write(0xFF)
                output.write(marker)
                output.write(high)
                output.write(low)
                copyExactly(data, output, length - 2)
                data.copyTo(output, 1024 * 1024)
                return
            }

            if (marker == 0xD9) {
                output.write(0xFF)
                output.write(marker)
                data.copyTo(output, 1024 * 1024)
                return
            }

            if (marker == 0x01 || marker in 0xD0..0xD7 || marker == 0xD8) {
                output.write(0xFF)
                output.write(marker)
                continue
            }

            val high = data.readUnsignedByte()
            val low = data.readUnsignedByte()
            val length = (high shl 8) or low
            require(length >= 2) { "Invalid JPEG segment length" }
            val payload = ByteArray(length - 2)
            data.readFully(payload)

            val isOldXmp = marker == 0xE1 && payload.size >= XMP_HEADER.size &&
                payload.copyOfRange(0, XMP_HEADER.size).contentEquals(XMP_HEADER)
            if (!isOldXmp) {
                output.write(0xFF)
                output.write(marker)
                output.write(high)
                output.write(low)
                output.write(payload)
            }
        }
        error("JPEG ended before its image scan marker")
    }

    private fun copyExactly(input: InputStream, output: OutputStream, bytes: Int) {
        var remaining = bytes
        val buffer = ByteArray(8192)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size, remaining))
            require(read > 0) { "Truncated JPEG segment" }
            output.write(buffer, 0, read)
            remaining -= read
        }
    }
}

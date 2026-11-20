package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class DigitalZoomXmpWriterTest {
    @Test
    fun digitalZoomPacketIncludesRequestedCameraAndCropRatios() {
        val xml = DigitalZoomXmpWriter.buildDigitalZoomXml(
            requestedZoomRatio = 100f,
            cameraControlZoomRatio = 50f,
            digitalCropRatio = 2f
        )
        assertTrue(xml.contains("<fuji:ZoomType>DIGITAL</fuji:ZoomType>"))
        assertTrue(xml.contains("<fuji:RequestedZoomRatio>100.000</fuji:RequestedZoomRatio>"))
        assertTrue(xml.contains("<fuji:CameraControlZoomRatio>50.000</fuji:CameraControlZoomRatio>"))
        assertTrue(xml.contains("<fuji:DigitalCropRatio>2.00000</fuji:DigitalCropRatio>"))
    }

    @Test
    fun jpegWriterKeepsJfifAndExifAndReplacesOldXmpPacket() {
        val oldHeader = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(StandardCharsets.US_ASCII)
        val oldXmp = oldHeader + "old".toByteArray(StandardCharsets.UTF_8)
        val jpeg = ByteArrayOutputStream().apply {
            write(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
            writeSegment(0xE0, "JFIF".toByteArray(StandardCharsets.US_ASCII))
            writeSegment(0xE1, "Exif\u0000\u0000".toByteArray(StandardCharsets.US_ASCII))
            writeSegment(0xE1, oldXmp)
            write(byteArrayOf(0xFF.toByte(), 0xDA.toByte(), 0x00, 0x02))
            write(byteArrayOf(0x31, 0x32, 0xFF.toByte(), 0xD9.toByte()))
        }.toByteArray()

        val xml = DigitalZoomXmpWriter.buildDigitalZoomXml(70f, 50f, 1.4f)
        val output = ByteArrayOutputStream()
        DigitalZoomXmpWriter.injectXmp(ByteArrayInputStream(jpeg), output, xml)
        val result = output.toByteArray()
        val resultText = String(result, StandardCharsets.ISO_8859_1)
        val xmpHeader = "http://ns.adobe.com/xap/1.0/"
        assertTrue(resultText.contains("JFIF"))
        assertTrue(resultText.contains("Exif"))
        assertTrue(resultText.contains("<fuji:ZoomType>DIGITAL</fuji:ZoomType>"))
        assertTrue(resultText.contains("70.000"))
        assertEquals(1, occurrences(resultText, xmpHeader))
        assertTrue(resultText.endsWith(String(byteArrayOf(0x31, 0x32), StandardCharsets.ISO_8859_1) + "\u00FF\u00D9"))
    }

    private fun ByteArrayOutputStream.writeSegment(marker: Int, payload: ByteArray) {
        val length = payload.size + 2
        write(0xFF)
        write(marker)
        write((length shr 8) and 0xFF)
        write(length and 0xFF)
        write(payload)
    }

    private fun occurrences(text: String, needle: String): Int {
        var count = 0
        var index = 0
        while (true) {
            index = text.indexOf(needle, index)
            if (index < 0) return count
            count++
            index += needle.length
        }
    }
}

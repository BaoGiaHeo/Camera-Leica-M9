package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UiTranslationCatalogTest {
    @Test
    fun cameraAndProcessingStatusMessagesHaveEnglishTranslations() {
        val cases = listOf(
            "Camera này không hỗ trợ RAW/RawDream 2.0 đồng thời. Không thể dùng chế độ RAW đã chọn.",
            "Star Trails tự dừng để bảo vệ bộ nhớ tạm. Đang ghép các khung đã ghi.",
            "LunaDream cần Photo/JPEG; RawDream 2.0 cần RAW + JPEG được camera hỗ trợ. Hãy tắt Ultra HDR, Burst và Bracketing.",
            "Đang hoàn tất chuỗi Bracketing hiện tại…",
            "Đã lưu ảnh RAW DNG gốc vào Pictures/FujifilmMode/RAW.",
            "ĐANG QUÉT CAMERA…"
        )
        cases.forEach { source ->
            assertNotNull("Missing English translation for: $source", UiTranslationCatalog.toEnglish(source))
        }
    }

    @Test
    fun commonEnglishCameraAndSettingsLabelsHaveVietnameseTranslations() {
        val cases = mapOf(
            "Camera Presets" to "Thiết lập camera",
            "Film Simulation Customizer" to "Tùy chỉnh mô phỏng phim",
            "RAW Capture" to "Chụp RAW",
            "Night capture and Star Trails use separate processing pipelines" to
                "Chụp đêm và Star Trails sử dụng các pipeline xử lý riêng."
        )
        cases.forEach { (source, expected) ->
            assertEquals(expected, UiTranslationCatalog.toVietnamese(source))
        }
    }

    @Test
    fun dynamicMessagesKeepTheirValuesWhenTranslated() {
        assertEquals("2 bộ lọc", UiTranslationCatalog.toVietnamese("2 filters"))
        assertEquals("2 filters", UiTranslationCatalog.toEnglish("2 bộ lọc"))
        assertEquals(
            "Đã lưu Camera Preset 3",
            UiTranslationCatalog.toVietnamese("Saved Camera Preset 3")
        )
        assertEquals(
            "Preset 3 has not been saved.",
            UiTranslationCatalog.toEnglish("Preset 3 chưa được lưu")
        )
        assertEquals(
            "NIGHTSHOT · OFF",
            UiTranslationCatalog.toEnglish("NIGHTSHOT · TẮT")
        )
        assertEquals(
            "Merging Star Trails · 8 frames",
            UiTranslationCatalog.toEnglish("Đang ghép vệt sao · 8 khung hình")
        )
        assertEquals(
            "Saved Star Trails from 8 frames.",
            UiTranslationCatalog.toEnglish("Đã lưu Star Trails từ 8 khung hình")
        )
        assertEquals(
            "Could not set focus position 3; stopping Focus Bracketing.",
            UiTranslationCatalog.toEnglish("Không thể đặt khoảng nét 3; dừng Focus Bracketing.")
        )
        assertEquals(
            "Applied ISO 800 · 1.0s · WB 3800K. Capture 8 photos for alignment and noise reduction.",
            UiTranslationCatalog.toEnglish("Đã áp dụng ISO 800 · 1.0s · WB 3800K. Chụp 8 ảnh để căn chỉnh và giảm nhiễu.")
        )
        assertEquals(
            "Showing 10 / 25 matching LUTs · Total 42",
            UiTranslationCatalog.toEnglish("Hiển thị 10 / 25 LUT khớp · Tổng 42")
        )
        assertEquals(
            "Are you sure you want to delete LUT Classic.cube? This LUT will also be removed from Filters.",
            UiTranslationCatalog.toEnglish("Bạn có chắc chắn muốn xóa LUT \"Classic.cube\"? LUT này cũng sẽ bị gỡ khỏi mục Bộ lọc.")
        )
        assertEquals(
            "LunaDream is optimizing output size to 50MP",
            UiTranslationCatalog.toEnglish("LunaDream đang tối ưu kích thước 50MP")
        )
        assertEquals("Color temperature 4200 K", UiTranslationCatalog.toEnglish("Tông màu 4200 K"))
        assertEquals("12 filters", UiTranslationCatalog.toEnglish("12 bộ lọc"))
        assertEquals("4 photos", UiTranslationCatalog.toEnglish("4 ảnh"))
        assertEquals("Saved Camera Preset 2", UiTranslationCatalog.toEnglish("Đã lưu Camera Preset 2"))
        assertEquals(
            "Preset 2 has not been saved.",
            UiTranslationCatalog.toEnglish("Preset 2 chưa được lưu")
        )
        assertEquals(
            "Loaded Camera Preset 2 · capture settings restored.",
            UiTranslationCatalog.toEnglish("Đã nạp Camera Preset 2 · cấu hình chụp đã khôi phục")
        )
        assertEquals("Saved Film Recipe 2", UiTranslationCatalog.toEnglish("Đã lưu Film Recipe 2"))
        assertEquals(
            "Film Recipe 2 has not been saved.",
            UiTranslationCatalog.toEnglish("Film Recipe 2 chưa được lưu")
        )
        assertEquals("Loaded Film Recipe 2", UiTranslationCatalog.toEnglish("Đã nạp Film Recipe 2"))
        assertEquals("10 seconds", UiTranslationCatalog.toEnglish("10 giây"))
        assertEquals("10 giây", UiTranslationCatalog.toVietnamese("10 seconds"))
        assertEquals(
            "The LUT package exceeds the limit of 700 filters.",
            UiTranslationCatalog.toEnglish("Gói LUT vượt quá 700 bộ lọc.")
        )
        assertEquals(
            "Gói LUT vượt quá 700 bộ lọc.",
            UiTranslationCatalog.toVietnamese("The LUT package exceeds the limit of 700 filters.")
        )
        assertEquals(
            "Multi-frame: capturing frame 4/5",
            UiTranslationCatalog.toEnglish("Multi-frame: chụp khung 4/5")
        )
        assertEquals(
            "NightShotMode: capturing frame 3/8 · queue 2",
            UiTranslationCatalog.toEnglish("NightShotMode: thu khung 3/8 · hàng đợi 2")
        )
        assertEquals(
            "HDR Bracketing: saved 3/3 frames. Merged the composite photo and preserved 3 original photos.",
            UiTranslationCatalog.toEnglish("HDR Bracketing: đã lưu 3/3 khung. Đã ghép ảnh tổng hợp và vẫn giữ 3 ảnh gốc.")
        )
        assertEquals(
            "Đã lưu Live Photo: ảnh IMG_0001.jpg và clip VID_0001.mp4 trong Movies/FujifilmMode/LivePhoto.",
            UiTranslationCatalog.toVietnamese("Saved Live Photo: photo IMG_0001.jpg and clip VID_0001.mp4 in Movies/FujifilmMode/LivePhoto.")
        )
    }
}

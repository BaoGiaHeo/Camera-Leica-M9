package com.example.camui

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Typography
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize

private tailrec fun Context.findActivityResultRegistryOwner(): ActivityResultRegistryOwner? {
    return (this as? ActivityResultRegistryOwner)
        ?: (this as? ContextWrapper)?.baseContext?.findActivityResultRegistryOwner()
}

object CamColors {
    var Bg by mutableStateOf(Color(0xFF121212))
    var Shutter by mutableStateOf(Color.White)
    var TextColor by mutableStateOf(Color.White)
    var Sub by mutableStateOf(Color(0xFF9E9E9E))
    val Card = Color(0xFF1E1E1E)
    val Glass = Color(0xD92A2A2A)
    val Panel = Color(0xF21A1A1A)
    val Chip = Color(0xFF2A2A2A)
    val Yellow = Color(0xFFF5C842)
    val Stroke = Color(0x33FFFFFF)
}

private fun parseAppColor(value: String?, fallback: Color): Color {
    return runCatching { value?.let { Color(android.graphics.Color.parseColor(it)) } ?: fallback }
        .getOrDefault(fallback)
}

/** All selected frame ratios use height / width in the portrait coordinate system. */
val aspectRatios = listOf(
    "9:16" to (16f / 9f),
    "3:4" to (4f / 3f),
    "1:1" to 1f,
    "9:21" to (21f / 9f)
)

/** Returns height / width for the selected frame in its current physical orientation. */
fun aspectRatioForOrientation(index: Int, isLandscape: Boolean): Float {
    val portraitHeightOverWidth = aspectRatios[index.coerceIn(0, aspectRatios.lastIndex)].second
    return if (isLandscape) 1f / portraitHeightOverWidth else portraitHeightOverWidth
}

/** Labels are expressed in the orientation currently shown by the camera UI. */
fun aspectRatioDisplayLabel(index: Int, isLandscape: Boolean): String {
    return when (index.coerceIn(0, aspectRatios.lastIndex)) {
        0 -> if (isLandscape) "16:9" else "9:16"
        1 -> if (isLandscape) "4:3" else "3:4"
        2 -> "1:1"
        else -> if (isLandscape) "21:9" else "9:21"
    }
}

@Composable
fun CamTheme(content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    // Keep ActivityResultRegistryOwner from the original Activity before
    // LocalContext is replaced by the locale-specific configuration context.
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current
        ?: baseContext.findActivityResultRegistryOwner()
        ?: error("CamTheme must be hosted by an ActivityResultRegistryOwner.")
    val activeLanguage = AppLanguageManager.currentLanguage
    val context = AppLanguageManager.localizedContext(baseContext, activeLanguage)
    val prefs = context.getSharedPreferences("fujifilm_settings", 0)
    val shutterHex = prefs.getString("shutter_button_color", "#FFFFFFFF")
    SideEffect {
        val fixedBackground = Color(0xFF121212)
        CamColors.Bg = fixedBackground
        CamColors.TextColor = Color.White
        CamColors.Sub = Color(0xFF9E9E9E)
        CamColors.Shutter = parseAppColor(shutterHex, Color.White)
    }
    val appFontFamily = FujiTypography.composeFamily(context)
    val baseTypography = Typography()
    val appTypography = baseTypography.copy(
        displayLarge = baseTypography.displayLarge.copy(fontFamily = appFontFamily),
        displayMedium = baseTypography.displayMedium.copy(fontFamily = appFontFamily),
        displaySmall = baseTypography.displaySmall.copy(fontFamily = appFontFamily),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = appFontFamily),
        headlineMedium = baseTypography.headlineMedium.copy(fontFamily = appFontFamily),
        headlineSmall = baseTypography.headlineSmall.copy(fontFamily = appFontFamily),
        titleLarge = baseTypography.titleLarge.copy(fontFamily = appFontFamily),
        titleMedium = baseTypography.titleMedium.copy(fontFamily = appFontFamily),
        titleSmall = baseTypography.titleSmall.copy(fontFamily = appFontFamily),
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = appFontFamily),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = appFontFamily),
        bodySmall = baseTypography.bodySmall.copy(fontFamily = appFontFamily),
        labelLarge = baseTypography.labelLarge.copy(fontFamily = appFontFamily),
        labelMedium = baseTypography.labelMedium.copy(fontFamily = appFontFamily),
        labelSmall = baseTypography.labelSmall.copy(fontFamily = appFontFamily)
    )
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = FujiSkin.orange,
            onPrimary = Color.Black,
            background = CamColors.Bg,
            surface = CamColors.Card
        ),
        typography = appTypography
    ) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides context.resources.configuration,
            LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
            LocalTextStyle provides appTypography.bodyMedium
        ) {
            Box(Modifier.fillMaxSize().background(CamColors.Bg).animaTContent()) {
                content()
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FujiChip(text = text, selected = selected, onClick = onClick, modifier = modifier)
}


private val englishUi = mapOf(
    "SETTINGS · MENU" to "SETTINGS · MENU", "TOOLBOX" to "TOOLBOX", "CONTENTS MANAGEMENT" to "CONTENTS MANAGEMENT",
    "PHOTO FRAMES" to "PHOTO FRAMES", "FILM FILTERS" to "FILM FILTERS", "LUNADREAM" to "LUNADREAM",
    "OTHER · DEVICE OPTIONS" to "OTHER · DEVICE OPTIONS", "MENU TABS" to "MENU TABS", "OPEN" to "OPEN",
    "STARTING..." to "STARTING...", "NO SIGNAL" to "NO SIGNAL", "Refresh rate" to "Refresh rate", "EV N/A" to "EV unavailable",
    "DISPLAY / APPEARANCE" to "DISPLAY / APPEARANCE", "SOUND & HAPTIC" to "SOUND & HAPTIC", "NIGHTSHOTMODE" to "NIGHTSHOTMODE",
    "▲▼ SELECT    MENU/OK OPEN    DISP/BACK BACK" to "▲▼ SELECT    MENU/OK OPEN    DISP/BACK BACK",
    "Settings" to "Settings", "Toolbox" to "Toolbox", "Viewfinder overlays" to "Viewfinder overlays",
    "Grid Lines" to "Grid Lines", "Thirds composition guide" to "Thirds composition guide",
    "Histogram" to "Histogram", "Live luminance histogram" to "Live luminance histogram",
    "Focus Peaking" to "Focus Peaking", "Highlight high-contrast focus edges" to "Highlight high-contrast focus edges",
    "Watermark" to "Watermark", "Enable Watermark" to "Enable Watermark", "Shutter button" to "Shutter button",
    "Assistive Tools" to "Assistive Tools", "Focus & Lens" to "Focus & Lens",
    "Capture & Quality" to "Capture & Quality", "Photo Mode Setting" to "Photo Mode Setting",
    "Appearance" to "Appearance", "Language" to "Language", "About" to "About",
    "Photo" to "Photo", "Pro" to "Pro", "Bộ lọc" to "Filter", "AI Bố cục" to "AI Composition",
    "AI Bố cục ✓" to "AI Composition ✓", "Tắt frame" to "No frame",
    "Màu frame" to "Frame color", "Màu khung" to "Frame color",
    "Hiện thời gian" to "Show date and time", "Hiện model máy" to "Show device model",
    "Hiện thông số" to "Show camera settings", "Save Model" to "Save Model",
    "Import ảnh cho nút chụp" to "Import shutter image", "Đổi ảnh nút chụp" to "Change shutter image",
    "Đã chọn ảnh cho nút chụp." to "Shutter image selected.", "Dùng nút màu" to "Use solid color",
    "Đang phơi sáng" to "Exposing", "Vui lòng để điện thoại cố định" to "Keep the phone steady",
    "Không rung máy cho đến khi hoàn tất" to "Do not move the phone until capture finishes",
    "Đang phơi sáng vui lòng để điện thoại cố định" to "Exposure in progress. Please keep the phone steady.",
    "AI vẫn đang phát triển trong quá trình dùng chức năng có thể phát sinh lỗi hoặc không đúng như người dùng mong muốn"
        to "AI is still under development. Results may be inaccurate or unexpected, and errors may occur.",
    "Tắt frame" to "Off", "Classic Card" to "Classic Card", "Clean Corners" to "Clean Corners",
    "35mm Film" to "35mm Film", "Instant / Polaroid" to "Instant / Polaroid",
    "Center Logo" to "Center Logo", "Expo Around" to "Expo Around",
    "Expo Left" to "Expo Left", "Expo Right" to "Expo Right", "Double Line" to "Double Line",
    "Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm." to "These tools control the live viewfinder overlays.",
    "Thông tin camera do Camera2 đọc trực tiếp từ thiết bị." to "Camera information is read directly from the device through Camera2.",
    "Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng." to "Could not read Camera IDs. Grant camera permission to the app.",
    "Aspect Ratio" to "Aspect Ratio", "Image Quality" to "Image Quality",
    "Ảnh được lưu vào Pictures/FujifilmMode." to "Photos are saved to Pictures/FujifilmMode.",
    "Capture Quality" to "Capture Quality", "Noise Reduction" to "Noise Reduction",
    "Shutter" to "Shutter", "Hiệu ứng khi bấm chụp" to "Shutter effect",
    "Mẫu watermark · chọn riêng từng kiểu" to "Watermark templates · select a style",
    "Màu khung" to "Frame color", "Model tùy chỉnh" to "Custom model",
    "Auto sẽ lấy tên model của điện thoại: " to "Auto uses the phone model: ",
    "Tên model tùy chỉnh được lưu trong dữ liệu ứng dụng và giữ nguyên cho đến khi xóa dữ liệu hoặc gỡ ứng dụng."
        to "The custom model name is stored in app data until app data is cleared or the app is uninstalled.",
    "Watermark được đóng trực tiếp lên ảnh sau khi chụp. Thông số mm, khẩu độ, shutter và ISO lấy từ camera đang hoạt động khi HAL có cung cấp."
        to "The watermark is embedded after capture. Focal length, aperture, shutter and ISO are read from the active camera when exposed by the HAL.",
    "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút." to "Change the shutter button color or use a PNG/JPG image.",
    "Đã chọn ảnh cho nút chụp." to "Shutter image selected.",
    "Chọn ngôn ngữ hiển thị. Ứng dụng sẽ tải lại giao diện sau khi đổi." to "Choose the interface language. The app will reload after the change.",
    "Tắt sẽ yêu cầu camera không khử nhiễu; mức hỗ trợ thực tế tùy Camera2 HAL." to "Off requests no camera noise reduction; actual support depends on the Camera2 HAL.",
    "Best sử dụng mức nén JPEG cao nhất; độ phân giải thực tế phụ thuộc độ phân giải mà Camera2/CameraX cung cấp cho camera đang chọn."
        to "Best uses the highest JPEG quality. Actual resolution depends on the output sizes exposed by the active Camera2/CameraX camera.",
    "Người làm App: Baoaka06" to "By Baoaka06",
    "Frame Manage" to "Frame Manager", "Nhập khung phủ PNG/JPG để quản lý." to "Import PNG/JPG overlay frames.",
    "Chưa có frame nào được nhập." to "No frames imported yet.",
    "Đang tải ảnh…" to "Loading photo…", "Đang tải ảnh..." to "Loading photo...",
    "Đóng" to "Close", "Chọn frame để xem trước và đóng lên ảnh sau khi chụp." to "Select a frame preview; it will be embedded after capture.",
    "Frame dựng sẵn" to "Built-in frame", "Imported frame" to "Imported frame",
    "PNG trong suốt khuyên dùng · JPG có thể che ảnh" to "Transparent PNG recommended · JPG may cover the photo",
    "Transparent: bỏ viền và panel nền; các chi tiết chữ/ảnh PNG vẫn được giữ." to "Transparent removes the frame border and backing panel; text and PNG artwork remain.",
    "Áp dụng frame" to "Apply frame",
    "Lens calibration · EIS+ · sensor diagnostics" to "Lens calibration · EIS+ · sensor diagnostics",
    "Fujifilm Mode (app-debug)" to "FujiFilm Mode",
    "Version 1.1 experimental · Debug build" to "Version 1.1 experimental · Debug build",
    "Chưa có ảnh trong Pictures/FujifilmMode." to "No photos in Pictures/FujifilmMode yet.",
    "Không rung máy cho đến khi hoàn tất" to "Do not move the phone until capture finishes."
)

private val fullEnglishUi = mapOf(
    "Cần cấp quyền Camera\n(chạm để cấp)" to "Camera permission required\n(tap to grant)",
    "AF LOCK" to "AF LOCK", "EV không được hỗ trợ" to "EV unavailable",
    "AF tự động" to "AF Auto", "Ấm" to "Warm", "Lạnh" to "Cool",
    "ISO Auto" to "ISO Auto", "Shutter Auto" to "Shutter Auto",
    "Focus locked" to "Focus locked", "Tông màu tự động" to "Auto color temperature",
    "LENS TRANSITION" to "LENS TRANSITION",
    "Không dùng bộ lọc · Ảnh gốc" to "No filter · Original photo",
    "Mở Contents Management" to "Open Contents Management",
    "Nhấn để chọn áp dụng sau khi chụp" to "Tap to select and apply after capture",
    "✓ ĐANG DÙNG" to "✓ ACTIVE", "Xem trước · áp dụng ảnh" to "Preview · applied to photo",
    "Settings" to "Settings", "Toolbox" to "Toolbox", "Contents" to "Contents",
    "Metering" to "Metering", "Render Engine" to "Render Engine", "Ultra HDR" to "Ultra HDR",
    "Settings" to "Settings",
    "Photo" to "Photo", "Pro" to "Pro", "Histogram" to "Histogram",
    "Viewfinder overlays" to "Viewfinder overlays", "Thirds composition guide" to "Thirds composition guide",
    "Live luminance histogram" to "Live luminance histogram", "Highlight high-contrast focus edges" to "Highlight high-contrast focus edges",
    "Khung ảnh phong cách rangefinder, không có logo thương hiệu" to "Rangefinder-style photo frame without a brand logo",
    "Mẫu watermark · chọn riêng từng kiểu" to "Watermark templates · choose a style",
    "EXIF cổ điển" to "Classic EXIF", "Viền bo + khoảng trống" to "Rounded border + margin",
    "Nền mờ, thông số nổi" to "Soft background with prominent settings",
    "Thông số góc phải" to "Settings in the right corner", "Ngày chụp tối giản" to "Minimal capture date",
    "Logo ở chính giữa" to "Centered logo",
    "Hiện thời gian" to "Show date and time", "Bật/tắt ngày và giờ trên watermark" to "Show or hide date and time on the watermark",
    "Hiện model máy" to "Show device model", "Bật/tắt tên model điện thoại" to "Show or hide the phone model",
    "Hiện thông số" to "Show camera settings", "Bật/tắt tiêu cự, khẩu độ, tốc độ màn trập và ISO" to "Show or hide focal length, aperture, shutter speed and ISO",
    "Model tùy chỉnh" to "Custom model", "Custom Model (để trống = Auto)" to "Custom model (blank = Auto)",
    "Auto sẽ lấy tên model của điện thoại: " to "Auto uses the phone model: ",
    "Save Model" to "Save Model", "Tên model tùy chỉnh được lưu trong dữ liệu ứng dụng và giữ nguyên cho đến khi xóa dữ liệu hoặc gỡ ứng dụng." to "The custom model name remains in app data until app data is cleared or the app is uninstalled.",
    "Watermark được đóng trực tiếp lên ảnh sau khi chụp. Thông số mm, khẩu độ, shutter và ISO lấy từ camera đang hoạt động khi HAL có cung cấp." to "The watermark is embedded after capture. Focal length, aperture, shutter and ISO are read from the active camera when the HAL provides them.",
    "LENS TRANSITION" to "LENS TRANSITION",
    "Âm thanh màn trập" to "Shutter sound", "Dùng âm click chụp ảnh hệ thống của Android" to "Use Android's system camera shutter sound",
    "Photo Frame" to "Photo Frame", "Chọn frame tại đây. Frame được đóng lên ảnh sau khi chụp; không còn nút Frame ở màn hình chụp." to "Choose frames here. The selected frame is embedded after capture; there is no Frame button on the camera screen.",
    "Màu frame" to "Frame color", "Transparent bỏ viền và panel của frame dựng sẵn; với frame PNG sẽ giữ nguyên độ trong suốt gốc." to "Transparent removes built-in frame borders and panels; imported PNG transparency is preserved.",
    "Muốn thêm frame PNG/JPG, vào Contents Management → Frame Manage → Import Frame." to "To add a PNG/JPG frame, open Contents Management → Frame Manager → Import Frame.",
    "PNG / JPG" to "PNG / JPG", "ORIGINAL" to "ORIGINAL", "Imported frame" to "Imported frame",
    "Contents Management" to "Contents Management", "Frame Manage" to "Frame Manager",
    "Nhập khung phủ PNG/JPG để quản lý." to "Import PNG/JPG overlay frames.",
    "Import Frame" to "Import Frame", "Xóa" to "Delete", "Hủy" to "Cancel",
    "Album · Fujifilm Mode" to "Album · Fujifilm Mode",
    "Đang tải ảnh…" to "Loading photos…", "Đang tải ảnh..." to "Loading photos...",
    "Đóng" to "Close", "Photo Frames" to "Photo Frames",
    "Chọn frame để xem trước và đóng lên ảnh sau khi chụp." to "Select a frame preview; it will be embedded after capture.",
    "Frame color" to "Frame color", "Frame dựng sẵn" to "Built-in frame",
    "Transparent: bỏ viền và panel nền; các chi tiết chữ/ảnh PNG vẫn được giữ." to "Transparent removes the frame border and backing panel; text and PNG artwork remain.",
    "Áp dụng frame" to "Apply frame",
    "Language" to "Language", "Phát hiện ngôn ngữ hệ thống" to "Detect system language",
    "Capture Quality" to "Capture Quality", "HDR · Multi-frame" to "HDR · Multi-frame",
    "Ưu tiên HDR nhiều mức phơi sáng từ CameraX/OEM. Nếu máy không có HDR Extension, ứng dụng thử Ultra HDR JPEG_R khi camera hỗ trợ. Quá trình hợp nhất dùng pipeline xử lý của thiết bị; Android không cho ứng dụng ép chọn riêng GPU hay CPU."
        to "Prioritizes multi-exposure HDR from CameraX/OEM. If the device has no HDR Extension, the app tries Ultra HDR JPEG_R when supported. Merging uses the device imaging pipeline; Android does not let apps force a GPU-only or CPU-only path.",
    "UHD · Độ phân giải tối đa" to "UHD · Maximum resolution",
    "Yêu cầu CameraX chọn kích thước JPEG lớn nhất mà camera đang dùng hỗ trợ; không nội suy vượt cảm biến." to "Requests the largest JPEG size supported by the active camera; does not upscale beyond the sensor.",
    "Noise Reduction" to "Noise Reduction",
    "Tắt sẽ yêu cầu camera không khử nhiễu; mức hỗ trợ thực tế tùy Camera2 HAL." to "Off requests no camera noise reduction; actual support depends on the Camera2 HAL.",
    "Best sử dụng mức nén JPEG cao nhất; độ phân giải thực tế phụ thuộc độ phân giải mà Camera2/CameraX cung cấp cho camera đang chọn." to "Best uses the highest JPEG quality. Actual resolution depends on the output sizes exposed by the active Camera2/CameraX camera.",
    "Ảnh được lưu vào Pictures/FujifilmMode." to "Photos are saved to Pictures/FujifilmMode.",
    "Import ảnh cho nút chụp" to "Import shutter image", "Đổi ảnh nút chụp" to "Change shutter image",
    "Đã chọn ảnh cho nút chụp." to "Shutter image selected.", "Dùng nút màu" to "Use solid color",
    "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút." to "Change the shutter button color or use a PNG/JPG image.",
    "Chọn ngôn ngữ hiển thị. Ứng dụng sẽ tải lại giao diện sau khi đổi." to "Choose the display language. The app reloads after you change it.",
    "Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm." to "These tools directly control the viewfinder overlays.",
    "Thông tin camera do Camera2 đọc trực tiếp từ thiết bị." to "Camera information is read directly from the device through Camera2.",
    "Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng." to "Could not read Camera IDs. Grant camera permission to the app.",
    "Thông tin tên mã cảm biến không được Android Camera2 công bố thống nhất trên mọi thiết bị; ứng dụng hiển thị kích thước cảm biến và độ phân giải tối đa mà hệ thống cung cấp." to "Android Camera2 does not expose sensor codenames consistently across devices; the app shows sensor size and the maximum resolution reported by the system.",
    "Lens calibration · EIS+ · sensor diagnostics" to "Lens calibration · EIS+ · sensor diagnostics",
    "Fujifilm Mode (app-debug)" to "Fujifilm Mode (app-debug)", "Người làm App: Baoaka06" to "By Baoaka06",
    "Version 1.1 experimental · Debug build" to "Version 1.1 experimental · Debug build",
    "Camera này không hỗ trợ phơi sáng dài." to "This camera does not support long exposure.",
    "Camera không hỗ trợ phơi sáng thủ công để chụp đêm." to "The camera does not support manual exposure for night capture.",
    "Camera không hỗ trợ Ultra HDR; đã chuyển về JPEG." to "This camera does not support Ultra HDR; switched to JPEG.",
    "Camera không hỗ trợ phơi sáng thủ công để chụp đêm." to "The camera does not support manual exposure for night capture.",
    "Chuyển AF sang Auto trước khi khóa nét." to "Switch AF to Auto before locking focus.",
    "Đã khóa nét · chạm lại để lấy nét mới." to "Focus locked · tap again to refocus.",
    "Chưa có ảnh trong Pictures/FujifilmMode." to "No photos in Pictures/FujifilmMode yet.",
    "Đang đồng bộ hướng và tỉ lệ khung ngắm, hãy chụp lại sau một chút." to "Synchronizing viewfinder orientation and aspect ratio. Please capture again in a moment.",
    "Ảnh đã chụp nhưng MediaStore không trả về URI." to "The photo was captured, but MediaStore did not return a URI.",
    "Camera/API này không hỗ trợ chụp JPEG Ultra HDR." to "This camera/API does not support Ultra HDR JPEG capture.",
    "Ảnh đã lưu nhưng watermark chưa được đóng. Kiểm tra lại mục Watermark." to "Photo saved, but the watermark was not applied. Check Watermark settings.",
    "Không thể bật Ultra HDR hoặc khởi tạo camera:" to "Could not enable Ultra HDR or initialize the camera:",
    "Không mở được camera:" to "Could not open camera:",
    "Lỗi chụp ảnh:" to "Capture error:",
    "Chủ thể ở 1/3 trái · chừa khoảng trống bên phải" to "Subject on left third · leave space on the right",
    "Chủ thể ở 1/3 phải · chừa khoảng trống bên trái" to "Subject on right third · leave space on the left",
    "Chủ thể ở giữa · thử lệch nhẹ theo quy tắc 1/3" to "Subject centered · try shifting slightly using the rule of thirds",
    "Chưa thấy khuôn mặt · thử đặt chủ thể vào 1/3" to "No face detected · try placing the subject on a third",
    "Chủ thể" to "Subject", "ISO Auto" to "ISO Auto", "Shutter Auto" to "Shutter Auto",
    "EV không được hỗ trợ" to "EV unavailable", "AF tự động" to "AF Auto",
    "Tông màu tự động" to "Auto white balance", "Ảnh gốc" to "Original photo",
    "Cần cấp quyền Camera\n(chạm để cấp)" to "Camera permission required\n(tap to grant)",
    "Image Quality" to "Image Quality",
    "Shutter button" to "Shutter button",
    "Watermark" to "Watermark",
    "Màu khung" to "Frame color",
    "Chọn bộ lọc để xem tông màu; bộ lọc được xử lý trên ảnh sau khi chụp." to "Choose a filter to preview its look; it is applied to the photo after capture.",
    "TAP AF · HOLD LOCK" to "TAP AF · HOLD TO LOCK",
    "ĐANG DÙNG" to "ACTIVE",
    "CHỌN" to "SELECT",
    "Đang phơi sáng" to "Exposing",
    "Vui lòng để điện thoại cố định" to "Please keep the phone steady",
    "Giữ thiết bị ổn định" to "Keep the device steady",
    "Bạn có thể di chuyển thiết bị tự do" to "You can move the device freely",
    "BRACKETING \$bracketCompletedFrames" to "BRACKETING \$bracketCompletedFrames",
    "BURST \$burstCompletedFrames/25" to "BURST \$burstCompletedFrames/25",
    "MILKY WAY INTELLIGENCE · CẢNH TỐI" to "MILKY WAY INTELLIGENCE · DARK SCENE",
    "Gợi ý từ độ sáng preview và thông số ống kính; không đo được mây / trăng / ô nhiễm sáng." to "Estimated from preview brightness and lens metadata; it does not measure clouds, moonlight or light pollution.",
    "Giữ máy cố định · nhấn nút chụp lần nữa để dừng sau khung hiện tại" to "Keep the device steady · press the shutter again to stop after the current frame",
    "Khuyên dùng tripod; camera hỗ trợ manual sensor mới dùng được 1–8 giây thật" to "A tripod is recommended; true 1–8 second exposures require Manual Sensor support.",
    "\$nightShotCapturedFrameCount/\$nightShotTargetFrameCount khung hình · 30 FPS mục tiêu" to "\$nightShotCapturedFrameCount/\$nightShotTargetFrameCount frames · 30 FPS target",
    "Danh sách bộ lọc · " to "Filter list · ",
    "Chưa có LUT. Có thể nhập Xiaomi .zip hoặc Blackmagic .cube tại đây." to "No LUTs yet. Import a Xiaomi .zip or Blackmagic .cube file here.",
    "LUT đã nhập. Mở Content Manager → LUT Manage rồi nhấn Add để đưa LUT vào bộ lọc." to "LUT imported. Open Content Manager → LUT Manager and tap Add to include it in the filters.",
    "✓ ĐANG CHỌN" to "✓ SELECTED",
    "Dùng tripod, tắt flash và hướng máy vào bầu trời. Nhấn nút chụp lần nữa để dừng sau khung hiện tại; ứng dụng sẽ ghép các vị trí sao sáng thành vệt dài. Chỉ tự dừng sớm khi bộ nhớ tạm gần đầy." to "Use a tripod, turn off flash and point the camera at the sky. Press the shutter again to stop after the current frame; the app stacks bright star positions into trails. It stops early only when temporary storage is nearly full.",
    "Vendor keys: " to "Vendor keys: ",
    "Không có vendor key ngoài namespace Android được công bố trong metadata đọc được." to "No vendor keys outside the Android namespace were exposed in the readable metadata.",
    "Scanner đã kiểm tra Logical Camera, các Physical ID mà OEM đưa vào metadata và vendor metadata có thể đọc được. Camera ID bị camera service/firmware ẩn hoàn toàn không thể được liệt kê bằng API Android công khai; muốn truy cập cần OEM/system API hoặc quyền hệ thống. App không tạo ID giả và không dùng root/hidden-API reflection để vượt giới hạn." to "The scanner checked Logical Cameras, Physical IDs exposed by the OEM and readable vendor metadata. Camera IDs fully hidden by camera service or firmware cannot be listed through public Android APIs; access requires OEM/system APIs or system privileges. The app does not invent IDs or bypass restrictions with root or hidden-API reflection.",
    "Lưu" to "Save",
    "Nạp" to "Load",
    "Điều chỉnh này được áp dụng giống nhau trong preview bộ lọc và ảnh JPEG sau chụp." to "These adjustments are applied consistently to filter previews and captured JPEGs.",
    "Preview mô phỏng mẫu watermark đang chọn. Các thông số được áp dụng lên lớp kính khi lưu JPEG; DNG RAW luôn giữ nguyên dữ liệu cảm biến." to "This preview simulates the selected watermark. Parameters are applied to the glass layer when saving JPEG; RAW DNG always keeps the original sensor data.",
    "Custom Model (để trống = Auto)" to "Custom model (blank = Auto)",
    "Transparent bỏ viền và panel của frame dựng sẵn; với frame PNG sẽ giữ nguyên độ trong suốt gốc." to "Transparent removes the built-in frame border and backing panel; imported PNG transparency is preserved.",
    "Nhập Xiaomi Mi-LOG bằng tệp .zip (ứng dụng tự tìm .cube bên trong, kể cả ZIP lồng) hoặc LUT Blackmagic .cube." to "Import Xiaomi Mi-LOG using a .zip file (the app searches for .cube files, including nested ZIPs) or a Blackmagic .cube LUT.",
    "Xóa LUT?" to "Delete LUT?",
    "Đang tải thư viện ảnh…" to "Loading photo library…",
    "Thông tin" to "Information",
    "Đang đọc metadata ảnh…" to "Reading photo metadata…",
    "Ví dụ: Winnipeg · để trống = ẩn" to "Example: Winnipeg · leave blank to hide",
    "Mặc định" to "Default",
    "Your location" to "Your location",
    "Chế độ chụp đêm" to "Night capture",
    "Thời lượng thu khung" to "Capture duration",
    "Thời gian mỗi khung" to "Exposure time per frame",
    "Milky Way" to "Milky Way",
    "RAW Capture" to "RAW Capture",
    "Quét Camera2 công khai, Physical Camera và vendor metadata được HAL công bố." to "Scan public Camera2, physical camera and vendor metadata exposed by the HAL.",
    "Đang đọc CameraCharacteristics…" to "Reading CameraCharacteristics…",
    "Không có Camera ID nào được hệ thống công khai cho ứng dụng. Hãy kiểm tra quyền Camera và thử quét lại." to "The system exposed no Camera IDs to this app. Check Camera permission and scan again.",
    "TikTok: @baoaka06" to "TikTok: @baoaka06",
    "Pro HUD elements" to "Pro HUD elements",
    "Camera Presets" to "Camera Presets",
    "Film Simulation Customizer" to "Film Simulation Customizer",
    "Reset Recipe" to "Reset Recipe",
    "Saved Film Recipes" to "Saved Film Recipes",
    "Liquid Glass · Live preview" to "Liquid Glass · Live preview",
    "Hình khối & tán sắc" to "Shape & refraction",
    "Bề mặt & độ mờ" to "Surface & blur",
    "Ánh sáng & viền" to "Light & rim",
    "Màu nhuộm kính" to "Glass tint",
    "Model tùy chỉnh" to "Custom model",
    "Địa điểm watermark (dùng cho mẫu Fuji Signature)" to "Watermark location (for Fuji Signature)",
    "Save Location" to "Save Location",
    "Photo Frame" to "Photo Frame",
    "Frame Manage" to "Frame Manager",
    "LUT Manage" to "LUT Manager",
    "Photo Inspector" to "Photo Inspector",
    "Photo Frames" to "Photo Frames",
    "Không thể tạo preview cho bộ lọc này" to "Could not create a preview for this filter",
    "Đang tạo preview…" to "Creating preview…",
    "Bộ lọc phim" to "Film filters",
    "Chạm để preview" to "Tap to preview",
    "LunaDream đang tắt" to "LunaDream is off",
    "Chọn chế độ trước khi chụp. LunaDream giữ nguyên lựa chọn trong suốt quá trình xử lý." to "Choose a mode before capture. LunaDream keeps that selection throughout processing.",
    "Chụp đêm và Star Trails có pipeline riêng." to "Night capture and Star Trails use separate processing pipelines.",
    "Milky Way được bật/tắt trong Cài đặt → NightShotMode." to "Milky Way can be enabled or disabled in Settings → NightShotMode.",
    "LunaDream 1.5 · đang chụp khung đầu tiên" to "LunaDream 1.5 · capturing first frame",
    "LunaDream 2.0 · đang chụp khung đầu tiên" to "LunaDream 2.0 · capturing first frame",
    "RawDream 2.0 · đang chụp khung đầu tiên" to "RawDream 2.0 · capturing first frame",
    "NightShotMode · đang chụp khung đầu tiên" to "NightShotMode · capturing first frame",
    "NightShotMode thu nhiều khung trong thời lượng đã chọn, căn chỉnh và ghép để giảm nhiễu, tăng sáng có kiểm soát. Số khung thực tế phụ thuộc tốc độ camera; giữ máy ổn định đến khi ghép xong." to "NightShotMode captures multiple frames for the selected duration, aligns and merges them to reduce noise and lift shadows carefully. The actual frame count depends on camera speed; keep the device steady until merging finishes.",
    "Mỗi khung dùng màn trập 1, 2, 4 hoặc 8 giây, ISO 800 và lấy nét vô cực khi camera hỗ trợ MANUAL_SENSOR. Máy không hỗ trợ sẽ báo rõ và dùng AE tự động; không giả lập thời gian phơi sáng." to "Each frame uses a 1, 2, 4 or 8 second shutter, ISO 800 and infinity focus when MANUAL_SENSOR is supported. Unsupported devices report this and use auto exposure; exposure time is never simulated.",
    "RAW lưu DNG gốc theo dữ liệu cảm biến. RawDream 2.0 chụp đồng thời DNG và JPEG; JPEG được căn chỉnh, ghép đa khung, khử nhiễu và phục hồi chi tiết bằng LunaDream 2.0, đầu ra mục tiêu 108MP. RawDream 2.0 không kết hợp NightShotMode, Star Trails, Burst hoặc Bracketing. Nếu camera không hỗ trợ đồng thời RAW + JPEG, ứng dụng báo lỗi và không tự chuyển sang JPEG thường." to "RAW saves the original sensor DNG. RawDream 2.0 captures DNG and JPEG together; the JPEG is aligned, multi-frame merged, denoised and detail-recovered with LunaDream 2.0, targeting 108MP. RawDream 2.0 cannot be combined with NightShotMode, Star Trails, Burst or Bracketing. If the camera cannot capture RAW + JPEG together, the app reports an error and does not silently switch to ordinary JPEG.",
    "Off tắt khử nhiễu. Low / Medium / High chỉ dùng thuật toán khử nhiễu có sẵn của camera (Camera2); không có xử lý AI sau chụp. Ultra HDR giữ nguyên dữ liệu JPEG_R." to "Off disables noise reduction. Low / Medium / High use only the camera's built-in Camera2 noise-reduction algorithm; no post-capture AI processing is applied. Ultra HDR preserves JPEG_R data.",
    "Bật để dùng nhánh căn chỉnh và ghép khung bầu trời trong NightShotMode. Tắt mặc định; không hoạt động ở Photo thường." to "Enable sky-frame alignment and stacking in NightShotMode. Off by default; unavailable in standard Photo mode.",
    "Camera liên tục chụp trong khoảng thời gian đã chọn; số khung thực tế tùy tốc độ cảm biến và giới hạn CameraX." to "The camera captures continuously for the selected duration. The actual frame count depends on sensor speed and CameraX limits.",
    "Auto sẽ lấy tên model của điện thoại: " to "Auto uses the phone model: ",
    "ĐANG GHI STAR TRAILS" to "RECORDING STAR TRAILS",
    "Giữ máy ổn định đến khi thu và ghép xong. Khi LunaDream bắt đầu xử lý, có thể di chuyển tự do." to "Keep the device steady until capture and merging finish. You can move freely once LunaDream Render starts.",
    "Astro Stack ×8 cần Manual Sensor, ISO và thời gian phơi sáng do Camera2 công bố." to "Astro Stack ×8 requires Manual Sensor, ISO and exposure time ranges reported by Camera2.",
    "Mẫu xem trước được xử lý từ cùng một ảnh nguồn, không cộng dồn bộ lọc khi đổi lựa chọn." to "Each preview is generated from the same source image; changing filters does not stack effects.",
    "Lưu và nạp nhanh tỉ lệ, zoom, EV, bộ lọc, HDR, NightShotMode, RAW và cấu hình LunaDream." to "Quickly save and load aspect ratio, zoom, EV, filters, HDR, NightShotMode, RAW and LunaDream settings.",
    "LunaDream Render · bắt đầu xử lý ảnh JPEG đã ghép" to "LunaDream Render · processing the merged JPEG",
    "LunaDream Render · bắt đầu phục hồi chi tiết" to "LunaDream Render · detail recovery started",
    "LunaDream Render · 1.5 đang phục hồi chi tiết" to "LunaDream Render · 1.5 detail recovery",
    "LunaDream Render · RawDream 2.0 đang xử lý JPEG ghép" to "LunaDream Render · RawDream 2.0 merged JPEG processing",
    "LunaDream Render · 2.0 đang phục hồi chi tiết" to "LunaDream Render · 2.0 detail recovery",
    "NightShot Render · bắt đầu hoàn tất ảnh đã ghép" to "NightShot Render · finishing the merged image",
    "NightShot Render · đang hoàn tất ảnh đã ghép" to "NightShot Render · finishing the merged image",
    "NightShot Render · đang hoàn tất ảnh đã chụp" to "NightShot Render · finishing the captured image",
    "Không rung máy cho đến khi hoàn tất" to "Do not move the phone until capture finishes",
    "AI vẫn đang phát triển trong quá trình dùng chức năng có thể phát sinh lỗi hoặc không đúng như người dùng mong muốn"
        to "AI is still under development. Errors or unexpected results may occur while using this feature.",
    "HDR · Multi-frame" to "HDR · Multi-frame",
    "Timer" to "Timer",
    "Quick settings" to "Quick settings",
    "Đổi camera" to "Switch camera",
    "Đóng ảnh" to "Close photo",
    "Camera không hỗ trợ Ultra HDR hoặc HDR Extension; đã chuyển về JPEG SDR." to "This camera supports neither Ultra HDR nor the HDR extension; switched to standard JPEG.",
    "Đã lưu Ultra HDR nguyên bản và giữ gain map. Watermark, bộ lọc màu, frame và sharpen UHD hậu kỳ được bỏ qua để bảo toàn HDR." to "Saved the original Ultra HDR and preserved its gain map. Watermarks, color filters, frames and post-capture UHD sharpening are skipped to preserve HDR.",
    "HDR đa khung đang hoạt động; máy không hỗ trợ JPEG_R nên ảnh được lưu dạng JPEG." to "Multi-frame HDR is active. This camera does not support JPEG_R, so the photo will be saved as JPEG.",
    "No photos yet" to "No photos yet",
    "Sao lưu & Khôi phục" to "Backup & Restore",
    "Camera · Zoom · Các tính năng đã bật" to "Camera · Zoom · Enabled features",
    "Dữ liệu" to "Data",
    "Sao lưu cấu hình camera, zoom và các tính năng đã bật." to "Back up camera, zoom and enabled app features.",
    "Xuất bản sao lưu" to "Export backup",
    "Khôi phục bản sao lưu" to "Restore backup",
    "Đã xuất bản sao lưu cài đặt." to "Settings backup exported.",
    "Đã khôi phục cài đặt camera, zoom và các tính năng đã bật." to "Camera, zoom and enabled feature settings restored.",
    "Không thể tạo bản sao lưu. Hãy kiểm tra vị trí lưu và thử lại." to "Could not create the backup. Check the save location and try again.",
    "Không thể khôi phục. Hãy chọn tệp sao lưu Fujifilm Mode hợp lệ." to "Could not restore. Select a valid Fujifilm Mode backup file.",
    "Lưu ý: tệp này lưu cấu hình camera, frame và ảnh nút chụp; không nhúng tệp frame/ảnh gốc. Sau khi cài lại ứng dụng hoặc chuyển máy, cần nhập lại frame/ảnh tùy chỉnh và cấp quyền đọc chúng." to "Note: this file stores camera settings and references to frames and shutter-button images; source frame/image files are not embedded. Re-import custom frames/images and grant read access after reinstalling or moving devices.",
    "Ưu tiên HDR Extension nhiều khung hình của camera hoặc Ultra HDR JPEG_R. Nếu máy không hỗ trợ cả hai, ứng dụng dùng Software HDR bằng GPU hoặc CPU để cân bằng sáng một ảnh; không thể khôi phục chi tiết vùng đã cháy sáng." to "Prefer the camera's multi-frame HDR extension or Ultra HDR JPEG_R. If neither is supported, Software HDR uses GPU or CPU single-image tone mapping; clipped highlights cannot be recovered.",
    "Chọn độ phân giải JPEG cao nhất mà camera hỗ trợ, sau đó tăng độ nét 2× bằng GPU khi ảnh vừa bộ nhớ; ảnh lớn hoặc GPU không phù hợp sẽ chuyển sang CPU. Không tạo thêm chi tiết vượt cảm biến." to "Capture at the highest JPEG resolution supported by the camera, then apply 2× sharpening on the GPU when memory allows; large images or unsupported GPUs fall back to CPU. No detail beyond the sensor is created.",
    "LunaDream không thể hoàn tất; ảnh gốc đã được giữ. Lý do:" to "LunaDream could not finish; the original photo was preserved. Reason:",
    "Ảnh đã chụp nhưng hậu kỳ gặp lỗi; tệp ảnh hợp lệ gần nhất đã được giữ. Lý do:" to "The photo was captured but post-processing failed; the last valid image file was preserved. Reason:",
    "LunaDream 2.0 không hoàn tất; đã dùng thuật toán dự phòng 1.5. Lý do:" to "LunaDream 2.0 did not finish; the LunaDream 1.5 fallback was used. Reason:"
)

private val vietnameseUi = buildMap {
    englishUi.forEach { (source, translated) ->
        if (source != translated) put(translated, source)
    }
    fullEnglishUi.forEach { (source, translated) ->
        if (source != translated) put(translated, source)
    }
    putAll(
        mapOf(
            "Settings" to "Cài đặt",
            "Toolbox" to "Hộp công cụ",
            "Contents" to "Nội dung",
            "Metering" to "Đo sáng",
            "Render Engine" to "Bộ xử lý kết xuất",
            "Aspect Ratio" to "Tỉ lệ khung hình",
            "Live Photo" to "Ảnh động",
            "Performance" to "Hiệu năng",
            "Center" to "Trung tâm",
            "Frames" to "Khung hình",
            "Other" to "Khác",
            "Device options" to "Tùy chọn thiết bị",
            "Assistive Tools" to "Công cụ hỗ trợ",
            "Focus & Lens" to "Lấy nét & Ống kính",
            "Capture & Quality" to "Chụp & Chất lượng",
            "Photo Mode Setting" to "Cài đặt chế độ ảnh",
            "Appearance" to "Giao diện",
            "Language" to "Ngôn ngữ",
            "About" to "Giới thiệu",
            "Noise Reduction" to "Giảm nhiễu",
            "Noise reduction" to "Giảm nhiễu",
            "Save Location" to "Vị trí lưu",
            "Save location" to "Vị trí lưu",
            "Image Quality" to "Chất lượng ảnh",
            "Backup & Restore" to "Sao lưu & Khôi phục",
            "Viewfinder overlays" to "Lớp phủ kính ngắm",
            "Grid Lines" to "Đường lưới",
            "Focus Peaking" to "Viền hỗ trợ lấy nét",
            "Histogram" to "Biểu đồ histogram",
            "Sensor information" to "Thông tin cảm biến",
            "Camera ID" to "ID camera",
            "Lens" to "Ống kính",
            "Photo" to "Ảnh",
            "Night capture" to "Chụp đêm",
            "Long Exposure" to "Phơi sáng lâu",
            "Star Trails" to "Vệt sao",
            "Custom shutter button" to "Nút chụp tùy chỉnh",
            "Pro HUD elements" to "Thành phần HUD Pro",
            "EV/Shutter/ISO display" to "Hiển thị EV/Shutter/ISO",
            "Choose a mode before capture" to "Chọn chế độ trước khi chụp",
            "LunaDream is off" to "LunaDream đang tắt",
            "LunaDream is on" to "LunaDream đang bật",
            "Hold steady" to "Giữ máy ổn định",
            "Move freely" to "Có thể di chuyển máy tự do",
            "English" to "ENGLISH",
            "SYSTEM" to "SYSTEM",
            "TIẾNG VIỆT" to "TIẾNG VIỆT",
            "Filters · Tone Pad guide" to "Bộ lọc · Hướng dẫn Tone Pad",
            "Show Tone Pad guide" to "Hiện hướng dẫn Tone Pad",
            "Show the first-use guide and direction hint in the filter Tone Pad." to "Hiện hướng dẫn lần đầu và gợi ý thao tác trong Tone Pad."
        )
    )
}

private fun translateFullUiText(value: String, language: String): String? {
    if (language != "en") return null
    fullEnglishUi[value]?.let { return it }
    val dynamic = listOf(
        "Camera ID " to "Camera ID ",
        "ISO " to "ISO ",
        "Shutter " to "Shutter ",
        "Tông màu " to "Color temperature ",
        "Auto sẽ lấy tên model của điện thoại: " to "Auto uses phone model: ",
        "Không thể khởi tạo HDR hoặc camera: " to "Could not initialize HDR or camera: ",
        "Bộ lọc: " to "Filter: ",
        "AI BỐ CỤC · " to "AI COMPOSITION · ",
        "Lỗi chụp ảnh:" to "Capture error:",
        "Không mở được camera:" to "Could not open camera:",
        "Không thể bật Ultra HDR hoặc khởi tạo camera:" to "Could not enable Ultra HDR or initialize camera:",
        "BRACKETING " to "BRACKETING ",
        "BURST " to "BURST ",
        "Danh sách bộ lọc · " to "Filter list · ",
        "Nhiệt pin: " to "Battery temperature: ",
        "Trạng thái nhiệt Android: " to "Android thermal status: ",
        "LunaDream 1.5: thu khung " to "LunaDream 1.5: capturing frame ",
        "LunaDream 2.0: thu khung " to "LunaDream 2.0: capturing frame ",
        "RawDream 2.0: thu khung " to "RawDream 2.0: capturing frame ",
        "NightShotMode: thu khung " to "NightShotMode: capturing frame ",
        "NightShotMode Milky Way: thu khung " to "NightShotMode Milky Way: capturing frame ",
        "NightShotMode: căn chỉnh và gộp " to "NightShotMode: aligning and merging "
    )
    dynamic.firstOrNull { value.startsWith(it.first) }?.let {
        return it.second + value.removePrefix(it.first)
    }
    Regex("""^(\d+) ảnh$""").matchEntire(value)?.let { match ->
        return "${match.groupValues[1]} photos"
    }
    return null
}

private fun translateUiText(value: String, language: String): String {
    if (language == "vi") {
        vietnameseUi[value]?.let { return it }
        val dynamicVietnamese = listOf(
            "LunaDream 1.5: capturing frame " to "LunaDream 1.5: thu khung ",
            "LunaDream 2.0: capturing frame " to "LunaDream 2.0: thu khung ",
            "RawDream 2.0: capturing frame " to "RawDream 2.0: thu khung ",
            "NightShotMode: capturing frame " to "NightShotMode: thu khung ",
            "NightShotMode Milky Way: capturing frame " to "NightShotMode Milky Way: thu khung ",
            "NightShotMode: aligning and merging " to "NightShotMode: căn chỉnh và gộp ",
            "Battery temperature: " to "Nhiệt pin: ",
            "Android thermal status: " to "Trạng thái nhiệt Android: "
        )
        dynamicVietnamese.firstOrNull { value.startsWith(it.first) }?.let {
            return it.second + value.removePrefix(it.first)
        }
        Regex("""^(\d+) photos$""").matchEntire(value)?.let {
            return "${it.groupValues[1]} ảnh"
        }
        return value
    }
    translateFullUiText(value, "en")?.let { return it }
    englishUi[value]?.let { return it }
    var translated = value
    val fragments = listOf(
        "Camera ID " to "Camera ID ",
        "Bộ lọc" to "Filter",
        "Chưa có ảnh" to "No photos yet",
        "Đang tải" to "Loading",
        "Đã xử lý: " to "Processed: ",
        "Nhiệt pin: " to "Battery temperature: ",
        "Trạng thái nhiệt Android: " to "Android thermal status: ",
        "Nhẹ" to "Light",
        "Trung bình" to "Moderate",
        "Cao" to "Severe"
    )
    fragments.forEach { (from, to) -> translated = translated.replace(from, to) }
    return translated
}

fun localizedUiText(context: Context, value: String, languageOverride: String? = null): String {
    val language = languageOverride ?: AppLanguageManager.languageFor(context)
    return translateUiText(value, language)
}

fun localizedToast(context: Context, value: String, duration: Int): Toast {
    return Toast.makeText(context, localizedUiText(context, value), duration)
}

/**
 * Central localized Text wrapper for the camera UI. A selected app language is read
 * from preferences; AppCompat applies the same locale to the activity/resources.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val context = LocalContext.current
    val activeLanguage = AppLanguageManager.currentLanguage
    val localized = localizedUiText(context, text, activeLanguage)
    val appFont = fontFamily ?: FujiTypography.composeFamily(context)
    val annotated = FujiTypography.withMissingGlyphFallback(localized)
    MaterialText(
        text = annotated,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = appFont,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

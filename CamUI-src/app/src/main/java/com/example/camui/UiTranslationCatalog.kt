package com.example.camui

/**
 * Supplemental translations for camera-state, processing and settings copy that is
 * intentionally composed in Kotlin rather than stored as Android string resources.
 *
 * Vietnamese is the canonical source for these messages. English-to-Vietnamese
 * entries are included for text that is authored directly in English.
 */
internal object UiTranslationCatalog {
    private val vietnameseToEnglish = linkedMapOf(
        "Pin còn rất thấp; Star Trails đã dừng và đang ghép các khung đã chụp." to
            "Battery is critically low; Star Trails stopped and is merging the captured frames.",
        "Pin còn rất thấp; đã dừng thu khung và xử lý các khung hình hợp lệ đã chụp." to
            "Battery is critically low; frame capture stopped and the valid captured frames are being processed.",
        "Camera này không hỗ trợ RAW/RawDream 2.0 đồng thời. Không thể dùng chế độ RAW đã chọn." to
            "This camera cannot capture RAW and RawDream 2.0 simultaneously. The selected RAW mode is unavailable.",
        "Hình xem trước có dấu hiệu mờ. Có thể ống kính bị bẩn hoặc che khuất; hãy kiểm tra và lau kính camera." to
            "The preview appears blurred. The lens may be dirty or obstructed; inspect and clean the camera glass.",
        "Đang dùng Software HDR bằng CPU vì thiết bị không cung cấp HDR Extension." to
            "Using CPU Software HDR because the device does not provide an HDR Extension.",
        "Thiết bị không có HDR phần cứng; sẽ dùng Software HDR bằng CPU sau khi chụp." to
            "Hardware HDR is unavailable; CPU Software HDR will be applied after capture.",
        "Camera OEM Extension không khởi chạy được; đang dùng pipeline ISP mặc định của thiết bị." to
            "The OEM camera extension could not start; using the device's default ISP pipeline.",
        "Không thể khởi tạo camera theo pipeline gốc của thiết bị." to
            "The camera could not be initialized through the device's native pipeline.",
        "HDR Extension không ghép được với camera này; đang chuyển sang Software HDR xử lý bằng CPU." to
            "The HDR Extension is incompatible with this camera; switching to CPU Software HDR.",
        "HDR Extension không thể ghép với luồng live; đang dùng Ultra HDR JPEG_R thay thế." to
            "The HDR Extension cannot run with the live stream; using Ultra HDR JPEG_R instead.",
        "Live Photo dùng 12.5MP trên tổ hợp camera hiện tại để giữ được clip chuyển động." to
            "Live Photo is using 12.5MP on this camera combination to preserve the motion clip.",
        "Camera này không hỗ trợ Live Photo đồng thời với ảnh tĩnh; đã tắt Live Photo và khôi phục chế độ chụp thường." to
            "This camera cannot capture Live Photo and still photos together. Live Photo was disabled and standard capture restored.",
        "Camera không hỗ trợ HDR phần cứng; đang dùng Software HDR bằng CPU." to
            "Hardware HDR is not supported; using CPU Software HDR.",
        "Camera đang chọn không có đèn LED hỗ trợ Torch." to
            "The selected camera has no LED that supports Torch mode.",
        "Không thể bật đèn LED liên tục trên camera này." to
            "Continuous LED illumination could not be enabled on this camera.",
        "Star Trails tự dừng để bảo vệ bộ nhớ tạm. Đang ghép các khung đã ghi." to
            "Star Trails stopped automatically to protect temporary storage. Merging the captured frames.",
        "Không thể ghép vệt sao từ phiên này. Hãy thử lại với máy ổn định hơn." to
            "Star Trails could not merge this session. Try again with a steadier device.",
        "Cần ít nhất 2 khung hình để tạo vệt sao. Hãy thử lại." to
            "At least two frames are required to create star trails. Try again.",
        "RAW không kết hợp với LunaDream, NightShotMode, Burst hoặc Bracketing. Hãy tắt chế độ tính toán hoặc chuyển định dạng về JPEG." to
            "RAW cannot be combined with LunaDream, NightShotMode, Burst or Bracketing. Turn off computational modes or switch the format to JPEG.",
        "Đang hoàn tất chuỗi Bracketing hiện tại…" to
            "Finishing the current Bracketing sequence…",
        "Chọn HDR Bracketing hoặc Focus Bracketing, không bật đồng thời." to
            "Choose HDR Bracketing or Focus Bracketing; they cannot run at the same time.",
        "Camera không công bố hỗ trợ lấy nét thủ công để Focus Bracketing." to
            "The camera does not report manual-focus support for Focus Bracketing.",
        "Camera không công bố dải bù trừ phơi sáng cho HDR Bracketing." to
            "The camera does not report an exposure-compensation range for HDR Bracketing.",
        "Đang dừng Burst sau khung hình hiện tại…" to
            "Stopping Burst after the current frame…",
        "Burst hoàn tất · đã lưu \$burstCompletedFrames/25 ảnh JPEG gốc. Tốc độ phụ thuộc camera và bộ nhớ." to
            "Burst complete · saved \$burstCompletedFrames/25 original JPEG photos. Speed depends on the camera and storage.",
        "Đang dừng Star Trails sau khung hình hiện tại…" to
            "Stopping Star Trails after the current frame…",
        "LunaDream cần Photo/JPEG; RawDream 2.0 cần RAW + JPEG được camera hỗ trợ. Hãy tắt Ultra HDR, Burst và Bracketing." to
            "LunaDream requires Photo/JPEG; RawDream 2.0 requires camera-supported RAW + JPEG. Turn off Ultra HDR, Burst and Bracketing.",
        "Milky Way Stack cần Photo/JPEG 1x; hãy tắt RAW, UHD, FHD, Burst và Bracketing." to
            "Milky Way Stack requires Photo/JPEG at 1x. Turn off RAW, UHD, FHD, Burst and Bracketing.",
        "Đang đồng bộ hướng cảm biến, hãy chụp lại sau một chút." to
            "Synchronizing sensor orientation. Please capture again in a moment.",
        "Đã lưu ảnh RAW DNG gốc vào Pictures/FujifilmMode/RAW." to
            "Saved the original RAW DNG to Pictures/FujifilmMode/RAW.",
        "Camera không trả về dữ liệu RAW_SENSOR hợp lệ." to
            "The camera did not return valid RAW_SENSOR data.",
        "Đã lưu Ultra HDR nguyên bản và giữ gain map." to
            "Saved the original Ultra HDR and preserved its gain map.",
        "Đã lưu tệp Ultra HDR nguyên bản; giữ nguyên gain map." to
            "Saved the original Ultra HDR file and preserved its gain map.",
        "Live Photo chưa sẵn sàng: camera chưa tạo được luồng video. Ảnh chưa được chụp." to
            "Live Photo is not ready: the camera has not created a video stream. No photo was captured.",
        "Camera không bắt đầu được clip Live Photo. Ảnh chưa được chụp." to
            "The camera could not start the Live Photo clip. No photo was captured.",
        "Đã tắt Milky Way Stack: camera đang chọn không có Manual Sensor phù hợp." to
            "Milky Way Stack was disabled because the selected camera has no compatible Manual Sensor support.",
        "Tripod · nét vô cực · Astro Stack ×8 nếu Manual Sensor hỗ trợ" to
            "Tripod · infinity focus · Astro Stack ×8 when Manual Sensor is supported",
        "TẮT MILKY WAY STACK ×8" to
            "TURN OFF MILKY WAY STACK ×8",
        "ÁP DỤNG GỢI Ý + BẬT ASTRO STACK ×8" to
            "APPLY RECOMMENDATION + ENABLE ASTRO STACK ×8",
        "Đã tắt Milky Way Stack và trả phơi sáng về Auto." to
            "Milky Way Stack was disabled and exposure returned to Auto.",
        "Camera chưa sẵn sàng." to
            "Camera is not ready.",
        "Pin còn rất thấp. Hãy sạc pin trước khi dùng Star Trails, NightShot hoặc phơi sáng dài." to
            "Battery is critically low. Charge the phone before using Star Trails, NightShot or long exposure.",
        "Không thể áp dụng Manual Sensor/WB trên camera này." to
            "Manual Sensor/WB could not be applied on this camera.",
        "ĐANG KHÓA NÉT VÔ CỰC · CHẠM ĐỂ TRỞ VỀ AF" to
            "LOCKING INFINITY FOCUS · TAP TO RETURN TO AF",
        "ĐẶT LẤY NÉT VÔ CỰC" to
            "SET INFINITY FOCUS",
        "Đã trở về AF tự động." to
            "Returned to automatic AF.",
        "Đã đặt nét vô cực theo Camera2." to
            "Infinity focus was set through Camera2.",
        "Camera không chấp nhận lấy nét vô cực." to
            "The camera rejected infinity focus.",
        "Vui lòng tắt Live Photo" to
            "Please turn off Live Photo.",
        "Vui lòng tắt Xử lý ảnh gốc thiết bị để sử dụng LunaDream." to
            "Please turn off Device-native Photo Processing to use LunaDream.",
        "ẢNH GỐC · TRƯỚC LUNADREAM" to
            "ORIGINAL · BEFORE LUNADREAM",
        "ẢNH ĐÃ XỬ LÝ · LUNADREAM" to
            "PROCESSED · LUNADREAM",
        "ẢNH GỐC · THẢ ĐỂ XEM LỌC" to
            "ORIGINAL PHOTO · RELEASE TO VIEW FILTER",
        "GIỮ ĐỂ XEM ẢNH GỐC" to
            "HOLD TO VIEW ORIGINAL PHOTO",
        "Lưu hoặc nạp lại các thiết lập camera" to
            "Save or restore camera settings.",
        "Scanline overlay chỉ hiển thị trên preview, không lưu vào ảnh." to
            "The scanline overlay appears only in the preview and is never saved into photos.",
        "Giảm texture và hiệu ứng để tiết kiệm GPU trên máy yếu." to
            "Reduce textures and effects to save GPU resources on lower-powered devices.",
        "Đảo vị trí cụm chế độ và cụm nút/dial." to
            "Swap the positions of the mode group and the button/dial group.",
        "Màn trập cơ dùng WAV riêng 210 ms; tiếng phím và detent được tạo tổng hợp, có thể tắt riêng." to
            "The mechanical shutter uses a dedicated 210 ms WAV. Key and dial-detent sounds are synthesized and can be disabled separately.",
        "AE bình thường · EV 0 · căn khung và gộp nhiều ảnh" to
            "Standard AE · EV 0 · align and merge multiple photos",
        "ĐANG QUÉT CAMERA…" to
            "SCANNING CAMERAS…",
        "Không thể hoàn tất quét" to
            "The scan could not be completed.",
        "ĐANG QUÉT CAMERA…" to "SCANNING CAMERAS…",
        "Bật/tắt âm thanh chụp mặc định." to
            "Enable or disable the default shutter sound.",
        "Hiện hoặc ẩn thông số bù phơi sáng trong hàng Pro HUD." to
            "Show or hide exposure compensation in the Pro HUD row.",
        "Nhập LUT .cube, LUT Canon RGB 33x33x33 (.bin), hoặc ZIP chứa tối đa 700 LUT .cube/.bin. Hỗ trợ ZIP lồng. LUT Log được giữ riêng, không áp nhầm lên JPEG sRGB." to
            "Import a .cube LUT, Canon RGB 33x33x33 (.bin) LUT, or a ZIP containing up to 700 .cube/.bin LUTs. Nested ZIPs are supported. Log LUTs remain separate and are not incorrectly applied to sRGB JPEGs.",
        "Đã thêm · " to
            "Added · ",
        "Chưa thêm · " to
            "Not added · ",
        "Lưu" to "Save",
        "Nạp" to "Load",
        "Camera không hỗ trợ RAW + JPEG đồng thời. RawDream 2.0 chưa chụp; ảnh không bị chuyển âm thầm sang JPEG thường." to
            "The camera cannot capture RAW + JPEG simultaneously. RawDream 2.0 did not capture, and the app did not silently switch to standard JPEG.",
        "Đang hoàn tất chuỗi Bracketing hiện tại…" to "Finishing the current Bracketing sequence…",
        "Chọn HDR Bracketing hoặc Focus Bracketing, không bật đồng thời." to
            "Choose HDR Bracketing or Focus Bracketing; they cannot be enabled together.",
        "Camera không công bố hỗ trợ lấy nét thủ công để Focus Bracketing." to
            "The camera does not report manual focus support for Focus Bracketing.",
        "Camera không công bố dải bù trừ phơi sáng cho HDR Bracketing." to
            "The camera does not report an exposure compensation range for HDR Bracketing.",
        "Không thể đặt khoảng nét \${index + 1}; dừng Focus Bracketing." to
            "Could not set focus position \${index + 1}; stopping Focus Bracketing.",
        "Đang dừng Burst sau khung hình hiện tại…" to "Stopping Burst after the current frame…",
        "Đang dừng Star Trails sau khung hình hiện tại…" to "Stopping Star Trails after the current frame…",
        "LunaDream cần Photo/JPEG; RawDream 2.0 cần RAW + JPEG được camera hỗ trợ. Hãy tắt Ultra HDR, Burst và Bracketing." to
            "LunaDream requires Photo/JPEG; RawDream 2.0 requires camera-supported RAW + JPEG. Turn off Ultra HDR, Burst and Bracketing.",
        "Milky Way Stack cần Photo/JPEG 1x; hãy tắt RAW, UHD, FHD, Burst và Bracketing." to
            "Milky Way Stack requires Photo/JPEG at 1x. Turn off RAW, UHD, FHD, Burst and Bracketing.",
        "Đang đồng bộ hướng cảm biến, hãy chụp lại sau một chút." to
            "Synchronizing sensor orientation. Please capture again in a moment.",
        "Đã lưu ảnh RAW DNG gốc vào Pictures/FujifilmMode/RAW." to
            "Saved the original RAW DNG to Pictures/FujifilmMode/RAW.",
        "Camera không trả về dữ liệu RAW_SENSOR hợp lệ." to "The camera did not return valid RAW_SENSOR data.",
        "Đã lưu tệp Ultra HDR nguyên bản; giữ nguyên gain map." to
            "Saved the original Ultra HDR file and preserved its gain map.",
        "Live Photo chưa sẵn sàng: camera chưa tạo được luồng video. Ảnh chưa được chụp." to
            "Live Photo is not ready: the camera has not created a video stream. No photo was captured.",
        "Camera không bắt đầu được clip Live Photo. Ảnh chưa được chụp." to
            "The camera could not start the Live Photo clip. No photo was captured.",
        "Đã tắt Milky Way Stack: camera đang chọn không có Manual Sensor phù hợp." to
            "Milky Way Stack was disabled because the selected camera has no compatible Manual Sensor support.",
        "Tripod · nét vô cực · Astro Stack ×8 nếu Manual Sensor hỗ trợ" to
            "Tripod · infinity focus · Astro Stack ×8 when Manual Sensor is supported",
        "Đã tắt Milky Way Stack và trả phơi sáng về Auto." to
            "Milky Way Stack was disabled and exposure returned to Auto.",
        "Không thể áp dụng Manual Sensor/WB trên camera này." to
            "Manual Sensor/WB could not be applied on this camera.",
        "ĐANG KHÓA NÉT VÔ CỰC · CHẠM ĐỂ TRỞ VỀ AF" to
            "LOCKING INFINITY FOCUS · TAP TO RETURN TO AF",
        "ĐẶT LẤY NÉT VÔ CỰC" to "SET INFINITY FOCUS",
        "Đã trở về AF tự động." to "Returned to automatic AF.",
        "Đã đặt nét vô cực theo Camera2." to "Infinity focus was set through Camera2.",
        "Camera không chấp nhận lấy nét vô cực." to "The camera rejected infinity focus.",
        "Vui lòng tắt Live Photo" to "Please turn off Live Photo.",
        "Vui lòng tắt Xử lý ảnh gốc thiết bị để sử dụng LunaDream." to
            "Please turn off Device-native Photo Processing to use LunaDream.",
        "ẢNH GỐC · TRƯỚC LUNADREAM" to "ORIGINAL · BEFORE LUNADREAM",
        "ẢNH ĐÃ XỬ LÝ · LUNADREAM" to "PROCESSED · LUNADREAM",
        "ẢNH GỐC · THẢ ĐỂ XEM LỌC" to "ORIGINAL PHOTO · RELEASE TO VIEW FILTER",
        "GIỮ ĐỂ XEM ẢNH GỐC" to "HOLD TO VIEW ORIGINAL PHOTO",
        "Màn trập cơ dùng WAV riêng 210 ms; tiếng phím và detent được tạo tổng hợp, có thể tắt riêng." to
            "The mechanical shutter uses a dedicated 210 ms WAV. Key and dial-detent sounds are synthesized and can be disabled separately.",
        "AE bình thường · EV 0 · căn khung và gộp nhiều ảnh" to
            "Standard AE · EV 0 · align and merge multiple photos",
        "ĐANG QUÉT CAMERA…" to "SCANNING CAMERAS…",
        "Không thể hoàn tất quét" to "The scan could not be completed.",
        "Còn \${scan.vendorMetadataKeys.size - 30} key khác." to
            "\${scan.vendorMetadataKeys.size - 30} more keys.",
        "Lưu hoặc nạp lại các thiết lập camera" to "Save or restore camera settings.",
        "Scanline overlay chỉ hiển thị trên preview, không lưu vào ảnh." to
            "The scanline overlay appears only in the preview and is never saved into photos.",
        "Giảm texture và hiệu ứng để tiết kiệm GPU trên máy yếu." to
            "Reduce textures and effects to save GPU resources on lower-powered devices.",
        "Đảo vị trí cụm chế độ và cụm nút/dial." to
            "Swap the positions of the mode group and the button/dial group.",
        "Bật/tắt âm thanh chụp mặc định." to "Enable or disable the default shutter sound.",
        "Hiện hoặc ẩn thông số bù phơi sáng trong hàng Pro HUD." to
            "Show or hide exposure compensation in the Pro HUD row.",
        "Sao lưu camera trước/sau, tỷ lệ khung hình, zoom, chế độ Pro/Photo, flash, hẹn giờ, HDR, UHD, chống rung và các tùy chọn đã bật." to
            "Back up front/rear camera selection, aspect ratio, zoom, Pro/Photo mode, flash, timer, HDR, UHD, stabilization and enabled options.",
        "Night capture and Star Trails use separate processing pipelines" to
            "Chụp đêm và Star Trails sử dụng các pipeline xử lý riêng.",
        "Thông tin" to "Information",
        "Đóng" to "Close",
        "Ảnh gốc" to "Original photo",
        "Bộ lọc" to "Filter",
        "Đang tải ảnh..." to "Loading photo...",
        "Ấm" to "Warm",
        "Lạnh" to "Cool",
        "Không rõ nguyên nhân" to "Unknown cause",
        "không rõ nguyên nhân" to "unknown cause",
        "không rõ lỗi" to "unknown error",
        "khung hình đã chụp" to "captured frames",
        " khung hình" to " frames",
        " (cần kiểm tra lại metadata)." to " (metadata still needs verification)."
    )

    private val englishToVietnamese = mapOf(
        "RAW Capture" to "Chụp RAW",
        "Battery is critically low. Charge the phone before using Star Trails, NightShot or long exposure." to
            "Pin còn rất thấp. Hãy sạc pin trước khi dùng Star Trails, NightShot hoặc phơi sáng dài.",
        "SETTINGS  ›" to "CÀI ĐẶT  ›",
        "MENU/OK  >  SETTINGS" to "MENU/OK  >  CÀI ĐẶT",

        "Camera Presets" to "Thiết lập camera",
        "Film Simulation Customizer" to "Tùy chỉnh mô phỏng phim",
        "Saved Film Recipes" to "Công thức mô phỏng phim đã lưu",
        "Night capture and Star Trails use separate processing pipelines" to
            "Chụp đêm và Star Trails sử dụng các pipeline xử lý riêng.",
        "ISO Auto" to "ISO tự động",
        "Shutter Auto" to "Màn trập tự động",
        "Original photo before LunaDream" to "Ảnh gốc trước LunaDream",
        "Photo after LunaDream" to "Ảnh sau LunaDream",
        "LUT MANAGER" to "QUẢN LÝ LUT",
        "Color Temperature" to "Nhiệt độ màu",
        "Color Tone" to "Tông màu",
        "Green ↔ Magenta" to "Xanh lá ↔ Đỏ tươi",
        "Saturation" to "Độ bão hòa",
        "Highlight" to "Vùng sáng",
        "Shadow" to "Vùng tối",
        "Battery temperature:" to "Nhiệt độ pin:",
        "Android thermal status:" to "Trạng thái nhiệt Android:"
    )

    private val additionalVietnameseToEnglish = mapOf(
        "Đang kiểm tra hỗ trợ phơi sáng" to "Checking exposure support",
        "Camera này không hỗ trợ RAW/RawDream 2.0 đồng thời. Không thể dùng chế độ RAW đã chọn." to
            "This camera cannot capture RAW and RawDream 2.0 simultaneously. The selected RAW mode is unavailable.",
        "Camera chưa sẵn sàng." to "Camera is not ready.",
        "Không thể ghép vệt sao từ phiên này. Hãy thử lại với máy ổn định hơn." to
            "Could not merge star trails from this session. Try again with a steadier device.",
        "Cần ít nhất 2 khung hình để tạo vệt sao. Hãy thử lại." to
            "At least two frames are required to create star trails. Try again.",
        "Không rõ nguyên nhân" to "Unknown reason",
        "không rõ nguyên nhân" to "unknown reason",
        "không rõ lỗi" to "unknown error",
        "Camera không hỗ trợ RAW + JPEG đồng thời. RawDream 2.0 chưa chụp; ảnh không bị chuyển âm thầm sang JPEG thường." to
            "The camera cannot capture RAW + JPEG simultaneously. RawDream 2.0 did not capture, and the app did not silently switch to standard JPEG.",
        "LunaDream cần Photo/JPEG; RawDream 2.0 cần RAW + JPEG được camera hỗ trợ. Hãy tắt Ultra HDR, Burst và Bracketing." to
            "LunaDream requires Photo/JPEG; RawDream 2.0 requires camera-supported RAW + JPEG. Turn off Ultra HDR, Burst and Bracketing.",
        "Milky Way Stack cần Photo/JPEG 1x; hãy tắt RAW, UHD, FHD, Burst và Bracketing." to
            "Milky Way Stack requires Photo/JPEG at 1x. Turn off RAW, UHD, FHD, Burst and Bracketing.",
        "Đã lưu ảnh RAW DNG gốc vào Pictures/FujifilmMode/RAW." to
            "Saved the original RAW DNG to Pictures/FujifilmMode/RAW.",
        "Camera không trả về dữ liệu RAW_SENSOR hợp lệ." to "The camera did not return valid RAW_SENSOR data.",
        "Live Photo chưa sẵn sàng: camera chưa tạo được luồng video. Ảnh chưa được chụp." to
            "Live Photo is not ready: the camera has not created a video stream. No photo was captured.",
        "Camera không bắt đầu được clip Live Photo. Ảnh chưa được chụp." to
            "The camera could not start the Live Photo clip. No photo was captured.",
        "Đã tắt Milky Way Stack: camera đang chọn không có Manual Sensor phù hợp." to
            "Milky Way Stack was disabled because the selected camera has no compatible Manual Sensor support.",
        "Đã tắt Milky Way Stack và trả phơi sáng về Auto." to
            "Milky Way Stack was disabled and exposure returned to Auto.",
        "Đã trở về AF tự động." to "Returned to automatic AF.",
        "Đã đặt nét vô cực theo Camera2." to "Infinity focus was set through Camera2.",
        "Camera không chấp nhận lấy nét vô cực." to "The camera rejected infinity focus.",
        "TẮT MILKY WAY STACK ×8" to "TURN OFF MILKY WAY STACK ×8",
        "ÁP DỤNG GỢI Ý + BẬT ASTRO STACK ×8" to "APPLY RECOMMENDATION + ENABLE ASTRO STACK ×8",
        "Vui lòng tắt Live Photo" to "Please turn off Live Photo.",
        "Vui lòng tắt Xử lý ảnh gốc thiết bị để sử dụng LunaDream." to
            "Please turn off Device-native Photo Processing to use LunaDream.",
        "ẢNH GỐC · TRƯỚC LUNADREAM" to "ORIGINAL · BEFORE LUNADREAM",
        "ẢNH ĐÃ XỬ LÝ · LUNADREAM" to "PROCESSED · LUNADREAM",
        "ẢNH GỐC · THẢ ĐỂ XEM LỌC" to "ORIGINAL PHOTO · RELEASE TO VIEW FILTER",
        "GIỮ ĐỂ XEM ẢNH GỐC" to "HOLD TO VIEW ORIGINAL PHOTO",
        "Lưu hoặc nạp lại các thiết lập camera" to "Save or restore camera settings.",
        "Scanline overlay chỉ hiển thị trên preview, không lưu vào ảnh." to
            "The scanline overlay appears only in the preview and is never saved into photos.",
        "Giảm texture và hiệu ứng để tiết kiệm GPU trên máy yếu." to
            "Reduce textures and effects to save GPU resources on lower-powered devices.",
        "Đảo vị trí cụm chế độ và cụm nút/dial." to
            "Swap the positions of the mode group and the button/dial group.",
        "AE bình thường · EV 0 · căn khung và gộp nhiều ảnh" to
            "Standard AE · EV 0 · align and merge multiple photos",
        "ĐANG QUÉT CAMERA…" to "SCANNING CAMERAS…",
        "Không thể hoàn tất quét" to "The scan could not be completed.",
        "Hiện hoặc ẩn thông số bù phơi sáng trong hàng Pro HUD." to
            "Show or hide exposure compensation in the Pro HUD row.",
        "LƯU" to "SAVE",
        "Bật/tắt âm thanh chụp mặc định." to "Enable or disable the default shutter sound.",
        "khung hình đã chụp" to "captured frames",
        "Ảnh gốc" to "Original photo",
        "Ảnh gần nhất" to "Most recent photo",
        "SIÊU RỘNG" to "ULTRAWIDE",
        "CAM THƯỜNG" to "MAIN CAMERA",
        "Đã nhập " to "Imported ",
        "Đã thêm · " to "Added · ",
        "Chưa thêm · " to "Not added · ",
        "Thông tin" to "Information",
        "Đóng" to "Close",
        "Bộ lọc" to "Filter",
        "Camera không hỗ trợ phơi sáng thủ công; Star Trails dùng AE tự động. Nhấn lần nữa để dừng và ghép." to
            "Manual exposure is unsupported; Star Trails is using automatic exposure. Press again to stop and merge.",
        "AE tự động · không hỗ trợ manual sensor" to "Automatic AE · manual sensor unsupported",
        "Đang hoàn tất ảnh Star Trails" to "Finishing Star Trails image",
        "Đang ghép HDR Bracketing" to "Merging HDR Bracketing",
        "Đang ghép Focus Stack" to "Merging Focus Stack",
        "HDR Fusion · đang ghép vùng phơi sáng" to "HDR Fusion · merging exposure regions",
        "Focus Stack · đang chọn vùng nét" to "Focus Stack · selecting focused regions",
        "Không đủ khung hình để ghép; các ảnh chụp thành công vẫn được giữ." to
            "Not enough frames to merge; successfully captured photos are preserved.",
        "Ghép ảnh chưa thành công; toàn bộ ảnh gốc vẫn được giữ." to
            "Image merging failed; all original photos are preserved.",
        "Đang giữ nguyên dữ liệu Ultra HDR" to "Preserving original Ultra HDR data",
        "LunaDream 1.5 · chuẩn bị ghép tối đa 5 khung" to "LunaDream 1.5 · preparing to merge up to 5 frames",
        "RawDream 2.0 · chuẩn bị ghép 8 khung, giữ DNG gốc" to "RawDream 2.0 · preparing to merge 8 frames and preserve the original DNG",
        "LunaDream 2.0 · chuẩn bị ghép 8 khung" to "LunaDream 2.0 · preparing to merge 8 frames",
        "NightShotMode Milky Way · chuẩn bị căn chỉnh 8 khung" to "NightShotMode Milky Way · preparing to align 8 frames",
        "NightShotMode · chuẩn bị căn chỉnh và ghép nhiều khung" to "NightShotMode · preparing to align and merge multiple frames",
        "Đang xử lý Software HDR" to "Processing Software HDR",
        "Ảnh đang được xử lý" to "Processing photo",
        "LunaDream 1.5 Render · khử nhiễu và phục hồi chi tiết" to "LunaDream 1.5 Render · denoising and detail recovery",
        "RawDream 2.0 Render · hoàn thiện ảnh JPEG" to "RawDream 2.0 Render · finishing JPEG",
        "LunaDream 2.0 Render · khử nhiễu và phục hồi chi tiết" to "LunaDream 2.0 Render · denoising and detail recovery",
        "NightShot Render · đang hoàn tất ảnh đã ghép" to "NightShot Render · finishing merged photo",
        "LunaDream 2.0 · đang hoàn tất bằng dự phòng 1.5" to "LunaDream 2.0 · finishing with 1.5 fallback",
        "RawDream 2.0 · đang hoàn tất JPEG bằng dự phòng LunaDream 1.5" to "RawDream 2.0 · finishing JPEG with LunaDream 1.5 fallback",
        "LunaDream Render · 1.5 đang phục hồi chi tiết" to "LunaDream Render · 1.5 detail recovery",
        "LunaDream Render · RawDream 2.0 đang xử lý JPEG ghép" to "LunaDream Render · RawDream 2.0 processing merged JPEG",
        "LunaDream Render · 2.0 đang phục hồi chi tiết" to "LunaDream Render · 2.0 detail recovery",
        "Đang hoàn tất ảnh" to "Finishing photo",
        "Xử lý ảnh gốc thiết bị · tối ưu tông sáng và chi tiết" to "Device-native photo processing · optimizing tones and detail",
        "Đang giữ trọn khung ảnh theo tỷ lệ đã chọn" to "Preserving the full frame at the selected aspect ratio",
        "Đang đóng watermark ở độ phân giải cuối" to "Applying watermark at final output resolution",
        "NightShotMode: căn khung và giảm nhiễu" to "NightShotMode: aligning frames and reducing noise",
        "Milky Way: căn chỉnh sao và giảm nhiễu" to "Milky Way: aligning stars and reducing noise",
        "Multi-frame: ghép 5 khung" to "Multi-frame: merging 5 frames",
        "NightShotMode · ghép đa khung" to "NightShotMode · multi-frame merge",
        "NightShotMode · chụp đêm" to "NightShotMode · night capture",
        "Xử lý ảnh gốc thiết bị · ISP + tối ưu tông/chi tiết" to
            "Device-native photo processing · ISP + tone/detail optimization",
        "Xử lý ảnh gốc thiết bị chưa hoàn tất · giữ ảnh JPEG" to
            "Device-native photo processing did not finish · preserving JPEG",
        "Chụp RAW" to "RAW capture",
        "Không thu đủ dữ liệu hoặc engine không trả kết quả ghép" to "Insufficient capture data or the engine returned no merged result",
        "Không nội suy độ phân giải" to "No resolution interpolation",
        "Không công bố" to "Not reported",
        "HAL không công bố" to "Not reported by HAL",
        "Chụp ảnh thường" to "Standard photo capture",
        "LunaDream không thể hoàn tất; ảnh gốc đã được giữ. Lý do:" to
            "LunaDream could not finish; the original photo was preserved. Reason:",
        "LunaDream không hoàn tất; đã dùng thuật toán dự phòng 1.5. Lý do:" to
            "LunaDream did not finish; the 1.5 fallback algorithm was used. Reason:",
        "LunaDream không hoàn tất; đã dùng thuật toán dự phòng 1.5. Lý do:" to
            "LunaDream did not finish; the 1.5 fallback algorithm was used. Reason:",
        "LunaDream đã ghép và phục hồi ảnh nhưng không thể đạt kích thước đầu ra an toàn; ảnh đã xử lý được giữ lại." to
            "LunaDream merged and recovered the photo but could not safely reach the requested output size; the processed image was preserved.",
        "RawDream 2.0 đã giữ DNG gốc và ghép JPEG bằng LunaDream 2.0, nhưng chưa thể nâng kích thước JPEG an toàn." to
            "RawDream 2.0 preserved the original DNG and merged the JPEG with LunaDream 2.0, but could not safely upscale the JPEG.",
        "NightShotMode đã xử lý bằng chế độ NIGHT của camera." to
            "NightShotMode was processed using the camera's NIGHT mode.",
        "NightShotMode không ghép đủ khung hình; ảnh gốc vẫn được giữ." to
            "NightShotMode could not merge enough frames; the original photo is preserved.",
        "Đã lưu Ultra HDR nguyên bản và giữ gain map. Watermark được bỏ qua để bảo toàn gain map." to
            "Saved original Ultra HDR and preserved the gain map. Watermark was skipped to preserve the gain map.",
        "Đã lưu Ultra HDR nguyên bản. Watermark được bỏ qua để bảo toàn gain map." to
            "Saved original Ultra HDR. Watermark was skipped to preserve the gain map.",
        "Đã lưu ảnh đúng tỉ lệ, đúng hướng, có màu và watermark." to "Saved the photo at the correct ratio and orientation, with color and watermark.",
        "Đã lưu ảnh đúng tỉ lệ, đúng hướng và có watermark." to "Saved the photo at the correct ratio and orientation, with watermark.",
        "Watermark đã đóng nhưng tỉ lệ/hướng ảnh chưa được xác minh." to "Watermark was applied, but image ratio/orientation has not been verified.",
        "Đã lưu ảnh đúng tỉ lệ, đúng hướng và áp dụng bộ lọc màu." to "Saved the photo at the correct ratio and orientation with the color filter applied.",
        "Đã áp dụng màu nhưng chưa xác minh được tỉ lệ/hướng ảnh." to "Color was applied, but image ratio/orientation could not be verified.",
        "Đã lưu ảnh đã tối ưu kích thước đầu ra." to "Saved the photo with optimized output dimensions.",
        "Đã lưu ảnh đúng hướng; đã áp dụng Software HDR." to "Saved the photo in the correct orientation with Software HDR applied.",
        "Đã lưu ảnh đúng tỉ lệ và chuẩn hóa metadata vào album." to "Saved the photo at the correct ratio and normalized its metadata in the album.",
        "Ảnh đã lưu nhưng watermark chưa được đóng. Kiểm tra lại mục Watermark." to
            "The photo was saved, but the watermark was not applied. Check Watermark settings.",
        "Đã lưu LunaDream 1.5 · 50MP." to "Saved LunaDream 1.5 · 50MP.",
        "Đã lưu RawDream 2.0 · DNG gốc + JPEG LunaDream 2.0 108MP." to
            "Saved RawDream 2.0 · original DNG + LunaDream 2.0 JPEG at 108MP.",
        "Đã lưu LunaDream 2.0 · 108MP." to "Saved LunaDream 2.0 · 108MP.",
        "LunaDream không hoàn tất: " to "LunaDream did not finish: ",
        "Ảnh đã lưu nhưng chưa xác minh được tỉ lệ/hướng metadata. Hãy kiểm tra lại ảnh." to
            "The photo was saved, but its ratio/orientation metadata could not be verified. Please check the photo.",
        "Ảnh đã chụp nhưng MediaStore không trả về URI." to "Photo captured, but MediaStore did not return a URI.",
        "Ảnh đã chụp nhưng hậu kỳ gặp lỗi; tệp ảnh hợp lệ gần nhất đã được giữ. Lý do:" to
            "The photo was captured, but post-processing failed; the most recent valid image file was preserved. Reason:",
        "Không thể tạo preview cho bộ lọc này" to "Could not create a preview for this filter",
        "Đang tạo preview…" to "Creating preview…",
        "MILKY WAY INTELLIGENCE · CẢNH TỐI" to "MILKY WAY INTELLIGENCE · DARK SCENE",
        "HAL không công bố Manual Sensor · chụp Auto riêng lẻ, Astro Stack không khả dụng" to
            "HAL does not report Manual Sensor support · single Auto captures only; Astro Stack is unavailable",
        "Gợi ý từ độ sáng preview và thông số ống kính; không đo được mây / trăng / ô nhiễm sáng." to
            "Suggestions are based on preview brightness and lens metadata; clouds, moonlight and light pollution are not measured.",
        "Khuyên dùng tripod; camera hỗ trợ manual sensor mới dùng được 1–8 giây thật" to
            "A tripod is recommended; true 1–8 second exposures require Manual Sensor support.",
        "NO SIGNAL\nCần cấp quyền Camera (chạm để cấp)" to "NO SIGNAL\nCamera permission required (tap to grant)",
        "Dùng nút màu" to "Use color button",
        "Chưa có ảnh trong Pictures/FujifilmMode." to "No photos in Pictures/FujifilmMode.",
        "EV không được hỗ trợ" to "EV is not supported",
        "AF tự động" to "Automatic AF",
        "Tông màu tự động" to "Automatic color temperature",
        "Camera/API này không hỗ trợ chụp JPEG Ultra HDR." to "This camera/API does not support Ultra HDR JPEG capture.",
        "ĐANG GHI STAR TRAILS" to "STAR TRAILS RECORDING",
        "Giữ máy cố định · nhấn nút chụp lần nữa để dừng sau khung hiện tại" to
            "Keep the phone steady · press the shutter again to stop after the current frame",
        "Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm." to
            "These tools directly control viewfinder overlays.",
        "Best sử dụng mức nén JPEG cao nhất; độ phân giải thực tế phụ thuộc độ phân giải mà Camera2/CameraX cung cấp cho camera đang chọn." to
            "Best uses the highest JPEG quality setting; actual resolution depends on the resolution provided by Camera2/CameraX for the selected camera.",
        "Ảnh được lưu vào Pictures/FujifilmMode." to "Photos are saved to Pictures/FujifilmMode.",
        "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút." to
            "Change the shutter button color or use a PNG/JPG image as its face.",
        "Đã chọn ảnh cho nút chụp." to "Image selected for the shutter button.",
        "Thời lượng thu khung" to "Frame capture duration",
        "Thời gian mỗi khung" to "Time per frame",
        "Người làm App: Baoaka06" to "App creator: Baoaka06",
        "Đang đọc CameraCharacteristics…" to "Reading CameraCharacteristics…",
        "Đã sao chép log" to "Log copied",
        "Lưu và nạp nhanh tỉ lệ, zoom, EV, bộ lọc, HDR, NightShotMode, RAW và cấu hình LunaDream." to
            "Quickly save and restore aspect ratio, zoom, EV, filters, HDR, NightShotMode, RAW and LunaDream settings.",
        "Điều chỉnh này được áp dụng giống nhau trong preview bộ lọc và ảnh JPEG sau chụp." to
            "This adjustment is applied consistently in the filter preview and the captured JPEG.",
        "Xóa LUT?" to "Delete LUT?",
        "Đang tải thư viện ảnh…" to "Loading photo library…",
        "Đang tải ảnh…" to "Loading photo…",
        "Đang đọc metadata ảnh…" to "Reading photo metadata…",
        "Sao lưu & Khôi phục" to "Backup & Restore",
        "Nút chụp tùy chỉnh" to "Custom shutter button",
        "Âm thanh màn trập" to "Shutter sound",
        "LunaDream 2.0 không hoàn tất; đã dùng thuật toán dự phòng 1.5. Lý do:" to
            "LunaDream 2.0 did not finish; the 1.5 fallback algorithm was used. Reason:",
        "Sao lưu camera trước/sau, tỷ lệ khung hình, zoom, chế độ Pro/Photo, flash, hẹn giờ, HDR, UHD, chống rung, AI và các tùy chọn đã bật." to
            "Back up front/rear camera selection, aspect ratio, zoom, Pro/Photo mode, flash, timer, HDR, UHD, stabilization, AI and enabled options.",
        "Chế độ chụp đêm" to "Night capture mode",
        "Tắt" to "Off",
        "Phơi sáng lâu · Star Trails" to "Long exposure · Star Trails",
        "Chụp ảnh bình thường" to "Standard photo capture",
        "Thời lượng thu khung" to "Capture duration",
        "Camera liên tục chụp trong khoảng thời gian đã chọn; số khung thực tế tùy tốc độ cảm biến và giới hạn CameraX." to
            "The camera continuously captures during the selected duration; the actual frame count depends on sensor speed and CameraX limits.",
        "Mỗi khung dùng màn trập 1, 2, 4 hoặc 8 giây, ISO 800 và lấy nét vô cực khi camera hỗ trợ MANUAL_SENSOR. Máy không hỗ trợ sẽ báo rõ và dùng AE tự động; không giả lập thời gian phơi sáng." to
            "Each frame uses a 1, 2, 4 or 8 second shutter, ISO 800 and infinity focus when the camera supports MANUAL_SENSOR. Unsupported devices are identified and use automatic AE; exposure time is never simulated.",
        "Dùng tripod, tắt flash và hướng máy vào bầu trời. Nhấn nút chụp lần nữa để dừng sau khung hiện tại; ứng dụng sẽ ghép các vị trí sao sáng thành vệt dài. Chỉ tự dừng sớm khi bộ nhớ tạm gần đầy." to
            "Use a tripod, turn off the flash and point the camera at the sky. Press the shutter again to stop after the current frame; the app merges bright star positions into trails. It stops early only when temporary storage is nearly full.",
        "Bật để dùng nhánh căn chỉnh và ghép khung bầu trời trong NightShotMode. Tắt mặc định; không hoạt động ở Photo thường." to
            "Enable the sky-frame alignment and stacking pipeline in NightShotMode. Disabled by default; it does not run in standard Photo mode.",
        "Engine HDR tone mapping độc lập, tự điều chỉnh mức xử lý theo dữ liệu sáng/tối và tài nguyên thiết bị. HDR không thay đổi độ phân giải đầu ra của LunaDream." to
            "The independent HDR tone-mapping engine adjusts processing based on highlight/shadow data and device resources. HDR does not change LunaDream output resolution.",
        "RAW lưu DNG gốc theo dữ liệu cảm biến. RawDream 2.0 chụp đồng thời DNG và JPEG; JPEG được căn chỉnh, ghép đa khung, khử nhiễu và phục hồi chi tiết bằng LunaDream 2.0, đầu ra mục tiêu 108MP. RawDream 2.0 không kết hợp NightShotMode, Star Trails, Burst hoặc Bracketing. Nếu camera không hỗ trợ đồng thời RAW + JPEG, ứng dụng báo lỗi và không tự chuyển sang JPEG thường." to
            "RAW saves the original sensor data as DNG. RawDream 2.0 captures DNG and JPEG together; the JPEG is aligned, multi-frame merged, denoised and detail-recovered by LunaDream 2.0, targeting 108MP output. RawDream 2.0 cannot be combined with NightShotMode, Star Trails, Burst or Bracketing. If the camera cannot capture RAW + JPEG simultaneously, the app reports an error and does not silently switch to standard JPEG.",
        "Off tắt khử nhiễu. Low / Medium / High chỉ dùng thuật toán khử nhiễu có sẵn của camera (Camera2); không có xử lý AI sau chụp. Ultra HDR giữ nguyên dữ liệu JPEG_R." to
            "Off disables noise reduction. Low / Medium / High use only the camera's built-in Camera2 noise-reduction algorithms; no post-capture AI processing is applied. Ultra HDR preserves the JPEG_R data.",
        "Quét Camera2 công khai, Physical Camera và vendor metadata được HAL công bố." to
            "Scan public Camera2 cameras, physical cameras and vendor metadata exposed by the HAL.",
        "QUÉT LẠI PHẦN CỨNG CAMERA" to "RESCAN CAMERA HARDWARE",
        "Không thể hoàn tất quét" to "Scan could not be completed",
        "Camera ID công khai" to "Public Camera IDs",
        "Physical ID được công bố" to "Reported physical IDs",
        "Tổng mục đã đọc" to "Total entries read",
        "Nhóm camera chạy đồng thời" to "Concurrent camera groups",
        "ID không đọc được" to "Unreadable IDs",
        "Chưa tìm thấy camera" to "No cameras found",
        "Không có Camera ID nào được hệ thống công khai cho ứng dụng. Hãy kiểm tra quyền Camera và thử quét lại." to
            "The system has not exposed any Camera IDs to this app. Check Camera permission and scan again.",
        "Hướng" to "Facing",
        "Tiêu cự" to "Focal length",
        "Tương đương 35mm" to "35mm equivalent",
        "Lấy nét gần nhất" to "Nearest focus",
        "Không công bố / không manual focus" to "Not reported / no manual focus",
        "Kích thước cảm biến" to "Sensor size",
        "JPEG lớn nhất" to "Maximum JPEG",
        "RAW_SENSOR lớn nhất" to "Maximum RAW_SENSOR",
        "Zoom số tối đa" to "Maximum digital zoom",
        "Vendor metadata mở rộng" to "Extended vendor metadata",
        "Key riêng của OEM được HAL công bố trên camera truy cập được" to
            "OEM-specific keys exposed by the HAL on accessible cameras",
        "Không có vendor key ngoài namespace Android được công bố trong metadata đọc được." to
            "No vendor keys outside the Android namespace were exposed in the readable metadata.",
        "Camera ID OEM không công khai" to "Non-public OEM camera IDs",
        "Giới hạn truy cập của Android" to "Android access limitations",
        "Scanner đã kiểm tra Logical Camera, các Physical ID mà OEM đưa vào metadata và vendor metadata có thể đọc được. Camera ID bị camera service/firmware ẩn hoàn toàn không thể được liệt kê bằng API Android công khai; muốn truy cập cần OEM/system API hoặc quyền hệ thống. App không tạo ID giả và không dùng root/hidden-API reflection để vượt giới hạn." to
            "The scanner checked logical cameras, physical IDs provided by the OEM in metadata and readable vendor metadata. Camera IDs fully hidden by the camera service or firmware cannot be listed through public Android APIs; accessing them requires OEM/system APIs or system permissions. The app does not invent IDs or use root/hidden-API reflection to bypass these limits.",
        "Hiện hoặc ẩn tốc độ màn trập trong hàng Pro HUD." to "Show or hide shutter speed in the Pro HUD row.",
        "Hiện hoặc ẩn độ nhạy ISO trong hàng Pro HUD." to "Show or hide ISO sensitivity in the Pro HUD row.",
        "Hiện hoặc ẩn trạng thái lấy nét trong hàng Pro HUD." to "Show or hide focus status in the Pro HUD row.",
        "Hiện hoặc ẩn nhiệt độ màu / cân bằng trắng trong hàng Pro HUD." to "Show or hide color temperature / white balance in the Pro HUD row.",
        "Biểu đồ phân bố kênh đỏ, lục và lam từ khung preview." to "Histogram of red, green and blue channels from the preview frame.",
        "Ưu tiên khuôn mặt khi Camera2/HAL công bố face detection; tự đưa AF về vùng mặt." to
            "Prioritize faces when Camera2/HAL exposes face detection; move AF to the detected face region.",
        "Giảm trượt/scale khi máy nóng hoặc khi bật thủ công; Android tự kích hoạt khi nhiệt độ đạt mức trung bình." to
            "Reduce sliding and scaling when the device is hot or when manually enabled; Android activates this automatically at a moderate thermal level.",
        "Yêu cầu Camera2/HAL hiệu chỉnh méo hình, viền màu và tối góc bằng các chế độ công khai nếu ống kính hỗ trợ; không tự bịa dữ liệu lens profile." to
            "Request public Camera2/HAL correction for distortion, chromatic aberration and vignetting when supported by the lens; no lens profiles are fabricated.",
        "Đánh dấu vùng sáng gần cháy sáng trên kính ngắm (không ảnh hưởng ảnh lưu)." to
            "Mark near-clipped highlights in the viewfinder; this does not affect saved photos.",
        "Đo độ nghiêng bằng cảm biến thiết bị và hiển thị đường chân trời trong kính ngắm." to
            "Measure device tilt using its sensors and show the horizon in the viewfinder.",
        "Chụp 3 khung JPEG với EV thấp / chuẩn / cao nếu camera công bố dải phơi sáng." to
            "Capture three JPEG frames at low / normal / high EV when the camera exposes an exposure-compensation range.",
        "Chụp 5 khoảng nét từ gần đến vô cực khi ống kính hỗ trợ lấy nét thủ công." to
            "Capture five focus distances from near to infinity when the lens supports manual focus.",
        "Trống" to "Empty",
        "Chưa lưu" to "Not saved",
        "NẠP" to "LOAD",
        "LUT tùy chỉnh" to "Custom LUT",
        "Tệp LUT vượt quá giới hạn dung lượng cho phép." to "The LUT file exceeds the allowed size limit.",
        "LUT nhập" to "Imported LUT",
        "ZIP LUT lồng quá nhiều cấp." to "The LUT ZIP contains too many nested levels.",
        "ZIP chứa quá nhiều tệp." to "The ZIP contains too many files.",
        "Dung lượng giải nén ZIP vượt giới hạn an toàn." to "The expanded ZIP size exceeds the safety limit.",
        "Tệp .cube bên trong ZIP không đúng định dạng LUT 3D." to "The .cube file inside the ZIP is not a valid 3D LUT.",
        "Tệp .bin không phải LUT RGB 33x33x33 được hỗ trợ." to "The .bin file is not a supported 33x33x33 RGB LUT.",
        "LUT đã nhập" to "Imported LUT",
        "Không thể mở tệp LUT đã chọn." to "Could not open the selected LUT file.",
        "Tệp .cube không hợp lệ hoặc không phải LUT 3D." to "The .cube file is invalid or is not a 3D LUT.",
        "Hỗ trợ .cube, LUT .bin RGB 33x33x33 và ZIP chứa các định dạng này." to
            "Supports .cube, 33x33x33 RGB .bin LUTs and ZIP files containing these formats.",
        "Không tìm thấy LUT .cube hoặc .bin hợp lệ bên trong ZIP." to "No valid .cube or .bin LUT was found in the ZIP.",
        "Tìm LUT theo tên hoặc nhóm" to "Search LUTs by name or category",
        "ĐANG NHẬP LUT…" to "IMPORTING LUT…",
        "Chưa có LUT nào. Hỗ trợ .cube, Canon RGB .bin và ZIP chứa nhiều LUT." to
            "No LUTs yet. Supports .cube, Canon RGB .bin and ZIP files containing multiple LUTs.",
        "HỦY" to "CANCEL",
        "XÓA" to "DELETE",
        "Thông tin ảnh" to "Photo information",
        "Không thể đọc metadata: " to "Could not read metadata: ",
        "Chưa có ảnh. Ảnh chụp trong ứng dụng sẽ xuất hiện ở đây." to
            "No photos yet. Photos captured in the app will appear here.",
        "Ảnh trong album" to "Photo in album",
        "Không có" to "Not available",
        "Không có dữ liệu phiên LunaDream" to "No LunaDream session data",
        "Không ghi nhận" to "Not recorded",
        "Không có EXIF" to "No EXIF",
        "Tên tệp" to "File name",
        "Độ phân giải" to "Resolution",
        "Dung lượng" to "File size",
        "Định dạng" to "Format",
        "Thiết bị" to "Device",
        "Camera ID logic" to "Logical Camera ID",
        "Physical Camera ID thực tế" to "Actual Physical Camera ID",
        "Ống kính" to "Lens",
        "Tốc độ màn trập" to "Shutter speed",
        "Khẩu độ" to "Aperture",
        "Ngày chụp" to "Capture date",
        "Độ nét (ước tính)" to "Sharpness (estimated)",
        "Vùng cháy sáng" to "Highlight clipping",
        "Vùng tối bệt" to "Shadow clipping",
        "Nhiễu nền (ước tính)" to "Noise level (estimated)",
        "Gợi ý chất lượng" to "Quality recommendation",
        "Kích thước sau xử lý" to "Processed output size",
        "Trạng thái LunaDream" to "LunaDream status",
        "Không phân tích được" to "Could not analyze",
        "Không có dữ liệu phiên LunaDream" to "No LunaDream session data",
        "50MP · chụp một lần · xử lý trực tiếp, ưu tiên tốc độ" to
            "50MP · single capture · direct processing, optimized for speed",
        "108MP · đa khung hình · ưu tiên phục hồi chi tiết tối đa" to
            "108MP · multi-frame · prioritizes maximum detail recovery",
        "Milky Way được bật/tắt trong Cài đặt → NightShotMode." to
            "Milky Way can be toggled in Settings → NightShotMode.",
        "Không mở được tệp đích" to "Could not open the destination file",
        "Đã xuất bản sao lưu cài đặt." to "Settings backup exported.",
        "Không thể tạo bản sao lưu. Hãy kiểm tra vị trí lưu và thử lại." to
            "Could not create the backup. Check the save location and try again.",
        "Không đọc được tệp" to "Could not read the file",
        "Đã khôi phục cài đặt camera, zoom và các tính năng đã bật." to
            "Camera settings, zoom and enabled features have been restored.",
        "Không thể khôi phục. Hãy chọn tệp sao lưu Fujifilm Mode hợp lệ." to
            "Could not restore. Select a valid FujiFilm Mode backup file.",
        "XUẤT BẢN SAO LƯU" to "EXPORT BACKUP",
        "KHÔI PHỤC BẢN SAO LƯU" to "RESTORE BACKUP",
        "Lưu ý: tệp này lưu cấu hình camera, frame và ảnh nút chụp; không nhúng tệp frame/ảnh gốc. Sau khi cài lại ứng dụng hoặc chuyển máy, cần nhập lại frame/ảnh tùy chỉnh và cấp quyền đọc chúng." to
            "Note: this file stores camera settings, frames and shutter-button images; it does not embed the original frame/image files. After reinstalling the app or moving to another device, re-import custom frames/images and grant read access.",
        "Không đúng định dạng Fujifilm Mode" to "Invalid FujiFilm Mode format",
        "Phiên bản sao lưu không được hỗ trợ" to "Backup version is not supported",
        "Thiếu dữ liệu cài đặt" to "Missing settings data",
        "Thiếu cấu hình camera" to "Missing camera settings",
        "Không ghi được cấu hình camera" to "Could not write camera settings",
        "Không ghi được danh sách nội dung" to "Could not write the content list",
        "Camera không hỗ trợ Ultra HDR; đã chuyển về JPEG thường." to "This camera does not support Ultra HDR; switched to standard JPEG.",
        "HDR đa khung đang hoạt động; máy không hỗ trợ JPEG_R nên ảnh được lưu dạng JPEG." to
            "Multi-frame HDR is active; this device does not support JPEG_R, so the photo will be saved as JPEG.",
        "Đang đồng bộ hướng và tỉ lệ khung ngắm, hãy chụp lại sau một chút." to
            "Synchronizing viewfinder orientation and aspect ratio; capture again in a moment.",
        "LunaDream đã ghép ảnh nhưng không đạt độ phân giải yêu cầu. Ảnh được giữ riêng trong Pictures/FujifilmMode/Unprocessed; không xuất như ảnh 50/108MP." to
            "LunaDream merged the photo but did not reach the requested resolution. The image is kept separately in Pictures/FujifilmMode/Unprocessed and is not exported as a 50/108MP photo.",
        "LunaDream 2.0 · ghép đa khung và phục hồi chi tiết" to "LunaDream 2.0 · multi-frame merge and detail recovery",
        "RawDream 2.0 · căn chỉnh và ghép 8 khung" to "RawDream 2.0 · aligning and merging 8 frames",
        "LunaDream 2.0 · căn chỉnh và ghép 8 khung" to "LunaDream 2.0 · aligning and merging 8 frames",
        "RawDream 2.0 đã giữ DNG gốc và ghép JPEG bằng LunaDream 2.0, nhưng chưa thể nâng kích thước JPEG an toàn." to
            "RawDream 2.0 preserved the original DNG and merged the JPEG with LunaDream 2.0, but could not safely upscale the JPEG.",
        "LunaDream fallback · " to "LunaDream fallback · ",
        "NightShotMode · ghép đa khung" to "NightShotMode · multi-frame merge",
        "NightShotMode · chụp đêm" to "NightShotMode · night capture",
        "Xử lý ảnh gốc thiết bị · ISP + tối ưu tông/chi tiết" to
            "Device-native photo processing · ISP + tone/detail optimization",
        "Xử lý ảnh gốc thiết bị chưa hoàn tất · giữ ảnh JPEG" to
            "Device-native photo processing did not finish · preserving JPEG",
        "Xử lý ảnh gốc thiết bị đã hoàn tất: tối ưu dải sáng và độ sắc nét." to
            "Device-native photo processing completed: optimized dynamic range and sharpness.",
        "Bước xử lý ảnh gốc thiết bị không hoàn tất; ảnh JPEG gốc vẫn được giữ." to
            "Device-native photo processing did not finish; the original JPEG is preserved.",
        "LunaDream 1.5 đã ghép " to "LunaDream 1.5 merged ",
        " khung hình." to " frames.",
        "RawDream 2.0 giữ DNG cảm biến gốc và ghép " to "RawDream 2.0 preserved the original sensor DNG and merged ",
        " JPEG bằng thuật toán LunaDream 2.0." to " JPEG with the LunaDream 2.0 algorithm.",
        "LunaDream 2.0 đã ghép và phục hồi chi tiết đa khung." to "LunaDream 2.0 merged multiple frames and recovered detail.",
        "LunaDream vẫn được giữ làm chế độ đã chọn." to "LunaDream remains the selected mode.",
        "engine không cung cấp nguyên nhân" to "engine did not provide a reason",
        "Không nội suy độ phân giải" to "No resolution interpolation",
        "HAL không công bố" to "Not reported by HAL",
        "Không công bố" to "Not reported",
        "NO SIGNAL\nCần cấp quyền Camera (chạm để cấp)" to "NO SIGNAL\nCamera permission required (tap to grant)",
        "Chưa có LUT nào. Hỗ trợ .cube, Canon RGB .bin và ZIP chứa nhiều LUT." to
            "No LUTs yet. Supports .cube, Canon RGB .bin and ZIP files containing multiple LUTs.",
        "ĐANG NHẬP LUT…" to "IMPORTING LUT…",
        "Tìm LUT theo tên hoặc nhóm" to "Search LUTs by name or category",
        "Hỗ trợ .cube, LUT .bin RGB 33x33x33 và ZIP chứa các định dạng này." to
            "Supports .cube, 33x33x33 RGB .bin LUTs and ZIP files containing these formats.",
        "Tệp .cube không hợp lệ hoặc không phải LUT 3D." to "The .cube file is invalid or is not a 3D LUT.",
        "Tệp .bin không phải LUT RGB 33x33x33 được hỗ trợ." to "The .bin file is not a supported 33x33x33 RGB LUT.",
        "LunaDream vẫn được giữ làm chế độ đã chọn." to "LunaDream remains the selected mode.",
        "LunaDream 2.0 đã ghép và phục hồi chi tiết đa khung." to "LunaDream 2.0 merged multiple frames and recovered detail.",
        "Không lưu được clip Live Photo; ảnh tĩnh vẫn được giữ nếu đã chụp thành công." to
            "Could not save the Live Photo clip; the still photo is preserved if capture succeeded.",
        "Astro Stack ×8 cần Manual Sensor, ISO và thời gian phơi sáng do Camera2 công bố." to
            "Astro Stack ×8 requires Manual Sensor, ISO and exposure time exposed by Camera2.",
        "Ảnh gốc trước LunaDream" to "Original photo before LunaDream",
        "Ảnh sau LunaDream" to "Photo after LunaDream",
        "ẢNH GỐC · TRƯỚC LUNADREAM" to "ORIGINAL · BEFORE LUNADREAM",
        "ẢNH ĐÃ XỬ LÝ · LUNADREAM" to "PROCESSED · LUNADREAM",
        "Sau LunaDream" to "After LunaDream",
        "ĐÓNG" to "CLOSE",
        "Đổi ảnh nút chụp" to "Change shutter button image",
        "Âm thanh màn trập của ảnh chụp." to "Photo shutter sound.",
        "Click nhẹ theo từng nấc của vòng zoom và command dial." to "Subtle clicks for each zoom-ring and command-dial detent.",
        "Tick phím, clack công tắc và tiếng xác nhận panel." to "Key ticks, switch clacks and panel confirmation sounds.",
        "Rung nhẹ khi xoay núm, gạt công tắc và nhấn nửa cò." to "Subtle haptics when turning dials, toggling switches and half-pressing the shutter.",
        "Tắt mặc định; không hoạt động ở Photo thường." to "Disabled by default; not active in standard Photo mode.",
        "Không có EXIF" to "No EXIF",
        "Không phân tích được" to "Unable to analyze",
        "Không có dữ liệu phiên LunaDream" to "No LunaDream session data",
        "Không ghi nhận" to "Not recorded",
        "LUT MANAGER" to "LUT MANAGER",
        "DELETE LUT" to "DELETE LUT",
        "PHOTO INSPECTOR" to "PHOTO INSPECTOR",

        "Import ảnh cho nút chụp" to "Import shutter button image",
        "Đổi ảnh nút chụp" to "Change shutter button image",
        "Âm thanh màn trập của ảnh chụp." to "Photo shutter sound.",
        "Click nhẹ theo từng nấc của vòng zoom và command dial." to "Subtle clicks for each zoom-ring and command-dial detent.",
        "Tick phím, clack công tắc và tiếng xác nhận panel." to "Key ticks, switch clacks and panel confirmation sounds.",
        "Rung nhẹ khi xoay núm, gạt công tắc và nhấn nửa cò." to "Subtle haptics when turning dials, toggling switches and half-pressing the shutter.",
        "Phơi sáng 1–8 giây mỗi khung trên máy hỗ trợ; nhấn chụp lần nữa để dừng rồi ghép vệt sao" to
            "1–8 second exposure per frame on supported devices; press the shutter again to stop and merge star trails",
        "Tắt mặc định; không hoạt động ở Photo thường." to "Off by default; inactive in standard Photo mode.",
        "QUÉT LẠI PHẦN CỨNG CAMERA" to "RESCAN CAMERA HARDWARE",
        "Physical ID được công bố" to "Reported physical IDs",
        "Tổng mục đã đọc" to "Total entries read",
        "Nhóm camera chạy đồng thời" to "Concurrent camera groups",
        "ID không đọc được" to "Unreadable IDs",
        "Chưa tìm thấy camera" to "No cameras found",
        "Camera ID OEM không công khai" to "Non-public OEM Camera IDs",
        "Giới hạn truy cập của Android" to "Android access limits",
        "Còn " to "Remaining "

    )

    private val additionalEnglishToVietnamese = mapOf(
        "Checking exposure support" to "Đang kiểm tra hỗ trợ phơi sáng",
        "Unknown reason" to "Không rõ nguyên nhân",
        "unknown reason" to "không rõ nguyên nhân",
        "unknown error" to "không rõ lỗi",
        "TURN OFF MILKY WAY STACK ×8" to "TẮT MILKY WAY STACK ×8",
        "APPLY RECOMMENDATION + ENABLE ASTRO STACK ×8" to "ÁP DỤNG GỢI Ý + BẬT ASTRO STACK ×8",
        "ULTRAWIDE" to "SIÊU RỘNG",
        "MAIN CAMERA" to "CAM THƯỜNG",
        "Most recent photo" to "Ảnh gần nhất",
        "Custom shutter button" to "Nút chụp tùy chỉnh",
        "Shutter sound" to "Âm thanh màn trập",
        "Nhật ký lỗi FujiFilm Mode" to "FujiFilm Mode crash log",
        "TỶ LỆ KHUNG HÌNH" to "ASPECT RATIO",
        "ẢNH ĐỘNG" to "LIVE PHOTO",
        "MENU/OK  >  CÀI ĐẶT" to "MENU/OK  >  SETTINGS",
        "XEM TRƯỚC OSD · PRO" to "OSD PREVIEW · PRO",
        "XEM TRƯỚC OSD · PHOTO" to "OSD PREVIEW · PHOTO",
        "FujiFilm Mode crash log" to "Nhật ký lỗi FujiFilm Mode",
        "ASPECT RATIO" to "TỶ LỆ KHUNG HÌNH",
        "LIVE PHOTO" to "ẢNH ĐỘNG",
        "MENU/OK  >  SETTINGS" to "MENU/OK  >  CÀI ĐẶT",
        "OSD PREVIEW · PRO" to "XEM TRƯỚC OSD · PRO",
        "OSD PREVIEW · PHOTO" to "XEM TRƯỚC OSD · PHOTO",
        "Highlight" to "Vùng sáng",
        "Shadow" to "Vùng tối",
        "Green ↔ Magenta" to "Xanh lá ↔ Đỏ tươi"
    )

    private data class Prefix(val source: String, val translated: String)

    private val vietnamesePrefixes = listOf(
        Prefix("Không thể khởi tạo HDR hoặc camera: ", "Could not initialize HDR or camera: "),
        Prefix("Không mở được camera: ", "Could not open camera: "),
        Prefix("Không thể khởi tạo camera. Giao diện vẫn được giữ mở: ", "Could not initialize the camera. The UI will remain open: "),
        Prefix("Không thể khởi tạo dịch vụ camera: ", "Could not initialize the camera service: "),
        Prefix("Star Trails đang ghi: ", "Star Trails recording: "),
        Prefix("Đã lưu Star Trails từ ", "Saved Star Trails from "),
        Prefix("Star Trails gặp lỗi: ", "Star Trails error: "),
        Prefix("Không thể đặt khoảng nét ", "Could not set focus position "),
        Prefix("Import LUT thất bại: ", "LUT import failed: "),
        Prefix("Lỗi chụp ảnh: ", "Capture error: "),
        Prefix("LunaDream chưa hoàn tất · ", "LunaDream did not complete · "),
        Prefix("LunaDream dự phòng · ", "LunaDream fallback · "),
        Prefix("Live Photo không khởi động được: ", "Live Photo could not start: "),
        Prefix("Đã nhập ", "Imported "),
        Prefix("NIGHTSHOT · BẬT · ", "NIGHTSHOT · ON · "),
        Prefix("NIGHTSHOT · TẮT", "NIGHTSHOT · OFF")
    )

    private fun translatePrefix(value: String, prefixes: List<Prefix>): String? {
        val prefix = prefixes.firstOrNull { value.startsWith(it.source) } ?: return null
        return prefix.translated + value.removePrefix(prefix.source)
    }

    fun toEnglish(value: String): String? {
        vietnameseToEnglish[value]?.let { return it }
        additionalVietnameseToEnglish[value]?.let { return it }
        Regex("^Đã nhập (.+) LUT\\. Vào Content Manager để Add LUT vào bộ lọc\\.$").matchEntire(value)?.let {
            return "Imported ${it.groupValues[1]} LUT. Open Content Manager to add the LUT to filters."
        }
        Regex("^Đã nhập (.+) LUT\\. Chọn Add để bật từng bộ lọc\\.$").matchEntire(value)?.let {
            return "Imported ${it.groupValues[1]} LUT. Select Add to enable each filter."
        }
        Regex("^(\\d+) bộ lọc$").matchEntire(value)?.let {
            return it.groupValues[1] + " filters"
        }
        Regex("^Còn (\\d+) key khác\\.$").matchEntire(value)?.let {
            return it.groupValues[1] + " more keys."
        }
        Regex("^Đã lưu Camera Preset (\\d+)$").matchEntire(value)?.let {
            return "Saved Camera Preset " + it.groupValues[1]
        }
        Regex("^Preset (\\d+) chưa được lưu$").matchEntire(value)?.let {
            return "Preset " + it.groupValues[1] + " has not been saved."
        }
        Regex("^Đã nạp Camera Preset (\\d+) · cấu hình chụp đã khôi phục$").matchEntire(value)?.let {
            return "Loaded Camera Preset " + it.groupValues[1] + " · capture settings restored."
        }
        Regex("^Đã lưu Film Recipe (\\d+)$").matchEntire(value)?.let {
            return "Saved Film Recipe " + it.groupValues[1]
        }
        Regex("^Film Recipe (\\d+) chưa được lưu$").matchEntire(value)?.let {
            return "Film Recipe " + it.groupValues[1] + " has not been saved."
        }
        Regex("^Đã nạp Film Recipe (\\d+)$").matchEntire(value)?.let {
            return "Loaded Film Recipe " + it.groupValues[1]
        }
        Regex("^Star Trails đang ghi: (.+)\\. Nhấn nút chụp lần nữa để dừng và ghép vệt sao\\.$").matchEntire(value)?.let {
            return "Star Trails recording: ${it.groupValues[1]}. Press the shutter again to stop after the current frame and merge the trails."
        }
        Regex("^Đã lưu Star Trails từ (\\d+) khung hình(?: \\(cần kiểm tra lại metadata\\)\\.)?$").matchEntire(value)?.let {
            return "Saved Star Trails from ${it.groupValues[1]} frames."
        }
        Regex("^Multi-frame: chụp khung (\\d+)/5$").matchEntire(value)?.let {
            return "Multi-frame: capturing frame ${it.groupValues[1]}/5"
        }
        Regex("^NightShotMode: thu khung (\\d+)/(\\d+) · hàng đợi 2$").matchEntire(value)?.let {
            return "NightShotMode: capturing frame ${it.groupValues[1]}/${it.groupValues[2]} · queue 2"
        }
        Regex("^(LunaDream 1\\.5|RawDream 2\\.0|LunaDream 2\\.0|NightShotMode Milky Way): thu khung (\\d+)/(\\d+)$").matchEntire(value)?.let {
            return "${it.groupValues[1]}: capturing frame ${it.groupValues[2]}/${it.groupValues[3]}"
        }
        Regex("^LunaDream 1\\.5 · đang chụp khung đầu tiên$").matchEntire(value)?.let {
            return "LunaDream 1.5 · capturing first frame"
        }
        Regex("^RawDream 2\\.0 · đang chụp khung đầu tiên$").matchEntire(value)?.let {
            return "RawDream 2.0 · capturing first frame"
        }
        Regex("^LunaDream 2\\.0 · đang chụp khung đầu tiên$").matchEntire(value)?.let {
            return "LunaDream 2.0 · capturing first frame"
        }
        Regex("^NightShotMode · đang chụp khung đầu tiên$").matchEntire(value)?.let {
            return "NightShotMode · capturing first frame"
        }
        Regex("^LunaDream 2\\.0 không hoàn tất; đã dùng thuật toán dự phòng 1\\.5\\. Lý do:$").matchEntire(value)?.let {
            return "LunaDream 2.0 did not finish; the 1.5 fallback algorithm was used. Reason:"
        }
        Regex("^RawDream 2\\.0 · JPEG fallback LunaDream 1\\.5 · reason: (.*)$").matchEntire(value)?.let {
            val reason = toEnglish(it.groupValues[1]) ?: it.groupValues[1]
            return "RawDream 2.0 · JPEG fallback LunaDream 1.5 · reason: $reason"
        }
        Regex("^LunaDream 2\\.0 · fallback LunaDream 1\\.5 · reason: (.*)$").matchEntire(value)?.let {
            val reason = toEnglish(it.groupValues[1]) ?: it.groupValues[1]
            return "LunaDream 2.0 · fallback LunaDream 1.5 · reason: $reason"
        }
        Regex("^LunaDream dự phòng · (.*)$").matchEntire(value)?.let {
            val reason = toEnglish(it.groupValues[1]) ?: it.groupValues[1]
            return "LunaDream fallback · $reason"
        }
        Regex("^(HDR Bracketing|Focus Bracketing): đã lưu (\\d+)/(\\d+) khung\\. (.*)$").matchEntire(value)?.let {
            val mode = it.groupValues[1]
            val result = toEnglish(it.groupValues[4]) ?: it.groupValues[4]
            return "$mode: saved ${it.groupValues[2]}/${it.groupValues[3]} frames. $result"
        }
        Regex("^Đã lưu Live Photo: ảnh (.+)\\.jpg và clip (.+)\\.mp4 trong Movies/FujifilmMode/LivePhoto\\.$").matchEntire(value)?.let {
            return "Saved Live Photo: photo ${it.groupValues[1]}.jpg and clip ${it.groupValues[2]}.mp4 in Movies/FujifilmMode/LivePhoto."
        }
        Regex("^Live Photo không khởi động được: (.*)\\. Ảnh chưa được chụp\\.$").matchEntire(value)?.let {
            val cause = toEnglish(it.groupValues[1]) ?: it.groupValues[1]
            return "Live Photo could not start: $cause. No photo was captured."
        }
        Regex("^Burst hoàn tất · đã lưu (\\d+)/25 ảnh JPEG gốc\\. Tốc độ phụ thuộc camera và bộ nhớ\\.$").matchEntire(value)?.let {
            return "Burst complete · saved ${it.groupValues[1]}/25 original JPEG photos. Speed depends on the camera and storage."
        }
        Regex("^Đã áp dụng ISO (\\d+) · (.+) · WB (\\d+)K\\. Chụp 8 ảnh để căn chỉnh và giảm nhiễu\\.$").matchEntire(value)?.let {
            return "Applied ISO ${it.groupValues[1]} · ${it.groupValues[2]} · WB ${it.groupValues[3]}K. Capture 8 photos for alignment and noise reduction."
        }
        Regex("^(.+?) · WB gợi ý (.+)$").matchEntire(value)?.let {
            return "${it.groupValues[1]} · suggested WB ${it.groupValues[2]}"
        }
        Regex("^Tông màu (\\d+) K$").matchEntire(value)?.let {
            return "Color temperature ${it.groupValues[1]} K"
        }
        Regex("^(\\d+) ảnh$").matchEntire(value)?.let {
            return "${it.groupValues[1]} photos"
        }
        Regex("^(\\d+) giây$").matchEntire(value)?.let {
            return "${it.groupValues[1]} seconds"
        }
        Regex("^Gói LUT vượt quá (\\d+) bộ lọc\\.$").matchEntire(value)?.let {
            return "The LUT package exceeds the limit of ${it.groupValues[1]} filters."
        }
        Regex("^Hiển thị (\\d+) / (\\d+) LUT khớp · Tổng (\\d+)$").matchEntire(value)?.let {
            return "Showing ${it.groupValues[1]} / ${it.groupValues[2]} matching LUTs · Total ${it.groupValues[3]}"
        }
        Regex("""^Bạn có chắc chắn muốn xóa LUT "(.+)"\? LUT này cũng sẽ bị gỡ khỏi mục Bộ lọc\.$""").matchEntire(value)?.let {
            return """Are you sure you want to delete LUT "${it.groupValues[1]}"? This LUT will also be removed from Filters."""
        }
        Regex("^Đang ghép vệt sao · (\\d+) khung hình$").matchEntire(value)?.let {
            return "Merging Star Trails · ${it.groupValues[1]} frames"
        }
        Regex("^Không thể đặt khoảng nét (\\d+); dừng Focus Bracketing\\.$").matchEntire(value)?.let {
            return "Could not set focus position ${it.groupValues[1]}; stopping Focus Bracketing."
        }
        Regex("^Đã ghép ảnh tổng hợp và vẫn giữ (\\d+) ảnh gốc\\.$").matchEntire(value)?.let {
            return "Merged the composite photo and preserved ${it.groupValues[1]} original photos."
        }
        Regex("^Chỉ thu được (\\d+)/(\\d+) khung hình hợp lệ$").matchEntire(value)?.let {
            return "Captured only ${it.groupValues[1]}/${it.groupValues[2]} valid frames"
        }
        Regex("^LunaDream 1\\.5: căn chỉnh và khử nhiễu (\\d+)/5 khung$").matchEntire(value)?.let {
            return "LunaDream 1.5: aligning and denoising ${it.groupValues[1]}/5 frames"
        }
        Regex("^(RawDream 2\\.0|LunaDream 2\\.0): căn chỉnh và khử nhiễu (\\d+)/8 khung$").matchEntire(value)?.let {
            return "${it.groupValues[1]}: aligning and denoising ${it.groupValues[2]}/8 frames"
        }
        Regex("^NightShotMode Milky Way: căn chỉnh và gộp (\\d+)/8 khung$").matchEntire(value)?.let {
            return "NightShotMode Milky Way: aligning and merging ${it.groupValues[1]}/8 frames"
        }
        Regex("^NightShotMode: căn chỉnh và gộp (\\d+) khung$").matchEntire(value)?.let {
            return "NightShotMode: aligning and merging ${it.groupValues[1]} frames"
        }
        Regex("^Multi-frame: căn chỉnh và gộp (\\d+)/5 khung$").matchEntire(value)?.let {
            return "Multi-frame: aligning and merging ${it.groupValues[1]}/5 frames"
        }
        Regex("^LunaDream 1\\.5 · căn chỉnh và ghép (\\d+)/5 khung$").matchEntire(value)?.let {
            return "LunaDream 1.5 · aligning and merging ${it.groupValues[1]}/5 frames"
        }
        Regex("^LunaDream đang tối ưu kích thước (\\d+)MP$").matchEntire(value)?.let {
            return "LunaDream is optimizing output size to ${it.groupValues[1]}MP"
        }
        Regex("^Đã lưu LunaDream 1\\.5 · (\\d+)MP theo giới hạn bộ nhớ\\.$").matchEntire(value)?.let {
            return "Saved LunaDream 1.5 · ${it.groupValues[1]}MP within memory limits."
        }
        Regex("^Đã lưu RawDream 2\\.0 · DNG gốc \\+ JPEG LunaDream 2\\.0 (\\d+)MP theo giới hạn bộ nhớ\\.$").matchEntire(value)?.let {
            return "Saved RawDream 2.0 · original DNG + LunaDream 2.0 JPEG ${it.groupValues[1]}MP within memory limits."
        }
        Regex("^Đã lưu LunaDream 2\\.0 · (\\d+)MP theo giới hạn bộ nhớ\\.$").matchEntire(value)?.let {
            return "Saved LunaDream 2.0 · ${it.groupValues[1]}MP within memory limits."
        }
        Regex("^Đã lưu NightShotMode · (\\d+) khung thực tế\\.$").matchEntire(value)?.let {
            return "Saved NightShotMode · ${it.groupValues[1]} captured frames."
        }
        Regex("^LunaDream không hoàn tất: (.*)\\. Đã lưu JPEG gốc làm ảnh dự phòng\\.$").matchEntire(value)?.let {
            val reason = toEnglish(it.groupValues[1]) ?: it.groupValues[1]
            return "LunaDream did not finish: $reason. The original JPEG was saved as a fallback."
        }
        Regex("^Phiên bản (.+) · Bản dựng (.+)$").matchEntire(value)?.let {
            return "Version ${it.groupValues[1]} · Build ${it.groupValues[2]}"
        }
        Regex("^Công thức (\\d+) · (.+)$").matchEntire(value)?.let {
            return "Recipe ${it.groupValues[1]} · ${it.groupValues[2]}"
        }
        Regex("^Star Trails gặp lỗi: (.*)$").matchEntire(value)?.let {
            val reason = toEnglish(it.groupValues[1]) ?: it.groupValues[1]
            return "Star Trails error: $reason"
        }
        Regex("^Lỗi chụp ảnh: (.*)$").matchEntire(value)?.let {
            return "Capture error: ${it.groupValues[1]}"
        }
        Regex("^Live Photo không khởi động được: (.*)$").matchEntire(value)?.let {
            return "Live Photo could not start: ${it.groupValues[1]}"
        }
        Regex("^LunaDream 1\\.5 dự phòng · (\\d+)%$").matchEntire(value)?.let {
            return "LunaDream 1.5 fallback · ${it.groupValues[1]}%"
        }
        Regex("^Ảnh nguồn trong khi kết xuất (.+)$").matchEntire(value)?.let {
            return "Preview source while rendering ${it.groupValues[1]}"
        }
        Regex("^Xem trước (.+)$").matchEntire(value)?.let {
            return "${it.groupValues[1]} preview"
        }
        Regex("^Ảnh gốc trước LunaDream$").matchEntire(value)?.let { return "Original photo before LunaDream" }
        Regex("^LunaDream không hoàn tất: (.*)\\. JPEG gốc vẫn được giữ nguyên\\.$").matchEntire(value)?.let {
            val reason = toEnglish(it.groupValues[1]) ?: it.groupValues[1]
            return "LunaDream did not complete: $reason. The original JPEG was preserved."
        }
        Regex("^Ảnh sau LunaDream$").matchEntire(value)?.let { return "Photo after LunaDream" }
        translatePrefix(value, vietnamesePrefixes)?.let { return it }
        return null
    }

    fun toVietnamese(value: String): String? {
        englishToVietnamese[value]?.let { return it }
        additionalEnglishToVietnamese[value]?.let { return it }
        additionalVietnameseToEnglish.entries.firstOrNull { it.value == value }?.let { return it.key }
        vietnameseToEnglish.entries.firstOrNull { it.value == value }?.let { return it.key }
        Regex("^Star Trails recording: (.+)\\. Press the shutter again to stop after the current frame and merge the trails\\.$").matchEntire(value)?.let {
            return "Star Trails đang ghi: ${it.groupValues[1]}. Nhấn nút chụp lần nữa để dừng và ghép vệt sao."
        }
        Regex("^Saved Star Trails from (\\d+) frames\\.$").matchEntire(value)?.let {
            return "Đã lưu Star Trails từ ${it.groupValues[1]} khung hình"
        }
        Regex("^Multi-frame: capturing frame (\\d+)/5$").matchEntire(value)?.let {
            return "Multi-frame: chụp khung ${it.groupValues[1]}/5"
        }
        Regex("^NightShotMode: capturing frame (\\d+)/(\\d+) · queue 2$").matchEntire(value)?.let {
            return "NightShotMode: thu khung ${it.groupValues[1]}/${it.groupValues[2]} · hàng đợi 2"
        }
        Regex("^(LunaDream 1\\.5|RawDream 2\\.0|LunaDream 2\\.0|NightShotMode Milky Way): capturing frame (\\d+)/(\\d+)$").matchEntire(value)?.let {
            return "${it.groupValues[1]}: thu khung ${it.groupValues[2]}/${it.groupValues[3]}"
        }
        Regex("^LunaDream 1\\.5 · capturing first frame$").matchEntire(value)?.let {
            return "LunaDream 1.5 · đang chụp khung đầu tiên"
        }
        Regex("^RawDream 2\\.0 · capturing first frame$").matchEntire(value)?.let {
            return "RawDream 2.0 · đang chụp khung đầu tiên"
        }
        Regex("^LunaDream 2\\.0 · capturing first frame$").matchEntire(value)?.let {
            return "LunaDream 2.0 · đang chụp khung đầu tiên"
        }
        Regex("^NightShotMode · capturing first frame$").matchEntire(value)?.let {
            return "NightShotMode · đang chụp khung đầu tiên"
        }
        Regex("^LunaDream 2\\.0 did not finish; the 1\\.5 fallback algorithm was used\\. Reason:$").matchEntire(value)?.let {
            return "LunaDream 2.0 không hoàn tất; đã dùng thuật toán dự phòng 1.5. Lý do:"
        }
        Regex("^RawDream 2\\.0 · JPEG fallback LunaDream 1\\.5 · reason: (.*)$").matchEntire(value)?.let {
            val reason = toVietnamese(it.groupValues[1]) ?: it.groupValues[1]
            return "RawDream 2.0 · JPEG fallback LunaDream 1.5 · lý do: $reason"
        }
        Regex("^LunaDream 2\\.0 · fallback LunaDream 1\\.5 · reason: (.*)$").matchEntire(value)?.let {
            val reason = toVietnamese(it.groupValues[1]) ?: it.groupValues[1]
            return "LunaDream 2.0 · fallback LunaDream 1.5 · lý do: $reason"
        }
        Regex("^LunaDream fallback · (.*)$").matchEntire(value)?.let {
            val reason = toVietnamese(it.groupValues[1]) ?: it.groupValues[1]
            return "LunaDream dự phòng · $reason"
        }
        Regex("^(HDR Bracketing|Focus Bracketing): saved (\\d+)/(\\d+) frames\\. (.*)$").matchEntire(value)?.let {
            val result = toVietnamese(it.groupValues[4]) ?: it.groupValues[4]
            return "${it.groupValues[1]}: đã lưu ${it.groupValues[2]}/${it.groupValues[3]} khung. $result"
        }
        Regex("^Saved Live Photo: photo (.+)\\.jpg and clip (.+)\\.mp4 in Movies/FujifilmMode/LivePhoto\\.$").matchEntire(value)?.let {
            return "Đã lưu Live Photo: ảnh ${it.groupValues[1]}.jpg và clip ${it.groupValues[2]}.mp4 trong Movies/FujifilmMode/LivePhoto."
        }
        Regex("^Live Photo could not start: (.*)\\. No photo was captured\\.$").matchEntire(value)?.let {
            val cause = toVietnamese(it.groupValues[1]) ?: it.groupValues[1]
            return "Live Photo không khởi động được: $cause. Ảnh chưa được chụp."
        }
        Regex("^Burst complete · saved (\\d+)/25 original JPEG photos\\. Speed depends on the camera and storage\\.$").matchEntire(value)?.let {
            return "Burst hoàn tất · đã lưu ${it.groupValues[1]}/25 ảnh JPEG gốc. Tốc độ phụ thuộc camera và bộ nhớ."
        }
        Regex("^Applied ISO (\\d+) · (.+) · WB (\\d+)K\\. Capture 8 photos for alignment and noise reduction\\.$").matchEntire(value)?.let {
            return "Đã áp dụng ISO ${it.groupValues[1]} · ${it.groupValues[2]} · WB ${it.groupValues[3]}K. Chụp 8 ảnh để căn chỉnh và giảm nhiễu."
        }
        Regex("^(.+?) · suggested WB (.+)$").matchEntire(value)?.let {
            return "${it.groupValues[1]} · WB gợi ý ${it.groupValues[2]}"
        }
        Regex("^Color temperature (\\d+) K$").matchEntire(value)?.let {
            return "Tông màu ${it.groupValues[1]} K"
        }
        Regex("^(\\d+) photos$").matchEntire(value)?.let {
            return "${it.groupValues[1]} ảnh"
        }
        Regex("^(\\d+) seconds$").matchEntire(value)?.let {
            return "${it.groupValues[1]} giây"
        }
        Regex("^The LUT package exceeds the limit of (\\d+) filters\\.$").matchEntire(value)?.let {
            return "Gói LUT vượt quá ${it.groupValues[1]} bộ lọc."
        }
        Regex("^Showing (\\d+) / (\\d+) matching LUTs · Total (\\d+)$").matchEntire(value)?.let {
            return "Hiển thị ${it.groupValues[1]} / ${it.groupValues[2]} LUT khớp · Tổng ${it.groupValues[3]}"
        }
        Regex("""^Are you sure you want to delete LUT "(.+)"\? This LUT will also be removed from Filters\.$""").matchEntire(value)?.let {
            return """Bạn có chắc chắn muốn xóa LUT "${it.groupValues[1]}"? LUT này cũng sẽ bị gỡ khỏi mục Bộ lọc."""
        }
        Regex("^Imported (.+) LUT\\. Open Content Manager to add the LUT to filters\\.$").matchEntire(value)?.let {
            return "Đã nhập ${it.groupValues[1]} LUT. Vào Content Manager để Add LUT vào bộ lọc."
        }
        Regex("^Imported (.+) LUT\\. Select Add to enable each filter\\.$").matchEntire(value)?.let {
            return "Đã nhập ${it.groupValues[1]} LUT. Chọn Add để bật từng bộ lọc."
        }
        Regex("^Version (.+) · Build (.+)$").matchEntire(value)?.let {
            return "Phiên bản ${it.groupValues[1]} · Bản dựng ${it.groupValues[2]}"
        }
        Regex("^Recipe (\\d+) · (.+)$").matchEntire(value)?.let {
            return "Công thức ${it.groupValues[1]} · ${it.groupValues[2]}"
        }
        listOf(
            "Highlight · " to "Vùng sáng · ",
            "Shadow · " to "Vùng tối · ",
            "Color Temperature · " to "Nhiệt độ màu · ",
            "Color Tone · " to "Tông màu · ",
            "Saturation · " to "Độ bão hòa · "
        ).firstOrNull { value.startsWith(it.first) }?.let {
            return it.second + value.removePrefix(it.first)
        }
        Regex("^Merging Star Trails · (\\d+) frames$").matchEntire(value)?.let {
            return "Đang ghép vệt sao · ${it.groupValues[1]} khung hình"
        }
        Regex("^Could not set focus position (\\d+); stopping Focus Bracketing\\.$").matchEntire(value)?.let {
            return "Không thể đặt khoảng nét ${it.groupValues[1]}; dừng Focus Bracketing."
        }
        Regex("^Merged the composite photo and preserved (\\d+) original photos\\.$").matchEntire(value)?.let {
            return "Đã ghép ảnh tổng hợp và vẫn giữ ${it.groupValues[1]} ảnh gốc."
        }
        Regex("^Captured only (\\d+)/(\\d+) valid frames$").matchEntire(value)?.let {
            return "Chỉ thu được ${it.groupValues[1]}/${it.groupValues[2]} khung hình hợp lệ"
        }
        Regex("^LunaDream 1\\.5: aligning and denoising (\\d+)/5 frames$").matchEntire(value)?.let {
            return "LunaDream 1.5: căn chỉnh và khử nhiễu ${it.groupValues[1]}/5 khung"
        }
        Regex("^(RawDream 2\\.0|LunaDream 2\\.0): aligning and denoising (\\d+)/8 frames$").matchEntire(value)?.let {
            return "${it.groupValues[1]}: căn chỉnh và khử nhiễu ${it.groupValues[2]}/8 khung"
        }
        Regex("^NightShotMode Milky Way: aligning and merging (\\d+)/8 frames$").matchEntire(value)?.let {
            return "NightShotMode Milky Way: căn chỉnh và gộp ${it.groupValues[1]}/8 khung"
        }
        Regex("^NightShotMode: aligning and merging (\\d+) frames$").matchEntire(value)?.let {
            return "NightShotMode: căn chỉnh và gộp ${it.groupValues[1]} khung"
        }
        Regex("^Multi-frame: aligning and merging (\\d+)/5 frames$").matchEntire(value)?.let {
            return "Multi-frame: căn chỉnh và gộp ${it.groupValues[1]}/5 khung"
        }
        Regex("^LunaDream 1\\.5 · aligning and merging (\\d+)/5 frames$").matchEntire(value)?.let {
            return "LunaDream 1.5 · căn chỉnh và ghép ${it.groupValues[1]}/5 khung"
        }
        Regex("^LunaDream is optimizing output size to (\\d+)MP$").matchEntire(value)?.let {
            return "LunaDream đang tối ưu kích thước ${it.groupValues[1]}MP"
        }
        Regex("^Saved LunaDream 1\\.5 · (\\d+)MP within memory limits\\.$").matchEntire(value)?.let {
            return "Đã lưu LunaDream 1.5 · ${it.groupValues[1]}MP theo giới hạn bộ nhớ."
        }
        Regex("^Saved RawDream 2\\.0 · original DNG \\+ LunaDream 2\\.0 JPEG (\\d+)MP within memory limits\\.$").matchEntire(value)?.let {
            return "Đã lưu RawDream 2.0 · DNG gốc + JPEG LunaDream 2.0 ${it.groupValues[1]}MP theo giới hạn bộ nhớ."
        }
        Regex("^Saved LunaDream 2\\.0 · (\\d+)MP within memory limits\\.$").matchEntire(value)?.let {
            return "Đã lưu LunaDream 2.0 · ${it.groupValues[1]}MP theo giới hạn bộ nhớ."
        }
        Regex("^Saved NightShotMode · (\\d+) captured frames\\.$").matchEntire(value)?.let {
            return "Đã lưu NightShotMode · ${it.groupValues[1]} khung thực tế."
        }
        Regex("^LunaDream did not finish: (.*)\\. The original JPEG was saved as a fallback\\.$").matchEntire(value)?.let {
            return "LunaDream không hoàn tất: ${it.groupValues[1]}. Đã lưu JPEG gốc làm ảnh dự phòng."
        }
        val reversePrefixes = vietnamesePrefixes.map { Prefix(it.translated, it.source) }
        translatePrefix(value, reversePrefixes)?.let { return it }
        Regex("^(\\d+) filters$").matchEntire(value)?.let {
            return it.groupValues[1] + " bộ lọc"
        }
        Regex("^Loaded Camera Preset (\\d+) · capture settings restored\\.$").matchEntire(value)?.let {
            return "Đã nạp Camera Preset ${it.groupValues[1]} · cấu hình chụp đã khôi phục"
        }
        Regex("^Saved Camera Preset (\\d+)$").matchEntire(value)?.let {
            return "Đã lưu Camera Preset ${it.groupValues[1]}"
        }
        Regex("^Preset (\\d+) has not been saved\\.$").matchEntire(value)?.let {
            return "Preset ${it.groupValues[1]} chưa được lưu"
        }
        Regex("^Saved Film Recipe (\\d+)$").matchEntire(value)?.let {
            return "Đã lưu Film Recipe ${it.groupValues[1]}"
        }
        Regex("^Film Recipe (\\d+) has not been saved\\.$").matchEntire(value)?.let {
            return "Film Recipe ${it.groupValues[1]} chưa được lưu"
        }
        Regex("^Loaded Film Recipe (\\d+)$").matchEntire(value)?.let {
            return "Đã nạp Film Recipe ${it.groupValues[1]}"
        }
        Regex("^(\\d+) more keys\\.$").matchEntire(value)?.let {
            return "Còn " + it.groupValues[1] + " key khác."
        }
        Regex("^LunaDream 1\\.5 fallback · (\\d+)%$").matchEntire(value)?.let {
            return "LunaDream 1.5 dự phòng · ${it.groupValues[1]}%"
        }
        Regex("^LunaDream fallback · (.*)$").matchEntire(value)?.let {
            val reason = toVietnamese(it.groupValues[1]) ?: it.groupValues[1]
            return "LunaDream dự phòng · $reason"
        }
        Regex("^Preview source while rendering (.+)$").matchEntire(value)?.let {
            return "Ảnh nguồn trong khi kết xuất ${it.groupValues[1]}"
        }
        Regex("^(.+) preview$").matchEntire(value)?.let {
            return "Xem trước ${it.groupValues[1]}"
        }
        Regex("^Original photo before LunaDream$").matchEntire(value)?.let {
            return "Ảnh gốc trước LunaDream"
        }
        Regex("^LunaDream did not complete: (.*)\\. The original JPEG was preserved\\.$").matchEntire(value)?.let {
            val reason = toVietnamese(it.groupValues[1]) ?: it.groupValues[1]
            return "LunaDream không hoàn tất: $reason. JPEG gốc vẫn được giữ nguyên."
        }
        Regex("^Photo after LunaDream$").matchEntire(value)?.let {
            return "Ảnh sau LunaDream"
        }
        return null
    }
}

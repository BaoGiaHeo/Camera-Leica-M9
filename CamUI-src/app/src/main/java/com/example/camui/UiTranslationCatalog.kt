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
        "Sao lưu camera trước/sau, tỷ lệ khung hình, zoom, chế độ Pro/Photo, flash, hẹn giờ, HDR, UHD, chống rung, AI và các tùy chọn đã bật." to
            "Back up front/rear camera selection, aspect ratio, zoom, Pro/Photo mode, flash, timer, HDR, UHD, stabilization, AI and enabled options.",
        "Night capture and Star Trails use separate processing pipelines" to
            "Chụp đêm và Star Trails sử dụng các pipeline xử lý riêng."
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
        "Camera Presets" to "Thiết lập camera",
        "Film Simulation Customizer" to "Tùy chỉnh mô phỏng phim",
        "Saved Film Recipes" to "Công thức mô phỏng phim đã lưu",
        "Night capture and Star Trails use separate processing pipelines" to
            "Chụp đêm và Star Trails dùng các pipeline xử lý riêng.",
        "ISO Auto" to "ISO tự động",
        "Shutter Auto" to "Màn trập tự động",
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
            "Back up front/rear camera selection, aspect ratio, zoom, Pro/Photo mode, flash, timer, HDR, UHD, stabilization, AI and enabled options."
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
        Regex("^Burst hoàn tất · đã lưu (\\d+)/25 ảnh JPEG gốc\\. Tốc độ phụ thuộc camera và bộ nhớ\\.$").matchEntire(value)?.let {
            return "Burst complete · saved ${it.groupValues[1]}/25 original JPEG photos. Speed depends on the camera and storage."
        }
        Regex("^Đã áp dụng ISO (\\d+) · (.+) · WB (\\d+)K\\. Chụp 8 ảnh để căn chỉnh và giảm nhiễu\\.$").matchEntire(value)?.let {
            return "Applied ISO ${it.groupValues[1]} · ${it.groupValues[2]} · WB ${it.groupValues[3]}K. Capture 8 photos for alignment and noise reduction."
        }
        Regex("^Tông màu (\\d+) K$").matchEntire(value)?.let {
            return "Color temperature ${it.groupValues[1]} K"
        }
        Regex("^(\\d+) ảnh$").matchEntire(value)?.let {
            return "${it.groupValues[1]} photos"
        }
        Regex("^Hiển thị (\\d+) / (\\d+) LUT khớp · Tổng (\\d+)$").matchEntire(value)?.let {
            return "Showing ${it.groupValues[1]} / ${it.groupValues[2]} matching LUTs · Total ${it.groupValues[3]}"
        }
        Regex("^Bạn có chắc chắn muốn xóa LUT \\"(.+)\\"\\? LUT này cũng sẽ bị gỡ khỏi mục Bộ lọc\\.$").matchEntire(value)?.let {
            return "Are you sure you want to delete LUT \\"${it.groupValues[1]}\\"? This LUT will also be removed from Filters."
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
        Regex("^Burst complete · saved (\\d+)/25 original JPEG photos\\. Speed depends on the camera and storage\\.$").matchEntire(value)?.let {
            return "Burst hoàn tất · đã lưu ${it.groupValues[1]}/25 ảnh JPEG gốc. Tốc độ phụ thuộc camera và bộ nhớ."
        }
        Regex("^Applied ISO (\\d+) · (.+) · WB (\\d+)K\\. Capture 8 photos for alignment and noise reduction\\.$").matchEntire(value)?.let {
            return "Đã áp dụng ISO ${it.groupValues[1]} · ${it.groupValues[2]} · WB ${it.groupValues[3]}K. Chụp 8 ảnh để căn chỉnh và giảm nhiễu."
        }
        Regex("^Color temperature (\\d+) K$").matchEntire(value)?.let {
            return "Tông màu ${it.groupValues[1]} K"
        }
        Regex("^(\\d+) photos$").matchEntire(value)?.let {
            return "${it.groupValues[1]} ảnh"
        }
        Regex("^Showing (\\d+) / (\\d+) matching LUTs · Total (\\d+)$").matchEntire(value)?.let {
            return "Hiển thị ${it.groupValues[1]} / ${it.groupValues[2]} LUT khớp · Tổng ${it.groupValues[3]}"
        }
        Regex("^Are you sure you want to delete LUT \\"(.+)\\"\\? This LUT will also be removed from Filters\\.$").matchEntire(value)?.let {
            return "Bạn có chắc chắn muốn xóa LUT \\"${it.groupValues[1]}\\"? LUT này cũng sẽ bị gỡ khỏi mục Bộ lọc."
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
        return null
    }
}
